package com.caioribasregis.desafiovotacao.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.caioribasregis.desafiovotacao.dto.PautaRequest;
import com.caioribasregis.desafiovotacao.dto.PautaResponse;
import com.caioribasregis.desafiovotacao.service.PautaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pautas")
@RequiredArgsConstructor
public class PautaController {

	private final PautaService pautaService;

	@PostMapping
	public ResponseEntity<PautaResponse> criar(@Valid @RequestBody PautaRequest request) {
		PautaResponse pauta = pautaService.criar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(pauta.id())
				.toUri();
		return ResponseEntity.created(location).body(pauta);
	}

}
