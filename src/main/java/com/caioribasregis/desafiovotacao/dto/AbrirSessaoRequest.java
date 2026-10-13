package com.caioribasregis.desafiovotacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "Dados para abrir a sessão de votação")
public record AbrirSessaoRequest(
		@Schema(description = "Duração da sessão em minutos. Se não for informada, a sessão dura 1 minuto.",
				example = "5", minimum = "1")
		@Positive(message = "A duração deve ser maior que zero")
		Integer duracaoMinutos) {
}
