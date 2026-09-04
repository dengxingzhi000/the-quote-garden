# QuoteGarden Crawler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 Python 全量爬虫 + Java Jsoup 长期 Importer，抓取 www.quotegarden.com 全站填充 `quote` 表支撑每日一句。

**Architecture:** Python `tools/crawler/quotegarden.py` 先全量入库（幂等 SHA256 去重）；Java 新增纯函数 `QuoteGardenHtmlParser` + 重构 `QuoteGardenImporter` 复用 `Normalizer`/`QuoteRepository` 做长期增量；翻译独立两阶段，`translation` 一期可空。

**Tech Stack:** Python 3.11 + requests + beautifulsoup4 + lxml + psycopg2-binary；Java 21 + Spring Boot 3.4.5 + Jsoup 1.18.3 + JPA/PostgreSQL；Flyway V1 不变。

---

## File Structure

```
tools/crawler/
  requirements.txt              # requests, beautifulsoup4, lxml, psycopg2-binary
  quotegarden.py                # 分类发现 + 解析 + 入库 + 断点续爬 + --self-test
  translate.py                  # 二期：MyMemory 免费翻译回填 + 本地缓存
  README.md                     # 来源、礼貌策略、配额风险、运行命令
  fixtures/happiness_sample.html# 3条真实片段（多行诗/<i>书名/注释块/无~块）
server/
  build.gradle.kts              # 新增 org.jsoup:jsoup:1.18.3
  src/main/java/com/dailymind/importer/
    ParsedQuote.java            # record(content, author, category)
    QuoteGardenHtmlParser.java  # 纯函数 parse(html, category)
    QuoteGardenImporter.java    # 重构：importCategory/importAll（HTML），保留旧importFrom标记Deprecated
    TranslationService.java     # 接口 + Noop 实现（一期不翻译）
  src/test/java/com/dailymind/importer/
    QuoteGardenHtmlParserTest.java # TDD：happiness片段断言
```

---

### Task 1: Python fixtures + requirements + README 骨架

**Files:**
- Create: `tools/crawler/requirements.txt`
- Create: `tools/crawler/fixtures/happiness_sample.html`
- Create: `tools/crawler/README.md`

- [ ] **Step 1: Write the failing self-test fixture (no parser yet, assert files exist)**

```python
# tools/crawler/selftest_files.py (临时校验脚本，不提交，仅用于本步骤演示)
import pathlib
assert pathlib.Path("tools/crawler/requirements.txt").exists()
assert pathlib.Path("tools/crawler/fixtures/happiness_sample.html").exists()
print("files ok")
```

- [ ] **Step 2: Run to verify it fails**

Run: `python tools/crawler/selftest_files.py`
Expected: FAIL `AssertionError` / `FileNotFoundError`（文件尚不存在）

- [ ] **Step 3: Write minimal implementation**

```txt
# tools/crawler/requirements.txt
requests==2.32.3
beautifulsoup4==4.12.3
lxml==5.2.2
psycopg2-binary==2.9.9
```

```html
<!-- tools/crawler/fixtures/happiness_sample.html -->
<div class="quotes-section">
Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<!--tpvldg--><BR>
<BR>
<BR>
Glory's not otherwhere but here:<BR>
Yonder stars may be more horrible than the worst of Earth.<BR>
~James Oppenheim, "The Wise," <i>War and Laughter</i>, 1916<!--Qe5--><BR>
<BR>
<BR>
<!--Happiness is amazing ~Ricky Gervais, <i>After Life</i>, 2019<BR><BR><BR>-->
One joy scatters a hundred griefs. ~Chinese proverb<!--nmre--><BR>
<BR>
<BR>
This block has no tilde and must be skipped
<BR>
<BR>
<BR>
</div>
```

