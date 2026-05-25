package com.example.auditor.core.domain;

import java.time.Instant;
import java.util.UUID;

public class ErrorRecord {

	private UUID errorId;
	private String queueName;
	private String payload;
	private Instant timestamp;
	private Status status;
	private Severity severity;

	public ErrorRecord() {
	}

	public ErrorRecord(UUID errorId, String queueName, String payload, Instant timestamp, Status status, Severity severity) {
		this.errorId = errorId;
		this.queueName = queueName;
		this.payload = payload;
		this.timestamp = timestamp;
		this.status = status;
		this.severity = severity;
	}

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

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Severity getSeverity() {
		return severity;
	}

	public void setSeverity(Severity severity) {
		this.severity = severity;
	}
}

