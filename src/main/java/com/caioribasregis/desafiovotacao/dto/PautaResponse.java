package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.Pauta;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pauta cadastrada")
public record PautaResponse(
		@Schema(description = "Id da pauta", example = "1") Long id,
		@Schema(description = "Título da pauta", example = "Compra da nova sede") String titulo,
		@Schema(description = "Descrição da pauta",
				example = "Aprovar a compra do imóvel para a nova sede da cooperativa") String descricao) {

	public static PautaResponse from(Pauta pauta) {
		return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
	}

}
