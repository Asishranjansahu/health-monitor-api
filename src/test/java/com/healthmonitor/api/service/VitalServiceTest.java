package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Alert;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.VitalReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    VitalService service;

    @BeforeEach
    void setUp() {
        service = new VitalService(repo, new AlertService(), messaging);
    }

    private VitalReading reading(int hr, int spo2, double temp) {
        VitalReading reading = new VitalReading();
        reading.setPatientId("P1");
        reading.setHeartRate(hr);
        reading.setSpo2(spo2);
        reading.setTemperature(temp);
        return reading;
    }

    @Test
    void normalVitals_sendNoAlert() {
        VitalReading reading = reading(72, 98, 36.8);
        when(repo.save(reading)).thenReturn(reading);

        service.record(reading);

        verify(messaging, never()).convertAndSend(anyString(), any(Alert.class));
    }

    @Test
    void lowSpo2_sendsAlert() {
        VitalReading reading = reading(72, 88, 36.8);
        when(repo.save(reading)).thenReturn(reading);

        service.record(reading);

        verify(messaging, times(1)).convertAndSend(eq("/topic/alerts"), any(Alert.class));
    }

    @Test
    void multipleAbnormalVitals_sendMultipleAlerts() {
        VitalReading reading = reading(150, 85, 39.5);
        when(repo.save(reading)).thenReturn(reading);

        service.record(reading);

        verify(messaging, times(3)).convertAndSend(eq("/topic/alerts"), any(Alert.class));
    }
}
