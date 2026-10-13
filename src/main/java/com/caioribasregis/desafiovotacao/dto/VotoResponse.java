package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Voto;

public record VotoResponse(Long id, Long pautaId, String cpf, EscolhaVoto escolha) {

	public static VotoResponse from(Voto voto) {
		return new VotoResponse(voto.getId(), voto.getPauta().getId(), voto.getCpfAssociado(), voto.getEscolha());
	}

}
