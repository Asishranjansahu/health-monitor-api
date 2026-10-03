package com.healthmonitor.api.model;

/**
 * The effective alert limits for one patient (global defaults merged with any
 * per-patient overrides).
 */
public record Thresholds(int heartRateMin, int heartRateMax, int spo2Min, double temperatureMax) {
}
