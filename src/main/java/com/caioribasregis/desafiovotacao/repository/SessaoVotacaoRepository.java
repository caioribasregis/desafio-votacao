package com.caioribasregis.desafiovotacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

	boolean existsByPautaId(Long pautaId);

}
