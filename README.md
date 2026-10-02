# Health Monitor API

![CI](https://github.com/Asishranjansahu/health-monitor-api/actions/workflows/ci.yml/badge.svg)

Spring Boot backend that stores patient vitals, exposes REST history endpoints, and broadcasts real-time alerts via WebSocket/STOMP.

## Features
- Record vitals (`heartRate`, `spo2`, `temperature`) per patient.
- Validate input with Bean Validation (`@Valid`, `@Min`, `@Max`, `@NotBlank`).
- Alert rules:
  - heart rate `< 50` or `> 120`
  - SpO2 `< 92`
  - temperature `> 38.0`
- Publish each triggered alert to `/topic/alerts`.
- Unit tests (JUnit 5 + Mockito) and GitHub Actions CI.

## Run
```bash
mvn spring-boot:run
```

## REST quick test
```bash
curl -X POST localhost:8080/api/vitals -H "Content-Type: application/json" \
  -d '{"patientId":"P1","heartRate":150,"spo2":88,"temperature":39.2}'

curl localhost:8080/api/vitals/P1
```

## WebSocket quick proof
1. Open `http://localhost:8080` in multiple tabs.
2. Send the POST request above.
3. All tabs receive live alerts from `/topic/alerts`.
