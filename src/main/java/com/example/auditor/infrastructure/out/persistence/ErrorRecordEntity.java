package com.example.auditor.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "error_records")
public class ErrorRecordEntity {

	@Id
	@Column(name = "error_id", nullable = false, updatable = false)
	private UUID errorId;

	@Column(name = "queue_name", nullable = false)
	private String queueName;

	@Column(name = "payload", nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Column(name = "timestamp", nullable = false)
	private Instant timestamp;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "severity", nullable = false)
	private String severity;

	public UUID getErrorId() {
		return errorId;
	}

	public void setErrorId(UUID errorId) {
		this.errorId = errorId;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}
}

