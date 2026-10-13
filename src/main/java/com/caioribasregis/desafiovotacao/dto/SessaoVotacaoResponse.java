package com.caioribasregis.desafiovotacao.dto;

import java.time.LocalDateTime;

import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;

public record SessaoVotacaoResponse(Long id, Long pautaId, LocalDateTime dataHoraAbertura, LocalDateTime dataHoraFecho) {

	public static SessaoVotacaoResponse from(SessaoVotacao sessao) {
		return new SessaoVotacaoResponse(sessao.getId(), sessao.getPauta().getId(),
				sessao.getDataHoraAbertura(), sessao.getDataHoraFecho());
	}

}
