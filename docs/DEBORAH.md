# Deborah: Backend core specification

**Role:** team lead and owner of the backend core: how KingaAPI checks APIs and diagnoses failures.
**In the pitch you explain:** *how the checks work and how a failure becomes a clear report.*

---

## 1. What Deborah has done ✅

| Task | Result |
|---|---|
| Created the GitHub repo | [Ideborah-tech/kinga-api](https://github.com/Ideborah-tech/kinga-api) |
| Chose the tech stack | React (Vite) frontend, Spring Boot 3 backend, H2 database |
| Split the work and assigned owners | [TEAM.md](../TEAM.md), [Sifa's spec](SIFA.md), [Gisele's spec](GISELE.md) |
| Built the monitoring engine | Every endpoint is checked every minute: status code, response time, JSON format |
| Built failure classification | Auth, rate limit, server error, timeout, connection, bad format, unexpected status, slow |
| Built the Dependency Failure Report | Verdict, expected vs. actual, response body, what to check next |
| Built community detection, mock providers, shared setups and demo data | Handed to Gisele to own and present |
| Built the React frontend | Handed to Sifa to own and present |
| Designed the “Aurora” look and the new layout | Glass panels, sunset gradient, bento dashboard, phone dock |
| Tested the demo end to end | “It's them”, “it's you” and schema-change stories; light, dark and phone |
| Wrote the docs and pushed everything | README, TEAM.md and specs, all on `main` |

## 2. How a check works

File: `backend/src/main/java/com/kinga/api/service/`

1. **`MonitoringService`** runs every 5 seconds (`kinga.check.tick-ms`). Any monitor whose last check is older than its interval (60 s by default) is sent to a pool of 16 threads, so slow providers don't hold up the others.
2. **`CheckerService.check()`** sends the request (method, URL, headers, body) with the monitor's timeout (8 s by default) and measures the time.
3. It **classifies** the result, in this order:

| What happened | Failure type |
|---|---|
| No answer before the timeout | `TIMEOUT` |
| Could not connect (DNS, refused, TLS) | `CONNECTION` |
| HTTP 401 or 403 | `AUTH` |
| HTTP 429 | `RATE_LIMIT` |
| HTTP 5xx | `SERVER_ERROR` |
| Any other status that isn't the expected one | `UNEXPECTED_STATUS` |
| Body isn't JSON, or expected fields are missing | `BAD_FORMAT` (lists the missing fields) |
| Correct, but slower than 2 s | `SLOW` (counts as Up, shown as Slow) |
| Correct | `NONE` |

4. The result is saved as a `CheckResult`, and the monitor's status is updated: **UP**, **DEGRADED** (slow) or **DOWN**.
5. `CommunityService.evaluate()` (Gisele's) then checks whether many developers are failing on the same provider.

Expected fields use dot paths: `data.0.code` means "the `code` of the first item in `data`".

## 3. How the report is built

File: `service/ReportService.java`, endpoint `GET /api/monitors/{id}/report`

- Takes the **latest failed check** and the monitor's settings to build **Expected vs. What came back**.
- Works out **how long it has been failing** (since the last successful check).
- Gives the **verdict** from community data: `THEM` if a provider incident is open; `YOU` if every other developer on that provider is healthy; otherwise `UNCLEAR`.
- Adds **what to check next**: 2–4 suggestions written for each failure type. For example, `AUTH` mentions sandbox vs. production keys and token expiry (M-Pesa tokens last one hour), and `TIMEOUT` warns that a timed-out payment may still go through.

## 4. Your files

```
backend/pom.xml
backend/src/main/resources/application.properties
backend/src/main/java/com/kinga/api/KingaApiApplication.java
backend/src/main/java/com/kinga/api/config/WebConfig.java          CORS, HTTP client, check thread pool
backend/src/main/java/com/kinga/api/model/                         Monitor, CheckResult, MonitorStatus, FailureType, ProviderIncident, MonitorTemplate
backend/src/main/java/com/kinga/api/repository/                    the 4 database repositories
backend/src/main/java/com/kinga/api/service/CheckerService.java     sends the request, classifies the result
backend/src/main/java/com/kinga/api/service/MonitoringService.java  every-minute scheduler
backend/src/main/java/com/kinga/api/service/MonitorService.java     create / edit / list / share monitors
backend/src/main/java/com/kinga/api/service/ReportService.java      Dependency Failure Report + suggestions
backend/src/main/java/com/kinga/api/dto/                           MonitorRequest, MonitorSummary, FailureReport
backend/src/main/java/com/kinga/api/controller/MonitorController.java
backend/src/main/java/com/kinga/api/controller/DashboardController.java
```

## 5. Remaining tasks for today

| # | Task | Done when |
|---|---|---|
| 1 | Read through `CheckerService`, `MonitoringService` and `ReportService` | You can explain sections 2 and 3 without notes |
| 2 | Help Sifa and Gisele get the app running on their laptops | Both see the dashboard with 5 green monitors |
| 3 | ✅ (Bonus) Write 3–5 unit tests for `CheckerService` and `ReportService` | `mvn test` passes: 19 tests in `backend/src/test/java/com/kinga/api/service/` |
| 4 | Final run-through together, then submit | Project submitted before the deadline |

## 6. Your part of the pitch (about 40 seconds)

> "Every minute, KingaAPI calls each endpoint a developer depends on, exactly as their app would, with their method, headers and body. We don't just check whether it answered. We check the status code, the speed, and whether the JSON still has the fields their code needs. Every failure is classified: wrong credentials, rate limit, provider error, timeout, or a silent schema change. That classification drives the report: instead of a vague error, the developer gets the exact cause and the next thing to check."

## 7. Questions judges may ask you

| Question | Answer |
|---|---|
| Why Spring Boot? | Mature, reliable for scheduled background jobs, with built-in database access (JPA) and validation. |
| How do you avoid checking the same endpoint twice at once? | `MonitoringService` keeps a set of monitors with a check in progress and skips them until the check finishes. |
| What database? | H2, in memory for the demo, so data resets on every start. One line in `application.properties` switches it to a file, and PostgreSQL would be next for production. |
| Can it scale? | Checks run in parallel on a thread pool. For thousands of monitors, the next step is a job queue and several worker instances. |
| Do you store API keys? | Headers are stored to make the calls, and shared setups always remove secrets. For production we would encrypt stored credentials. |
