package com.caioribasregis.desafiovotacao;

import static com.caioribasregis.desafiovotacao.VotoIntegrationTest.assertLog;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;

/**
 * Verifica os logs de negócio da criação de pautas e da abertura de sessões.
 * Os logs do registo de votos são verificados em {@link VotoIntegrationTest}.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:logs-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LogsNegocioIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PautaRepository pautaRepository;

	@Test
	void pautaCriadaRegistaInfoComId(CapturedOutput output) throws Exception {
		mockMvc.perform(post("/api/v1/pautas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\": \"Nova sede\"}"))
				.andExpect(status().isCreated());

		assertLog(output, "INFO", "Pauta criada: pautaId=");
	}

	@Test
	void sessaoAbertaRegistaInfoComPautaEDuracao(CapturedOutput output) throws Exception {
		Long pautaId = novaPauta();

		abrirSessao(pautaId, "{\"duracaoMinutos\": 5}").andExpect(status().isCreated());

		assertLog(output, "INFO", "Sessão de votação aberta: sessaoId=");
		assertLog(output, "INFO", "pautaId=" + pautaId + ", duracaoMinutos=5, fecho=");
	}

	@Test
	void sessaoAbertaSemDuracaoIndicaQueUsouOPadrao(CapturedOutput output) throws Exception {
		Long pautaId = novaPauta();

		abrirSessao(pautaId, "{}").andExpect(status().isCreated());

		assertLog(output, "INFO", "pautaId=" + pautaId + ", duracaoMinutos=1 (padrão)");
	}

	@Test
	void aberturaDeSegundaSessaoRegistaWarn(CapturedOutput output) throws Exception {
		Long pautaId = novaPauta();
		abrirSessao(pautaId, "{}").andExpect(status().isCreated());

		abrirSessao(pautaId, "{}").andExpect(status().isConflict());

		assertLog(output, "WARN", "Abertura de sessão recusada: pautaId=" + pautaId + ", motivo=a pauta já tem sessão");
	}

	@Test
	void aberturaDeSessaoComPautaInexistenteRegistaWarn(CapturedOutput output) throws Exception {
		abrirSessao(999_999L, "{}").andExpect(status().isNotFound());

		assertLog(output, "WARN", "Abertura de sessão recusada: pautaId=999999, motivo=pauta inexistente");
	}

	private Long novaPauta() {
		return pautaRepository.save(Pauta.builder().titulo("Pauta").build()).getId();
	}

	private org.springframework.test.web.servlet.ResultActions abrirSessao(Long pautaId, String corpo) throws Exception {
		return mockMvc.perform(post("/api/v1/pautas/{id}/sessao", pautaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

}
