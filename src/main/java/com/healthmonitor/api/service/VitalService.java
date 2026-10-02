package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Alert;
import com.healthmonitor.api.model.AlertRecord;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.AlertRecordRepository;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.repository.VitalReadingRepository;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class VitalService {
    private final VitalReadingRepository repo;
    private final AlertService alertService;
    private final SimpMessageSendingOperations messaging;
    private final PatientRepository patientRepo;
    private final AlertRecordRepository alertRepo;

    public VitalService(
        VitalReadingRepository repo,
        AlertService alertService,
        SimpMessageSendingOperations messaging,
        PatientRepository patientRepo,
        AlertRecordRepository alertRepo
    ) {
        this.repo = repo;
        this.alertService = alertService;
        this.messaging = messaging;
        this.patientRepo = patientRepo;
        this.alertRepo = alertRepo;
    }

    public VitalReading record(VitalReading reading) {
        if (!patientRepo.existsById(reading.getPatientId())) {
            throw new NoSuchElementException("Patient not found: " + reading.getPatientId());
        }

        VitalReading saved = repo.save(reading);
        messaging.convertAndSend("/topic/vitals", saved);
        for (String msg : alertService.check(saved)) {
            AlertRecord alertRecord = new AlertRecord();
            alertRecord.setPatientId(saved.getPatientId());
            alertRecord.setMessage(msg);
            alertRepo.save(alertRecord);

            Alert alert = new Alert(String.valueOf(saved.getPatientId()), msg, Instant.now());
            messaging.convertAndSend("/topic/alerts", alert);
            messaging.convertAndSend("/topic/alerts/" + saved.getPatientId(), alert);
        }

        return saved;
    }

    public List<VitalReading> history(Long patientId) {
        return repo.findByPatientIdOrderByRecordedAtDesc(patientId);
    }
}
