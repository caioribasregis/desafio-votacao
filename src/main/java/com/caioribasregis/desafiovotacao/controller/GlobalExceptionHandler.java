package com.caioribasregis.desafiovotacao.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.caioribasregis.desafiovotacao.exception.AssociadoNaoAptoException;
import com.caioribasregis.desafiovotacao.exception.ConflitoException;
import com.caioribasregis.desafiovotacao.exception.RecursoNaoEncontradoException;
import com.caioribasregis.desafiovotacao.exception.RegraNegocioException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
		Map<String, String> erros = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
		problema.setProperty("erros", erros);
		return problema;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleCorpoIlegivel(HttpMessageNotReadableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Corpo do pedido inválido ou com valores não reconhecidos");
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail handleNaoEncontrado(RecursoNaoEncontradoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflitoException.class)
	public ProblemDetail handleConflito(ConflitoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(RegraNegocioException.class)
	public ProblemDetail handleRegraNegocio(RegraNegocioException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
	}

	@ExceptionHandler(AssociadoNaoAptoException.class)
	public ProblemDetail handleAssociadoNaoApto(AssociadoNaoAptoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
	}

}
