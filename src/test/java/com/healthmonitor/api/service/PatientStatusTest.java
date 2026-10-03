package com.healthmonitor.api.service;

import com.healthmonitor.api.model.PatientStatus;
import com.healthmonitor.api.model.Thresholds;
import com.healthmonitor.api.model.VitalReading;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** B2 status logic: red at an alert, amber near a limit, green comfortably inside. */
class PatientStatusTest {

    private final Thresholds defaults = new Thresholds(50, 120, 92, 38.0);

    private VitalReading reading(int hr, int spo2, double temp) {
        VitalReading reading = new VitalReading();
        reading.setPatientId(1L);
        reading.setHeartRate(hr);
        reading.setSpo2(spo2);
        reading.setTemperature(temp);
        return reading;
    }

    @Test
    void comfortableVitalsAreGreen() {
        assertEquals(PatientStatus.GREEN, PatientService.status(reading(72, 98, 36.8), defaults));
    }

    @Test
    void nearALimitIsAmber() {
        assertEquals(PatientStatus.AMBER, PatientService.status(reading(115, 98, 36.8), defaults));
        assertEquals(PatientStatus.AMBER, PatientService.status(reading(72, 94, 36.8), defaults));
        assertEquals(PatientStatus.AMBER, PatientService.status(reading(72, 98, 37.7), defaults));
    }

    @Test
    void outsideALimitIsRed() {
        assertEquals(PatientStatus.RED, PatientService.status(reading(150, 98, 36.8), defaults));
        assertEquals(PatientStatus.RED, PatientService.status(reading(72, 88, 36.8), defaults));
        assertEquals(PatientStatus.RED, PatientService.status(reading(72, 98, 39.2), defaults));
        assertEquals(PatientStatus.RED, PatientService.status(reading(45, 98, 36.8), defaults));
    }

    @Test
    void customThresholdsShiftTheBands() {
        Thresholds strict = new Thresholds(55, 100, 95, 37.5);
        // 95 bpm is green with defaults but amber against a max of 100.
        assertEquals(PatientStatus.GREEN, PatientService.status(reading(95, 98, 36.8), defaults));
        assertEquals(PatientStatus.AMBER, PatientService.status(reading(95, 98, 36.8), strict));
    }
}
