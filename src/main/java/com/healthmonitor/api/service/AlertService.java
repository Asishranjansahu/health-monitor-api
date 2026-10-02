package com.healthmonitor.api.service;

import com.healthmonitor.api.model.VitalReading;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {
    public List<String> check(VitalReading reading) {
        List<String> issues = new ArrayList<>();

        if (reading.getHeartRate() < 50 || reading.getHeartRate() > 120) {
            issues.add("Abnormal heart rate: " + reading.getHeartRate());
        }
        if (reading.getSpo2() < 92) {
            issues.add("Low SpO2: " + reading.getSpo2());
        }
        if (reading.getTemperature() > 38.0) {
            issues.add("High temperature: " + reading.getTemperature());
        }

        return issues;
    }
}
