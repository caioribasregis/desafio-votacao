package com.caioribasregis.desafiovotacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.caioribasregis.desafiovotacao.client.StatusVotoAssociado;
import com.caioribasregis.desafiovotacao.client.ValidadorCpfClient;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;

/**
 * Teste de desempenho (bônus 2): mede a API com centenas de milhares de votos já gravados.
 *
 * <p>Não roda no {@code ./mvnw test} normal, por ser mais demorado. Para rodar:
 * {@code ./mvnw test -Pperformance}.
 *
 * <p>A carga inicial é inserida em lote via JDBC, só para preparar o cenário rapidamente.
 * As medições passam pela API completa (MockMvc, controller, serviço, JPA e banco).
 * O validador de CPF é simulado como sempre apto, para que nenhum voto seja recusado ao acaso.
 * Os limites dos asserts são folgados de propósito: servem para detectar regressões graves
 * (por exemplo, a contagem passar a carregar os votos para a memória), não para comparar máquinas.
 */
@Tag("performance")
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:performance;DB_CLOSE_DELAY=-1",
		"spring.datasource.hikari.maximum-pool-size=20",
		"logging.level.com.caioribasregis.desafiovotacao.service=WARN" })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VotacaoPerformanceTest {

	private static final int VOTOS_PRE_CARREGADOS = 300_000;
	private static final int TAMANHO_LOTE = 5_000;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@MockitoBean
	private ValidadorCpfClient validadorCpfClient;

	private Long pautaId;
	private long votosSim;
	private long votosNao;

	@BeforeAll
	void carregarVotos() {
		Pauta pauta = pautaRepository.save(Pauta.builder().titulo("Pauta com muitos votos").build());
		pautaId = pauta.getId();
		LocalDateTime agora = LocalDateTime.now();
		sessaoVotacaoRepository.save(SessaoVotacao.builder()
				.pauta(pauta).dataHoraAbertura(agora).dataHoraFecho(agora.plusHours(1)).build());

		long inicio = System.nanoTime();
		for (int inicioLote = 0; inicioLote < VOTOS_PRE_CARREGADOS; inicioLote += TAMANHO_LOTE) {
			List<Object[]> lote = new ArrayList<>(TAMANHO_LOTE);
			for (int i = inicioLote; i < inicioLote + TAMANHO_LOTE; i++) {
				// cerca de 2/3 SIM e 1/3 NAO
				String escolha = i % 3 == 0 ? "NAO" : "SIM";
				lote.add(new Object[] { pautaId, cpf(i), escolha });
			}
			jdbcTemplate.batchUpdate("insert into voto (pauta_id, cpf_associado, escolha) values (?, ?, ?)", lote);
		}
		votosNao = (VOTOS_PRE_CARREGADOS + 2) / 3;
		votosSim = VOTOS_PRE_CARREGADOS - votosNao;
		relatorio("Carga inicial: %,d votos inseridos em %d ms", VOTOS_PRE_CARREGADOS, ms(System.nanoTime() - inicio));
	}

	@Test
	@Order(1)
	void consultaDoResultadoComCentenasDeMilharesDeVotos() throws Exception {
		// aquecimento: JIT, cache de planos de consulta e páginas do banco
		for (int i = 0; i < 5; i++) {
			consultarResultado();
		}

		long[] tempos = new long[30];
		for (int i = 0; i < tempos.length; i++) {
			long inicio = System.nanoTime();
			consultarResultado();
			tempos[i] = System.nanoTime() - inicio;
		}

		Estatisticas e = Estatisticas.de(tempos);
		relatorio("Consulta do resultado (%,d votos): média %.1f ms, p95 %.1f ms, máx %.1f ms",
				VOTOS_PRE_CARREGADOS, e.mediaMs(), e.p95Ms(), e.maxMs());
		assertThat(e.p95Ms()).as("p95 da consulta do resultado (ms)").isLessThan(500);
	}

	@Test
	@Order(2)
	void registroDeVotosComCentenasDeMilharesJaGravados() throws Exception {
		when(validadorCpfClient.consultarStatus(anyString())).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);
		int base = VOTOS_PRE_CARREGADOS;

		for (int i = 0; i < 50; i++) {
			votar(cpf(base + i)).andExpect(status().isCreated());
		}

		long[] tempos = new long[1_000];
		for (int i = 0; i < tempos.length; i++) {
			String cpf = cpf(base + 50 + i);
			long inicio = System.nanoTime();
			votar(cpf).andExpect(status().isCreated());
			tempos[i] = System.nanoTime() - inicio;
		}

		Estatisticas e = Estatisticas.de(tempos);
		relatorio("Registro de voto (%,d votos já gravados): média %.2f ms, p95 %.2f ms, máx %.1f ms",
				VOTOS_PRE_CARREGADOS, e.mediaMs(), e.p95Ms(), e.maxMs());
		assertThat(e.p95Ms()).as("p95 do registro de voto (ms)").isLessThan(100);

		votosSim += 1_050;
	}

	@Test
	@Order(3)
	void votosConcorrentesDeAssociadosDiferentes() throws Exception {
		when(validadorCpfClient.consultarStatus(anyString())).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);
		int threads = 16;
		int votos = 4_000;
		int base = VOTOS_PRE_CARREGADOS + 10_000;

		List<Callable<Integer>> tarefas = IntStream.range(0, votos)
				.<Callable<Integer>>mapToObj(i -> () -> votar(cpf(base + i)).andReturn().getResponse().getStatus())
				.toList();

		long inicio = System.nanoTime();
		List<Integer> statusRecebidos = executar(threads, tarefas);
		long duracao = System.nanoTime() - inicio;

		relatorio("Votos concorrentes: %,d votos em %d threads em %d ms (%,.0f votos/s)",
				votos, threads, ms(duracao), votos / (duracao / 1e9));
		assertThat(statusRecebidos).as("todos os votos aceitos").containsOnly(201);

		votosSim += votos;
	}

	@Test
	@Order(4)
	void votosConcorrentesDoMesmoCpfSoUmEAceito() throws Exception {
		when(validadorCpfClient.consultarStatus(anyString())).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);
		String mesmoCpf = cpf(VOTOS_PRE_CARREGADOS + 50_000);

		List<Callable<Integer>> tarefas = IntStream.range(0, 50)
				.<Callable<Integer>>mapToObj(i -> () -> votar(mesmoCpf).andReturn().getResponse().getStatus())
				.toList();
		List<Integer> statusRecebidos = executar(16, tarefas);

		relatorio("Mesmo CPF em 50 requisições concorrentes: %d aceita(s), %d recusada(s) com 409",
				statusRecebidos.stream().filter(s -> s == 201).count(),
				statusRecebidos.stream().filter(s -> s == 409).count());
		assertThat(statusRecebidos).filteredOn(s -> s == 201).hasSize(1);
		assertThat(statusRecebidos).filteredOn(s -> s != 201).containsOnly(409);

		votosSim += 1;
	}

	@Test
	@Order(5)
	void contagemFinalBateComTodosOsVotosGravados() throws Exception {
		mockMvc.perform(get("/api/v1/pautas/{id}/resultado", pautaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.votosSim").value(votosSim))
				.andExpect(jsonPath("$.votosNao").value(votosNao))
				.andExpect(jsonPath("$.totalVotos").value(votosSim + votosNao))
				.andExpect(jsonPath("$.resultado").value("APROVADA"));
		relatorio("Contagem final conferida: %,d SIM, %,d NAO", votosSim, votosNao);
	}

	private void consultarResultado() throws Exception {
		mockMvc.perform(get("/api/v1/pautas/{id}/resultado", pautaId)).andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions votar(String cpf) throws Exception {
		return mockMvc.perform(post("/api/v1/pautas/{id}/votos", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cpf\": \"" + cpf + "\", \"escolha\": \"SIM\"}"));
	}

	private static List<Integer> executar(int threads, List<Callable<Integer>> tarefas) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			List<Integer> resultados = new ArrayList<>(tarefas.size());
			for (Future<Integer> f : executor.invokeAll(tarefas)) {
				resultados.add(f.get());
			}
			return resultados;
		} finally {
			executor.shutdown();
		}
	}

	private static String cpf(int i) {
		return String.format("%011d", i);
	}

	private static long ms(long nanos) {
		return Duration.ofNanos(nanos).toMillis();
	}

	private static void relatorio(String formato, Object... args) {
		System.out.println("[PERFORMANCE] " + String.format(formato, args));
	}

	private record Estatisticas(double mediaMs, double p95Ms, double maxMs) {
		static Estatisticas de(long[] temposNanos) {
			long[] ordenados = temposNanos.clone();
			Arrays.sort(ordenados);
			double media = Arrays.stream(ordenados).average().orElse(0) / 1e6;
			double p95 = ordenados[(int) Math.ceil(ordenados.length * 0.95) - 1] / 1e6;
			double max = ordenados[ordenados.length - 1] / 1e6;
			return new Estatisticas(media, p95, max);
		}
	}

}
