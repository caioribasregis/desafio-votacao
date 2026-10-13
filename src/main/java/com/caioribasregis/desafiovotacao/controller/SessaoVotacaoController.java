package com.caioribasregis.desafiovotacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caioribasregis.desafiovotacao.dto.AbrirSessaoRequest;
import com.caioribasregis.desafiovotacao.dto.SessaoVotacaoResponse;
import com.caioribasregis.desafiovotacao.service.SessaoVotacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessao")
@RequiredArgsConstructor
@Tag(name = "Sessões de votação", description = "Abertura da sessão de votação de uma pauta")
public class SessaoVotacaoController {

	private final SessaoVotacaoService sessaoVotacaoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Abrir a sessão de votação de uma pauta",
			description = "Abre a sessão a partir do momento atual, pela duração indicada em minutos. "
					+ "Sem corpo, ou sem `duracaoMinutos`, a sessão fica aberta durante 1 minuto. "
					+ "Cada pauta só pode ter uma sessão.")
	@ApiResponse(responseCode = "201", description = "Sessão aberta")
	@ApiResponse(responseCode = "400", description = "Duração inválida (zero ou negativa)",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Pauta não encontrada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "A pauta já tem uma sessão de votação",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public SessaoVotacaoResponse abrir(
			@Parameter(description = "Id da pauta", example = "1") @PathVariable Long pautaId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
					description = "Opcional. Sem corpo, a sessão dura 1 minuto.")
			@Valid @RequestBody(required = false) AbrirSessaoRequest request) {
		Integer duracaoMinutos = request != null ? request.duracaoMinutos() : null;
		return sessaoVotacaoService.abrir(pautaId, duracaoMinutos);
	}

}
