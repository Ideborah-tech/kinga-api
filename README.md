# KingaAPI

**API dependency monitoring for African developers.** KingaAPI watches the payment and service APIs your app depends on (M-Pesa, MTN MoMo, Airtel Money, Orange Money, Flutterwave, Paystack, Wave) and tells you whether a failure is your code or the provider's, before your users notice.

*Kinga* means “protection” in Swahili.

## The problem

Across Africa, apps depend on payment APIs that fail often. One aggregator logged 608 mobile money operator incidents in 90 days, and 41% of teams still learn about outages from their customers. When a payment fails, a developer can't tell quickly whether their own code broke or the provider is down.

## What KingaAPI does

| Feature | Where |
|---|---|
| Checks every endpoint once a minute: status code, response time, response format (required JSON fields) | `MonitoringService`, `CheckerService` |
| **Dependency Failure Report**: what failed, what was expected, what came back, and what to check next (auth, rate limits, provider status…) | `ReportService`, `FailureReport.jsx` |
| **“It's them, not you”**: when at least half of all developers' monitors for a provider fail at once, KingaAPI opens a provider incident and tells every affected developer | `CommunityService` |
| **Shared setups**: ready-made monitors for M-Pesa, MTN MoMo, Airtel Money, Orange Money, Flutterwave, Paystack and Wave; share your own with secrets removed | `providers.json`, `TemplateController`, `Templates.jsx` |
| **Built-in mock providers** that can be broken on cue (503, timeout, 401, 429, schema change, slow, flaky) for a reliable live demo without real API keys | `mock/` package, `DemoControl.jsx` |

## Tech stack

- **Backend:** Java 17+, Spring Boot 3.3, Spring Data JPA, H2 database (in memory, demo data on every start)
- **Frontend:** React 18 with Vite, React Router and Recharts

## Run it

You need **JDK 17 or newer** (check with `java -version`), **Maven 3.9+** and **Node 18+**. The app runs fully offline after the first install; it does not need any real API keys.

```bash
git clone https://github.com/Ideborah-tech/kinga-api.git
cd kinga-api
```

### 1. Backend: http://localhost:8085

In a terminal, from the project folder:

```bash
cd backend
mvn spring-boot:run
```

If `java -version` shows 1.8, point Maven at a newer JDK first. In PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"
```

Run the backend unit tests (failure classification and the Dependency Failure Report) with `mvn test` from `backend/`.

On start, the backend seeds 10 developers, 29 monitors, 24 hours of check history and one past MTN MoMo outage. The H2 console is at http://localhost:8085/h2-console (JDBC URL `jdbc:h2:mem:kinga`, user `sa`, no password).

### 2. Frontend: http://localhost:5173

Leave the backend running. In a **second terminal**, again from the project folder (not from `backend/`):

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The dashboard should show 5 green monitors for the developer `you`. If every page says “Something went wrong”, the backend is not running yet.

If the backend runs on another URL, create `frontend/.env.local` containing `VITE_API_URL=http://host:port`.

## Demo script (2 minutes)

1. **Dashboard:** all of “you”'s payment APIs are green, with 24 h of uptime and response-time history.
2. **“It's them”:** on **Demo control**, set M-Pesa to *503 error*. Within seconds, every developer's M-Pesa monitors fail, an incident opens and the dashboard shows the purple **“It's them, not you”** banner. Open the failure report: expected vs. actual, the response body, what to check next, and how many other developers are affected.
3. **“It's you”:** click *Reset everything*, then *Break my key* for MTN MoMo. Only your monitor fails, and the report says *“Probably your setup”* and points at your credentials.
4. **Silent breakage:** set Paystack to *Schema change*. It still returns HTTP 200, but KingaAPI lists exactly which JSON fields disappeared.
5. **Templates:** show the shared setups and add a monitor in one click.

## Pitch material

- Slides: [`docs/pitch/KingaAPI-pitch.pptx`](docs/pitch/KingaAPI-pitch.pptx) (8 slides, speaker notes included)
- Backup demo video (2 min, captioned): [`docs/demo/kinga-demo-backup.mp4`](docs/demo/kinga-demo-backup.mp4)
- Screenshots: [`docs/screenshots/`](docs/screenshots/)

## API overview

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/dashboard?owner=you` | Counts, uptime, incidents affecting this developer |
| GET/POST | `/api/monitors` | List (`?owner=`) / create monitors |
| GET/PUT/DELETE | `/api/monitors/{id}` | One monitor with 24 h stats |
| GET | `/api/monitors/{id}/checks?limit=60` | Check history |
| POST | `/api/monitors/{id}/check` | Check now |
| GET | `/api/monitors/{id}/report` | Dependency Failure Report |
| POST | `/api/monitors/{id}/share` | Share as a template (secrets removed) |
| GET | `/api/providers` | Providers plus live community health |
| POST | `/api/providers/{code}/check-now` | Re-check every monitor of a provider |
| GET | `/api/incidents?active=true` | Provider incidents |
| GET/POST | `/api/templates` | Shared monitoring setups |
| GET/PUT | `/api/demo/modes[/{provider}]` | Mock provider modes |
| POST | `/api/demo/my-key?owner=&provider=&broken=` | Break or fix one developer's key |
| POST | `/api/demo/reset?owner=you` | Everything healthy again |
| ANY | `/mock/{provider}/**` | The mock provider APIs themselves |

## How “it's them, not you” works

After every check, `CommunityService` looks at every active monitor for that provider that was checked in the last 5 minutes. If there are at least 3 monitors and at least half of them are failing, it opens a `ProviderIncident`, which closes once no monitor is failing. The failure report then gives a verdict:

- **THEM:** a provider incident is open.
- **YOU:** you are failing but every other developer on that provider is healthy.
- **UNCLEAR:** some others are failing too, but not enough to confirm an outage, or there is too little data.

Thresholds are in `backend/src/main/resources/application.properties` (`kinga.community.*`).

## Team

- **Deborah**: backend core (monitoring engine, failure reports)
- **Sifa**: frontend (React)
- **Gisele**: community outage detection, provider setups, demo and pitch

See [TEAM.md](TEAM.md) for who owns which files.
