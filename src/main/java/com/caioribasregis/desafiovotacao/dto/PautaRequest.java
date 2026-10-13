package com.caioribasregis.desafiovotacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastrar uma pauta")
public record PautaRequest(
		@Schema(description = "Título da pauta", example = "Compra da nova sede",
				requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
		@NotBlank(message = "O título é obrigatório")
		@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
		String titulo,

		@Schema(description = "Descrição detalhada da pauta (opcional)",
				example = "Aprovar a compra do imóvel para a nova sede da cooperativa", maxLength = 2000)
		@Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
		String descricao) {
}
