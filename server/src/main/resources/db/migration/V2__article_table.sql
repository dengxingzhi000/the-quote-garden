-- 文章表：800 字以上长文与名言区分存放（阈值见 tools/crawler/README.md）。
-- 存量长文从 quote 搬移，quote 侧软删以便同步传播到端侧（sync 不过滤 deleted 行）。
CREATE TABLE article (
    id VARCHAR(64) PRIMARY KEY,
    content TEXT NOT NULL,
    translation TEXT,
    author VARCHAR(255),
    category VARCHAR(64),
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_article_updated_at ON article(updated_at);

INSERT INTO article (id, content, translation, author, category, updated_at)
SELECT id, content, translation, author, category, updated_at FROM quote
WHERE LENGTH(content) >= 800
ON CONFLICT (id) DO NOTHING;

UPDATE quote
SET deleted_at = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
WHERE LENGTH(content) >= 800 AND deleted_at IS NULL;
