package com.caioribasregis.desafiovotacao.exception;

/**
 * O associado não pode votar. A mensagem não inclui o CPF, por ser um dado pessoal.
 */
public class AssociadoNaoAptoException extends RuntimeException {

	public AssociadoNaoAptoException() {
		super("O associado não está apto a votar");
	}

}
