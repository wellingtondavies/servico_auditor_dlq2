package com.example.auditor.mapper;

/** O corpo da mensagem não é um JSON de pedido válido. */
public class InvalidPayloadException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidPayloadException(String message, Throwable cause) {
		super(message, cause);
	}
}
