package com.example.auditor.mapper;

import com.example.auditor.dto.OrderEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class OrderEventMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OrderEventMapper() {
    }

    public static OrderEventDTO fromJson(String rawJson) {
        try {
            return OBJECT_MAPPER.readValue(rawJson, OrderEventDTO.class);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Erro ao converter payload bruto para OrderEventDTO", exception);
        }
    }

    public static String toJson(OrderEventDTO dto) {
        try {
            return OBJECT_MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Erro ao converter OrderEventDTO para JSON", exception);
        }
    }
}
