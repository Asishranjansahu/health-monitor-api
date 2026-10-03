package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Patient;
import com.healthmonitor.api.model.PatientStatus;
import com.healthmonitor.api.model.PatientView;
import com.healthmonitor.api.model.Thresholds;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.repository.VitalReadingRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds the patient list with a traffic-light status derived from each
 * patient's most recent reading and their effective thresholds:
 *
 * RED   - the latest reading would trigger an alert.
 * AMBER - inside the limits but within the guard band of one (HR +/- 10 bpm,
 *         SpO2 +3, temperature -0.5 C).
 * GREEN - comfortably inside the limits, or no readings yet.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepo;
    private final VitalReadingRepository vitalRepo;
    private final ThresholdService thresholdService;

    public PatientService(
        PatientRepository patientRepo,
        VitalReadingRepository vitalRepo,
        ThresholdService thresholdService
    ) {
        this.patientRepo = patientRepo;
        this.vitalRepo = vitalRepo;
        this.thresholdService = thresholdService;
    }

    public List<PatientView> listWithStatus() {
        return patientRepo.findAll().stream()
            .map(this::toView)
            .toList();
    }

    public PatientView view(Patient patient) {
        return toView(patient);
    }

    private PatientView toView(Patient patient) {
        VitalReading latest = vitalRepo.findTopByPatientIdOrderByRecordedAtDesc(patient.getId()).orElse(null);
        PatientStatus status = latest == null ? PatientStatus.GREEN : status(latest, thresholdService.effective(patient.getId()));
        return new PatientView(
            patient.getId(),
            patient.getName(),
            patient.getAge(),
            patient.getWard(),
            status,
            latest == null ? null : latest.getRecordedAt()
        );
    }

    static PatientStatus status(VitalReading reading, Thresholds t) {
        boolean red = reading.getHeartRate() < t.heartRateMin()
            || reading.getHeartRate() > t.heartRateMax()
            || reading.getSpo2() < t.spo2Min()
            || reading.getTemperature() > t.temperatureMax();
        if (red) {
            return PatientStatus.RED;
        }

        boolean amber = reading.getHeartRate() <= t.heartRateMin() + 10
            || reading.getHeartRate() >= t.heartRateMax() - 10
            || reading.getSpo2() <= t.spo2Min() + 3
            || reading.getTemperature() >= t.temperatureMax() - 0.5;
        return amber ? PatientStatus.AMBER : PatientStatus.GREEN;
    }
}
