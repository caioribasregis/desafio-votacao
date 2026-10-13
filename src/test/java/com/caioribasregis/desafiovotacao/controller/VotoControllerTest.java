package com.caioribasregis.desafiovotacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.dto.VotoRequest;
import com.caioribasregis.desafiovotacao.dto.VotoResponse;
import com.caioribasregis.desafiovotacao.exception.AssociadoNaoAptoException;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.CpfInvalidoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.exception.RegraNegocioException;
import com.caioribasregis.desafiovotacao.service.VotoService;

@WebMvcTest(VotoController.class)
class VotoControllerTest {

	private static final String CORPO_VALIDO = """
			{"cpf": "12345678901", "escolha": "SIM"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VotoService votoService;

	@Test
	void votoBemSucedidoDevolve201() throws Exception {
		when(votoService.votar(1L, new VotoRequest("12345678901", EscolhaVoto.SIM)))
				.thenReturn(new VotoResponse(100L, 1L, "12345678901", EscolhaVoto.SIM));

		votar(CORPO_VALIDO)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(100))
				.andExpect(jsonPath("$.pautaId").value(1))
				.andExpect(jsonPath("$.cpf").value("12345678901"))
				.andExpect(jsonPath("$.escolha").value("SIM"));
	}

	static Stream<Arguments> recusasDoServico() {
		return Stream.of(
				Arguments.of(new RecursoNaoEncontradoException("Pauta 1 não encontrada"), 404),
				Arguments.of(new RegraNegocioException("A pauta 1 ainda não tem sessão de votação"), 422),
				Arguments.of(new RegraNegocioException("A sessão de votação da pauta 1 está encerrada"), 422),
				Arguments.of(new ConflitoException("Este CPF já votou na pauta 1"), 409),
				Arguments.of(new CpfInvalidoException(), 404),
				Arguments.of(new AssociadoNaoAptoException(), 403));
	}

	@ParameterizedTest
	@MethodSource("recusasDoServico")
	void recusaDoServicoDevolveStatusEMensagem(RuntimeException recusa, int statusEsperado) throws Exception {
		when(votoService.votar(eq(1L), any(VotoRequest.class))).thenThrow(recusa);

		votar(CORPO_VALIDO)
				.andExpect(status().is(statusEsperado))
				.andExpect(jsonPath("$.detail").value(recusa.getMessage()));
	}

	@Test
	void cpfComFormatoInvalidoDevolve400() throws Exception {
		votar("""
				{"cpf": "123.456.789-01", "escolha": "SIM"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.cpf").value("O CPF deve ter 11 dígitos numéricos"));
		verifyNoInteractions(votoService);
	}

	@Test
	void semEscolhaDevolve400() throws Exception {
		votar("""
				{"cpf": "12345678901"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.escolha").value("A escolha é obrigatória (SIM ou NAO)"));
		verifyNoInteractions(votoService);
	}

	@Test
	void escolhaDesconhecidaDevolve400() throws Exception {
		votar("""
				{"cpf": "12345678901", "escolha": "TALVEZ"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Corpo do pedido inválido ou com valores não reconhecidos"));
		verifyNoInteractions(votoService);
	}

	private ResultActions votar(String corpo) throws Exception {
		return mockMvc.perform(post("/api/v1/pautas/1/votos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

}
