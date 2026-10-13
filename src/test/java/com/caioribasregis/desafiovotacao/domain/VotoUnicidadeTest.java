package com.caioribasregis.desafiovotacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@DataJpaTest
@ExtendWith(OutputCaptureExtension.class)
class VotoUnicidadeTest {

	@Autowired
	private TestEntityManager em;

	@Test
	void naoPermiteMesmoCpfVotarDuasVezesNaMesmaPauta(CapturedOutput output) {
		Pauta pauta = em.persist(Pauta.builder().titulo("Pauta").build());
		em.persistAndFlush(Voto.builder().pauta(pauta).cpfAssociado("12345678901").escolha(EscolhaVoto.SIM).build());

		Voto duplicado = Voto.builder().pauta(pauta).cpfAssociado("12345678901").escolha(EscolhaVoto.NAO).build();

		assertThatThrownBy(() -> em.persistAndFlush(duplicado))
				.isInstanceOf(ConstraintViolationException.class);
		assertThat(output).doesNotContain("12345678901");
	}

	@Test
	void permiteMesmoCpfVotarEmPautasDiferentes() {
		Pauta pauta1 = em.persist(Pauta.builder().titulo("Pauta 1").build());
		Pauta pauta2 = em.persist(Pauta.builder().titulo("Pauta 2").build());

		em.persistAndFlush(Voto.builder().pauta(pauta1).cpfAssociado("12345678901").escolha(EscolhaVoto.SIM).build());
		em.persistAndFlush(Voto.builder().pauta(pauta2).cpfAssociado("12345678901").escolha(EscolhaVoto.NAO).build());
	}

}
