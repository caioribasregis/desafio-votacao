package com.caioribasregis.desafiovotacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
import com.caioribasregis.desafiovotacao.dto.SessaoVotacaoResponse;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;
import com.caioribasregis.desafiovotacao.repository.SessaoVotacaoRepository;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoServiceTest {

	private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
	private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 13, 18, 0);

	@Mock
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Mock
	private PautaRepository pautaRepository;

	private SessaoVotacaoService service;

	private final Pauta pauta = Pauta.builder().id(1L).titulo("Pauta").build();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);
		service = new SessaoVotacaoService(sessaoVotacaoRepository, pautaRepository, clock);
	}

	@Test
	void abrirSemDuracaoUsaUmMinutoPorPadrao() {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class))).thenAnswer(this::comId);

		SessaoVotacaoResponse response = service.abrir(1L, null);

		assertThat(response).isEqualTo(new SessaoVotacaoResponse(10L, 1L, AGORA, AGORA.plusMinutes(1)));
	}

	@Test
	void abrirComDuracaoInformadaCalculaFecho() {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class))).thenAnswer(this::comId);

		SessaoVotacaoResponse response = service.abrir(1L, 30);

		assertThat(response.dataHoraAbertura()).isEqualTo(AGORA);
		assertThat(response.dataHoraFecho()).isEqualTo(AGORA.plusMinutes(30));
	}

	@Test
	void abrirComPautaInexistenteLancaNaoEncontrado() {
		when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.abrir(99L, null))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Pauta 99 não encontrada");
		verify(sessaoVotacaoRepository, never()).saveAndFlush(any());
	}

	@Test
	void abrirComSessaoJaExistenteLancaConflito() {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(sessaoVotacaoRepository.existsByPautaId(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.abrir(1L, null))
				.isInstanceOf(ConflitoException.class)
				.hasMessage("A pauta 1 já tem uma sessão de votação");
		verify(sessaoVotacaoRepository, never()).saveAndFlush(any());
	}

	@Test
	void abrirConcorrenteQueViolaRestricaoUnicaLancaConflito() {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(sessaoVotacaoRepository.saveAndFlush(any(SessaoVotacao.class)))
				.thenThrow(new DataIntegrityViolationException("uk_sessao_votacao_pauta"));

		assertThatThrownBy(() -> service.abrir(1L, null)).isInstanceOf(ConflitoException.class);
	}

	private SessaoVotacao comId(InvocationOnMock invocacao) {
		SessaoVotacao sessao = invocacao.getArgument(0);
		sessao.setId(10L);
		return sessao;
	}

}
