package com.example.auditor.mapper;

import com.example.auditor.dto.OrderEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class OrderEventMapper {

	// Campos extras no payload não devem impedir a auditoria da mensagem.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private OrderEventMapper() {
	}

	public static OrderEventDTO fromJson(String rawJson) {
		if (rawJson == null || rawJson.isBlank()) {
			throw new InvalidPayloadException("Payload da DLQ vazio", null);
		}
		try {
			OrderEventDTO dto = OBJECT_MAPPER.readValue(rawJson, OrderEventDTO.class);
			if (dto == null) {
				throw new InvalidPayloadException("Payload da DLQ é JSON null", null);
			}
			return dto;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new InvalidPayloadException("Erro ao converter payload bruto para OrderEventDTO", exception);
		}
	}

	public static String toJson(OrderEventDTO dto) {
		try {
			return OBJECT_MAPPER.writeValueAsString(dto);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Erro ao converter OrderEventDTO para JSON", exception);
		}
	}
}
