package com.healthmonitor.api.service;

import com.healthmonitor.api.model.AlertIssue;
import com.healthmonitor.api.model.AlertType;
import com.healthmonitor.api.model.Thresholds;
import com.healthmonitor.api.model.VitalReading;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Evaluates a reading against the patient's effective thresholds. Pure logic,
 * no persistence or messaging, which keeps it trivial to unit test.
 */
@Service
public class AlertService {

    public List<AlertIssue> check(VitalReading reading, Thresholds thresholds) {
        List<AlertIssue> issues = new ArrayList<>();

        if (reading.getHeartRate() < thresholds.heartRateMin()
            || reading.getHeartRate() > thresholds.heartRateMax()) {
            issues.add(new AlertIssue(AlertType.HEART_RATE, "Abnormal heart rate: " + reading.getHeartRate()));
        }
        if (reading.getSpo2() < thresholds.spo2Min()) {
            issues.add(new AlertIssue(AlertType.SPO2, "Low SpO2: " + reading.getSpo2()));
        }
        if (reading.getTemperature() > thresholds.temperatureMax()) {
            issues.add(new AlertIssue(AlertType.TEMPERATURE, "High temperature: " + reading.getTemperature()));
        }

        return issues;
    }
}
