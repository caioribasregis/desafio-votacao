package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.ResultadoVotacao;
import com.caioribasregis.desafiovotacao.domain.StatusVotacao;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado da votação de uma pauta. Enquanto {@code statusVotacao} for {@code EM_ANDAMENTO},
 * a contagem e o resultado são parciais.
 */
@Schema(description = "Contagem dos votos e resultado da votação de uma pauta")
public record ResultadoVotacaoResponse(
		@Schema(description = "Id da pauta", example = "1") Long pautaId,
		@Schema(description = "Título da pauta", example = "Compra da nova sede") String titulo,
		@Schema(description = "Número de votos SIM", example = "3") long votosSim,
		@Schema(description = "Número de votos NAO", example = "1") long votosNao,
		@Schema(description = "Total de votos", example = "4") long totalVotos,
		@Schema(description = "APROVADA (mais SIM), REPROVADA (mais NAO), EMPATE (igual número) "
				+ "ou SEM_VOTOS (nenhum voto)", example = "APROVADA") ResultadoVotacao resultado,
		@Schema(description = "NAO_INICIADA (pauta sem sessão), EM_ANDAMENTO (sessão aberta; resultado parcial) "
				+ "ou ENCERRADA (passou a hora de fecho; resultado final)", example = "ENCERRADA") StatusVotacao statusVotacao) {
}
