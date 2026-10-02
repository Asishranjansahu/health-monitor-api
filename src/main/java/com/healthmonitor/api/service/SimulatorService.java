package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Patient;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.PatientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@ConditionalOnProperty(name = "healthmonitor.simulator.enabled", havingValue = "true")
public class SimulatorService {
    private final VitalService vitalService;
    private final PatientRepository patientRepo;
    private final Random random = new Random();

    public SimulatorService(VitalService vitalService, PatientRepository patientRepo) {
        this.vitalService = vitalService;
        this.patientRepo = patientRepo;
    }

    @Scheduled(fixedRate = 3000)
    public void generate() {
        for (Patient patient : patientRepo.findAll()) {
            VitalReading reading = new VitalReading();
            reading.setPatientId(patient.getId());
            reading.setHeartRate(55 + random.nextInt(80));
            reading.setSpo2(88 + random.nextInt(12));
            reading.setTemperature(Math.round((36.0 + random.nextDouble() * 3.5) * 10) / 10.0);
            vitalService.record(reading);
        }
    }
}
