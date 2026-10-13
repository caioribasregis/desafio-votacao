package com.caioribasregis.desafiovotacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caioribasregis.desafiovotacao.dto.VotoRequest;
import com.caioribasregis.desafiovotacao.dto.VotoResponse;
import com.caioribasregis.desafiovotacao.service.VotoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/votos")
@RequiredArgsConstructor
public class VotoController {

	private final VotoService votoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VotoResponse votar(@PathVariable Long pautaId, @Valid @RequestBody VotoRequest request) {
		return votoService.votar(pautaId, request);
	}

}