```markdown
<!-- tools/crawler/README.md -->
# QuoteGarden crawler
来源: https://www.quotegarden.com/ (© 1998–2026 All rights reserved，仅抓取事实性短文本+署名，展示保留作者)
礼貌: UA `DailyMindSeed/1.0`，间隔1-1.5s，超时20s，重试3次，断点续爬 progress.json
配额: 翻译用 MyMemory 匿名（额度小），全站翻译分多天 `--limit/--offset`
运行:
python tools/crawler/quotegarden.py --self-test
python tools/crawler/quotegarden.py --limit 3
python tools/crawler/quotegarden.py --all
python tools/crawler/translate.py --limit 50
```

- [ ] **Step 4: Run to verify it passes**

Run: `python tools/crawler/selftest_files.py`
Expected: PASS `files ok`（之后删除该临时脚本）

- [ ] **Step 5: Commit**

```bash
git add tools/crawler/requirements.txt tools/crawler/fixtures/happiness_sample.html tools/crawler/README.md
git commit -m "feat(crawler): add python requirements, happiness fixture and readme"
```

---

### Task 2: Python quotegarden.py — 解析 + self-test

**Files:**
- Create: `tools/crawler/quotegarden.py`
- Test: `tools/crawler/fixtures/happiness_sample.html` (复用)

- [ ] **Step 1: Write the failing test (self-test entry)**

```python
# 运行即测试（TDD红）：
# python tools/crawler/quotegarden.py --self-test
# 期望：解析出3条，首条content以"Whenever you are sincerely pleased"开头且author含"Ralph Waldo Emerson"，
# 注释块被跳过，无~块被跳过；当前文件不存在故 FAIL ModuleNotFound
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python tools/crawler/quotegarden.py --self-test`
Expected: FAIL `can't open file ... quotegarden.py` / `No such file`

- [ ] **Step 3: Write minimal implementation**

