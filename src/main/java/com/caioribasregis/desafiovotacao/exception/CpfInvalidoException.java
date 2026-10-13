package com.caioribasregis.desafiovotacao.exception;

/**
 * CPF rejeitado pelo validador externo. Estende {@link RecursoNaoEncontradoException}
 * para ser devolvido como HTTP 404. A mensagem não inclui o CPF, por ser um dado pessoal.
 */
public class CpfInvalidoException extends RecursoNaoEncontradoException {

	public CpfInvalidoException() {
		super("CPF inválido");
	}

}
