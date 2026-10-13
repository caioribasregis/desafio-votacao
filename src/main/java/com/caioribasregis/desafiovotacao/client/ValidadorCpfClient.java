package com.caioribasregis.desafiovotacao.client;

import com.caioribasregis.desafiovotacao.exception.CpfInvalidoException;

/**
 * Fachada para o sistema externo que valida o CPF de um associado.
 */
public interface ValidadorCpfClient {

	/**
	 * Consulta se o associado com o CPF indicado pode votar.
	 *
	 * @throws CpfInvalidoException se o CPF for inválido
	 */
	StatusVotoAssociado consultarStatus(String cpf);

}
