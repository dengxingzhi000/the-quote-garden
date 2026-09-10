# Batch Translation Fill Design

**Date:** 2026-09-09
**Status:** Draft — pending user review
**Scope:** Backfill `quote.translation` and `article.translation` for rows where translation IS NULL on the server-side PostgreSQL database.

## 1. Goal

Provide a one-shot, reproducible way to fill the `translation` column for existing English content in `quote` and `article` tables. Translation target: Simplified Chinese (zh-CN). The default provider is **MyMemory** (free, no API key). The architecture is pluggable so a paid provider can be swapped in later without touching callers.

## 2. Non-goals

- No schema change. The `translation` column already exists in both tables (V1, V2 migrations).
- No on-demand translation at request time. Only batch backfill; runtime API responses stay unchanged.
- No new App-facing feature. The Android client already has `translation` on the DTO; once the column is filled, the client gets Chinese translations for free.
- No importer integration. Existing `QuoteGardenImporter.importHtml` is untouched; future imports remain English-only and rely on a subsequent batch run.
- No third language. Single target (zh-CN).
- No admin UI or REST endpoint. The batch is a Spring profile, not a runtime feature.

## 3. Architecture

```
server/src/main/java/com/quotegarden/importer/
  TranslationService.java          (existing) - interface + provider selection
  MyMemoryTranslationService.java  (new)      - @Component, free provider
  BatchTranslateRunner.java        (new)      - @Component, CommandLineRunner, profile-gated
```

### 3.1 `TranslationService` (existing)

Already declares:
```java
public interface TranslationService {
    String translate(String englishContent);
}
```

A `@ConditionalOnProperty(name = "app.translator.provider", havingValue = "mymemory", matchIfMissing = true)` selects the MyMemory implementation. The current `NoopTranslationService` is removed (callers already tolerate `null`).

### 3.2 `MyMemoryTranslationService` (new)

```java
@Component
@ConditionalOnProperty(name = "app.translator.provider", havingValue = "mymemory", matchIfMissing = true)
class MyMemoryTranslationService implements TranslationService {
    private static final String URL = "https://api.mymemory.translated.net/get";
    private final RestTemplate rt = new RestTemplate();
    private final long perCallDelayMs;

    public MyMemoryTranslationService(
        @Value("${app.translator.per-call-delay-ms:200}") long perCallDelayMs
    ) { this.perCallDelayMs = perCallDelayMs; }

    @Override
    public String translate(String englishContent) {
        if (englishContent == null || englishContent.isBlank()) return null;
        try {
            Thread.sleep(perCallDelayMs);
            String url = URL + "?q=" + URLEncoder.encode(englishContent, StandardCharsets.UTF_8)
                       + "&langpair=en|zh-CN";
            Map<?,?> res = rt.getForObject(url, Map.class);
            Map<?,?> data = (Map<?,?>) res.get("responseData");
            String t = (String) data.get("translatedText");
            if (t == null || t.isBlank()) return null;
            // MyMemory encodes "I'M BACK" / quota errors as plain translatedText with low quality
            if (t.toUpperCase().contains("PLEASE ENTER TWO") || t.contains("MYMEMORY WARNING")) return null;
            return t;
        } catch (Exception e) {
            log.warn("translate failed ({} chars): {}", englishContent.length(), e.toString());
            return null;
        }
    }
}
```

The 200ms default delay is empirically safe for the anonymous tier (~5000 chars/day/IP). Configurable via `app.translator.per-call-delay-ms` for slower/paid tiers.

### 3.3 `BatchTranslateRunner` (new)

```java
@Component
@Profile("translate-batch")
public class BatchTranslateRunner implements CommandLineRunner {
    private final QuoteRepository quotes;
    private final ArticleRepository articles;
    private final TranslationService translator;
    private final int pageSize;

    @Override
    @Transactional
    public void run(String... args) {
        translateTable("quote",   this::nextQuotePage);
        translateTable("article", this::nextArticlePage);
    }
    // translateTable loops page 0..N until a page comes back partial or empty;
    // for each row: translator.translate(row.content) -> if non-null, mutate row.translation and save.
}
```

