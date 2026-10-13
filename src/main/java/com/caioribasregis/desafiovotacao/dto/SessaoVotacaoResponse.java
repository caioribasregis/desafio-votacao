package com.caioribasregis.desafiovotacao.dto;

import java.time.LocalDateTime;

import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sessão de votação aberta")
public record SessaoVotacaoResponse(
		@Schema(description = "Id da sessão", example = "1") Long id,
		@Schema(description = "Id da pauta", example = "1") Long pautaId,
		@Schema(description = "Data e hora de abertura", example = "2026-10-13T18:00:00") LocalDateTime dataHoraAbertura,
		@Schema(description = "Data e hora de fecho; a partir deste instante já não se aceitam votos",
				example = "2026-10-13T18:01:00") LocalDateTime dataHoraFecho) {

	public static SessaoVotacaoResponse from(SessaoVotacao sessao) {
		return new SessaoVotacaoResponse(sessao.getId(), sessao.getPauta().getId(),
				sessao.getDataHoraAbertura(), sessao.getDataHoraFecho());
	}

}