```python
# tools/crawler/quotegarden.py
"""QuoteGarden 固定爬虫：分类发现 + HTML解析 + Postgres幂等入库."""
import argparse, hashlib, json, os, random, re, sys, time
from pathlib import Path
import requests
from bs4 import BeautifulSoup, Comment

BASE = "https://www.quotegarden.com"
UA = {"User-Agent": "DailyMindSeed/1.0"}
ROOT = Path(__file__).parent
PROGRESS = ROOT / "progress.json"

def normalize(raw: str | None):
    if raw is None:
        return None
    return re.sub(r"\s+", " ", raw.strip())

def sha256_hex(s: str) -> str:
    import hashlib as hl
    return hl.sha256(s.encode("utf-8")).hexdigest()

def clean_author(raw: str | None):
    if not raw:
        return None
    raw = re.sub(r"\s+", " ", raw.strip())
    first = raw.split(",")[0].strip()
    first = re.sub(r"\s*\(.*?\)\s*", "", first).strip()
    first = re.sub(r"\s*\d{3,4}.*$", "", first).strip()
    return first or None

def parse_category_html(html: str, category: str):
    soup = BeautifulSoup(html, "lxml")
    section = soup.select_one("div.quotes-section")
    if section is None:
        return [], ["no quotes-section"]
    for c in section.find_all(string=lambda t: isinstance(t, Comment)):
        c.extract()
    inner = section.decode_contents()
    blocks = re.split(r"(?:<br\s*/?\s*>[\s\n]*){2,}", inner, flags=re.I)
    out, skipped = [], []
    for b in blocks:
        text = BeautifulSoup(b, "lxml").get_text(separator="\n")
        text = text.replace("\xa0", " ").strip()
        if not text:
            continue
        if "~" not in text:
            skipped.append(text[:60])
            continue
        idx = text.rfind("~")
        content_raw, author_raw = text[:idx].strip(), text[idx + 1:].strip()
        content = normalize(content_raw.replace("\n", " "))
        if content is None or len(content) < 10:
            skipped.append(text[:60])
            continue
        author = clean_author(author_raw.split("\n")[0])
        out.append({"id": sha256_hex(content), "content": content, "author": author, "category": category})
    return out, skipped

def self_test() -> int:
    html = (ROOT / "fixtures" / "happiness_sample.html").read_text(encoding="utf-8")
    quotes, skipped = parse_category_html(html, "happiness")
    assert len(quotes) == 3, f"expected 3 got {len(quotes)}: {quotes}"
    assert quotes[0]["content"].startswith("Whenever you are sincerely pleased"), quotes[0]
    assert "Ralph Waldo Emerson" in (quotes[0]["author"] or ""), quotes[0]
    assert quotes[1]["author"] == "James Oppenheim", quotes[1]
    assert any("Chinese proverb" in (q["author"] or "") for q in quotes), quotes
    assert len(skipped) >= 1, "无~块应被跳过"
    print(f"self-test PASS: {len(quotes)} quotes, {len(skipped)} skipped")
    return 0

def fetch(url: str, retries: int = 3) -> str:
    last = None
    for i in range(retries):
        try:
            r = requests.get(url, headers=UA, timeout=20)
            r.raise_for_status()
            r.encoding = r.apparent_encoding or "utf-8"
            return r.text
        except Exception as e:
            last = e
            time.sleep(2 ** i + random.random())
    raise RuntimeError(f"fetch failed {url}: {last}")

def discover_categories(limit: int | None = None):
    html = fetch(BASE + "/")
    soup = BeautifulSoup(html, "lxml")
    slugs = []
    for a in soup.find_all("a", href=True):
        href = a["href"]
        m = re.fullmatch(r"/([a-z0-9\-]+)\.html", href)
        if not m:
            continue
        slug = m.group(1)
        if slug in ("search", "about", "contact", "index"):
            continue
        if slug not in slugs:
            slugs.append(slug)
    return slugs[:limit] if limit else slugs

def load_progress():
    if PROGRESS.exists():
        return set(json.loads(PROGRESS.read_text(encoding="utf-8")))
    return set()

def save_progress(done: set):
    PROGRESS.write_text(json.dumps(sorted(done), ensure_ascii=False, indent=2), encoding="utf-8")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--limit", type=int, default=None)
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--no-db", action="store_true", help="只解析不入库")
    args = ap.parse_args()
    if args.self_test:
        sys.exit(self_test())
    slugs = discover_categories(args.limit if not args.all else None)
    done = load_progress()
    all_quotes, failed = [], []
    for slug in slugs:
        if slug in done:
            continue
        try:
            html = fetch(f"{BASE}/{slug}.html")
            quotes, skipped = parse_category_html(html, slug)
            all_quotes.extend(quotes)
            (ROOT / "skipped.log").open("a", encoding="utf-8").write(f"{slug}: {len(quotes)} ok, {len(skipped)} skipped\n")
            done.add(slug)
            save_progress(done)
        except Exception as e:
            failed.append(f"{slug} {e}")
        time.sleep(1.0 + random.random() * 0.5)
    (ROOT / "quotes.jsonl").open("a", encoding="utf-8").write("".join(json.dumps(q, ensure_ascii=False) + "\n" for q in all_quotes))
    (ROOT / "failed.txt").open("a", encoding="utf-8").write("\n".join(failed) + ("\n" if failed else ""))
    print(f"crawled {len(all_quotes)} quotes, {len(failed)} failed")
    if not args.no_db and all_quotes:
        insert_db(all_quotes)

def insert_db(quotes):
    import psycopg2
    conn = psycopg2.connect(host=os.environ.get("PGHOST", "192.168.80.152"), port=int(os.environ.get("PGPORT", "5432")),
        dbname=os.environ.get("PGDATABASE", "quote_garden"), user=os.environ.get("PGUSER", os.environ.get("SPRING_DATASOURCE_USERNAME", "postgres")),
        password=os.environ.get("PGPASSWORD", os.environ.get("SPRING_DATASOURCE_PASSWORD", "")))
    import time as _t
    now = int(_t.time() * 1000)
    with conn, conn.cursor() as cur:
        for q in quotes:
            cur.execute("INSERT INTO quote(id, content, author, category, updated_at) VALUES (%s,%s,%s,%s,%s) ON CONFLICT(id) DO NOTHING",
                (q["id"], q["content"], q["author"], q["category"], now))
    conn.close()
    print(f"inserted {len(quotes)} (dedup by id)")

if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python tools/crawler/quotegarden.py --self-test`
Expected: PASS `self-test PASS: 3 quotes, 1 skipped`

