package com.example.auditor.application.ports.in.service;

import com.example.auditor.core.domain.ErrorRecord;

public interface AuditServicePort {

    ErrorRecord createFromDlq(String rawPayload, String queueName);
}
