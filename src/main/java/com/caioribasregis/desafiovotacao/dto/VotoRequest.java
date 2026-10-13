package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VotoRequest(
		@NotBlank(message = "O CPF é obrigatório")
		@Pattern(regexp = "\\d{11}", message = "O CPF deve ter 11 dígitos numéricos")
		String cpf,

		@NotNull(message = "A escolha é obrigatória (SIM ou NAO)")
		EscolhaVoto escolha) {
}
