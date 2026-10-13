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
import com.caioribasregis.desafiovotacao.domain.StatusVotacao;
import com.caioribasregis.desafiovotacao.domain.Voto;
import com.caioribasregis.desafiovotacao.dto.VotoRequest;
import com.caioribasregis.desafiovotacao.dto.VotoResponse;
import com.caioribasregis.desafiovotacao.exception.AssociadoNaoAptoException;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.CpfInvalidoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.exception.RegraNegocioException;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;
import com.caioribasregis.desafiovotacao.repository.VotoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registo de votos. Os logs nunca incluem o CPF do associado, por ser um dado pessoal.
 */
@Slf4j
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
				.orElseThrow(() -> recusa(pautaId, "pauta inexistente",
						new RecursoNaoEncontradoException("Pauta " + pautaId + " não encontrada")));

		SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(pautaId)
				.orElseThrow(() -> recusa(pautaId, "pauta sem sessão de votação",
						new RegraNegocioException("A pauta " + pautaId + " ainda não tem sessão de votação")));

		switch (StatusVotacao.de(sessao, LocalDateTime.now(clock))) {
			case NAO_INICIADA -> throw recusa(pautaId, "sessão ainda não começou",
					new RegraNegocioException("A sessão de votação da pauta " + pautaId + " ainda não começou"));
			case ENCERRADA -> throw recusa(pautaId, "sessão encerrada",
					new RegraNegocioException("A sessão de votação da pauta " + pautaId + " está encerrada"));
			case EM_ANDAMENTO -> { }
		}

		String cpf = request.cpf();
		if (votoRepository.existsByPautaIdAndCpfAssociado(pautaId, cpf)) {
			throw recusa(pautaId, "CPF já votou nesta pauta", cpfJaVotou(pautaId));
		}

		StatusVotoAssociado status;
		try {
			status = validadorCpfClient.consultarStatus(cpf);
		} catch (CpfInvalidoException e) {
			throw recusa(pautaId, "CPF inválido segundo o serviço de validação", e);
		}
		if (status != StatusVotoAssociado.ABLE_TO_VOTE) {
			throw recusa(pautaId, "associado não apto a votar (" + status + ")", new AssociadoNaoAptoException());
		}

		Voto voto = Voto.builder()
				.pauta(pauta)
				.cpfAssociado(cpf)
				.escolha(request.escolha())
				.build();

		Voto gravado;
		try {
			// flush imediato para que a restrição única (pauta_id, cpf_associado) seja verificada aqui,
			// cobrindo o caso de dois votos concorrentes do mesmo CPF passarem pela verificação acima
			gravado = votoRepository.saveAndFlush(voto);
		} catch (DataIntegrityViolationException e) {
			throw recusa(pautaId, "CPF já votou nesta pauta (voto concorrente)", cpfJaVotou(pautaId));
		}

		log.info("Voto registado: votoId={}, pautaId={}, escolha={}", gravado.getId(), pautaId, gravado.getEscolha());
		return VotoResponse.from(gravado);
	}

	private static <E extends RuntimeException> E recusa(Long pautaId, String motivo, E excecao) {
		log.warn("Voto recusado: pautaId={}, motivo={}", pautaId, motivo);
		return excecao;
	}

	private static ConflitoException cpfJaVotou(Long pautaId) {
		return new ConflitoException("Este CPF já votou na pauta " + pautaId);
	}

}
