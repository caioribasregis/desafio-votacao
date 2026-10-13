package com.caioribasregis.desafiovotacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caioribasregis.desafiovotacao.dto.AbrirSessaoRequest;
import com.caioribasregis.desafiovotacao.dto.SessaoVotacaoResponse;
import com.caioribasregis.desafiovotacao.service.SessaoVotacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessao")
@RequiredArgsConstructor
public class SessaoVotacaoController {

	private final SessaoVotacaoService sessaoVotacaoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SessaoVotacaoResponse abrir(@PathVariable Long pautaId,
			@Valid @RequestBody(required = false) AbrirSessaoRequest request) {
		Integer duracaoMinutos = request != null ? request.duracaoMinutos() : null;
		return sessaoVotacaoService.abrir(pautaId, duracaoMinutos);
	}

}
