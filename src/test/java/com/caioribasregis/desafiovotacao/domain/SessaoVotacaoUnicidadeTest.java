package com.caioribasregis.desafiovotacao.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SessaoVotacaoUnicidadeTest {

	@Autowired
	private TestEntityManager em;

	@Test
	void naoPermiteDuasSessoesParaAMesmaPauta() {
		Pauta pauta = em.persist(Pauta.builder().titulo("Pauta").build());
		LocalDateTime agora = LocalDateTime.now();
		em.persistAndFlush(SessaoVotacao.builder().pauta(pauta)
				.dataHoraAbertura(agora).dataHoraFecho(agora.plusMinutes(1)).build());

		SessaoVotacao segunda = SessaoVotacao.builder().pauta(pauta)
				.dataHoraAbertura(agora).dataHoraFecho(agora.plusMinutes(5)).build();

		assertThatThrownBy(() -> em.persistAndFlush(segunda))
				.isInstanceOf(ConstraintViolationException.class);
	}

}
