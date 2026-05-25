package com.example.auditor.application.ports.out;

import com.example.auditor.core.domain.ErrorRecord;

public interface ErrorRecordRepositoryPort {

	ErrorRecord save(ErrorRecord record);
}