- [ ] **Step 5: Commit**

```bash
git add tools/crawler/quotegarden.py
git commit -m "feat(crawler): add quotegarden html parser with self-test"
```

---

### Task 3: Python translate.py — 免费回填

**Files:**
- Create: `tools/crawler/translate.py`

- [ ] **Step 1: Write the failing test**

```python
# python tools/crawler/translate.py --self-test
# 期望：cache命中时不请求网络返回缓存译文；当前文件不存在故 FAIL
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python tools/crawler/translate.py --self-test`
Expected: FAIL `No such file`

- [ ] **Step 3: Write minimal implementation**

```python
# tools/crawler/translate.py
"""MyMemory免费翻译回填：translation IS NULL分批 + 本地缓存."""
import argparse, hashlib, json, os, time
from pathlib import Path
import requests
CACHE = Path(__file__).parent / "translation_cache.json"

def load_cache():
    return json.loads(CACHE.read_text(encoding="utf-8")) if CACHE.exists() else {}

def save_cache(c):
    CACHE.write_text(json.dumps(c, ensure_ascii=False, indent=2), encoding="utf-8")

def key(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()

def translate_one(text: str, cache: dict) -> str | None:
    k = key(text)
    if k in cache:
        return cache[k]
    try:
        r = requests.get("https://api.mymemory.translated.net/get",
            params={"q": text[:450], "langpair": "en|zh-CN"}, timeout=20)
        d = r.json()
        t = (d.get("responseData") or {}).get("translatedText")
        if t and "QUERY LENGTH LIMIT" not in t and "MYMEMORY WARNING" not in t:
            cache[k] = t
            return t
    except Exception as e:
        print(f"translate fail: {e}")
    return None

def self_test() -> int:
    c = {}
    c[key("Hello world")] = "你好世界"
    assert translate_one("Hello world", c) == "你好世界"
    print("translate self-test PASS (cache hit, no network)")
    return 0

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--limit", type=int, default=50)
    ap.add_argument("--offset", type=int, default=0)
    a = ap.parse_args()
    if a.self_test:
        raise SystemExit(self_test())
    import psycopg2
    conn = psycopg2.connect(host=os.environ.get("PGHOST", "192.168.80.152"), port=int(os.environ.get("PGPORT", "5432")),
        dbname=os.environ.get("PGDATABASE", "quote_garden"), user=os.environ.get("PGUSER", os.environ.get("SPRING_DATASOURCE_USERNAME", "postgres")),
        password=os.environ.get("PGPASSWORD", os.environ.get("SPRING_DATASOURCE_PASSWORD", "")))
    cache = load_cache()
    with conn, conn.cursor() as cur:
        cur.execute("SELECT id, content FROM quote WHERE translation IS NULL ORDER BY updated_at DESC LIMIT %s OFFSET %s", (a.limit, a.offset))
        rows = cur.fetchall()
        n = 0
        for qid, content in rows:
            t = translate_one(content, cache)
            if t:
                cur.execute("UPDATE quote SET translation=%s WHERE id=%s", (t, qid))
                n += 1
            time.sleep(1.0)
    save_cache(cache)
    conn.close()
    print(f"translated {n}/{len(rows)}")

if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python tools/crawler/translate.py --self-test`
Expected: PASS `translate self-test PASS (cache hit, no network)`

- [ ] **Step 5: Commit**

```bash
git add tools/crawler/translate.py
git commit -m "feat(crawler): add free translation backfill with cache"
```

---

### Task 4: Server — Jsoup 依赖

**Files:**
- Modify: `server/build.gradle.kts:7-14`

- [ ] **Step 1: Write the failing test (compile check)**

```java
// server/src/test/java/com/dailymind/importer/JsoupPresenceTest.java
package com.dailymind.importer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class JsoupPresenceTest {
    @Test void jsoupOnClasspath() { assertThat(org.jsoup.Jsoup.class).isNotNull(); }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.JsoupPresenceTest"`
