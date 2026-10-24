# Sifa: Frontend specification

**Role:** owner of everything the developer sees in KingaAPI.
**In the pitch you explain:** *what the developer sees, and how the Dependency Failure Report reads.*

---

## 1. Your part in one paragraph

The frontend is a React app (built with Vite) that talks to the Spring Boot backend at `http://localhost:8085`. It shows a developer their monitored payment APIs, warns them when a provider is down ("It's them, not you"), and gives them a clear **Dependency Failure Report** when something breaks. It refreshes itself every few seconds, so outages appear live during the demo without anyone pressing reload.

## 2. Run it

```bash
cd backend
mvn spring-boot:run
```
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. The backend must be running first, or every page shows "Something went wrong".

## 3. How the frontend is built

```
main.jsx            starts React, wraps the app in the router and OwnerProvider
└─ App.jsx          the list of pages (routes)
   └─ Layout.jsx    floating top bar + live provider strip + phone dock, around every page
      └─ pages/*    one file per screen
```

| Idea | Where | What it means |
|---|---|---|
| **All backend calls in one place** | `api.js` | Pages never call `fetch` directly. They call `api.dashboard(owner)`, `api.report(id)` and so on. If the backend URL changes, only this file changes. |
| **Auto-refresh** | `hooks/usePolling.js` | `usePolling(loadFn, deps, ms)` loads data, then reloads it every `ms` milliseconds. It returns `{ data, error, loading, reload }`. The dashboard uses 5000 ms. |
| **Current developer** | `OwnerContext.jsx` | There is no login. The developer is picked in the top-right badge and remembered in the browser. The default is `you`. |
| **Formatting helpers** | `format.js` | `timeAgo`, `formatTime`, `formatDuration`, and readable labels for failure types ("Server error", "Rate limited"…). |
| **Design** | `styles.css` | The "Aurora" design: frosted-glass panels, a sunset gradient (emerald → amber → coral), light and dark mode, and phone layout. All colours are CSS variables at the top of the file. |

## 4. Your files

| File | What it does |
|---|---|
| `src/components/Layout.jsx` | Floating top bar (logo, links, developer badge), live provider strip ("LIVE · M-Pesa 230ms…"), and the bottom dock on phones |
| `src/components/Status.jsx` | Small reusable pieces: status pills (Up / Down / Slow), provider tags, loading and error boxes |
| `src/components/HealthRing.jsx` | The circular uptime gauge on the dashboard |
| `src/components/IncidentBanner.jsx` | The purple **"It's them, not you"** banner |
| `src/components/Sparkline.jsx` | Tiny response-time line in each monitor card |
| `src/components/ResponseChart.jsx` | Response-time chart on the monitor page (Recharts). Failed checks are red dots. |
| `src/components/StatCard.jsx` | Number tiles on the monitor page |
| `src/pages/Dashboard.jsx` | Home: welcome block with uptime ring, Up/Down/Slow tiles, monitor cards, Community pulse, Recent incidents |
| `src/pages/MonitorDetail.jsx` | One monitor: chart, recent checks, what KingaAPI expects, plus the Check now, Edit, Share and Delete buttons |
| `src/pages/FailureReport.jsx` | The **Dependency Failure Report** (see section 5) |
| `src/pages/MonitorForm.jsx` | Add or edit a monitor: pick a shared setup, choose Demo mock or Live sandbox, fill the form |

Gisele owns `Templates.jsx`, `Providers.jsx` and `DemoControl.jsx`. They use your components and `styles.css`, so tell her before you rename a CSS class.

## 5. The Dependency Failure Report: your key screen

This is the screen judges will remember. It reads from top to bottom like a story:

1. **Verdict.** A big coloured card with one of four answers:
   - 💜 **"It's them, not you."** (`THEM`): many developers on this provider are failing at once.
   - ❤️ **"Probably your setup."** (`YOU`): only you are failing; everyone else on this provider is fine.
   - 🧡 **"Still investigating"** (`UNCLEAR`): a few others fail too, but not enough to be sure.
   - 💚 **Healthy** (`NONE`): this monitor has never failed.
2. **What failed:** the failure type, the error message, and for how long.
3. **Expected vs What came back:** side by side. Expected status and fields on the left; actual status, missing fields (crossed out) and time on the right.
4. **What to check next:** numbered steps written for that failure type (auth, rate limit, server error, timeout, format…).
5. **Response body:** exactly what the provider returned.
6. **Side panel** (stays visible while you scroll): how many other developers are affected, a timeline of recent checks, and links to the provider status page and docs.
7. **Copy report:** copies a plain-text version for a support ticket or team chat.

The backend builds all the content (`GET /api/monitors/{id}/report`). Your job is to make it **clear and readable at a glance**.

## 6. Tasks for today

| # | Task | Done when |
|---|---|---|
| 1 | Pull the repo and run backend + frontend | The dashboard shows 5 green monitors for `you` |
| 2 | Click through **every page**: Dashboard, a monitor, its report, Add monitor, Edit, Shared setups, Providers, Demo | No page shows an error; every button does something |
| 3 | Check **phone width**: in Chrome, press F12, then the phone icon, then pick "iPhone 12" | Nothing spills off the side; the bottom dock works |
| 4 | Check **light and dark mode**: in Chrome DevTools, open Rendering, then "Emulate prefers-color-scheme" | Text is readable in both |
| 5 | Run the outage: on Demo, set M-Pesa to **503 error**, then go to the Dashboard | Purple banner appears within ~5 s; the M-Pesa cards turn red with a "Report →" tag |
| 6 | Open the report from the banner | Verdict says "It's them, not you." |
| 7 | Take **screenshots** for the slides: dashboard (normal), dashboard with banner, failure report, phone view | 4–5 clear PNGs sent to Gisele |
| 8 | (Bonus) Polish: spacing, wording, empty states | Your call, but keep commits small |

## 7. Your part of the pitch (about 40 seconds)

> "This is what a developer sees. Every payment API they depend on is checked once a minute: status, speed and response format. When M-Pesa goes down, they don't get a vague error. They get this banner: *it's them, not you*. One click opens the Dependency Failure Report: what we expected, what came back, the exact response, and what to check next. On a phone it works the same, so a developer can tell in seconds, wherever they are, whether to fix their code or just wait for the provider."

## 8. Questions judges may ask you

| Question | Answer |
|---|---|
| Why React? | Fast to build interactive dashboards, a huge ecosystem (Recharts for charts, React Router for pages), and the team knows it. |
| How does it update live? | The `usePolling` hook reloads data every 5 seconds. A next step would be WebSockets for instant push. |
| Is there a login? | Not in this prototype. The developer is chosen from a list. Real accounts are an obvious next step. |
| Does it work on mobile? | Yes. The layout switches to one column with an app-style bottom dock. |
| Why "Copy report"? | Developers often have to message the provider's support or their team. The copied text has everything they need. |

## 9. Git rules

- **Only edit your own files** (section 4). For `styles.css`, tell the team before big changes.
- Run `git pull` before you start and before you push.
- Small commits with clear messages, e.g. `Improve failure report spacing on phones`.
- If `git pull` shows a conflict, stop and ask Deborah before going further.
