package com.caioribasregis.desafiovotacao.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.caioribasregis.desafiovotacao.domain.ResultadoVotacao;
import com.caioribasregis.desafiovotacao.domain.StatusVotacao;
import com.caioribasregis.desafiovotacao.dto.ResultadoVotacaoResponse;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.service.ResultadoVotacaoService;

@WebMvcTest(ResultadoVotacaoController.class)
class ResultadoVotacaoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResultadoVotacaoService resultadoVotacaoService;

	@Test
	void consultarVotacaoEncerradaDevolve200ComContagemEResultado() throws Exception {
		when(resultadoVotacaoService.consultar(1L)).thenReturn(new ResultadoVotacaoResponse(
				1L, "Nova sede", 3, 1, 4, ResultadoVotacao.APROVADA, StatusVotacao.ENCERRADA));

		mockMvc.perform(get("/api/v1/pautas/1/resultado"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pautaId").value(1))
				.andExpect(jsonPath("$.titulo").value("Nova sede"))
				.andExpect(jsonPath("$.votosSim").value(3))
				.andExpect(jsonPath("$.votosNao").value(1))
				.andExpect(jsonPath("$.totalVotos").value(4))
				.andExpect(jsonPath("$.resultado").value("APROVADA"))
				.andExpect(jsonPath("$.statusVotacao").value("ENCERRADA"));
	}

	@Test
	void consultarVotacaoEmAndamentoDevolveStatusEmAndamento() throws Exception {
		when(resultadoVotacaoService.consultar(1L)).thenReturn(new ResultadoVotacaoResponse(
				1L, "Nova sede", 1, 0, 1, ResultadoVotacao.APROVADA, StatusVotacao.EM_ANDAMENTO));

		mockMvc.perform(get("/api/v1/pautas/1/resultado"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusVotacao").value("EM_ANDAMENTO"));
	}

	@Test
	void consultarPautaInexistenteDevolve404() throws Exception {
		when(resultadoVotacaoService.consultar(99L))
				.thenThrow(new RecursoNaoEncontradoException("Pauta 99 não encontrada"));

		mockMvc.perform(get("/api/v1/pautas/99/resultado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Pauta 99 não encontrada"));
	}

}
