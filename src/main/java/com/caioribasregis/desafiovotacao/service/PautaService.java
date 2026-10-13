package com.caioribasregis.desafiovotacao.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.dto.PautaRequest;
import com.caioribasregis.desafiovotacao.dto.PautaResponse;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PautaService {

	private final PautaRepository pautaRepository;

	@Transactional
	public PautaResponse criar(PautaRequest request) {
		Pauta pauta = Pauta.builder()
				.titulo(request.titulo().trim())
				.descricao(request.descricao())
				.build();
		return PautaResponse.from(pautaRepository.save(pauta));
	}

}
