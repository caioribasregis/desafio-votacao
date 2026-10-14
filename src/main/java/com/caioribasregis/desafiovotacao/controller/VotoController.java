package com.caioribasregis.desafiovotacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caioribasregis.desafiovotacao.dto.VotoRequest;
import com.caioribasregis.desafiovotacao.dto.VotoResponse;
import com.caioribasregis.desafiovotacao.service.VotoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiVersao.V1 + "/pautas/{pautaId}/votos")
@RequiredArgsConstructor
@Tag(name = "Votos", description = "Registo dos votos dos associados")
public class VotoController {

	private final VotoService votoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registar um voto",
			description = """
					Regista o voto de um associado numa pauta. As verificações são feitas por esta ordem:
					1. a pauta existe;
					2. a pauta tem uma sessão de votação;
					3. a sessão está aberta (entre a abertura e o fecho);
					4. o CPF ainda não votou nesta pauta;
					5. o serviço externo de validação considera o CPF válido e apto a votar (`ABLE_TO_VOTE`).

					Por ser um dado pessoal, o CPF não aparece nas mensagens de erro.""")
	@ApiResponse(responseCode = "201", description = "Voto registado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos: CPF sem 11 dígitos, escolha em falta ou diferente de SIM/NAO",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = "O associado não está apto a votar (`UNABLE_TO_VOTE`)",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Pauta não encontrada, ou CPF inválido segundo o serviço de validação",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Este CPF já votou nesta pauta",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "422", description = "A pauta não tem sessão, ou a sessão ainda não começou ou já está encerrada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public VotoResponse votar(
			@Parameter(description = "Id da pauta", example = "1") @PathVariable Long pautaId,
			@Valid @RequestBody VotoRequest request) {
		return votoService.votar(pautaId, request);
	}

}
