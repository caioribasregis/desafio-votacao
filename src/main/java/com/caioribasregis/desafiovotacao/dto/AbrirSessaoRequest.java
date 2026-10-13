package com.caioribasregis.desafiovotacao.dto;

import jakarta.validation.constraints.Positive;

public record AbrirSessaoRequest(
		@Positive(message = "A duração deve ser maior que zero")
		Integer duracaoMinutos) {
}
