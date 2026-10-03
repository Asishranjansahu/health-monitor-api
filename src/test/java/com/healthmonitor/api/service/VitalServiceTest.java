package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Alert;
import com.healthmonitor.api.model.AlertRecord;
import com.healthmonitor.api.model.AlertType;
import com.healthmonitor.api.model.Thresholds;
import com.healthmonitor.api.model.VitalReading;
import com.healthmonitor.api.repository.AlertRecordRepository;
import com.healthmonitor.api.repository.PatientRepository;
import com.healthmonitor.api.repository.VitalReadingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    @Mock
    ThresholdService thresholdService;

    VitalService service;
    AlertCooldown cooldown;
    MutableClock clock;
    RecordingNotifier notifier;

    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-03T12:00:00Z"));
        cooldown = new AlertCooldown(COOLDOWN, clock);
        notifier = new RecordingNotifier();
        service = new VitalService(
            repo,
            new AlertService(),
            messaging,
            patientRepo,
            alertRepo,
            thresholdService,
            cooldown,
            notifier
        );
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
        when(thresholdService.effective(1L)).thenReturn(new Thresholds(50, 120, 92, 38.0));

        service.record(reading);

        verify(messaging, never()).convertAndSend(anyString(), any(Alert.class));
        verify(alertRepo, never()).save(any(AlertRecord.class));
    }

    @Test
    void lowSpo2_sendsAlert() {
        VitalReading reading = reading(72, 88, 36.8);
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(reading)).thenReturn(reading);
        when(thresholdService.effective(1L)).thenReturn(new Thresholds(50, 120, 92, 38.0));
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
        when(thresholdService.effective(1L)).thenReturn(new Thresholds(50, 120, 92, 38.0));
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

    @Test
    void cooldown_suppressesRepeatAlertForSameType() {
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(any(VitalReading.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(thresholdService.effective(1L)).thenReturn(new Thresholds(50, 120, 92, 38.0));
        when(alertRepo.save(any(AlertRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Same abnormal reading twice inside the one-minute window.
        service.record(reading(72, 88, 36.8));
        clock.advance(Duration.ofSeconds(30));
        service.record(reading(72, 88, 36.8));

        verify(alertRepo, times(1)).save(any(AlertRecord.class));
        verify(messaging, times(1)).convertAndSend(eq("/topic/alerts"), any(Alert.class));
    }

    @Test
    void cooldown_firesAgainAfterWindowExpires() {
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(any(VitalReading.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(thresholdService.effective(1L)).thenReturn(new Thresholds(50, 120, 92, 38.0));
        when(alertRepo.save(any(AlertRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(reading(72, 88, 36.8));
        clock.advance(Duration.ofSeconds(61));
        service.record(reading(72, 88, 36.8));

        verify(alertRepo, times(2)).save(any(AlertRecord.class));
        verify(messaging, times(2)).convertAndSend(eq("/topic/alerts"), any(Alert.class));
    }

    @Test
    void customThresholds_changeWhichReadingsAlert() {
        // Per-patient maximum heart rate of 100: 110 now alerts (default 120 would not).
        when(patientRepo.existsById(1L)).thenReturn(true);
        when(repo.save(any(VitalReading.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(thresholdService.effective(1L)).thenReturn(new Thresholds(50, 100, 92, 38.0));
        when(alertRepo.save(any(AlertRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(reading(110, 98, 36.6));

        verify(alertRepo, times(1)).save(any(AlertRecord.class));
        org.junit.jupiter.api.Assertions.assertTrue(
            notifier.sent.contains("1|HEART_RATE|Abnormal heart rate: 110"),
            "expected a heart-rate notification, got: " + notifier.sent
        );
    }

    /** Test double: records notifications instead of calling Telegram. */
    static class RecordingNotifier implements AlertNotifier {
        final java.util.List<String> sent = new java.util.ArrayList<>();

        @Override
        public void send(Long patientId, AlertType type, String message) {
            sent.add(patientId + "|" + type + "|" + message);
        }
    }

    /** Adjustable clock so cooldown windows can be advanced without sleeping. */
    static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
