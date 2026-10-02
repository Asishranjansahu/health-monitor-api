package com.healthmonitor.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class VitalReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String patientId;

    @Min(0)
    @Max(300)
    private int heartRate;

    @Min(0)
    @Max(100)
    private int spo2;

    private double temperature;

    private Instant recordedAt = Instant.now();
}
