package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Alert;
import com.healthmonitor.api.model.AlertIssue;
import com.healthmonitor.api.model.AlertRecord;
import com.healthmonitor.api.model.Thresholds;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.AlertRecordRepository;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.repository.VitalReadingRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class VitalService {
    private final VitalReadingRepository repo;
    private final AlertService alertService;
    private final SimpMessageSendingOperations messaging;
    private final PatientRepository patientRepo;
    private final AlertRecordRepository alertRepo;
    private final ThresholdService thresholdService;
    private final AlertCooldown cooldown;
    private final AlertNotifier notifier;

    public VitalService(
        VitalReadingRepository repo,
        AlertService alertService,
        SimpMessageSendingOperations messaging,
        PatientRepository patientRepo,
        AlertRecordRepository alertRepo,
        ThresholdService thresholdService,
        AlertCooldown cooldown,
        AlertNotifier notifier
    ) {
        this.repo = repo;
        this.alertService = alertService;
        this.messaging = messaging;
        this.patientRepo = patientRepo;
        this.alertRepo = alertRepo;
        this.thresholdService = thresholdService;
        this.cooldown = cooldown;
        this.notifier = notifier;
    }

    public VitalReading record(VitalReading reading) {
        if (!patientRepo.existsById(reading.getPatientId())) {
            throw new NoSuchElementException("Patient not found: " + reading.getPatientId());
        }

        VitalReading saved = repo.save(reading);
        messaging.convertAndSend("/topic/vitals", saved);

        Thresholds thresholds = thresholdService.effective(saved.getPatientId());
        for (AlertIssue issue : alertService.check(saved, thresholds)) {
            // Cooldown: one alert per patient and type per minute.
            if (!cooldown.tryFire(saved.getPatientId(), issue.type())) {
                continue;
            }

            AlertRecord alertRecord = new AlertRecord();
            alertRecord.setPatientId(saved.getPatientId());
            alertRecord.setMessage(issue.message());
            alertRecord.setType(issue.type());
            alertRepo.save(alertRecord);

            Alert alert = new Alert(
                String.valueOf(saved.getPatientId()),
                issue.message(),
                issue.type(),
                Instant.now()
            );
            messaging.convertAndSend("/topic/alerts", alert);
            messaging.convertAndSend("/topic/alerts/" + saved.getPatientId(), alert);
            notifier.send(saved.getPatientId(), issue.type(), issue.message());
        }

        return saved;
    }

    public List<VitalReading> history(Long patientId) {
        return repo.findByPatientIdOrderByRecordedAtDesc(patientId);
    }
}
