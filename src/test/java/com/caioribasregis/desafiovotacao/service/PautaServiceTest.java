package com.caioribasregis.desafiovotacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.caioribasregis.desafiovotacao.domain.Pauta;
import com.caioribasregis.desafiovotacao.dto.PautaRequest;
import com.caioribasregis.desafiovotacao.dto.PautaResponse;
import com.caioribasregis.desafiovotacao.repository.PautaRepository;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

	@Mock
	private PautaRepository pautaRepository;

	@InjectMocks
	private PautaService pautaService;

	@Test
	void criarGuardaPautaEDevolveResponseComId() {
		when(pautaRepository.save(any(Pauta.class))).thenAnswer(invocacao -> {
			Pauta pauta = invocacao.getArgument(0);
			pauta.setId(1L);
			return pauta;
		});

		PautaResponse response = pautaService.criar(new PautaRequest("  Nova sede  ", "Aprovar a compra da nova sede"));

		ArgumentCaptor<Pauta> captor = ArgumentCaptor.forClass(Pauta.class);
		verify(pautaRepository).save(captor.capture());
		assertThat(captor.getValue().getTitulo()).isEqualTo("Nova sede");
		assertThat(captor.getValue().getDescricao()).isEqualTo("Aprovar a compra da nova sede");

		assertThat(response).isEqualTo(new PautaResponse(1L, "Nova sede", "Aprovar a compra da nova sede"));
	}

}
