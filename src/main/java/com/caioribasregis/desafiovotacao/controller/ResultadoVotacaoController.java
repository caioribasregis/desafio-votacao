package com.caioribasregis.desafiovotacao.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caioribasregis.desafiovotacao.dto.ResultadoVotacaoResponse;
import com.caioribasregis.desafiovotacao.service.ResultadoVotacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiVersao.V1 + "/pautas/{pautaId}/resultado")
@RequiredArgsConstructor
@Tag(name = "Resultado", description = "Contagem dos votos e resultado da votação")
public class ResultadoVotacaoController {

	private final ResultadoVotacaoService resultadoVotacaoService;

	@GetMapping
	@Operation(summary = "Consultar o resultado da votação de uma pauta",
			description = "Conta os votos SIM e NAO e determina o resultado. Enquanto `statusVotacao` "
					+ "for `EM_ANDAMENTO`, a contagem e o resultado são parciais.")
	@ApiResponse(responseCode = "200", description = "Contagem e resultado da votação")
	@ApiResponse(responseCode = "404", description = "Pauta não encontrada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public ResultadoVotacaoResponse consultar(
			@Parameter(description = "Id da pauta", example = "1") @PathVariable Long pautaId) {
		return resultadoVotacaoService.consultar(pautaId);
	}

}
