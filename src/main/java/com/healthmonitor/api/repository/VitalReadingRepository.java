package com.healthmonitor.api.repository;

import com.healthmonitor.api.model.VitalReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VitalReadingRepository extends JpaRepository<VitalReading, Long> {
    List<VitalReading> findByPatientIdOrderByRecordedAtDesc(String patientId);
}
