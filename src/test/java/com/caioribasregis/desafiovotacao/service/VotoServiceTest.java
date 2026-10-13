package com.caioribasregis.desafiovotacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.caioribasregis.desafiovotacao.client.StatusVotoAssociado;
import com.caioribasregis.desafiovotacao.client.ValidadorCpfClient;
import com.caioribasregis.desafiovotacao.domain.EscolhaVoto;
import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.domain.SessaoVotacao;
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

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

	private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
	private static final LocalDateTime ABERTURA = LocalDateTime.of(2026, 10, 13, 18, 0);
	private static final LocalDateTime FECHO = ABERTURA.plusMinutes(1);
	private static final String CPF = "12345678901";
	private static final VotoRequest REQUEST = new VotoRequest(CPF, EscolhaVoto.SIM);

	@Mock
	private VotoRepository votoRepository;

	@Mock
	private PautaRepository pautaRepository;

	@Mock
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Mock
	private ValidadorCpfClient validadorCpfClient;

	private final Pauta pauta = Pauta.builder().id(1L).titulo("Pauta").build();
	private final SessaoVotacao sessao = SessaoVotacao.builder()
			.id(10L).pauta(pauta).dataHoraAbertura(ABERTURA).dataHoraFecho(FECHO).build();

	private VotoService service;

	@BeforeEach
	void setUp() {
		emInstante(ABERTURA.plusSeconds(30));
	}

	@Test
	void votoBemSucedidoEGravado() {
		pautaComSessao();
		when(validadorCpfClient.consultarStatus(CPF)).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);
		when(votoRepository.saveAndFlush(any(Voto.class))).thenAnswer(invocacao -> {
			Voto voto = invocacao.getArgument(0);
			voto.setId(100L);
			return voto;
		});

		VotoResponse response = service.votar(1L, REQUEST);

		ArgumentCaptor<Voto> captor = ArgumentCaptor.forClass(Voto.class);
		verify(votoRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getPauta()).isSameAs(pauta);
		assertThat(captor.getValue().getCpfAssociado()).isEqualTo(CPF);
		assertThat(captor.getValue().getEscolha()).isEqualTo(EscolhaVoto.SIM);
		assertThat(response).isEqualTo(new VotoResponse(100L, 1L, CPF, EscolhaVoto.SIM));
	}

	@Test
	void recusaPautaInexistente() {
		when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.votar(99L, REQUEST))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Pauta 99 não encontrada");
		verifyNoInteractions(sessaoVotacaoRepository, validadorCpfClient);
		verify(votoRepository, never()).saveAndFlush(any());
	}

	@Test
	void recusaPautaSemSessao() {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(sessaoVotacaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A pauta 1 ainda não tem sessão de votação");
		verifyNoInteractions(votoRepository, validadorCpfClient);
	}

	@Test
	void recusaSessaoEncerrada() {
		emInstante(FECHO.plusSeconds(1));
		pautaComSessao();

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A sessão de votação da pauta 1 está encerrada");
		verifyNoInteractions(votoRepository, validadorCpfClient);
	}

	@Test
	void recusaSessaoQueAindaNaoComecou() {
		emInstante(ABERTURA.minusSeconds(1));
		pautaComSessao();

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A sessão de votação da pauta 1 ainda não começou");
		verifyNoInteractions(votoRepository, validadorCpfClient);
	}

	@Test
	void recusaVotoNoInstanteExatoDoFecho() {
		emInstante(FECHO);
		pautaComSessao();

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("A sessão de votação da pauta 1 está encerrada");
	}

	@Test
	void recusaCpfQueJaVotou() {
		pautaComSessao();
		when(votoRepository.existsByPautaIdAndCpfAssociado(1L, CPF)).thenReturn(true);

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(ConflitoException.class)
				.hasMessage("Este CPF já votou na pauta 1");
		verifyNoInteractions(validadorCpfClient);
		verify(votoRepository, never()).saveAndFlush(any());
	}

	@Test
	void recusaCpfInvalido() {
		pautaComSessao();
		when(validadorCpfClient.consultarStatus(CPF)).thenThrow(new CpfInvalidoException());

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(CpfInvalidoException.class)
				.hasMessage("CPF inválido");
		verify(votoRepository, never()).saveAndFlush(any());
	}

	@Test
	void recusaAssociadoNaoApto() {
		pautaComSessao();
		when(validadorCpfClient.consultarStatus(CPF)).thenReturn(StatusVotoAssociado.UNABLE_TO_VOTE);

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(AssociadoNaoAptoException.class)
				.hasMessage("O associado não está apto a votar");
		verify(votoRepository, never()).saveAndFlush(any());
	}

	@Test
	void recusaVotoConcorrenteQueViolaRestricaoUnica() {
		pautaComSessao();
		when(validadorCpfClient.consultarStatus(CPF)).thenReturn(StatusVotoAssociado.ABLE_TO_VOTE);
		when(votoRepository.saveAndFlush(any(Voto.class)))
				.thenThrow(new DataIntegrityViolationException("uk_voto_pauta_cpf"));

		assertThatThrownBy(() -> service.votar(1L, REQUEST))
				.isInstanceOf(ConflitoException.class)
				.hasMessage("Este CPF já votou na pauta 1");
	}

	private void pautaComSessao() {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(sessaoVotacaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
	}

	private void emInstante(LocalDateTime instante) {
		Clock clock = Clock.fixed(instante.atZone(ZONA).toInstant(), ZONA);
		service = new VotoService(votoRepository, pautaRepository, sessaoVotacaoRepository, validadorCpfClient, clock);
	}

}
