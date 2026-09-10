# DailyMind — Free hosting options for the Spring Boot backend

This is a survey only, not a commitment. None of these links earn a referral
fee. All facts are taken from the platforms' public pricing pages as of
2026-09-08.

## What we need to deploy

- **App server** — Spring Boot 4.1.1, JDK 21, fat JAR from `server/`.
  Builds in ~50 s on the CI runner, image ≈ 250 MB.
- **Database** — PostgreSQL ≥ 16 (Flyway migrations need `pg_database`,
  `pg_extension`, plus the schema under
  `server/src/main/resources/db/migration/V1__init.sql`,
  `V2__article_table.sql`, `V3__article_meta.sql`).
- **Outbound URL** — the Android app reads `BASE_URL` from
  `app/src/main/java/com/dailymind/core/network/di/NetworkModule.kt` (currently
  `http://10.0.2.2:8080/` for the emulator). For a real device you need a
  public HTTPS URL.

## Decision matrix (free-tier only)

| Platform | What is free | What is **not** free | Cold-start penalty | Card required |
| --- | --- | --- | --- | --- |
| **Render Web Service** | 1 instance, 0.1 CPU / 512 MB RAM, 750 instance-hours/mo, custom domains | Persistent disks, scaling, private ingress | **Spins down after 15 min idle; ~30-60 s to wake** | No |
| **Render Postgres** | 1 instance, 1 GB, **expires 30 days after creation** (data gone after 14-day grace if not upgraded) | Backups, connection pooling, >1 GB | None — Postgres stays warm | No |
| **Neon (Postgres)** | 0.5 GB, 100 CU-h/mo, scale-to-zero after 5 min idle, 10 branches | >0.5 GB storage, instant restore | **Scale-to-zero** — first query after idle can take 1-2 s | No |
| **Railway** | $1 credit / month (≈ ~30 days at the smallest tier) | Anything above the $1/mo credit | None if you stay within budget | **Yes** (post-paid card) |
| **Fly.io** | Free trial only (3-day trial, then credit card) | Anything outside the trial | n/a | **Yes |
| **Supabase (Postgres)** | 500 MB, pause after 7 days inactivity, project never expires | Storage >500 MB, daily backups | None if pinged every 6 days | No |
| **Zeabur** | Generous free credits per month (~$5 worth, varies) | Anything above the credit | None if you stay within credits | No (card optional for upgrade) |

## Recommendation for " fully free, no card, hobby use "

1. **Neon** for the database.
   - Sign up at <https://neon.tech>, create a project.
   - Copy the JDBC URL + user + password into Render's environment as
     `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
     `SPRING_DATASOURCE_PASSWORD`.
   - Run the Flyway migrations once by either:
     - Connecting with `psql` and running `server/src/main/resources/db/migration/V*.sql`
       manually, **or**
     - Letting Spring Boot run them on first boot (`spring.flyway.enabled: true`
       is set in the template `application.yml`).

3. **Render** for the Spring Boot web service.
   - Connect the GitHub repo, point Render at the `server/` directory.
   - Use Render's "Docker" environment — point it at a new
     `server/Dockerfile` (see "What we still need to add" below).
   - Set the environment variables above plus
     `SERVER_PORT=8080` (Render maps `$PORT` automatically; see below).
   - Build command: `./gradlew bootJar`.
   - Start command: `java -jar build/libs/server-4.1.1.jar`.

The combined stack has no time bomb other than:
- the **first request after 15 min idle** pays a ~30-60 s cold start on
  Render, and
- Neon's first query after 5 min idle pays an extra ~1 s while the
  compute wakes up.

For a "open the app once a day" usage pattern, the cold start is
acceptable.

## Alternative: all-Render, expires in 30 days

If you can accept a **30-day expiry** (the free Render Postgres is wiped
30 days after creation with a 14-day grace), keep everything inside
Render:

- Render Postgres free → `SPRING_DATASOURCE_URL` etc.
- Render Web Service free (same as above).

Pro: same dashboard, private network between DB and web service, no
extra accounts.
Con: data loss after 30 days unless you re-create the database or upgrade.

## Alternative: Supabase instead of Neon

Supabase is fine if you want a single vendor for both auth and the
database. The free plan **pauses after 7 days of inactivity**, so you
must ping the database at least once a week. For a hobby app where the
backend only sees one user, that's easy to arrange with a free cron
service (cron-job.org, GitHub Actions schedule, etc.).

## What we still need to add to make the project deployable

None of the above platforms will compile the repo as-is; they each need
a small addition.

1. **`server/Dockerfile`** — Render and Fly want a container, not a raw
   Gradle project. A minimal one:
   ```dockerfile
   FROM eclipse-temurin:21-jdk AS build
     WORKDIR /src
     COPY . .
     RUN ./gradlew bootJar

   FROM eclipse-temurin:21-jre
     WORKDIR /app
     COPY --from=build /src/build/libs/server-*.jar /app/server.jar
     EXPOSE 8080
     ENTRYPOINT ["java","-jar","/app/server.jar"]
   ```
2. **Render port mapping** — Render injects `$PORT`; the current
   `application.yml` only honours `SERVER_PORT`. Add a one-liner to
   `Normalizer.java`'s neighbour config so `server.port: ${PORT:8080}` —
   or pass `-Dserver.port=$PORT` in the start command. Two-minute change.
3. **Android client URL** — once the backend is live, point
   `NetworkModule.BASE_URL` at the public Render URL. Done.

## What we should **not** do for free hosting

- Do **not** put the database on the active remote `192.168.80.152` box
  (currently used as the team's "production" PostgreSQL). That host is
  not internet-exposed and would expose internal credentials.
- Do **not** commit the deployed database credentials back into
  `application.yml` — the template already documents the env-var
  contract (`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`).
- Do **not** rely on Railway's $1 free credit for anything longer than a
  demo month — the credit resets and the service stops.

## TL;DR

For DailyMind's "free + no card + hobby" target:

> **Neon free Postgres + Render free Web Service**
>
> Cold start penalty of ~30-60 s on first request after 15 min idle.
> Otherwise fully free, no time limit, no card required.

If you can accept re-provisioning the database every 30 days, **all-Render**
is simpler to operate.

If you want the cheapest "mostly-on" experience and don't mind a card,
Railway's $1 free credit works for ~30 days at the smallest tier.