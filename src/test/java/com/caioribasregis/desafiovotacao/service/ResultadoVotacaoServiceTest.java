package com.caioribasregis.desafiovotacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.caioribasregis.desafiovotacao.repository.VotoRepository.ContagemPorEscolha;

@ExtendWith(MockitoExtension.class)
class ResultadoVotacaoServiceTest {

	private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
	private static final LocalDateTime ABERTURA = LocalDateTime.of(2026, 10, 13, 18, 0);
	private static final LocalDateTime FECHO = ABERTURA.plusMinutes(1);

	@Mock
	private PautaRepository pautaRepository;

	@Mock
	private SessaoVotacaoRepository sessaoVotacaoRepository;

	@Mock
	private VotoRepository votoRepository;

	private ResultadoVotacaoService service;

	private final Pauta pauta = Pauta.builder().id(1L).titulo("Nova sede").build();
	private final SessaoVotacao sessao = SessaoVotacao.builder()
			.id(10L).pauta(pauta).dataHoraAbertura(ABERTURA).dataHoraFecho(FECHO).build();

	@BeforeEach
	void setUp() {
		// por omissão a consulta é feita depois do fecho, com a votação encerrada
		emInstante(FECHO.plusMinutes(5));
		lenient().when(sessaoVotacaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
	}

	// --- contagem e resultado ---

	@Test
	void maioriaSimAprova() {
		comVotos(contagem(EscolhaVoto.SIM, 3), contagem(EscolhaVoto.NAO, 1));

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 3, 1, 4, ResultadoVotacao.APROVADA, StatusVotacao.ENCERRADA));
	}

	@Test
	void maioriaNaoReprova() {
		comVotos(contagem(EscolhaVoto.SIM, 1), contagem(EscolhaVoto.NAO, 2));

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 1, 2, 3, ResultadoVotacao.REPROVADA, StatusVotacao.ENCERRADA));
	}

	@Test
	void mesmaQuantidadeDeSimENaoEmpata() {
		comVotos(contagem(EscolhaVoto.SIM, 2), contagem(EscolhaVoto.NAO, 2));

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 2, 2, 4, ResultadoVotacao.EMPATE, StatusVotacao.ENCERRADA));
	}

	@Test
	void pautaSemVotos() {
		comVotos();

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 0, 0, 0, ResultadoVotacao.SEM_VOTOS, StatusVotacao.ENCERRADA));
	}

	@Test
	void apenasVotosSimContaNaoComoZero() {
		comVotos(contagem(EscolhaVoto.SIM, 5));

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 5, 0, 5, ResultadoVotacao.APROVADA, StatusVotacao.ENCERRADA));
	}

	@Test
	void pautaInexistenteLancaNaoEncontrado() {
		when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.consultar(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Pauta 99 não encontrada");
		verifyNoInteractions(votoRepository);
	}

	// --- estado da votação ---

	@Test
	void sessaoAindaAbertaDevolveEmAndamentoComResultadoParcial() {
		emInstante(ABERTURA.plusSeconds(30));
		comVotos(contagem(EscolhaVoto.SIM, 1));

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 1, 0, 1, ResultadoVotacao.APROVADA, StatusVotacao.EM_ANDAMENTO));
	}

	@Test
	void sessaoDepoisDoFechoDevolveEncerrada() {
		emInstante(FECHO.plusSeconds(1));
		comVotos(contagem(EscolhaVoto.SIM, 1));

		assertThat(service.consultar(1L).statusVotacao()).isEqualTo(StatusVotacao.ENCERRADA);
	}

	@Test
	void noInstanteExatoDoFechoAVotacaoEstaEncerrada() {
		emInstante(FECHO);
		comVotos();

		assertThat(service.consultar(1L).statusVotacao()).isEqualTo(StatusVotacao.ENCERRADA);
	}

	@Test
	void pautaSemSessaoDevolveNaoIniciada() {
		when(sessaoVotacaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());
		comVotos();

		assertThat(service.consultar(1L)).isEqualTo(
				new ResultadoVotacaoResponse(1L, "Nova sede", 0, 0, 0, ResultadoVotacao.SEM_VOTOS, StatusVotacao.NAO_INICIADA));
	}

	private void emInstante(LocalDateTime instante) {
		Clock clock = Clock.fixed(instante.atZone(ZONA).toInstant(), ZONA);
		service = new ResultadoVotacaoService(pautaRepository, sessaoVotacaoRepository, votoRepository, clock);
	}

	private void comVotos(ContagemPorEscolha... contagens) {
		when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
		when(votoRepository.contarPorEscolha(1L)).thenReturn(List.of(contagens));
	}

	private static ContagemPorEscolha contagem(EscolhaVoto escolha, long quantidade) {
		return new ContagemPorEscolha() {
			@Override
			public EscolhaVoto getEscolha() {
				return escolha;
			}

			@Override
			public long getQuantidade() {
				return quantidade;
			}
		};
	}

}
