package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Alert;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.VitalReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VitalService {
    private final VitalReadingRepository repo;
    private final AlertService alertService;
    private final SimpMessageSendingOperations messaging;

    public VitalReading record(VitalReading reading) {
        VitalReading saved = repo.save(reading);
        alertService.check(saved).forEach(msg ->
            messaging.convertAndSend("/topic/alerts", new Alert(saved.getPatientId(), msg, Instant.now())));
        return saved;
    }

    public List<VitalReading> history(String patientId) {
        return repo.findByPatientIdOrderByRecordedAtDesc(patientId);
    }
}
