package com.healthmonitor.api.repository;

import com.healthmonitor.api.model.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    Page<AlertRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);
}
