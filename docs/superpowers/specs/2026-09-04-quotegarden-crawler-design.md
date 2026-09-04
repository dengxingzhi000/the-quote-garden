# QuoteGarden 固定爬虫设计

日期: 2026-09-04
状态: approved (design), pending spec review
目标: 爬取 https://www.quotegarden.com/ 填充 `quote` 表，支撑每日一句展示

## 1. 背景与现状

- 现有 `server/src/main/java/com/dailymind/importer/QuoteGardenImporter.java` 解析的是旧 JSON API (`data[].quoteText / quoteAuthor`)，与 www.quotegarden.com 的静态 HTML 完全对不上，当前无调用方。
- www.quotegarden.com 实测为静态站：首页列出 400+ 分类链接 (`/happiness.html` 等)；分类页正文在 `div.quotes-section` 内，语录块以 `<BR><BR><BR>` 分隔，格式为 `正文 ~作者, 出处 <!--元数据-->`，含 `<i>/<nobr>` 标签、HTML 实体、被 `<!-- -->` 注释掉的废弃块。
- DB: `quote(id VARCHAR64 PK, content TEXT NOT NULL, translation TEXT, author VARCHAR255, category VARCHAR64, difficulty INT, audio_url, image_url, updated_at BIGINT NOT NULL, deleted_at, created_at)`，`ddl-auto: validate`，变更只能走 Flyway。本次无需 schema 变更。
- 需求确认: Python + Java 两者都要；全站抓取；原文 + 作者 + 免费翻译。

## 2. 方案选择

- A 纯 Python: 快，一次性。
- B 纯 Java (Jsoup): 稳，长期可维护。
- C 混合 (采用): Python 先全量填充解燃眉之急，Java 按相同解析规则重构做长期增量。翻译两阶段，不阻塞展示。

## 3. 架构与组件

```
www.quotegarden.com/
  |-- index.html (400+分类链接)
  +-- /<slug>.html (quotes-section)
        |
        v
tools/crawler/quotegarden.py (Python, 一次性全量)
  |-- fetch categories -> fetch pages (1-1.5s间隔, 重试3次, 断点续爬)
  |-- QuoteGardenHtml规则 (与Java同构)
  |-- normalize+sha256 (与Normalizer同构) -> INSERT ON CONFLICT DO NOTHING
  +-- 输出 quotes.jsonl + skipped.log + failed.txt + translation_cache.json
server/src/main/java/com/dailymind/importer/ (Java, 长期)
  |-- QuoteGardenHtmlParser.java (纯函数: parse(html, category) -> List<ParsedQuote>)
  |-- QuoteGardenImporter.java (重构: importCategory/importAll, 复用Normalizer + QuoteRepository.existsById)
  +-- TranslationService.java (接口, 一期空实现/二期免费实现)
        |
        v
PostgreSQL quote表 -> GET /api/v1/quotes/daily|random + /api/v1/sync/quotes -> App每日一句
```

职责边界:
- Parser 只做 HTML->结构化数据，可独立测试，不碰 DB/网络。
- Importer 只做 去重+落库+触发翻译，不做字符串杂活。
- Python 与 Java 解析规则同构 (同一 fixture 断言)，避免两套数据口径。

## 4. 解析规则 (Python/Java 共用)

1. 取 `div.quotes-section`，无则整页跳过记 log。
2. 按连续 `<br>` (2个及以上) 切块；块内先剔除 HTML 注释 `<!--.*?-->` (含被注释掉的整条语录)。
3. 块转文本: 解 HTML 实体 (`&#160;`->空格等)，`<br>` 单个转 `\n` (保留诗歌换行)，strip。
4. 以最后一个 `~` 切分 正文/作者；无 `~` 则记 skipped 不中断。
5. 正文: collapse 空白但保留单换行 (多行诗)，长度 <10 字符跳过。
6. 作者清洗: 取 `~` 后第一段，去首尾空格；去掉年份/出处杂质 (如 `, 1860`、`, rbrault.blogspot.com` 保留人名主体，规则: 按 `,` 切首段 + 去 `(...)` 年份段，保留 `Robert Brault` 这类主体)；`<i>` 书名不 masuk 作者。
7. category = URL slug (如 `happiness`)，小写去空格。
8. 幂等: `id = SHA-256(normalize(content))`，`normalize = trim + \s+ -> 单空格 (换行先转空格再collapse，与现有Normalizer一致)`；Python 侧用同一算法，`ON CONFLICT DO NOTHING` / Java 侧 `existsById` 跳过。

