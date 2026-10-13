package com.caioribasregis.desafiovotacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.caioribasregis.desafiovotacao.dto.SessaoVotacaoResponse;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.service.SessaoVotacaoService;

@WebMvcTest(SessaoVotacaoController.class)
class SessaoVotacaoControllerTest {

	private static final LocalDateTime ABERTURA = LocalDateTime.of(2026, 10, 13, 18, 0);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SessaoVotacaoService sessaoVotacaoService;

	@Test
	void abrirComDuracaoDevolve201() throws Exception {
		when(sessaoVotacaoService.abrir(1L, 5))
				.thenReturn(new SessaoVotacaoResponse(10L, 1L, ABERTURA, ABERTURA.plusMinutes(5)));

		mockMvc.perform(post("/api/v1/pautas/1/sessao")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"duracaoMinutos": 5}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.pautaId").value(1))
				.andExpect(jsonPath("$.dataHoraAbertura").value("2026-10-13T18:00:00"))
				.andExpect(jsonPath("$.dataHoraFecho").value("2026-10-13T18:05:00"));
	}

	@Test
	void abrirSemCorpoPassaDuracaoNulaParaUsarOPadrao() throws Exception {
		when(sessaoVotacaoService.abrir(any(), isNull()))
				.thenReturn(new SessaoVotacaoResponse(10L, 1L, ABERTURA, ABERTURA.plusMinutes(1)));

		mockMvc.perform(post("/api/v1/pautas/1/sessao"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dataHoraFecho").value("2026-10-13T18:01:00"));
	}

	@Test
	void abrirComPautaInexistenteDevolve404() throws Exception {
		when(sessaoVotacaoService.abrir(99L, null))
				.thenThrow(new RecursoNaoEncontradoException("Pauta 99 não encontrada"));

		mockMvc.perform(post("/api/v1/pautas/99/sessao"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Pauta 99 não encontrada"));
	}

	@Test
	void abrirComSessaoJaExistenteDevolve409() throws Exception {
		when(sessaoVotacaoService.abrir(1L, null))
				.thenThrow(new ConflitoException("A pauta 1 já tem uma sessão de votação"));

		mockMvc.perform(post("/api/v1/pautas/1/sessao"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("A pauta 1 já tem uma sessão de votação"));
	}

	@Test
	void abrirComDuracaoInvalidaDevolve400() throws Exception {
		mockMvc.perform(post("/api/v1/pautas/1/sessao")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"duracaoMinutos": 0}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.duracaoMinutos").value("A duração deve ser maior que zero"));

		verifyNoInteractions(sessaoVotacaoService);
	}

}
