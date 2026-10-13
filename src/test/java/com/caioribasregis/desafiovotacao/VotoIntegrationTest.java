package com.caioribasregis.desafiovotacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.caioribasregis.desafiovotacao.client.StatusVotoAssociado;
import com.caioribasregis.desafiovotacao.client.ValidadorCpfClient;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.exception.CpfInvalidoException;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;
import com.caioribasregis.desafiovotacao.repository.VotoRepository;

/**
 * Percorre o fluxo completo de votação (HTTP, serviço e base de dados) com o validador de CPF simulado,
 * para que cada resultado seja determinístico.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:voto-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class VotoIntegrationTest {

	private static final String CPF = "12345678901";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Autowired
	private VotoRepository votoRepository;

	@MockitoBean
	private ValidadorCpfClient validadorCpfClient;

	@Test
	void votoBemSucedidoFicaGravado(CapturedOutput output) throws Exception {
		Long pautaId = pautaComSessaoAberta();
		when(validadorCpfClient.consultarStatus(CPF)).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);

		votar(pautaId, "SIM")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pautaId").value(pautaId))
				.andExpect(jsonPath("$.cpf").value(CPF))
				.andExpect(jsonPath("$.escolha").value("SIM"));

		assertThat(votoRepository.existsByPautaIdAndCpfAssociado(pautaId, CPF)).isTrue();
		assertLog(output, "INFO", "Voto registado: votoId=");
		assertLog(output, "INFO", "pautaId=" + pautaId + ", escolha=SIM");
		assertThat(output).doesNotContain(CPF);
	}

	@Test
	void pautaInexistenteDevolve404(CapturedOutput output) throws Exception {
		votar(999_999L, "SIM")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Pauta 999999 não encontrada"));
		assertLog(output, "WARN", "Voto recusado: pautaId=999999, motivo=pauta inexistente");
		assertThat(output).doesNotContain(CPF);
	}

	@Test
	void pautaSemSessaoDevolve422(CapturedOutput output) throws Exception {
		Long pautaId = novaPauta().getId();

		votar(pautaId, "SIM")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("A pauta " + pautaId + " ainda não tem sessão de votação"));
		assertLog(output, "WARN", "Voto recusado: pautaId=" + pautaId + ", motivo=pauta sem sessão de votação");
		assertThat(output).doesNotContain(CPF);
	}

	@Test
	void sessaoEncerradaDevolve422(CapturedOutput output) throws Exception {
		Pauta pauta = novaPauta();
		LocalDateTime abertura = LocalDateTime.now().minusMinutes(10);
		sessaoVotacaoRepository.save(SessaoVotacao.builder()
				.pauta(pauta).dataHoraAbertura(abertura).dataHoraFecho(abertura.plusMinutes(1)).build());

		votar(pauta.getId(), "SIM")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("A sessão de votação da pauta " + pauta.getId() + " está encerrada"));
		verifyNoInteractions(validadorCpfClient);
		assertLog(output, "WARN", "Voto recusado: pautaId=" + pauta.getId() + ", motivo=sessão encerrada");
		assertThat(output).doesNotContain(CPF);
	}

	@Test
	void votoDuplicadoDevolve409(CapturedOutput output) throws Exception {
		Long pautaId = pautaComSessaoAberta();
		when(validadorCpfClient.consultarStatus(CPF)).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);
		votar(pautaId, "SIM").andExpect(status().isCreated());

		votar(pautaId, "NAO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Este CPF já votou na pauta " + pautaId))
				.andExpect(content().string(not(containsString(CPF))));
		assertLog(output, "WARN", "Voto recusado: pautaId=" + pautaId + ", motivo=CPF já votou nesta pauta");
		assertThat(output).doesNotContain(CPF);
	}

	@Test
	void cpfInvalidoDevolve404(CapturedOutput output) throws Exception {
		Long pautaId = pautaComSessaoAberta();
		when(validadorCpfClient.consultarStatus(CPF)).thenThrow(new CpfInvalidoException());

		votar(pautaId, "SIM")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("CPF inválido"))
				.andExpect(content().string(not(containsString(CPF))));
		assertLog(output, "WARN", "Voto recusado: pautaId=" + pautaId + ", motivo=CPF inválido segundo o serviço de validação");
		assertThat(output).doesNotContain(CPF);
		assertThat(votoRepository.existsByPautaIdAndCpfAssociado(pautaId, CPF)).isFalse();
	}

	@Test
	void associadoNaoAptoDevolve403(CapturedOutput output) throws Exception {
		Long pautaId = pautaComSessaoAberta();
		when(validadorCpfClient.consultarStatus(CPF)).thenReturn(StatusVotoAssociado.UNABLE_TO_VOTE);

		votar(pautaId, "SIM")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("O associado não está apto a votar"))
				.andExpect(content().string(not(containsString(CPF))));
		assertLog(output, "WARN", "Voto recusado: pautaId=" + pautaId + ", motivo=associado não apto a votar (UNABLE_TO_VOTE)");
		assertThat(output).doesNotContain(CPF);
		assertThat(votoRepository.existsByPautaIdAndCpfAssociado(pautaId, CPF)).isFalse();
	}

	/** Verifica que existe uma linha de log com o nível e o texto indicados. */
	static void assertLog(CapturedOutput output, String nivel, String texto) {
		assertThat(output.getAll().lines())
				.as("linha de log %s com \"%s\"", nivel, texto)
				.anyMatch(linha -> linha.contains(" " + nivel + " ") && linha.contains(texto));
	}

	private Pauta novaPauta() {
		return pautaRepository.save(Pauta.builder().titulo("Pauta").build());
	}

	private Long pautaComSessaoAberta() throws Exception {
		Long pautaId = novaPauta().getId();
		mockMvc.perform(post("/api/v1/pautas/{id}/sessao", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"duracaoMinutos\": 10}"))
				.andExpect(status().isCreated());
		return pautaId;
	}

	private ResultActions votar(Long pautaId, String escolha) throws Exception {
		return mockMvc.perform(post("/api/v1/pautas/{id}/votos", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cpf\": \"" + CPF + "\", \"escolha\": \"" + escolha + "\"}"));
	}

}