Pagination strategy: `Pageable` with a fixed `pageSize` (default 50, configurable via `app.translator.page-size`). For each row in the page, call `translator.translate(content)`. If the result is non-null, set `row.translation` and `row.updatedAt = System.currentTimeMillis()` and `save()`. After the page is processed, the JPA flush at transaction commit issues the UPDATEs.

### 3.4 Repository additions

- `QuoteRepository`: add `Page<Quote> findByTranslationIsNull(Pageable p)`.
- `ArticleRepository`: mirror the same.

The runner uses load-then-save: read each row, mutate `translation`, `save()`. JPA dirty-checking flushes on transaction commit; no custom `@Modifying` query is needed and there is no risk of overwriting a parallel write because the `translation IS NULL` filter in the finder re-asserts the precondition on every page load.

## 4. Wiring

- `application.yml`:
  ```yaml
  app:
    translator:
      provider: mymemory           # mymemory (default)
      per-call-delay-ms: 200
      page-size: 50
  ```
- Profiles:
  - `application.yml` (default) — server boots as today; `BatchTranslateRunner` is not active.
  - `application-translate-batch.yml` (new) — overrides the active profile flag; running with `--spring.profiles.active=translate-batch` activates the runner.

### Invocation

```bat
:: from server/
.\gradlew.bat bootRun --args="--spring.profiles.active=translate-batch"
```

Alternative (Gradle `bootJar` + java):
```bat
.\gradlew.bat bootJar
java -jar build\libs\quote-garden-server-*.jar --spring.profiles.active=translate-batch
```

The Spring context starts, runs the batch, then exits (no web server bound unless `SERVER_PORT` is set — and even if set, the runner blocks startup until done, then `SpringApplication` shuts down when the runner finishes if `spring.main.web-application-type=none` is set in the `translate-batch` profile).

For the production remote DB (`192.168.80.152:5432`), set the same env vars used today:
```bat
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://192.168.80.152:5432/quote_garden"
$env:SPRING_DATASOURCE_USERNAME="..."
$env:SPRING_DATASOURCE_PASSWORD="..."
.\gradlew.bat bootJar
java -jar build\libs\quote-garden-server-*.jar --spring.profiles.active=translate-batch
```

## 5. Data Flow

```
[profile=translate-batch startup]
        │
        ▼
BatchTranslateRunner.run()
        │
        ├──► quote page 1 (50 rows, translation IS NULL)
        │       ├── row.content ──► MyMemory.translate ──► zh-CN text
        │       └── UPDATE quote SET translation = :t WHERE id = :id AND translation IS NULL
        │   (page done, flush, log progress [1/20 (50/1000)])
        ├──► quote page 2 ...
        │
        ▼
   article pages (same)
        │
        ▼
   Spring context shutdown
```

The `updated_at` bump is the same one used by the sync protocol; clients pull the changed rows on next sync (`updatedAfter` is epoch millis). No app-side change needed.

## 6. Error Handling

| Risk | Behavior |
|---|---|
| Single row translate fails (network, MyMemory returns error text) | WARN log, skip row, count++ in `failed` tally. Next rows continue. |
| MyMemory returns "PLEASE ENTER TWO DISTINCT LANGUAGES" or quota warning | Detect and return null from the service; row skipped silently. |
| Page exception (DB blip) | Caught at page boundary; batch aborts with ERROR summary; partial progress persisted. |
| Long content (>500 chars) hits MyMemory per-request size limit | The current MyMemory free tier allows up to ~500 chars per call. Longer `article.content` rows fail; log WARN, skip. Out of scope for this design (article translation backfill is best-effort). |
| Re-running the script | `WHERE translation IS NULL` makes it idempotent. Already-translated rows are untouched. |
| Profile left on by accident | Server boots as a CLI, exits when done. No long-lived web server (see §4 about `web-application-type=none`). |

## 7. Testing

