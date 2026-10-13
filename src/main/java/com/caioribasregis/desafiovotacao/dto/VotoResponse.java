package com.caioribasregis.desafiovotacao.dto;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Voto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Voto registado")
public record VotoResponse(
		@Schema(description = "Id do voto", example = "1") Long id,
		@Schema(description = "Id da pauta", example = "1") Long pautaId,
		@Schema(description = "CPF do associado", example = "12345678901") String cpf,
		@Schema(description = "Escolha registada", example = "SIM") EscolhaVoto escolha) {

	public static VotoResponse from(Voto voto) {
		return new VotoResponse(voto.getId(), voto.getPauta().getId(), voto.getCpfAssociado(), voto.getEscolha());
	}

}
