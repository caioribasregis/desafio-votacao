package com.caioribasregis.desafiovotacao.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

}
