-- 整文预留：单篇 essay 页（blog-*）整体入库，标题与来源链接供未来阅读功能使用。
ALTER TABLE article ADD COLUMN IF NOT EXISTS title VARCHAR(512);
ALTER TABLE article ADD COLUMN IF NOT EXISTS source_url VARCHAR(512);
