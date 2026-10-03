package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.PatientThreshold;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.service.ThresholdService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-patient alert thresholds. GET returns the effective limits (with a
 * source flag), PUT upserts an override, DELETE falls back to the defaults.
 * Writes are restricted to the DOCTOR role.
 */
@RestController
@RequestMapping("/api/thresholds")
public class ThresholdController {

    public record ThresholdView(
        Long patientId,
        int heartRateMin,
        int heartRateMax,
        int spo2Min,
        double temperatureMax,
        String source
    ) {
    }

    public record ThresholdUpsert(
        @NotNull @Min(0) @Max(300) Integer heartRateMin,
        @NotNull @Min(0) @Max(300) Integer heartRateMax,
        @NotNull @Min(0) @Max(100) Integer spo2Min,
        @NotNull @Min(0) @Max(50) Double temperatureMax
    ) {
    }

    private final ThresholdService thresholdService;
    private final PatientRepository patientRepository;

    public ThresholdController(ThresholdService thresholdService, PatientRepository patientRepository) {
        this.thresholdService = thresholdService;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/{patientId}")
    public ThresholdView get(@PathVariable Long patientId) {
        return toView(patientId);
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ThresholdView> upsert(
        @PathVariable Long patientId,
        @Valid @RequestBody ThresholdUpsert request
    ) {
        if (request.heartRateMin() >= request.heartRateMax()) {
            throw new IllegalArgumentException("heartRateMin must be lower than heartRateMax");
        }
        if (!patientRepository.existsById(patientId)) {
            throw new NoSuchElementException("Patient not found: " + patientId);
        }

        PatientThreshold row = thresholdService.findRow(patientId).orElseGet(() -> {
            PatientThreshold created = new PatientThreshold();
            created.setPatientId(patientId);
            return created;
        });
        row.setHeartRateMin(request.heartRateMin());
        row.setHeartRateMax(request.heartRateMax());
        row.setSpo2Min(request.spo2Min());
        row.setTemperatureMax(request.temperatureMax());
        thresholdService.save(row);

        return ResponseEntity.ok(toView(patientId));
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ThresholdView reset(@PathVariable Long patientId) {
        thresholdService.delete(patientId);
        return toView(patientId);
    }

    private ThresholdView toView(Long patientId) {
        var t = thresholdService.effective(patientId);
        String source = thresholdService.findRow(patientId).isPresent() ? "PATIENT" : "DEFAULT";
        return new ThresholdView(patientId, t.heartRateMin(), t.heartRateMax(), t.spo2Min(), t.temperatureMax(), source);
    }
}
