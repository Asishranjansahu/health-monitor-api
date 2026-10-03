package com.healthmonitor.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack integration test against a real PostgreSQL database, covering
 * login, role enforcement, device keys, cooldown persistence and thresholds.
 *
 * Skipped unless a local PostgreSQL matching application-postgres.properties
 * is available and the run enables it:
 *
 *   mvn verify -Dpostgres.test=true
 *
 * Note: uses a deliberately buffered RestTemplate. The JDK HTTP client throws
 * "cannot retry due to server authentication, in streaming mode" for any 401
 * response to a POST with an unbuffered (chunked) body, which would mask the
 * status-code assertions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "postgres.test", matches = "true")
class PostgresProfileTest {

    @LocalServerPort
    private int port;

    private RestTemplate rest;

    @BeforeEach
    void setUpClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setBufferRequestBody(true);
        rest = new RestTemplate(factory);
        rest.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity(
            url("/api/auth/login"),
            Map.of("username", username, "password", password),
            Map.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("token"));
        return (String) response.getBody().get("token");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @Test
    void healthIsPublicAndApiRequiresAuth() {
        ResponseEntity<String> health = rest.getForEntity(url("/actuator/health"), String.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertTrue(health.getBody().contains("\"status\":\"UP\""));

        ResponseEntity<String> unauthorized = rest.exchange(
            url("/api/patients"),
            HttpMethod.GET,
            new HttpEntity<>(bearer(null)),
            String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());

        // The JDK HTTP client throws HttpRetryException("...streaming mode") for
        // ANY 401 response to a POST whose body went through a streaming mode
        // (Spring's SimpleClientHttpRequestFactory always does). The wrapped
        // exception still carries the real status code, so assert on that —
        // and accept a plain 401 if the client ever returns it directly.
        byte[] badCredentials = "{\"username\":\"doctor\",\"password\":\"wrong-password\"}"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        int badLoginStatus;
        try {
            badLoginStatus = rest.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(badCredentials, loginHeaders),
                String.class
            ).getStatusCode().value();
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            assertTrue(ex.getCause() instanceof java.net.HttpRetryException,
                "expected HttpRetryException, got: " + ex.getCause());
            badLoginStatus = ((java.net.HttpRetryException) ex.getCause()).responseCode();
        }
        assertEquals(HttpStatus.UNAUTHORIZED.value(), badLoginStatus);
    }

    @Test
    void rolesDeviceKeysCooldownAndThresholdsWorkEndToEnd() {
        String doctor = login("doctor", "doctor123");
        String nurse = login("nurse", "nurse123");

        // A2: nurses cannot register patients.
        ResponseEntity<String> forbidden = rest.exchange(
            url("/api/patients"),
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "Blocked", "age", 30, "ward", "A"), bearer(nurse)),
            String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        // A2: doctors can.
        ResponseEntity<Map> created = rest.exchange(
            url("/api/patients"),
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "PG Patient", "age", 55, "ward", "ICU"), bearer(doctor)),
            Map.class
        );
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Number patientIdNumber = (Number) created.getBody().get("id");
        assertNotNull(patientIdNumber);
        long patientId = patientIdNumber.longValue();

        // B2: list includes a traffic-light status.
        ResponseEntity<String> list = rest.exchange(
            url("/api/patients"),
            HttpMethod.GET,
            new HttpEntity<>(bearer(nurse)),
            String.class
        );
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertTrue(list.getBody().contains("\"status\""), "expected status field in: " + list.getBody());

        // A3: issue a sensor key, then post a reading with it.
        ResponseEntity<Map> keyResponse = rest.exchange(
            url("/api/device-keys"),
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "pg-sensor"), bearer(doctor)),
            Map.class
        );
        assertEquals(HttpStatus.CREATED, keyResponse.getStatusCode());
        String deviceKey = (String) keyResponse.getBody().get("key");
        assertNotNull(deviceKey);

        HttpHeaders deviceHeaders = new HttpHeaders();
        deviceHeaders.setContentType(MediaType.APPLICATION_JSON);
        deviceHeaders.set("X-Device-Key", deviceKey);
        ResponseEntity<String> reading = rest.exchange(
            url("/api/vitals"),
            HttpMethod.POST,
            new HttpEntity<>(Map.of(
                "patientId", patientId,
                "heartRate", 150,
                "spo2", 88,
                "temperature", 39.2
            ), deviceHeaders),
            String.class
        );
        assertEquals(HttpStatus.CREATED, reading.getStatusCode());

        // A4: the same abnormal reading again inside the cooldown window adds nothing.
        ResponseEntity<String> repeated = rest.exchange(
            url("/api/vitals"),
            HttpMethod.POST,
            new HttpEntity<>(Map.of(
                "patientId", patientId,
                "heartRate", 150,
                "spo2", 88,
                "temperature", 39.2
            ), deviceHeaders),
            String.class
        );
        assertEquals(HttpStatus.CREATED, repeated.getStatusCode());
        ResponseEntity<String> alerts = rest.exchange(
            url("/api/alerts/" + patientId + "?page=0&size=10"),
            HttpMethod.GET,
            new HttpEntity<>(bearer(doctor)),
            String.class
        );
        assertEquals(HttpStatus.OK, alerts.getStatusCode());
        // Three types fired on the first reading, none on the repeat.
        assertEquals(3, alerts.getBody().split("\"patientId\"").length - 1);

        // A5: per-patient thresholds are readable and updatable by doctors only.
        ResponseEntity<String> nurseThreshold = rest.exchange(
            url("/api/thresholds/" + patientId),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of(
                "heartRateMin", 45, "heartRateMax", 110,
                "spo2Min", 90, "temperatureMax", 37.8
            ), bearer(nurse)),
            String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, nurseThreshold.getStatusCode());

        ResponseEntity<Map> threshold = rest.exchange(
            url("/api/thresholds/" + patientId),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of(
                "heartRateMin", 45, "heartRateMax", 110,
                "spo2Min", 90, "temperatureMax", 37.8
            ), bearer(doctor)),
            Map.class
        );
        assertEquals(HttpStatus.OK, threshold.getStatusCode());
        assertEquals("PATIENT", threshold.getBody().get("source"));

        ResponseEntity<Map> invalidThreshold = rest.exchange(
            url("/api/thresholds/" + patientId),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of(
                "heartRateMin", 120, "heartRateMax", 50,
                "spo2Min", 90, "temperatureMax", 37.8
            ), bearer(doctor)),
            Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidThreshold.getStatusCode());
    }
}
