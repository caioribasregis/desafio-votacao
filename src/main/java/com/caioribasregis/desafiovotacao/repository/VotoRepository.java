package com.caioribasregis.desafiovotacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.caioribasregis.desafiovotacao.domain.Voto;

public interface VotoRepository extends JpaRepository<Voto, Long> {

	boolean existsByPautaIdAndCpfAssociado(Long pautaId, String cpfAssociado);

}
