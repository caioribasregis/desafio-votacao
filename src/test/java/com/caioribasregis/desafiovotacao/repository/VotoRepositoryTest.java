package com.caioribasregis.desafiovotacao.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.Voto;

@DataJpaTest
class VotoRepositoryTest {

	@Autowired
	private PautaRepository pautaRepository;

	@Autowired
	private VotoRepository votoRepository;

	@Test
	void existsByPautaIdAndCpfAssociado() {
		Pauta pauta = pautaRepository.save(Pauta.builder().titulo("Pauta").build());
		Pauta outraPauta = pautaRepository.save(Pauta.builder().titulo("Outra pauta").build());
		votoRepository.save(Voto.builder().pauta(pauta).cpfAssociado("12345678901").escolha(EscolhaVoto.SIM).build());

		assertThat(votoRepository.existsByPautaIdAndCpfAssociado(pauta.getId(), "12345678901")).isTrue();
		assertThat(votoRepository.existsByPautaIdAndCpfAssociado(pauta.getId(), "10987654321")).isFalse();
		assertThat(votoRepository.existsByPautaIdAndCpfAssociado(outraPauta.getId(), "12345678901")).isFalse();
	}

}
