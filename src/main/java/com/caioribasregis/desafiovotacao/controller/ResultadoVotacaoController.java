package com.caioribasregis.desafiovotacao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caioribasregis.desafiovotacao.dto.ResultadoVotacaoResponse;
import com.caioribasregis.desafiovotacao.service.ResultadoVotacaoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/resultado")
@RequiredArgsConstructor
public class ResultadoVotacaoController {

	private final ResultadoVotacaoService resultadoVotacaoService;

	@GetMapping
	public ResultadoVotacaoResponse consultar(@PathVariable Long pautaId) {
		return resultadoVotacaoService.consultar(pautaId);
	}

}
