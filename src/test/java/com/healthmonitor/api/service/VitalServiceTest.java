package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Alert;
import com.healthmonitor.api.model.AlertRecord;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.AlertRecordRepository;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.repository.VitalReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalServiceTest {
    @Mock
    VitalReadingRepository repo;

    @Mock
    SimpMessageSendingOperations messaging;

    @Mock
    PatientRepository patientRepo;

    @Mock
    AlertRecordRepository alertRepo;

    VitalService service;

    @BeforeEach
    void setUp() {
        service = new VitalService(repo, new AlertService(), messaging, patientRepo, alertRepo);
    }

    private VitalReading reading(int hr, int spo2, double temp) {
        VitalReading reading = new VitalReading();
        reading.setPatientId(1L);
        reading.setHeartRate(hr);
        reading.setSpo2(spo2);
        reading.setTemperature(temp);
        return reading;
    }

    @Test
    void normalVitals_sendNoAlert() {
        VitalReading reading = reading(72, 98, 36.8);
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(reading)).thenReturn(reading);

        service.record(reading);

        verify(messaging, never()).convertAndSend(anyString(), any(Alert.class));
        verify(alertRepo, never()).save(any(AlertRecord.class));
    }

    @Test
    void lowSpo2_sendsAlert() {
        VitalReading reading = reading(72, 88, 36.8);
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(reading)).thenReturn(reading);
        when(alertRepo.save(any(AlertRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(reading);

        verify(messaging, times(1)).convertAndSend(eq("/topic/alerts"), any(Alert.class));
        verify(alertRepo, times(1)).save(any(AlertRecord.class));
    }

    @Test
    void multipleAbnormalVitals_sendMultipleAlerts() {
        VitalReading reading = reading(150, 85, 39.5);
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(reading)).thenReturn(reading);
        when(alertRepo.save(any(AlertRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(reading);

        verify(messaging, times(3)).convertAndSend(eq("/topic/alerts"), any(Alert.class));
    }

    @Test
    void unknownPatient_throwsAndDoesNotSaveReading() {
        VitalReading reading = reading(72, 98, 36.8);
        when(patientRepo.existsById(1L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> service.record(reading));
        verify(repo, never()).save(any(VitalReading.class));
    }
}
