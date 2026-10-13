package com.caioribasregis.desafiovotacao.controller;

import java.net.URI;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.caioribasregis.desafiovotacao.dto.PautaRequest;
import com.caioribasregis.desafiovotacao.dto.PautaResponse;
import com.caioribasregis.desafiovotacao.service.PautaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pautas")
@RequiredArgsConstructor
@Tag(name = "Pautas", description = "Cadastro das pautas a votar")
public class PautaController {

	private final PautaService pautaService;

	@PostMapping
	@Operation(summary = "Cadastrar uma pauta",
			description = "Cria uma nova pauta. O título é obrigatório; a descrição é opcional.")
	@ApiResponse(responseCode = "201", description = "Pauta criada",
			headers = @Header(name = "Location", description = "URL da pauta criada"))
	@ApiResponse(responseCode = "400", description = "Dados inválidos, por exemplo título em branco",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<PautaResponse> criar(@Valid @RequestBody PautaRequest request) {
		PautaResponse pauta = pautaService.criar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(pauta.id())
				.toUri();
		return ResponseEntity.created(location).body(pauta);
	}

}
