package com.caioribasregis.desafiovotacao.domain;

import java.time.LocalDateTime;

public enum StatusVotacao {
	NAO_INICIADA,
	EM_ANDAMENTO,
	ENCERRADA;

	/**
	 * Determina o estado da votação no instante indicado. Segue a mesma regra do registo de votos:
	 * a sessão está aberta a partir da abertura (inclusive) até ao fecho (exclusive).
	 *
	 * @param sessao a sessão da pauta, ou {@code null} se ainda não foi aberta nenhuma
	 */
	public static StatusVotacao de(SessaoVotacao sessao, LocalDateTime agora) {
		if (sessao == null || agora.isBefore(sessao.getDataHoraAbertura())) {
			return NAO_INICIADA;
		}
		return agora.isBefore(sessao.getDataHoraFecho()) ? EM_ANDAMENTO : ENCERRADA;
	}
}
