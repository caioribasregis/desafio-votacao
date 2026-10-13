package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Voto de um associado")
public record VotoRequest(
		@Schema(description = "CPF do associado, só com os 11 dígitos", example = "12345678901",
				requiredMode = Schema.RequiredMode.REQUIRED, pattern = "\\d{11}")
		@NotBlank(message = "O CPF é obrigatório")
		@Pattern(regexp = "\\d{11}", message = "O CPF deve ter 11 dígitos numéricos")
		String cpf,

		@Schema(description = "Escolha do associado", example = "SIM", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "A escolha é obrigatória (SIM ou NAO)")
		EscolhaVoto escolha) {
}
