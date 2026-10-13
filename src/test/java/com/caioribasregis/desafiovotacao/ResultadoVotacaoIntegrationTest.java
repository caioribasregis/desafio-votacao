package com.caioribasregis.desafiovotacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.domain.Voto;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;
import com.caioribasregis.desafiovotacao.repository.VotoRepository;

/**
 * Percorre o fluxo completo da consulta do resultado (HTTP, serviço, consulta agrupada e base de dados).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:resultado-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ResultadoVotacaoIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Autowired
	private VotoRepository votoRepository;

	@Test
	void votacaoEncerradaContaApenasOsVotosDaPautaConsultada() throws Exception {
		Pauta pauta = pautaComVotos("Nova sede", EscolhaVoto.SIM, EscolhaVoto.SIM, EscolhaVoto.NAO);
		comSessao(pauta, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().minusMinutes(5));
		pautaComVotos("Outra pauta", EscolhaVoto.NAO, EscolhaVoto.NAO, EscolhaVoto.NAO);

		mockMvc.perform(get("/api/v1/pautas/{id}/resultado", pauta.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pautaId").value(pauta.getId()))
				.andExpect(jsonPath("$.titulo").value("Nova sede"))
				.andExpect(jsonPath("$.votosSim").value(2))
				.andExpect(jsonPath("$.votosNao").value(1))
				.andExpect(jsonPath("$.totalVotos").value(3))
				.andExpect(jsonPath("$.resultado").value("APROVADA"))
				.andExpect(jsonPath("$.statusVotacao").value("ENCERRADA"));
	}

	@Test
	void votacaoComSessaoAbertaDevolveEmAndamento() throws Exception {
		Pauta pauta = pautaComVotos("Em curso", EscolhaVoto.NAO);
		comSessao(pauta, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));

		mockMvc.perform(get("/api/v1/pautas/{id}/resultado", pauta.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.votosNao").value(1))
				.andExpect(jsonPath("$.resultado").value("REPROVADA"))
				.andExpect(jsonPath("$.statusVotacao").value("EM_ANDAMENTO"));
	}

	@Test
	void pautaSemSessaoNemVotosDevolveNaoIniciadaESemVotos() throws Exception {
		Pauta pauta = pautaComVotos("Sem votos");

		mockMvc.perform(get("/api/v1/pautas/{id}/resultado", pauta.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.votosSim").value(0))
				.andExpect(jsonPath("$.votosNao").value(0))
				.andExpect(jsonPath("$.totalVotos").value(0))
				.andExpect(jsonPath("$.resultado").value("SEM_VOTOS"))
				.andExpect(jsonPath("$.statusVotacao").value("NAO_INICIADA"));
	}

	@Test
	void pautaInexistenteDevolve404() throws Exception {
		mockMvc.perform(get("/api/v1/pautas/{id}/resultado", 999_999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Pauta 999999 não encontrada"));
	}

	private Pauta pautaComVotos(String titulo, EscolhaVoto... escolhas) {
		Pauta pauta = pautaRepository.save(Pauta.builder().titulo(titulo).build());
		for (int i = 0; i < escolhas.length; i++) {
			votoRepository.save(Voto.builder()
					.pauta(pauta)
					.cpfAssociado(String.format("%011d", pauta.getId() * 100 + i))
					.escolha(escolhas[i])
					.build());
		}
		return pauta;
	}

	private void comSessao(Pauta pauta, LocalDateTime abertura, LocalDateTime fecho) {
		sessaoVotacaoRepository.save(SessaoVotacao.builder()
				.pauta(pauta).dataHoraAbertura(abertura).dataHoraFecho(fecho).build());
	}

}
