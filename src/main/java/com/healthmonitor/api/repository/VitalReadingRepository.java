package com.healthmonitor.api.repository;

import com.healthmonitor.api.model.VitalReading;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalReadingRepository extends JpaRepository<VitalReading, Long> {
    List<VitalReading> findByPatientIdOrderByRecordedAtDesc(Long patientId);

    Optional<VitalReading> findTopByPatientIdOrderByRecordedAtDesc(Long patientId);
}
