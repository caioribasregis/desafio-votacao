package com.caioribasregis.desafiovotacao.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sessao_votacao", uniqueConstraints = @UniqueConstraint(
		name = "uk_sessao_votacao_pauta", columnNames = "pauta_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoVotacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pauta_id", nullable = false)
	private Pauta pauta;

	@Column(name = "data_hora_abertura", nullable = false)
	private LocalDateTime dataHoraAbertura;

	@Column(name = "data_hora_fecho", nullable = false)
	private LocalDateTime dataHoraFecho;

}
