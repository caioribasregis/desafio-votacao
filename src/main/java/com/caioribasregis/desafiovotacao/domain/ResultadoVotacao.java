package com.caioribasregis.desafiovotacao.domain;

public enum ResultadoVotacao {
	APROVADA,
	REPROVADA,
	EMPATE,
	SEM_VOTOS;

	public static ResultadoVotacao de(long votosSim, long votosNao) {
		if (votosSim == 0 && votosNao == 0) {
			return SEM_VOTOS;
		}
		if (votosSim > votosNao) {
			return APROVADA;
		}
		if (votosNao > votosSim) {
			return REPROVADA;
		}
		return EMPATE;
	}
}
