package com.caioribasregis.desafiovotacao.controller;

/**
 * Prefixos de versão da API. A versão faz parte do caminho (ex.: {@code /api/v1/pautas}).
 *
 * <p>Só mudanças incompatíveis geram uma nova versão (por exemplo, {@code V2 = "/api/v2"}),
 * com controllers e DTOs próprios. Os serviços são reaproveitados e a versão anterior continua
 * funcionando durante o período de transição. Veja a seção "Versionamento da API" no README.
 */
public final class ApiVersao {

	public static final String V1 = "/api/v1";

	private ApiVersao() {
	}

}
