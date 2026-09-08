# Deploying DailyMind to Neon + Render (free)

This is the end-to-end recipe for going from a clean checkout to a public
Spring Boot 4.1.1 service talking to a free PostgreSQL. Everything in this
folder is now ready: `server/Dockerfile`, `render.yaml`, and the
`$PORT` / actuator wiring in `server/src/main/resources/application.yml`.

The only thing that **cannot** be automated from inside the repo is the
initial click-through on the Neon and Render dashboards. The walkthrough
below takes about ten minutes.

---

## 1. Provision the database on Neon

1. Create a free account at <https://neon.tech>.
2. **New Project → Region: pick closest to your phone (e.g. `Asia
   Pacific (Singapore)`). PostgreSQL 16. Free plan.**
3. On the project dashboard, copy three values:
   - **Connection string** — looks like
     `postgresql://neondb_owner:...@ep-xxx-yyy.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
   - **User** — the bit before the colon (`neondb_owner`).
   - **Password** — the bit after the colon.
4. The schema is managed by Flyway, so you do **not** need to run the
   migrations manually. Spring Boot runs them on first boot
   (`spring.flyway.enabled: true` is already on). All three migration
   files in `server/src/main/resources/db/migration/` (`V1__init.sql`,
   `V2__article_table.sql`, `V3__article_meta.sql`) are idempotent and
   boot-safe.

> **Tip:** Neon free tier auto-scales to zero after ~5 min of inactivity.
   For the first request after that, add 1-2 s to the wake-up.

---

## 2. Deploy the backend on Render

You have two paths; the Blueprint is one click.

### Option A — Blueprint (one click, uses `render.yaml`)

1. Go to <https://dashboard.render.com/b/blueprints>.
2. **New Blueprint Instance** → connect this GitHub repo
   (`dengxingzhi000/the-quote-garden`) → branch `master`.
3. Render reads `render.yaml` and proposes a single Web Service named
   `dailymind-server`, free plan, Docker runtime, healthcheck on
   `/actuator/health`. Click **Apply**.
4. Wait for the first build (~3 min, it pulls Temurin 21 and runs
   `./gradlew bootJar`). The deploy log ends with
   `BUILD SUCCESSFUL` and `BootJar`.
5. **Open the service → Environment → set the three secrets:**
   ```
   SPRING_DATASOURCE_URL = postgresql://neondb_owner:...@ep-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   SPRING_DATASOURCE_USERNAME = neondb_owner
   SPRING_DATASOURCE_PASSWORD = ******
   ```
6. Render redeploys automatically. The next log line to look for:
   ```
   o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080
   o.s.b.a.f.web.DefaultLifecycleProcessor  : Started DailyMindServerApplication in 12.3 seconds
   ```
7. The public URL is at the top of the service page — something like
   `https://dailymind-server.onrender.com`.

### Option B — Manual (only needed if you prefer not to use the Blueprint)

1. **Dashboard → New → Web Service** → connect the same repo.
2. **Root Directory:** `server`
3. **Runtime:** `Docker`
4. **Dockerfile Path:** `./Dockerfile` (relative to the root directory).
5. **Plan:** Free.
6. **Health Check Path:** `/actuator/health`.
7. **Environment:** same three variables as in Option A.
8. Click **Create Web Service**.

---

## 3. Smoke-test the live backend

```bash
# 1. The daily endpoint needs at least one row in the `quote` table. If
#    you already pushed data, this returns 200. If not, it throws and
#    that's also a sign Flyway + JDBC + TLS all wired up.
curl -i https://dailymind-server.onrender.com/api/v1/quotes/daily

# 2. The sync endpoint works against an empty database:
curl -i 'https://dailymind-server.onrender.com/api/v1/sync/quotes?updatedAfter=0&cursor=null&limit=10'

# 3. Health check (used by Render's healthcheck).
curl -i https://dailymind-server.onrender.com/actuator/health
# → {"status":"UP"}
```

If the sync endpoint returns 200 with `{"items":[],"nextCursor":null}`,
you have a working deployment.

---

## 4. Point the Android app at it

Edit
`app/src/main/java/com/dailymind/core/network/di/NetworkModule.kt`,
find:

```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/"
```

and replace it with:

```kotlin
// emulator → http://10.0.2.2:8080/
// real device → https://dailymind-server.onrender.com/
private const val BASE_URL = "https://dailymind-server.onrender.com/"
```

Rebuild and install the APK on a phone on the same WiFi as the
developer machine (or just `adb install` over USB).

---

## 5. Cold-start behaviour

Render's free plan spins the service down after 15 minutes without
inbound traffic. The first request after that takes ~30-60 s to wake
up; subsequent requests are normal. Neon's free compute scales down
after ~5 min, so the first request to the API right after a long idle
period can be a few seconds slower. Both are documented platform
behaviours — accept them, or upgrade.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `BUILD FAILED` during the Render build | `./gradlew` not executable, or network blocked from the build container | Make sure `server/gradlew` is `chmod +x`; Render runs the build inside the container, so a `Dockerfile` `RUN chmod +x ./gradlew` is already in place. |
| `503` on `/api/v1/quotes/daily` | Database empty, the controller throws `IllegalStateException` | Use the sync endpoint to confirm DB connection, then load sample quotes with the importer or `psql`. |
| `Connection refused` | `SPRING_DATASOURCE_URL` does not include `?sslmode=require` for Neon | Add `?sslmode=require` to the end of the JDBC string. |
| Health check fails | Boot takes >30 s on free plan | The default `start-period` in the Dockerfile is 40 s; bump it to 60 s if you start seeing `for health check failed` in Render logs. |
| Cold start every page load | Service is constantly idling | Set up a cron ping (cron-job.org is free) that hits `/actuator/health` every 14 minutes. Render considers the service "active" while it serves HTTP. |

---

## What this PR adds

- `server/Dockerfile` — multi-stage Temurin 21 build → fat JAR → small
  JRE runtime image with `HEALTHCHECK` and `$JAVA_OPTS` container-aware
  sizing.
- `server/src/main/resources/application.yml` — server port now reads
  `${SERVER_PORT:${PORT:8080}}`, so it works for both local
  `SERVER_PORT=8080` and Render's injected `PORT`. Adds an `/actuator/health`
  endpoint for the Render healthcheck.
- `server/build.gradle.kts` — pulls in
  `spring-boot-starter-actuator` so `/actuator/health` exists.
- `render.yaml` (repo root) — Render Blueprint that builds the
  Dockerfile from the `server/` sub-directory and wires the
  `SPRING_DATASOURCE_*` env vars.

No production credentials are committed.