package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.Pauta;

public record PautaResponse(Long id, String titulo, String descricao) {

	public static PautaResponse from(Pauta pauta) {
		return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
	}

}
