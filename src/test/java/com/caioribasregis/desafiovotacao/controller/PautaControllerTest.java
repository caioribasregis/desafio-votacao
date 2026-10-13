package com.caioribasregis.desafiovotacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.caioribasregis.desafiovotacao.dto.PautaRequest;
import com.caioribasregis.desafiovotacao.dto.PautaResponse;
import com.caioribasregis.desafiovotacao.service.PautaService;

@WebMvcTest(PautaController.class)
class PautaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PautaService pautaService;

	@Test
	void criarDevolve201ComPautaCriada() throws Exception {
		when(pautaService.criar(any(PautaRequest.class)))
				.thenReturn(new PautaResponse(1L, "Nova sede", "Aprovar a compra da nova sede"));

		mockMvc.perform(post("/api/v1/pautas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"titulo": "Nova sede", "descricao": "Aprovar a compra da nova sede"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/pautas/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.titulo").value("Nova sede"))
				.andExpect(jsonPath("$.descricao").value("Aprovar a compra da nova sede"));
	}

	@Test
	void criarSemTituloDevolve400() throws Exception {
		mockMvc.perform(post("/api/v1/pautas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"titulo": "   ", "descricao": "Sem título"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.titulo").value("O título é obrigatório"));

		verifyNoInteractions(pautaService);
	}

}
