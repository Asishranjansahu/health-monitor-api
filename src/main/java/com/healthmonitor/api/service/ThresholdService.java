package com.healthmonitor.api.service;

import com.healthmonitor.api.model.PatientThreshold;
import com.healthmonitor.api.model.Thresholds;
import com.healthmonitor.api.repository.PatientThresholdRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves the alert limits for a patient: the global defaults from
 * application.properties, overridden by any per-patient row in the database.
 */
@Service
public class ThresholdService {

    private final PatientThresholdRepository repository;
    private final Thresholds defaults;

    public ThresholdService(
        PatientThresholdRepository repository,
        @Value("${healthmonitor.alert.heart-rate-min}") int heartRateMin,
        @Value("${healthmonitor.alert.heart-rate-max}") int heartRateMax,
        @Value("${healthmonitor.alert.spo2-min}") int spo2Min,
        @Value("${healthmonitor.alert.temperature-max}") double temperatureMax
    ) {
        this.repository = repository;
        this.defaults = new Thresholds(heartRateMin, heartRateMax, spo2Min, temperatureMax);
    }

    /** Effective limits for a patient, merging any overrides with the defaults. */
    public Thresholds effective(Long patientId) {
        PatientThreshold row = repository.findByPatientId(patientId).orElse(null);
        if (row == null) {
            return defaults;
        }
        return new Thresholds(
            row.getHeartRateMin() != null ? row.getHeartRateMin() : defaults.heartRateMin(),
            row.getHeartRateMax() != null ? row.getHeartRateMax() : defaults.heartRateMax(),
            row.getSpo2Min() != null ? row.getSpo2Min() : defaults.spo2Min(),
            row.getTemperatureMax() != null ? row.getTemperatureMax() : defaults.temperatureMax()
        );
    }

    public Thresholds defaults() {
        return defaults;
    }

    public Optional<PatientThreshold> findRow(Long patientId) {
        return repository.findByPatientId(patientId);
    }

    public PatientThreshold save(PatientThreshold row) {
        return repository.save(row);
    }

    public void delete(Long patientId) {
        repository.deleteByPatientId(patientId);
    }
}
