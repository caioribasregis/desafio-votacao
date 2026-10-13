package com.caioribasregis.desafiovotacao.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.caioribasregis.desafiovotacao.exception.CpfInvalidoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;

class ValidadorCpfClientFakeTest {

	private static final String CPF = "12345678901";

	private final RandomGenerator random = mock(RandomGenerator.class);
	private final ValidadorCpfClientFake validador = new ValidadorCpfClientFake(random);

	@Test
	void cpfInvalidoLancaExcecaoQueResultaEm404() {
		when(random.nextBoolean()).thenReturn(false);

		assertThatThrownBy(() -> validador.consultarStatus(CPF))
				.isInstanceOf(CpfInvalidoException.class)
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("CPF inválido");
	}

	@Test
	void cpfValidoEAptoDevolveAbleToVote() {
		when(random.nextBoolean()).thenReturn(true, true);

		assertThat(validador.consultarStatus(CPF)).isEqualTo(StatusVotoAssociado.ABLE_TO_VOTE);
	}

	@Test
	void cpfValidoENaoAptoDevolveUnableToVote() {
		when(random.nextBoolean()).thenReturn(true, false);

		assertThat(validador.consultarStatus(CPF)).isEqualTo(StatusVotoAssociado.UNABLE_TO_VOTE);
	}

	@Test
	void comAleatoriedadeRealProduzOsTresResultados() {
		ValidadorCpfClientFake validadorReal = new ValidadorCpfClientFake(new Random(42));
		Map<StatusVotoAssociado, Integer> status = new EnumMap<>(StatusVotoAssociado.class);
		int invalidos = 0;

		for (int i = 0; i < 1000; i++) {
			try {
				status.merge(validadorReal.consultarStatus(CPF), 1, Integer::sum);
			} catch (CpfInvalidoException e) {
				invalidos++;
			}
		}

		assertThat(invalidos).isPositive();
		assertThat(status).containsOnlyKeys(StatusVotoAssociado.values());
	}

}
