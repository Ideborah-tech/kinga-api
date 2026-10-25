# Team split: who owns what

Team: **Deborah** (backend core), **Sifa** (frontend), **Gisele** (community intelligence, demo and pitch). Full specs: [Deborah](docs/DEBORAH.md) · [Sifa](docs/SIFA.md) · [Gisele](docs/GISELE.md). The code is complete and runs end to end. Each person owns one area: read it, understand it, be able to explain and demo it, and fix or polish it. In the pitch, each person presents their own part.

## Task board

### ✅ Done
| Task | Done by |
|---|---|
| Select the project and write the description | Team |
| Create the GitHub repo (`Ideborah-tech/kinga-api`) | Deborah |
| Choose the tech stack: React + Spring Boot, H2 database | Deborah |
| Split the work into To do / In progress / Done and assign owners | Deborah |
| Backend: every-minute checks, failure classification, Dependency Failure Report | Deborah |
| Backend: community outage detection, mock providers, shared setups, demo data | Deborah |
| Frontend: dashboard, monitor page, failure report, add/edit monitor, shared setups, providers, demo control | Deborah |
| Design: “Aurora” look (glass panels, sunset gradient), new logo, top bar + phone dock, bento dashboard | Deborah |
| Test both demo stories (“it's them” and “it's you”) and the schema-change story end to end | Deborah |
| Test light mode, dark mode and phone width | Deborah |
| Write README, TEAM.md and the specs for Sifa and Gisele; push everything to GitHub | Deborah |
| Unit tests for `CheckerService` (14) and `ReportService` (5); `mvn test` passes | Deborah |
| Click through every page on laptop and phone width, light and dark; fix layout issues (Community pulse names on phone, outage banner on phone, router console warnings) | Sifa |
| Screenshots of the dashboard, outage banner, failure reports and phone views in [`docs/screenshots/`](docs/screenshots/) | Sifa |

### 🔄 In progress (everyone, now)
- Each member pulls the repo, runs backend + frontend on their own laptop (see README) and reads their files below

### 📋 To do (this afternoon)
| Task | Owner |
|---|---|
| Walk through the backend code; be ready to explain the scheduler and the failure classification | Deborah |
| Rehearse the demo script on the Demo control page 3 times | Gisele |
| Build the pitch slides: problem → solution → live demo → impact → next steps | Gisele |
| Record a 2-minute backup demo video in case the live demo fails | Gisele |
| Final run-through together, then submit | All |

---

## Deborah: Backend core (monitoring engine and failure reports)

Explains: *how KingaAPI checks APIs and diagnoses failures.*

```
backend/pom.xml
backend/src/main/resources/application.properties
backend/src/main/java/com/kinga/api/KingaApiApplication.java
backend/src/main/java/com/kinga/api/config/WebConfig.java
backend/src/main/java/com/kinga/api/model/            (Monitor, CheckResult, MonitorStatus, FailureType, ProviderIncident, MonitorTemplate)
backend/src/main/java/com/kinga/api/repository/       (all 4 repositories)
backend/src/main/java/com/kinga/api/service/CheckerService.java      sends the request, classifies the result
backend/src/main/java/com/kinga/api/service/MonitoringService.java   every-minute scheduler
backend/src/main/java/com/kinga/api/service/MonitorService.java      create / edit / list monitors
backend/src/main/java/com/kinga/api/service/ReportService.java       Dependency Failure Report + suggestions
backend/src/main/java/com/kinga/api/service/NotFoundException.java
backend/src/main/java/com/kinga/api/dto/              (MonitorRequest, MonitorSummary, FailureReport)
backend/src/main/java/com/kinga/api/controller/MonitorController.java
backend/src/main/java/com/kinga/api/controller/DashboardController.java
```

## Sifa: Frontend (React)

Explains: *what the developer sees and how the report reads.*

```
frontend/package.json, vite.config.js, index.html, public/shield.svg
frontend/src/main.jsx, App.jsx, api.js, format.js, OwnerContext.jsx, styles.css
frontend/src/hooks/usePolling.js
frontend/src/components/   (Layout, Status, StatCard, Sparkline, ResponseChart, IncidentBanner, HealthRing)
frontend/src/pages/Dashboard.jsx
frontend/src/pages/MonitorDetail.jsx
frontend/src/pages/FailureReport.jsx
frontend/src/pages/MonitorForm.jsx
```

## Gisele: Community intelligence, providers, demo and pitch

Explains: *“it's them, not you”, the shared setups, and runs the live demo.*

```
backend/src/main/resources/providers.json                         M-Pesa, MoMo, Airtel, Orange, Flutterwave, Paystack
backend/src/main/java/com/kinga/api/catalog/ProviderCatalog.java
backend/src/main/java/com/kinga/api/service/CommunityService.java  provider-wide outage detection
backend/src/main/java/com/kinga/api/dto/ProviderHealth.java
backend/src/main/java/com/kinga/api/mock/                         (MockMode, MockState, MockProviderController, DemoController)
backend/src/main/java/com/kinga/api/seed/DemoDataSeeder.java       demo developers + 24 h history
backend/src/main/java/com/kinga/api/controller/ProviderController.java
backend/src/main/java/com/kinga/api/controller/TemplateController.java
frontend/src/pages/Templates.jsx
frontend/src/pages/Providers.jsx
frontend/src/pages/DemoControl.jsx
README.md, TEAM.md, pitch slides, backup video
```
