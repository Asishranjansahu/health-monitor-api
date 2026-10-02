package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.AlertRecord;
import com.healthmonitor.api.repository.AlertRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertRecordRepository alertRecordRepository;

    public AlertController(AlertRecordRepository alertRecordRepository) {
        this.alertRecordRepository = alertRecordRepository;
    }

    @GetMapping("/{patientId}")
    public Page<AlertRecord> byPatient(
        @PathVariable Long patientId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return alertRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(page, size));
    }
}
