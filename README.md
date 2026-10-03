# Health Monitor API

![CI](https://github.com/Asishranjansahu/health-monitor-api/actions/workflows/ci.yml/badge.svg)

Spring Boot backend that stores patient vitals, exposes REST history endpoints, evaluates readings
against per-patient thresholds and broadcasts real-time alerts via WebSocket/STOMP. Secured with
JWT login and roles, device keys for sensors, an alert cooldown and optional Telegram push.

## Features

- **REST API** for patients and vitals with Bean Validation (`@Valid`, `@Min`, `@Max`, `@NotBlank`).
- **JWT login with roles** — `DOCTOR` and `NURSE`, seeded on first start.
- **Device key authentication** — only sensors holding a registered `X-Device-Key` can post readings.
- **Alert rules with per-patient thresholds** stored in the database (defaults configurable):
  - heart rate outside `min–max` (default `< 50` or `> 120`)
  - SpO2 below `min` (default `< 92`)
  - temperature above `max` (default `> 38.0 °C`)
- **Alert cooldown** — at most one alert per patient, per type, per minute.
- **Real-time delivery** of readings (`/topic/vitals`) and alerts (`/topic/alerts`,
  `/topic/alerts/{patientId}`) over STOMP.
- **Telegram alerts** — bot message per triggered alert when configured.
- **Patient list with traffic-light status** (green / amber / red) from the latest reading.
- **Live dashboard** — three Chart.js charts (heart rate, SpO2, temperature) fed by the WebSocket.
- **Swagger UI** and **Actuator** health checks.
- **PostgreSQL** persistence (Docker Compose, one command) with H2 in-memory as the dev default.
- **Tests** (JUnit 5 + Mockito + a full-stack PostgreSQL integration test) and GitHub Actions CI.

## Run

```bash
mvn spring-boot:run          # dev default: H2 in-memory, port 8080
```

With PostgreSQL in one command:

```bash
docker compose up --build    # app + postgres:16, data persisted in the pgdata volume
```

Without Docker, use the `postgres` profile against any local server matching
`application-postgres.properties` (override with `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`).

### Pages

| URL | Purpose |
|---|---|
| `/login.html` | Sign in; stores the JWT in localStorage |
| `/dashboard.html` | Three live charts + alert feed |
| `/patients.html` | Patient list with colour status, threshold editor (doctors) |
| `/index.html` | Live alerts feed with a test-post button |
| `/swagger-ui.html` | OpenAPI docs |
| `/actuator/health` | Health check (public) |

### Seeded accounts and demo sensor

| Credential | Value |
|---|---|
| DOCTOR login | `doctor` / `doctor123` |
| NURSE login | `nurse` / `nurse123` |
| Demo device key | `demo-device-key` (header `X-Device-Key`) |

Seeding happens only on an empty database. Change or remove these in any shared environment.

## Security model

- `POST /api/auth/login` → `{ token, role, username, name }`; send it back as
  `Authorization: Bearer <token>` (HS256, 8 h default; override the secret with `JWT_SECRET`).
- **DOCTOR**: create patients, manage thresholds and device keys, read everything.
- **NURSE**: record readings, read dashboards, thresholds and alerts.
- **DEVICE** (via `X-Device-Key` instead of a JWT): record readings only.
- Static pages, login, WebSocket upgrade, Swagger and `/actuator/health` are public; everything
  else under `/api` requires authentication. Denied → `403`, unauthenticated → `401` JSON.

## REST quick test

```bash
# Login
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"doctor","password":"doctor123"}' | jq -r .token)

# Create a patient (DOCTOR only)
curl -X POST localhost:8080/api/patients -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"John Doe","age":45,"ward":"ICU"}'

# Record a reading as a sensor (device key) — triggers alerts unless cooling down
curl -X POST localhost:8080/api/vitals -H "Content-Type: application/json" \
  -H "X-Device-Key: demo-device-key" \
  -d '{"patientId":1,"heartRate":150,"spo2":88,"temperature":39.2}'

# History and paginated alerts
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/vitals/1
curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/alerts/1?page=0&size=10"

# Per-patient thresholds (DOCTOR only)
curl -X PUT localhost:8080/api/thresholds/1 -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"heartRateMin":45,"heartRateMax":110,"spo2Min":90,"temperatureMax":37.8}'

# Register another sensor key (returned once, only the hash is stored)
curl -X POST localhost:8080/api/device-keys -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" -d '{"name":"ward-bed-3"}'
```

## API overview

| Method and path | Auth | Purpose |
|---|---|---|
| `POST /api/auth/login` | public | Exchange credentials for a JWT |
| `GET /api/auth/me` | any | Current user and role |
| `POST /api/patients` | DOCTOR | Create a patient |
| `GET /api/patients` | any | Patient list with green/amber/red status |
| `GET /api/patients/{id}` | any | One patient |
| `POST /api/vitals` | NURSE, DOCTOR or device key | Record a reading, evaluate alerts |
| `GET /api/vitals/{patientId}` | any | Readings, newest first |
| `GET /api/alerts/{patientId}?page&size` | any | Paginated saved alerts |
| `GET /api/thresholds/{patientId}` | any | Effective limits (`source: DEFAULT`/`PATIENT`) |
| `PUT /api/thresholds/{patientId}` | DOCTOR | Set per-patient limits |
| `DELETE /api/thresholds/{patientId}` | DOCTOR | Back to defaults |
| `GET/POST /api/device-keys` | DOCTOR | Manage sensor keys (plaintext shown once) |

Errors: validation → `400`, missing JWT → `401`, wrong role → `403`, unknown patient → `404`.

## Telegram alerts (optional)

1. Create a bot with [@BotFather](https://t.me/BotFather), copy the token.
2. Message the bot once, then get your chat id (e.g. via `getUpdates`).
3. Set the environment variables `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`
   (in `.env.local` for local runs, or Settings → Environment for the Freebuff preview).

When both are set, every triggered alert is also sent to the chat — asynchronously, with
failures logged and never breaking the API. Without them the notifier is a no-op.

## Alert cooldown and thresholds

- Cooldown: `healthmonitor.alert.cooldown-seconds` (default `60`) per patient and alert type,
  held in memory per JVM instance and reset on restart.
- Defaults: `healthmonitor.alert.*` in `application.properties`; per-patient overrides live in
  the `patient_threshold` table and win over the defaults (null fields fall back).

## Data simulator

A scheduled service creates a random reading for every patient every three seconds so the
dashboard can be demonstrated without devices. Disabled by default; enable with
`healthmonitor.simulator.enabled=true`.

## Testing

```bash
mvn verify                      # unit + controller tests (5 core tests), skips Postgres IT
mvn verify -Dpostgres.test=true # + full-stack PostgreSQL integration test
```

The integration test needs a PostgreSQL matching `application-postgres.properties`
(locally: `CREATE ROLE health LOGIN PASSWORD 'health'; CREATE DATABASE healthdb OWNER health;`)
and covers login, role enforcement, device keys, cooldown persistence and threshold updates.

Service tests mock the repositories and the message broker, so alert logic, cooldown windows and
threshold overrides are checked without a database or network.

## Continuous Integration

GitHub Actions runs `mvn -B verify` on every push and pull request (Temurin 21).

## Limitations and future work

- Cooldown state is in-memory (single instance).
- The WebSocket channel is unauthenticated (HTTP APIs are not).
- Simulator produces random data, not device readings.
- Planned: Testcontainers-based integration tests, Prometheus/Grafana, JWT for STOMP connect,
  integration with real sensors (ESP32 + MAX30102), audit logging.
