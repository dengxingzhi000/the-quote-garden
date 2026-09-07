# Favorite 与互动观测 — Design Spec

**Date:** 2026-09-07
**Status:** Approved (brainstorming complete, option A phased)
**Scope:** P1 端侧收藏补完；P2 匿名设备身份 + 全局喜欢数 + 每日展示计数。账号注册、缓存列、全量事件流明确 out of scope。

## 1. 背景与现状

- App `FavoriteViewModel` 用 `filter { _ -> false }` 恒返回空列表；`toggleFavorite` 是 stub；`FavoriteEntity`/`history` 表建了但无 DAO。
- 服务端 V1 `favorite(quote_id PK, user_id, created_at)` 是单主键，存不了多用户多行，需重构。
- 无 user 表，无浏览/喜欢统计。

## 2. 决策（Q&A 确认）

- 身份：匿名设备身份先行，`user` 表预留 `email`/`phone` 可空字段，后期注册直接启用，无需改表。
- 喜欢量：都要，先个人（P1 纯端侧）后全局（P2 服务端聚合）。
- 浏览：先每日展示计数，后扩展全量事件；`daily_impression(user_id, quote_id, day)` 粒度天然可扩展。
- 双模：离线本地先生效，联网后 WorkManager 批量上传，服务端幂等去重。

## 3. 数据模型

### 3.1 服务端（Flyway V4 迁移）

```sql
CREATE TABLE app_user (
    id VARCHAR(64) PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(32),
    created_at BIGINT NOT NULL
);
-- favorite 重建为联合主键（V1 单主键无法多用户）
ALTER TABLE favorite DROP CONSTRAINT favorite_pkey;
ALTER TABLE favorite ADD COLUMN user_id VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE favorite ADD PRIMARY KEY (user_id, quote_id);
ALTER TABLE favorite ADD CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES app_user(id);
CREATE TABLE daily_impression (
    user_id VARCHAR(64) NOT NULL REFERENCES app_user(id),
    quote_id VARCHAR(64) NOT NULL,
    day DATE NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, day)
);
CREATE INDEX idx_impression_quote ON daily_impression(quote_id);
```

注：存量 `favorite` 表无数据（从未写入），可直接重建；`user_id DEFAULT ''` 仅为迁移语法需要，约束保证后续写入真实 id。

### 3.2 端侧（Room，无需升版：只加 DAO，不改 entity）

- 新增 `FavoriteDao`：`upsert(FavoriteEntity)`、`deleteById(quoteId)`、`observeIds(): Flow<List<String>>`、`getIdsOnce(): List<String>`。
- 每日展示打点复用 `history` 表（已有 `HistoryEntity(quoteId, viewedAt)` + 本轮新增的 `HistoryDao`）：`getDailyQuote()` 钉选成功即视为一次展示，无需新表。
- 待上传队列：新增 `PendingOp` 思想——`sync_metadata` 表已存在（V1），P2 用 DataStore 另存 `LAST_FAV_SYNC`/`LAST_IMP_SYNC` 游标（`viewedAt/createdAt` 时间戳），增量上传，无需 outbox 表。

## 4. 同步流（离线优先）

1. 收藏 toggle：写 Room 立即生效 → `DailySyncWorker` 捞 `createdAt > LAST_FAV_SYNC` 的收藏行 POST `/api/v1/favorites/batch` → 服务端 `ON CONFLICT (user_id, quote_id) DO NOTHING`；取消收藏 POST `delete` 语义（body 带 `removed: true`），服务端按主键删除。
2. 每日展示：`recordToday()` 已写 history → Worker 捞 `viewedAt > LAST_IMP_SYNC` 且为当日钉选的行 POST `/api/v1/impressions/batch` → 服务端按 `(user_id, day)` 幂等插入（同一天重复打开只记一条）。
3. 设备身份：首次启动生成 UUID 存 DataStore（`DEVICE_ID` key），注册时 `PUT /api/v1/users`（`device_id` 唯一约束，重复注册返回既有 id）。

## 5. 接口（P2 新增，P1 零接口）

- `PUT /api/v1/users` `{deviceId}` → `{id}`（幂等注册）。
- `POST /api/v1/favorites/batch` `[{quoteId, removed, createdAt}]` → `{ok:true}`。
- `POST /api/v1/impressions/batch` `[{quoteId, day}]` → `{ok:true}`。
- `GET /api/v1/quotes/{id}/stats` → `{likeCount, viewCount}`（`COUNT(*)` 直接聚合；数据量起来后再加缓存列，不在本次 scope）。
- P1 App 不调任何新接口；P2 Favorite 行展示喜欢数（调用 stats，可缓存展示）。

## 6. P1 范围（先行，可独立交付）

- `FavoriteDao` + `QuoteRepositoryImpl.toggleFavorite` 真实现 + `observeFavorites(): Flow<List<Quote>>`（favorite ids JOIN quote，按 createdAt 倒序）。
- `FavoriteViewModel` 接真数据流（空态/列表逻辑不变，沿用 editorial spec）。
- TDD：`FavoriteDaoTest`（androidTest 既有 `QuoteDaoTest` 同目录）、`QuoteRepositoryTest` toggle 单测。
- 验证：`:app:test` + 空/非空收藏手动验证。不碰服务端、不碰 sync、不加权限。

## 7. P2 范围（后行）

- 服务端：V4 迁移、`app_user`/`daily_impression` 实体与 Repository、4 个接口、幂等单测。
- 端侧：`DEVICE_ID` 生成与注册、`LAST_FAV_SYNC`/`LAST_IMP_SYNC` 游标、`DailySyncWorker` 批量上传、stats 展示。
- 验证：飞行模式 toggle → 联网后同步 → 服务端计数 +1；重复上传不 double count；`/stats` 返回正确聚合。

## 8. 非目标

注册/登录 UI 与密码体系、点赞缓存列、阅读进度、全量浏览事件流、推送。

## 9. Self-review

- 无 TBD/TODO，所有表、接口、字段、幂等键具体。
- 一致性：P1 不依赖 P2；P2 不改 P1 表结构（Room 无需升版）；服务端 V4 与 `ddl-auto: validate` 兼容（新增表+改主键，无 JPA 冲突——`favorite` 无对应 entity）。
- Scope：单 spec 覆盖 P1+P2 两阶段，各自可独立 plan；`favorite` 存量空表假设已在 §3.1 注明（若线上已有数据需先备份，当前确认为空）。
- 歧义：`day` 用服务端收到时间的 UTC 日期（`day DATE` 由服务端生成，不信任客户端时钟）；设备换机不迁移收藏（匿名身份已知限制，注册体系启用后解决）。
