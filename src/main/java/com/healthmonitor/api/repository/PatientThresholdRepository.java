package com.healthmonitor.api.repository;

import com.healthmonitor.api.model.PatientThreshold;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PatientThresholdRepository extends JpaRepository<PatientThreshold, Long> {
    Optional<PatientThreshold> findByPatientId(Long patientId);

    @Transactional
    void deleteByPatientId(Long patientId);
}
