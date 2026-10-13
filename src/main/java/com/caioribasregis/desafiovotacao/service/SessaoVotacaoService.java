package com.caioribasregis.desafiovotacao.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.dto.SessaoVotacaoResponse;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessaoVotacaoService {

	static final int DURACAO_PADRAO_MINUTOS = 1;

	private final SessaoVotacaoRepository sessaoVotacaoRepository;
	private final PautaRepository pautaRepository;
	private final Clock clock;

	@Transactional
	public SessaoVotacaoResponse abrir(Long pautaId, Integer duracaoMinutos) {
		Pauta pauta = pautaRepository.findById(pautaId)
				.orElseThrow(() -> {
					log.warn("Abertura de sessão recusada: pautaId={}, motivo=pauta inexistente", pautaId);
					return new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada");
				});

		if (sessaoVotacaoRepository.existsByPautaId(pautaId)) {
			throw sessaoJaAberta(pautaId);
		}

		int duracao = duracaoMinutos != null ? duracaoMinutos : DURACAO_PADRAO_MINUTOS;
		LocalDateTime abertura = LocalDateTime.now(clock);
		SessaoVotacao sessao = SessaoVotacao.builder()
				.pauta(pauta)
				.dataHoraAbertura(abertura)
				.dataHoraFecho(abertura.plusMinutes(duracao))
				.build();

		SessaoVotacao gravada;
		try {
			// flush imediato para que a restrição única de pauta_id seja verificada aqui,
			// cobrindo o caso de dois pedidos concorrentes passarem pela verificação acima
			gravada = sessaoVotacaoRepository.saveAndFlush(sessao);
		} catch (DataIntegrityViolationException e) {
			throw sessaoJaAberta(pautaId);
		}

		log.info("Sessão de votação aberta: sessaoId={}, pautaId={}, duracaoMinutos={}{}, fecho={}",
				gravada.getId(), pautaId, duracao, duracaoMinutos == null ? " (padrão)" : "", gravada.getDataHoraFecho());
		return SessaoVotacaoResponse.from(gravada);
	}

	private static ConflitoException sessaoJaAberta(Long pautaId) {
		log.warn("Abertura de sessão recusada: pautaId={}, motivo=a pauta já tem sessão", pautaId);
		return new ConflitoException("A pauta " + pautaId + " já tem uma sessão de votação");
	}

}
