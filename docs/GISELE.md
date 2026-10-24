# Gisele: Community intelligence, providers, demo and pitch specification

**Role:** owner of the feature that makes KingaAPI special ("it's them, not you"), the African provider catalogue, the demo, and the pitch.
**In the pitch you explain:** *how KingaAPI knows it's the provider and not you, and you run the live demo.*

---

## 1. Your part in one paragraph

One developer on their own can't tell whether M-Pesa is down or their own code is broken. KingaAPI can, because many developers monitor the same providers. When **many of them fail at the same moment**, KingaAPI opens a **provider incident** and tells every affected developer: *it's them, not you*. You also own the list of providers (M-Pesa, MTN MoMo, Airtel Money, Orange Money, Flutterwave, Paystack), the **shared monitoring setups**, the **fake provider APIs** that can be broken on cue, and the **demo data**.

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
Open http://localhost:5173. Every backend restart rebuilds the demo data from scratch, which is handy if the demo gets messy.

## 3. How "it's them, not you" works

File: `backend/src/main/java/com/kinga/api/service/CommunityService.java`

After **every** check, `evaluate(provider)` runs:

1. Take every active monitor for that provider, **from all developers**.
2. Keep the ones checked in the **last 5 minutes**.
3. Count how many are failing.
4. If there are **at least 3 monitors** and **at least half are failing** (and at least 2), it **opens an incident** (or updates the open one).
5. When **no monitor is failing** any more, it **closes** the incident (it gets a `resolvedAt` time).

The thresholds are in `application.properties`:
```
kinga.community.min-monitors=3
kinga.community.failure-ratio=0.5
kinga.community.window-minutes=5
```

