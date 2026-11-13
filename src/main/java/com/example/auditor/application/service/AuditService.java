package com.example.auditor.application.service;

import com.example.auditor.application.ports.in.service.AuditServicePort;
import com.example.auditor.application.ports.out.ErrorRecordRepositoryPort;
import com.example.auditor.core.domain.ErrorRecord;
import com.example.auditor.core.domain.Severity;
import com.example.auditor.core.domain.Status;
import com.example.auditor.dto.OrderEventDTO;
import com.example.auditor.dto.OrderItemDTO;
import com.example.auditor.mapper.OrderEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuditService implements AuditServicePort {

	private final ErrorRecordRepositoryPort repository;
	private final Clock clock;

	@Autowired
	public AuditService(ErrorRecordRepositoryPort repository) {
		this(repository, Clock.systemUTC());
	}

	/** Construtor com relógio injetável, usado nos testes para fixar o timestamp. */
	AuditService(ErrorRecordRepositoryPort repository, Clock clock) {
		this.repository = Objects.requireNonNull(repository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public ErrorRecord createFromDlq(String rawPayload, String queueName) {
		OrderEventDTO dto = OrderEventMapper.fromJson(rawPayload);
		int totalItems = totalItems(dto.getOrderItems());

		ErrorRecord record = new ErrorRecord(
				UUID.randomUUID(),
				queueName,
				rawPayload,
				Instant.now(clock),
				Status.PENDING_ANALYSIS,
				Severity.fromTotalItems(totalItems)
		);

		return repository.save(record);
	}

	/** Soma as quantidades dos itens; lista ou quantidade ausente contam como zero. */
	static int totalItems(List<OrderItemDTO> items) {
		if (items == null) {
			return 0;
		}
		return items.stream()
				.filter(Objects::nonNull)
				.mapToInt(item -> item.getAmount() == null ? 0 : item.getAmount())
				.sum();
	}
}
