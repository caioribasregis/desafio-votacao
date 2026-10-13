package com.caioribasregis.desafiovotacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sessao-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SessaoVotacaoIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "{}", "{\"duracaoMinutos\": null}" })
	void semDuracaoInformadaSessaoFicaAbertaExatamenteUmMinuto(String corpo) throws Exception {
		Pauta pauta = pautaRepository.save(Pauta.builder().titulo("Pauta").build());

		MockHttpServletRequestBuilder pedido = post("/api/v1/pautas/{id}/sessao", pauta.getId());
		if (corpo != null) {
			pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
		}
		mockMvc.perform(pedido).andExpect(status().isCreated());

		SessaoVotacao sessao = sessaoVotacaoRepository.findAll().stream()
				.filter(s -> s.getPauta().getId().equals(pauta.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(Duration.between(sessao.getDataHoraAbertura(), sessao.getDataHoraFecho()))
				.isEqualTo(Duration.ofMinutes(1));
	}

}
