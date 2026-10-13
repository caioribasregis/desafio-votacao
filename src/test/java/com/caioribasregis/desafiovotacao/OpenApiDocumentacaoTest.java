package com.caioribasregis.desafiovotacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:openapi-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class OpenApiDocumentacaoTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void especificacaoOpenApiDocumentaTodosOsEndpointsECodigos() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Desafio Votação API"))
				.andExpect(jsonPath("$.paths['/api/v1/pautas'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pautas'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/sessao'].post.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/votos'].post.responses['403']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/votos'].post.responses['422']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/resultado'].get.responses['404']").exists());
	}

	@Test
	void swaggerUiEstaDisponivel() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

}
