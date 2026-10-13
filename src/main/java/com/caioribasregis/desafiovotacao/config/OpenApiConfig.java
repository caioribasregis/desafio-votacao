package com.caioribasregis.desafiovotacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("Desafio Votação API")
				.version("v1")
				.description("""
						API para gerir as votações das assembleias de uma cooperativa.

						Fluxo típico:
						1. Cadastrar uma pauta (`POST /api/v1/pautas`).
						2. Abrir a sessão de votação da pauta (`POST /api/v1/pautas/{pautaId}/sessao`), \
						com duração de 1 minuto por padrão.
						3. Os associados votam SIM ou NAO enquanto a sessão está aberta \
						(`POST /api/v1/pautas/{pautaId}/votos`). Cada CPF vota uma única vez por pauta \
						e é validado por um serviço externo.
						4. Consultar o resultado (`GET /api/v1/pautas/{pautaId}/resultado`).

						Os erros seguem o formato Problem Details (RFC 9457), com a mensagem no campo `detail`."""));
	}

}
