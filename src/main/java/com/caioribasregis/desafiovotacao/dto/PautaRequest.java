package com.caioribasregis.desafiovotacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
		@NotBlank(message = "O título é obrigatório")
		@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
		String titulo,

		@Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
		String descricao) {
}
