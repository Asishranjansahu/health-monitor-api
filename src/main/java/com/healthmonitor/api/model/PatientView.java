package com.healthmonitor.api.model;

import java.time.Instant;

/** Read model returned by GET /api/patients: the patient plus its computed status. */
public record PatientView(
    Long id,
    String name,
    int age,
    String ward,
    PatientStatus status,
    Instant lastRecordedAt
) {
}
