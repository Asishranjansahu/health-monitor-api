package com.healthmonitor.api.model;

import java.time.Instant;

public record Alert(String patientId, String message, AlertType type, Instant at) {
}