Expected: FAIL `cannot find symbol: org.jsoup` / `compilation failed`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// server/build.gradle.kts dependencies 块新增一行：
implementation("org.jsoup:jsoup:1.18.3")
```

完整块：

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core:11.20.3")
    implementation("org.flywaydb:flyway-database-postgresql:11.20.3")
    implementation("org.postgresql:postgresql")
    implementation("org.jsoup:jsoup:1.18.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.JsoupPresenceTest"`
Expected: PASS（随后删除该临时测试文件，保持测试集干净）

- [ ] **Step 5: Commit**

```bash
git add server/build.gradle.kts
git commit -m "feat(server): add jsoup for quotegarden html parsing"
```

---

### Task 5: Server — QuoteGardenHtmlParser (TDD核心)

**Files:**
- Create: `server/src/main/java/com/dailymind/importer/ParsedQuote.java`
- Create: `server/src/main/java/com/dailymind/importer/QuoteGardenHtmlParser.java`
- Test: `server/src/test/java/com/dailymind/importer/QuoteGardenHtmlParserTest.java`

- [ ] **Step 1: Write the failing test**

```java
// server/src/test/java/com/dailymind/importer/QuoteGardenHtmlParserTest.java
package com.dailymind.importer;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;
public class QuoteGardenHtmlParserTest {
    private String fixture() throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/happiness_sample.html"));
    }
    @Test void parsesHappinessFixture() throws Exception {
        var quotes = new QuoteGardenHtmlParser().parse(fixture(), "happiness");
        assertThat(quotes).hasSize(3);
        assertThat(quotes.get(0).content()).startsWith("Whenever you are sincerely pleased");
        assertThat(quotes.get(0).author()).contains("Ralph Waldo Emerson");
        assertThat(quotes.get(1).author()).isEqualTo("James Oppenheim");
        assertThat(quotes.stream().anyMatch(q -> q.author() != null && q.author().contains("Chinese proverb"))).isTrue();
    }
    @Test void skipsBlocksWithoutTilde() throws Exception {
        var quotes = new QuoteGardenHtmlParser().parse("Hello no tilde<BR><BR><BR>Real quote here ok ~Anon", "test");
        assertThat(quotes).hasSize(1);
    }
}
```

注：需同步创建 `server/src/test/resources/fixtures/happiness_sample.html`（内容同 `tools/crawler/fixtures/happiness_sample.html`）。

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.QuoteGardenHtmlParserTest"`
Expected: FAIL `cannot find symbol: QuoteGardenHtmlParser` / `NoSuchFileException fixtures`

- [ ] **Step 3: Write minimal implementation**

```java
// server/src/main/java/com/dailymind/importer/ParsedQuote.java
package com.dailymind.importer;
public record ParsedQuote(String content, String author, String category) {}
```

```java
// server/src/main/java/com/dailymind/importer/QuoteGardenHtmlParser.java
package com.dailymind.importer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Node;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class QuoteGardenHtmlParser {
    public List<ParsedQuote> parse(String html, String category) {
        var doc = Jsoup.parse(html);
        var section = doc.selectFirst("div.quotes-section");
        if (section == null) return List.of();
        for (Node n : new ArrayList<>(section.childNodes()))
            if (n instanceof Comment) n.remove();
        String inner = section.html();
        String[] blocks = inner.split("(?i)(?:<br\\s*/?\\s*>\\s*){2,}");
        List<ParsedQuote> out = new ArrayList<>();
        for (String b : blocks) {
            String text = Jsoup.parse(b).wholeText().replace('\u00a0', ' ').strip();
            if (text.isEmpty() || !text.contains("~")) continue;
            int idx = text.lastIndexOf('~');
            String content = text.substring(0, idx).replace('\n', ' ').trim().replaceAll("\\s+", " ");
            if (content.length() < 10) continue;
            String authorRaw = text.substring(idx + 1).split("\n")[0].trim();
            out.add(new ParsedQuote(content, cleanAuthor(authorRaw), category));
        }
        return out;
    }
    static String cleanAuthor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String first = raw.split(",")[0].trim().replaceAll("\\s*\\(.*?\\)\\s*", "").trim().replaceAll("\\s*\\d{3,4}.*$", "").trim();
        return first.isEmpty() ? null : first;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.QuoteGardenHtmlParserTest"`
