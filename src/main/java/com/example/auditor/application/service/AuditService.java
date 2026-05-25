package com.example.auditor.application.service;

import com.example.auditor.application.ports.in.service.AuditServicePort;
import com.example.auditor.application.ports.out.ErrorRecordRepositoryPort;
import com.example.auditor.core.domain.ErrorRecord;
import com.example.auditor.core.domain.Severity;
import com.example.auditor.core.domain.Status;
import com.example.auditor.dto.OrderEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuditService implements AuditServicePort {

	private final ErrorRecordRepositoryPort repository;
	private final ObjectMapper mapper = new ObjectMapper();

	public AuditService(ErrorRecordRepositoryPort repository) {
		this.repository = Objects.requireNonNull(repository);
	}

	@Override
	public ErrorRecord createFromDlq(String rawPayload, String queueName) {
		try {
			OrderEventDTO dto = mapper.readValue(rawPayload, OrderEventDTO.class);
			int total = dto.getOrderItems() == null ? 0 : dto.getOrderItems().stream()
					.mapToInt(i -> i.getAmount() == null ? 0 : i.getAmount())
					.sum();

			Severity severity;
			if (total > 100) severity = Severity.HIGH;
			else if (total >= 50) severity = Severity.MEDIUM;
			else severity = Severity.LOW;

			ErrorRecord record = new ErrorRecord(
					UUID.randomUUID(),
					queueName,
					rawPayload,
					Instant.now(),
					Status.PENDING_ANALYSIS,
					severity
			);

			return repository.save(record);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao processar payload DLQ", e);
		}
	}
}
