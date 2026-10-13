package com.caioribasregis.desafiovotacao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Voto;

public interface VotoRepository extends JpaRepository<Voto, Long> {

	boolean existsByPautaIdAndCpfAssociado(Long pautaId, String cpfAssociado);

	/**
	 * Conta os votos da pauta agrupados por escolha, numa única consulta.
	 * Escolhas sem nenhum voto não aparecem no resultado.
	 */
	@Query("select v.escolha as escolha, count(v) as quantidade from Voto v where v.pauta.id = :pautaId group by v.escolha")
	List<ContagemPorEscolha> contarPorEscolha(Long pautaId);

	interface ContagemPorEscolha {
		EscolhaVoto getEscolha();
		long getQuantidade();
	}

}
