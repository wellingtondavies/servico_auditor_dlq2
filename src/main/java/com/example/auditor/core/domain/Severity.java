package com.example.auditor.core.domain;

/**
 * Gravidade de um pedido que caiu na DLQ, definida pela quantidade total de itens.
 * A regra fica no domínio para que qualquer porta de entrada classifique igual.
 */
public enum Severity {
	LOW,
	MEDIUM,
	HIGH;

	/** Acima deste total, o pedido é de alta gravidade. */
	public static final int LIMITE_ALTA = 100;

	/** A partir deste total (inclusive), o pedido é de gravidade média. */
	public static final int LIMITE_MEDIA = 50;

	public static Severity fromTotalItems(int totalItems) {
		if (totalItems > LIMITE_ALTA) {
			return HIGH;
		}
		if (totalItems >= LIMITE_MEDIA) {
			return MEDIUM;
		}
		return LOW;
	}
}