The failure report then gives the verdict (Deborah's `ReportService` uses your data):

| Situation | Verdict |
|---|---|
| A provider incident is open | **THEM**: "It's them, not you." |
| You fail, but every other developer on that provider is fine | **YOU**: "Probably your setup." |
| Some others fail too, but below the threshold, or too few monitors | **UNCLEAR** |

`health(provider)` also gives each provider a status for the Providers page and the live strip: **OPERATIONAL**, **DEGRADED** (some failing or slow), **OUTAGE** (incident open) or **UNKNOWN** (no recent data).

## 4. Your files

| File | What it does |
|---|---|
| `backend/src/main/resources/providers.json` | The provider catalogue: name, company, countries, colour, docs and status page links, plus 1–2 **endpoints** each (real sandbox URL, headers, expected status, expected JSON fields, sample response) |
| `catalog/ProviderCatalog.java` | Loads `providers.json` at startup and builds the mock URLs |
| `service/CommunityService.java` | Provider-wide outage detection (section 3) |
| `dto/ProviderHealth.java` | Live status of one provider (counts, developers, average ms, open incident) |
| `mock/MockProviderController.java` | **Fake provider APIs** at `/mock/{provider}/...`. When healthy, it answers with the sample response from `providers.json`. |
| `mock/MockMode.java`, `mock/MockState.java` | The failure modes and the current mode of each provider |
| `mock/DemoController.java` | Demo buttons: set a provider's mode, break one developer's key, reset everything |
| `seed/DemoDataSeeder.java` | Fills the database at startup: 10 developers, 26 monitors, shared setups, 24 h of history and one past MTN MoMo outage |
| `controller/ProviderController.java` | `/api/providers`, `/api/incidents` |
| `controller/TemplateController.java` | `/api/templates` (shared setups) |
| `frontend/src/pages/Providers.jsx` | Provider status cards and incident history |
| `frontend/src/pages/Templates.jsx` | Shared setups library ("Use" button) |
| `frontend/src/pages/DemoControl.jsx` | The page you drive during the demo |
| `README.md`, `TEAM.md` | Project docs |

## 5. The fake providers ("Demo mock")

Real sandbox keys for M-Pesa or MoMo take days to get, and a live demo needs failures **on cue**. So KingaAPI includes fake versions of each provider. Each one can be switched to:

| Mode | What the fake API does | What KingaAPI reports |
|---|---|---|
| Healthy | Correct answer in 60–350 ms | Up |
| Slow | Correct answer after 2.5–4 s | Slow |
| Flaky | Half of the requests return 500 | Server error, sometimes |
| 503 error | HTTP 503 Service Unavailable | Server error |
| Timeout | Never answers in time | Timeout |
| 401 for all | HTTP 401 for everyone | Auth failed |
| 429 limit | HTTP 429 Too Many Requests | Rate limited |
| Schema change | HTTP 200, but the JSON fields were renamed | Bad format + the list of missing fields |

**Break my key** swaps one developer's `Authorization` header for an invalid key. The fake API then returns 401 **only for that developer**, which shows the "it's you" story.

## 6. Demo data (who is in the system)

| Developer | Monitors |
|---|---|
| **you** (the demo user) | M-Pesa token, M-Pesa STK Push, MTN MoMo token, Flutterwave bank list, Paystack verify |
| amina-nairobi | M-Pesa ×2, Airtel Money, Flutterwave |
| brian-kisumu | M-Pesa, Airtel Money |
| kwame-accra | MTN MoMo, Paystack, Flutterwave |
| chidi-lagos | Paystack ×2, Flutterwave |
| grace-kampala | MTN MoMo ×2, Airtel Money |
| awa-abidjan | Orange Money, MTN MoMo |
| moussa-dakar, fatou-bamako | Orange Money |
| zanele-joburg | Paystack, M-Pesa |

History: one check every 10 minutes for the last 24 h, a few random timeouts, an **MTN MoMo outage 6 hours ago (40 minutes, all MTN monitors)**, and **Paystack slowness 14 hours ago**.

## 7. The live demo script (2 minutes)

Before you start, click **Reset everything** on Demo control and open the Dashboard as `you`.

| Time | Do | Say |
|---|---|---|
| 0:00 | Show the Dashboard | "This developer depends on M-Pesa, MTN MoMo, Flutterwave and Paystack. All green, 24 hours of history." |
| 0:15 | Demo → M-Pesa → **503 error**, then back to the Dashboard | "Now M-Pesa goes down, for everyone." |
| 0:25 | Purple banner appears; point at it | "KingaAPI sees 6 of 6 M-Pesa monitors from 4 different developers failing at once. So it tells you: *it's them, not you*." |
| 0:40 | Click **View failure report** | "Expected 200, got 503, here's the response body and what to do: retry with backoff, queue payments, check the status page." |
| 1:00 | Demo → **Reset everything**, then **Break my key** for MTN MoMo | "Different story: this time only my key is wrong." |
| 1:10 | Open the MTN monitor's report | "Only my monitor fails. 4 other MTN monitors from 3 developers are fine, so KingaAPI says *probably your setup*, and points to credentials and token expiry." |
| 1:30 | Demo → Paystack → **Schema change**, then open the Paystack report | "The scariest failure: HTTP 200, but the fields changed. KingaAPI lists exactly which fields disappeared." |
| 1:45 | Shared setups page | "Developers share ready-made setups for African providers. One click and you're monitoring." |
| 2:00 | Reset everything | |

Backup: if the Wi-Fi or a laptop fails, play the recorded video (task 4 below). The demo itself needs **no internet**: everything runs on the laptop. Only the fonts load from Google.

## 8. Tasks for today

| # | Task | Done when |
|---|---|---|
| 1 | Pull the repo and run backend + frontend | Demo control page shows all 6 providers |
| 2 | Rehearse the demo script (section 7) **3 times** with a timer | Under 2 minutes, without reading |
| 3 | Build the **pitch slides** (section 9) using Sifa's screenshots | 6–8 slides |
| 4 | Record a **backup video** of the demo (Windows: Win + G, or OBS) | 2-minute MP4 saved on 2 laptops and a USB stick |
| 5 | Check the README "Run it" steps on a clean laptop | Someone else can start the app from the README alone |
| 6 | (Bonus) Add another provider to `providers.json` (e.g. Chipper Cash or Wave) | It shows on Providers, Shared setups and Demo after a restart |

To add a provider, copy one block in `providers.json` and change `code` (UPPER_CASE), `slug` (lower-case, used in the mock URL), `name`, `countries`, `color`, links and endpoints. No Java change is needed. Restart the backend.

## 9. Pitch outline (5 minutes)

1. **Problem** (Gisele, 40 s): African apps depend on payment APIs that fail often. *608 mobile money operator incidents in 90 days* logged by one aggregator, and *41% of teams learn about outages from their customers*. When a payment fails, developers can't tell if it's their code or the provider.
2. **Solution** (Gisele, 20 s): KingaAPI, *kinga* means protection in Swahili. Monitors your provider APIs every minute and tells you whose problem it is.
3. **How it works** (Deborah, 40 s): every-minute checks; classifying the failure (auth, rate limit, server error, timeout, format); the failure report with suggestions.
4. **What you see** (Sifa, 40 s): dashboard, banner, report, mobile.
5. **Live demo** (Gisele, 2 min): section 7.
6. **Why it's different** (Gisele, 20 s): shared data across developers. Generic tools like UptimeRobot only see *your* endpoint; KingaAPI sees the whole community, so it can say "it's them, not you". Built for M-Pesa, MoMo, Airtel, Orange, Flutterwave and Paystack.
7. **Next steps** (20 s): real accounts, SMS/WhatsApp alerts, public status pages per provider, more providers.

## 10. Questions judges may ask you

| Question | Answer |
|---|---|
| How do you know it's the provider? | When at least 3 monitors exist and half or more of them, from different developers, fail within 5 minutes, it's very unlikely everyone broke their code at once. |
| What if only one developer uses a provider? | The verdict is "Not enough shared data yet". The report still shows the failure and suggestions. More users make it smarter. |
| Are these real APIs? | The demo uses built-in fake versions so failures happen on cue. Every setup also has the **real sandbox URL**; choose "Live sandbox" when adding a monitor and add your keys. |
| Is sharing safe? | Shared setups never include secrets. Header values like `Authorization` are replaced with placeholders automatically. |
| How would this make money? | Free for small developers (who also feed the shared data); paid plans for teams: more monitors, faster checks, SMS and WhatsApp alerts, history. |

## 11. Git rules

- **Only edit your own files** (section 4).
- Run `git pull` before you start and before you push.
- Small commits with clear messages, e.g. `Add Wave to providers.json`.
- If `git pull` shows a conflict, stop and ask Deborah before going further.
