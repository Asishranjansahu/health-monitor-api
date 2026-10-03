package com.healthmonitor.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Per-patient alert thresholds. Any field left null falls back to the global
 * default from application.properties, so a row can override a single limit
 * without repeating the others.
 */
@Entity
public class PatientThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Integer heartRateMin;
    private Integer heartRateMax;
    private Integer spo2Min;
    private Double temperatureMax;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Integer getHeartRateMin() {
        return heartRateMin;
    }

    public void setHeartRateMin(Integer heartRateMin) {
        this.heartRateMin = heartRateMin;
    }

    public Integer getHeartRateMax() {
        return heartRateMax;
    }

    public void setHeartRateMax(Integer heartRateMax) {
        this.heartRateMax = heartRateMax;
    }

    public Integer getSpo2Min() {
        return spo2Min;
    }

    public void setSpo2Min(Integer spo2Min) {
        this.spo2Min = spo2Min;
    }

    public Double getTemperatureMax() {
        return temperatureMax;
    }

    public void setTemperatureMax(Double temperatureMax) {
        this.temperatureMax = temperatureMax;
    }
}
