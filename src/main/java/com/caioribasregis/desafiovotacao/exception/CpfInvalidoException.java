package com.caioribasregis.desafiovotacao.exception;

/**
 * CPF rejeitado pelo validador externo. Estende {@link RecursoNaoEncontradoException}
 * para ser devolvido como HTTP 404.
 */
public class CpfInvalidoException extends RecursoNaoEncontradoException {

	public CpfInvalidoException(String cpf) {
		super("CPF " + cpf + " inválido");
	}

}
