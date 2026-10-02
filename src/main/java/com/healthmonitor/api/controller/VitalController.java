package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.service.VitalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vitals")
public class VitalController {
    private final VitalService service;

    public VitalController(VitalService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VitalReading> add(@Valid @RequestBody VitalReading reading) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.record(reading));
    }

    @GetMapping("/{patientId}")
    public List<VitalReading> history(@PathVariable Long patientId) {
        return service.history(patientId);
    }
}
