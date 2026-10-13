package com.caioribasregis.desafiovotacao.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.ResultadoVotacao;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.domain.StatusVotacao;
import com.caioribasregis.desafiovotacao.dto.ResultadoVotacaoResponse;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;
import com.caioribasregis.desafiovotacao.repository.VotoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResultadoVotacaoService {

	private final PautaRepository pautaRepository;
	private final SessaoVotacaoRepository sessaoVotacaoRepository;
	private final VotoRepository votoRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public ResultadoVotacaoResponse consultar(Long pautaId) {
		Pauta pauta = pautaRepository.findById(pautaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada"));

		SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(pautaId).orElse(null);
		StatusVotacao status = StatusVotacao.de(sessao, LocalDateTime.now(clock));

		Map<EscolhaVoto, Long> contagem = new EnumMap<>(EscolhaVoto.class);
		votoRepository.contarPorEscolha(pautaId)
				.forEach(c -> contagem.put(c.getEscolha(), c.getQuantidade()));

		long votosSim = contagem.getOrDefault(EscolhaVoto.SIM, 0L);
		long votosNao = contagem.getOrDefault(EscolhaVoto.NAO, 0L);

		return new ResultadoVotacaoResponse(pauta.getId(), pauta.getTitulo(), votosSim, votosNao,
				votosSim + votosNao, ResultadoVotacao.de(votosSim, votosNao), status);
	}

}
