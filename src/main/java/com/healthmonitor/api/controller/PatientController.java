package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.Patient;
import com.healthmonitor.api.model.PatientView;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.service.PatientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientRepository patientRepository;
    private final PatientService patientService;

    public PatientController(PatientRepository patientRepository, PatientService patientService) {
        this.patientRepository = patientRepository;
        this.patientService = patientService;
    }

    /** Patient list with traffic-light status; any authenticated role may view. */
    @GetMapping
    public List<PatientView> list() {
        return patientService.listWithStatus();
    }

    /** Only doctors may register patients. */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Patient> create(@Valid @RequestBody Patient patient) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientRepository.save(patient));
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable Long id) {
        return patientRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Patient not found: " + id));
    }
}
