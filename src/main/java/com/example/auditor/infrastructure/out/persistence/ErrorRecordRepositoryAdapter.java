package com.example.auditor.infrastructure.out.persistence;

import com.example.auditor.application.ports.out.ErrorRecordRepositoryPort;
import com.example.auditor.core.domain.ErrorRecord;
import com.example.auditor.core.domain.Severity;
import com.example.auditor.core.domain.Status;
import org.springframework.stereotype.Repository;

@Repository
public class ErrorRecordRepositoryAdapter implements ErrorRecordRepositoryPort {

	private final ErrorRecordJpaRepository repository;

	public ErrorRecordRepositoryAdapter(ErrorRecordJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public ErrorRecord save(ErrorRecord record) {
		ErrorRecordEntity entity = new ErrorRecordEntity();
		entity.setErrorId(record.getErrorId());
		entity.setQueueName(record.getQueueName());
		entity.setPayload(record.getPayload());
		entity.setTimestamp(record.getTimestamp());
		entity.setStatus(record.getStatus().name());
		entity.setSeverity(record.getSeverity().name());

		ErrorRecordEntity saved = repository.save(entity);

		return new ErrorRecord(
				saved.getErrorId(),
				saved.getQueueName(),
				saved.getPayload(),
				saved.getTimestamp(),
				Status.valueOf(saved.getStatus()),
				Severity.valueOf(saved.getSeverity())
		);
	}
}

