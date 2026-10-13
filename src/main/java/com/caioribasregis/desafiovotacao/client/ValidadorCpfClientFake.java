package com.caioribasregis.desafiovotacao.client;

import java.util.Random;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.caioribasregis.desafiovotacao.exception.CpfInvalidoException;

/**
 * Implementação fake do validador de CPF: o resultado é aleatório.
 * Metade das consultas devolve CPF inválido; das restantes, metade está apta a votar.
 */
@Component
public class ValidadorCpfClientFake implements ValidadorCpfClient {

	private final RandomGenerator random;

	public ValidadorCpfClientFake() {
		this(new Random());
	}

	ValidadorCpfClientFake(RandomGenerator random) {
		this.random = random;
	}

	@Override
	public StatusVotoAssociado consultarStatus(String cpf) {
		if (!random.nextBoolean()) {
			throw new CpfInvalidoException(cpf);
		}
		return random.nextBoolean() ? StatusVotoAssociado.ABLE_TO_VOTE : StatusVotoAssociado.UNABLE_TO_VOTE;
	}

}