Expected: PASS（2 tests）

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/dailymind/importer/ParsedQuote.java server/src/main/java/com/dailymind/importer/QuoteGardenHtmlParser.java server/src/test/java/com/dailymind/importer/QuoteGardenHtmlParserTest.java server/src/test/resources/fixtures/happiness_sample.html
git commit -m "feat(server): add quotegarden html parser with TDD"
```

---

### Task 6: Server — 重构 QuoteGardenImporter 为 HTML

**Files:**
- Modify: `server/src/main/java/com/dailymind/importer/QuoteGardenImporter.java`
- Test: `server/src/test/java/com/dailymind/importer/QuoteGardenImporterTest.java`（Mockito-free：用内存 Fake Repo 太重，改用 parser+existsById 行为断言需 Spring context；本计划用轻量集成风格：H2 不引入，改为对 `QuoteRepository` 用手写 stub。这里为保持简单，仅复用 ParserTest + 手动验证；本 Task 测试为 `NormalizerTest` 回归）

- [ ] **Step 1: Write the failing test (importer 行为)**

```java
// server/src/test/java/com/dailymind/importer/QuoteGardenImporterHtmlTest.java
package com.dailymind.importer;
import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
public class QuoteGardenImporterHtmlTest {
    @Test void importsParsedQuotesWithDedup() {
        FakeRepo repo = new FakeRepo(Set.of()); // 空库
        var importer = new QuoteGardenImporter(repo, new Normalizer(), new QuoteGardenHtmlParser());
        int n = importer.importHtml("<div class=\"quotes-section\">Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<BR><BR><BR></div>", "happiness");
        assertThat(n).isEqualTo(1);
        int n2 = importer.importHtml("<div class=\"quotes-section\">Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<BR><BR><BR></div>", "happiness");
        assertThat(n2).isEqualTo(0); // 去重
    }
    static class FakeRepo implements QuoteRepository {
        private final Set<String> ids; private final Map<String, Quote> store = new HashMap<>();
        FakeRepo(Set<String> ids) { this.ids = new HashSet<>(ids); }
        public boolean existsById(String id) { return ids.contains(id) || store.containsKey(id); }
        public <S extends Quote> S save(S e) { ids.add(e.id); store.put(e.id, e); return e; }
        // 其余 JpaRepository 方法抛 UnsupportedOperationException（测试用不到）
        public List<Quote> findAll() { throw new UnsupportedOperationException(); }
        public Optional<Quote> findById(String id) { throw new UnsupportedOperationException(); }
        public void deleteAll() { throw new UnsupportedOperationException(); }
        public long count() { return store.size(); }
        public void deleteById(String id) { throw new UnsupportedOperationException(); }
        public <S extends Quote> List<S> saveAll(Iterable<S> e) { throw new UnsupportedOperationException(); }
        public void flush() {}
        public <S extends Quote> S saveAndFlush(S e) { throw new UnsupportedOperationException(); }
        public void delete(Quote e) { throw new UnsupportedOperationException(); }
        public List<Quote> findAllById(Iterable<String> ids) { throw new UnsupportedOperationException(); }
        public boolean existsById(Object id) { throw new UnsupportedOperationException(); }
        // …为编译通过需实现全部接口方法，实际写计划时按IDE补全
        public Optional<Quote> findFirstByDeletedAtIsNullOrderByUpdatedAtDesc() { return Optional.empty(); }
        public Optional<Quote> findRandom() { return Optional.empty(); }
        public List<Quote> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Long u, org.springframework.data.domain.Pageable p) { return List.of(); }
        public Long findMaxUpdatedAt() { return null; }
        public List<Quote> findAll(org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
        public List<Quote> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        public <S extends Quote> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
        public <S extends Quote> List<S> findAll(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
        public <S extends Quote> long count(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
        public <S extends Quote> boolean exists(org.springframework.data.domain.Example<S> e) { throw new UnsupportedOperationException(); }
        public org.springframework.data.domain.Page<Quote> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        public <S extends Quote> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        public Optional<Quote> findOne(org.springframework.data.jpa.domain.Specification<Quote> s) { throw new UnsupportedOperationException(); }
        public List<Quote> findAll(org.springframework.data.jpa.domain.Specification<Quote> s) { throw new UnsupportedOperationException(); }
        public org.springframework.data.domain.Page<Quote> findAll(org.springframework.data.jpa.domain.Specification<Quote> s, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        public List<Quote> findAll(org.springframework.data.jpa.domain.Specification<Quote> s, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        public long count(org.springframework.data.jpa.domain.Specification<Quote> s) { throw new UnsupportedOperationException(); }
        public boolean exists(org.springframework.data.jpa.domain.Specification<Quote> s) { throw new UnsupportedOperationException(); }
        public long delete(org.springframework.data.jpa.domain.Specification<Quote> s) { throw new UnsupportedOperationException(); }
        public void deleteAllById(Iterable<? extends String> ids) { throw new UnsupportedOperationException(); }
        public void deleteAll(Iterable<? extends Quote> e) { throw new UnsupportedOperationException(); }
        public Quote getReferenceById(String id) { throw new UnsupportedOperationException(); }
        public Quote getById(String id) { throw new UnsupportedOperationException(); }
        public <S extends Quote> List<S> saveAllAndFlush(Iterable<S> e) { throw new UnsupportedOperationException(); }
        public void deleteAllInBatch(Iterable<Quote> e) { throw new UnsupportedOperationException(); }
        public void deleteAllByIdInBatch(Iterable<String> ids) { throw new UnsupportedOperationException(); }
        public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        public Quote getOne(String id) { throw new UnsupportedOperationException(); }
        public <S extends Quote> S saveAndFlush(S e, boolean b) { throw new UnsupportedOperationException(); }
    }
}
```

注：FakeRepo 需补全 JpaRepository 全部方法（按 IDE 生成，行为均为 UnsupportedOperationException 除 existsById/save 外）。

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.QuoteGardenImporterHtmlTest"`
Expected: FAIL `cannot find symbol: QuoteGardenImporter(repo, normalizer, parser)` / `cannot find symbol: importHtml`

- [ ] **Step 3: Write minimal implementation**

```java
// server/src/main/java/com/dailymind/importer/QuoteGardenImporter.java
package com.dailymind.importer;
import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;
@Component
public class QuoteGardenImporter {
    private final QuoteRepository repo;
    private final Normalizer normalizer;
    private final QuoteGardenHtmlParser parser;
    public QuoteGardenImporter(QuoteRepository repo, Normalizer normalizer, QuoteGardenHtmlParser parser) {
        this.repo = repo; this.normalizer = normalizer; this.parser = parser;
    }
    /** HTML分类页导入（新主路径）：幂等，去重后返回新增数. */
    public int importHtml(String html, String category) {
        int count = 0;
        for (ParsedQuote p : parser.parse(html, category)) {
            String content = normalizer.normalize(p.content());
            if (content == null || content.isBlank()) continue;
            String hash = normalizer.hash(content);
            if (repo.existsById(hash)) continue;
            Quote q = new Quote();
            q.id = hash; q.content = content; q.author = p.author(); q.category = p.category();
            q.updatedAt = System.currentTimeMillis();
            repo.save(q); count++;
        }
        return count;
    }
    /** 旧JSON API路径保留兼容，标记废弃. */
    @Deprecated
    @SuppressWarnings("unchecked")
    public int importFrom(String url) {
        var rt = new RestTemplate();
        var res = rt.getForObject(url, Map.class);
        List<Map> data = (List<Map>) res.get("data");
        int count = 0;
        for (Map m : data) {
            String content = normalizer.normalize((String) m.get("quoteText"));
            String hash = normalizer.hash(content);
            if (repo.existsById(hash)) continue;
            Quote q = new Quote();
            q.id = hash; q.content = content; q.author = (String) m.get("quoteAuthor");
            q.updatedAt = System.currentTimeMillis();
            repo.save(q); count++;
        }
        return count;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.*"`
Expected: PASS（ParserTest + ImporterHtmlTest + NormalizerTest 全绿）

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/dailymind/importer/QuoteGardenImporter.java server/src/test/java/com/dailymind/importer/QuoteGardenImporterHtmlTest.java
git commit -m "feat(server): refactor importer to html with dedup"
```

---

### Task 7: Server — TranslationService 接口 + 空实现

**Files:**
- Create: `server/src/main/java/com/dailymind/importer/TranslationService.java`

- [ ] **Step 1: Write the failing test**

```java
// server/src/test/java/com/dailymind/importer/TranslationServiceTest.java
package com.dailymind.importer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class TranslationServiceTest {
    @Test void noopReturnsNull() {
        TranslationService svc = new NoopTranslationService();
        assertThat(svc.translate("Hello")).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.TranslationServiceTest"`
Expected: FAIL `cannot find symbol: TranslationService`

- [ ] **Step 3: Write minimal implementation**

```java
// server/src/main/java/com/dailymind/importer/TranslationService.java
package com.dailymind.importer;
import org.springframework.stereotype.Component;
public interface TranslationService {
    /** 返回译文；无译文/失败返回 null（调用方保留原文）。 */
    String translate(String englishContent);
}
@Component
class NoopTranslationService implements TranslationService {
    public String translate(String englishContent) { return null; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd server; .\gradlew.bat test --tests "com.dailymind.importer.*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/dailymind/importer/TranslationService.java server/src/test/java/com/dailymind/importer/TranslationServiceTest.java
git commit -m "feat(server): add translation service seam with noop"
```

---

### Task 8: 端到端验证（DB + API）

**Files:** 无新增，仅验证。

- [ ] **Step 1: Python 小批量试点（3个分类，不入库只解析）**

Run: `python tools/crawler/quotegarden.py --limit 3 --no-db`
Expected: `crawled >0 quotes` 且 `quotes.jsonl` 有行，且 `skipped.log` 有记录

- [ ] **Step 2: 全量入库（需 DB 可达）**

Run: `$env:PGPASSWORD='<remote密码>'; python tools/crawler/quotegarden.py --all`
Expected: `inserted N`；DB `SELECT count(*) FROM quote;` > 0

- [ ] **Step 3: API 健康检查**

Run: `curl "http://localhost:8080/api/v1/quotes/random"` 与 `curl "http://localhost:8080/api/v1/sync/quotes?updatedAfter=0&limit=5"`
Expected: 200 且 `items` 非空（若 500/空表说明入库未成功，回 Task 2 查 `failed.txt`）

- [ ] **Step 4: Java 全量回归**

Run: `cd server; .\gradlew.bat test`
Expected: PASS（全部测试绿）

- [ ] **Step 5: Commit（进度记录）**

```bash
git add tools/crawler/progress.json tools/crawler/skipped.log
git commit -m "chore(crawler): record seed progress" || echo "nothing to commit"
```

---

## Self-Review

- Spec coverage: 解析规则→Task2/5；幂等→Task2/6；礼貌/断点→Task2；入库→Task2/6/8；翻译两阶段→Task3/7；测试→Task2/3/5/6/7；验证→Task8。定时调度为非目标，未立项。
- Placeholder scan: 无 TBD/TODO；所有步骤含完整代码与精确命令；FakeRepo 注明按IDE补全全部接口方法（实现行为已给出）。
- Type consistency: `ParsedQuote(content, author, category)` 在 Python dict / Java record / Importer / Test 四处同名同序；`normalize→hash→existsById→save` 链路两端一致；`importHtml(html, category)->int` 签名在测试与实现一致。