## 5. 抓取策略与礼貌

- User-Agent: `DailyMindSeed/1.0`；分类页请求间隔 1-1.5s 随机抖动；单页超时 20s，重试 3 次 (指数退避)。
- 断点续爬: `progress.json` 记录已完成 slug，中断重跑跳过。
- 失败记 `failed.txt` (slug + URL + reason)，跑完可单独重跑失败集。
- 版权: 站点 `© 1998–2026 All rights reserved`；入库仅 `content/author/category` 事实性短文本 + 保留来源分类，App 展示作者署名；`tools/crawler/README` 注明来源与 robots 遵循，不做高频并发。

## 6. 入库

- Python 直连 Postgres (复用 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`，默认远程 `192.168.80.152:5432/quote_garden`)：
  `INSERT INTO quote(id, content, author, category, updated_at) VALUES (...) ON CONFLICT(id) DO NOTHING`，`updated_at = now millis`，`translation/difficulty/audio/image` 留 NULL。
- Java: `ParsedQuote(content, author, category)` -> `normalize -> hash -> existsById ? skip : save(Quote{id, content, author, category, updatedAt=now})`，返回新增计数。
- 同时落盘 `quotes.jsonl` (每行 `{id, content, author, category}`) 便于审计与重放。

## 7. 翻译 (免费，两阶段)

- 一期入库 `translation` 允许 NULL；App `translation` nullable 已兼容 (无翻译显示原文)。
- 二期独立脚本 `tools/crawler/translate.py`: 读 DB 中 `translation IS NULL` 分批 (如 50 条/批)，调免费实现 (默认 MyMemory 匿名)，写回 + `translation_cache.json` (key=sha256(content)) 去重；QPS<=1，失败留空下次补。
- 配额风险: MyMemory 匿名额度小 (约5000 chars/day 级别)，全站数千条需分多天跑；文档与脚本均支持 `--limit/--offset` 分批；Java 侧 `TranslationService` 接口化，后续可换 Argos 离线或付费 key 而不改 Importer。

## 8. 错误处理

- 跳过不中断: 无 `quotes-section`、无 `~`、过短块、空作者 (author 置 NULL 保留正文)。
- 中断可续: 网络失败进 `failed.txt`，Ctrl-C 后 `progress.json` 续跑。
- 脏数据防护: 两端同一 normalize+hash；入库前 `content` 非空校验；SQL 参数化防注入。

## 9. 测试与验证 (TDD)

- Python: 附 3 个离线 fixture (happiness 真实片段: 多行诗 / `<i>`书名 / 注释块跳过)，`--self-test` 断言解析条数与首条 `content/author`。
- Java (先红后绿):
  - `QuoteGardenHtmlParserTest`: 同一 happiness 片段，断言 条数/首条 content 以 `Whenever you are sincerely pleased` 开头 + author `Ralph Waldo Emerson` + 注释块被跳过 + 无`~`块被跳过。
  - 复用 `NormalizerTest`。
- 验收: `SELECT count(*), count(translation) FROM quote;` + `GET /api/v1/quotes/random` 200 + `GET /api/v1/sync/quotes?updatedAfter=0&limit=5` 有 `items` 即通过。

## 10. 交付物

- `tools/crawler/quotegarden.py`、`tools/crawler/translate.py`、`tools/crawler/README.md`、`tools/crawler/fixtures/*.html`
- `server/.../importer/QuoteGardenHtmlParser.java` (新)、`QuoteGardenImporter.java` (重构)、`TranslationService.java` (新接口+空实现)
- `server/src/test/.../importer/QuoteGardenHtmlParserTest.java` (新)
- 需新增 Jsoup 依赖 (`server/build.gradle.kts`)；Python 依赖 `requirements.txt` (requests, beautifulsoup4, lxml, psycopg2-binary)。

## 11. 非目标

- 不改 DB schema / Flyway。
- 一期不做付费翻译、音频/图片、难度分级。
- 不做定时增量调度 (二期可加 `ApplicationRunner --import` / 管理接口)。

## Self-review

- Placeholder: 无 TBD/TODO；配额数字写为约数并注明分批手段。
- 一致性: 解析规则 Python/Java 同构；幂等键与现有 Normalizer 一致；translation nullable 与 App 兼容。
- 范围: 单 spec 覆盖 Python+Java+翻译两阶段，无需再拆；定时调度明确列为非目标。
