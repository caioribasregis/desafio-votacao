package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.ResultadoVotacao;
import com.caioribasregis.desafiovotacao.domain.StatusVotacao;

/**
 * Resultado da votação de uma pauta. Enquanto {@code statusVotacao} for {@code EM_ANDAMENTO},
 * a contagem e o resultado são parciais.
 */
public record ResultadoVotacaoResponse(
		Long pautaId,
		String titulo,
		long votosSim,
		long votosNao,
		long totalVotos,
		ResultadoVotacao resultado,
		StatusVotacao statusVotacao) {
}
