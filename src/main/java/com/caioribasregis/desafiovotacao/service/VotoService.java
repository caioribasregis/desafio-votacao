package com.caioribasregis.desafiovotacao.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caioribasregis.desafiovotacao.client.StatusVotoAssociado;
import com.caioribasregis.desafiovotacao.client.ValidadorCpfClient;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.domain.Voto;
import com.caioribasregis.desafiovotacao.dto.VotoRequest;
import com.caioribasregis.desafiovotacao.dto.VotoResponse;
import com.caioribasregis.desafiovotacao.exception.AssociadoNaoAptoException;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.exception.RegraNegocioException;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;
import com.caioribasregis.desafiovotacao.repository.VotoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VotoService {

	private final VotoRepository votoRepository;
	private final PautaRepository pautaRepository;
	private final SessaoVotacaoRepository sessaoVotacaoRepository;
	private final ValidadorCpfClient validadorCpfClient;
	private final Clock clock;

	@Transactional
	public VotoResponse votar(Long pautaId, VotoRequest request) {
		Pauta pauta = pautaRepository.findById(pautaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada"));

		SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(pautaId)
				.orElseThrow(() -> new RegraNegocioException("A pauta " + pautaId + " ainda não tem sessão de votação"));

		LocalDateTime agora = LocalDateTime.now(clock);
		if (agora.isBefore(sessao.getDataHoraAbertura())) {
			throw new RegraNegocioException("A sessão de votação da pauta " + pautaId + " ainda não começou");
		}
		if (!agora.isBefore(sessao.getDataHoraFecho())) {
			throw new RegraNegocioException("A sessão de votação da pauta " + pautaId + " está encerrada");
		}

		String cpf = request.cpf();
		if (votoRepository.existsByPautaIdAndCpfAssociado(pautaId, cpf)) {
			throw cpfJaVotou(pautaId);
		}

		if (validadorCpfClient.consultarStatus(cpf) != StatusVotoAssociado.ABLE_TO_VOTE) {
			throw new AssociadoNaoAptoException();
		}

		Voto voto = Voto.builder()
				.pauta(pauta)
				.cpfAssociado(cpf)
				.escolha(request.escolha())
				.build();

		try {
			// flush imediato para que a restrição única (pauta_id, cpf_associado) seja verificada aqui,
			// cobrindo o caso de dois votos concorrentes do mesmo CPF passarem pela verificação acima
			return VotoResponse.from(votoRepository.saveAndFlush(voto));
		} catch (DataIntegrityViolationException e) {
			throw cpfJaVotou(pautaId);
		}
	}

	private static ConflitoException cpfJaVotou(Long pautaId) {
		return new ConflitoException("Este CPF já votou na pauta " + pautaId);
	}

}
