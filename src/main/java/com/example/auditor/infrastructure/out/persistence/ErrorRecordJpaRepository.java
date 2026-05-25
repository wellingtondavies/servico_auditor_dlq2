package com.example.auditor.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ErrorRecordJpaRepository extends JpaRepository<ErrorRecordEntity, UUID> {
}