- `MyMemoryTranslationServiceTest`:
  - Happy path: mock `RestTemplate` to return JSON `{"responseData":{"translatedText":"你好"}}` → returns "你好".
  - Quota warning: `"PLEASE ENTER TWO..."` → returns null.
  - Empty / null input → returns null.
  - HTTP exception → returns null, no throw.
- `BatchTranslateRunnerTest`:
  - Mock repositories + `TranslationService`. Provide 3 pages of rows; assert each row gets UPDATEd exactly once.
  - Service returns null → row's translation stays null (no UPDATE issued).
  - Page throws → batch aborts with summary, prior progress preserved.
- `QuoteRepositoryTest` / `ArticleRepositoryTest`: smoke test for `findByTranslationIsNull` using the test profile's H2 DB (the project already wires `tasks.test { useJUnitPlatform() }`; add `@DataJpaTest` slice).
- Manual end-to-end against the local Docker Compose DB:
  - `cd server && docker compose up -d db`
  - Run the importer once to seed a few rows (existing `QuoteGardenImporter.importHtml` tests cover that path; for manual, run `QuoteGardenImporterApplication` if a main exists, else insert 3 rows via `psql`).
  - Invoke `bootRun --spring.profiles.active=translate-batch` against `localhost:5432`.
  - `SELECT id, content, translation FROM quote WHERE translation IS NULL;` → empty.

## 8. Manual Verification

- `git status`: only the files listed in §9 below are changed.
- `./gradlew.bat test` from `server/`: BUILD SUCCESSFUL.
- `./gradlew.bat bootJar` from `server/`: BUILD SUCCESSFUL.
- Empty-database run (Docker Compose db, no rows) → runner logs `[quote] 0 rows to translate`, exits cleanly.
- 3-row seeded DB run → all 3 translations non-null after the run, `updated_at` advanced.
- Re-run → 0 rows touched, logs `0 rows to translate`.

## 9. Files Changed

1. `server/src/main/java/com/quotegarden/importer/TranslationService.java` — drop `NoopTranslationService`, keep interface.
2. `server/src/main/java/com/quotegarden/importer/MyMemoryTranslationService.java` — new.
3. `server/src/main/java/com/quotegarden/importer/BatchTranslateRunner.java` — new.
4. `server/src/main/java/com/quotegarden/quote/infrastructure/QuoteRepository.java` — add `findByTranslationIsNull(Pageable)`.
5. `server/src/main/java/com/quotegarden/article/infrastructure/ArticleRepository.java` — add `findByTranslationIsNull(Pageable)`.
6. `server/src/main/resources/application.yml` — add `app.translator.*` defaults.
7. `server/src/main/resources/application-translate-batch.yml` — new, sets `spring.main.web-application-type=none`.
8. `server/src/test/java/com/quotegarden/importer/MyMemoryTranslationServiceTest.java` — new.
9. `server/src/test/java/com/quotegarden/importer/BatchTranslateRunnerTest.java` — new.
10. `docs/superpowers/specs/2026-09-09-batch-translation.md` — this spec (committed separately).
11. `docs/superpowers/plans/2026-09-09-batch-translation.md` — implementation plan (created via writing-plans skill after spec approval).

## 10. Risks & Rollback

- **Risk: MyMemory availability / quota exhausted mid-batch.** Mitigation: progress is committed per row, so a re-run picks up where the previous stopped. Worst case: switch `app.translator.provider=baidu` once a paid key is provisioned and re-run.
- **Risk: low-quality machine translation.** Mitigation: this is a free, best-effort backfill. If quality is unacceptable, the user can edit rows manually (`UPDATE quote SET translation='...' WHERE id=...`) — nothing in this design prevents manual override.
- **Risk: stale `updated_at` causes client re-fetch storms.** Mitigation: client uses `updatedAfter` cursor; the bumped `updated_at` only triggers sync of rows whose `translation` actually changed (delta sync protocol unchanged).
- **Rollback:** remove the new files (§9.2, .3, .7); revert .1 (re-add `NoopTranslationService`) and .4, .5 (remove the new finder). `git checkout --` is sufficient for files .1, .4, .5, .6. Files .2, .3, .7, .8, .9 are net-new and `rm` is enough.
