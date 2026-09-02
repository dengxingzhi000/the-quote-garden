CREATE TABLE quote (
    id VARCHAR(64) PRIMARY KEY,
    content TEXT NOT NULL,
    translation TEXT,
    author VARCHAR(255),
    category VARCHAR(64),
    difficulty INT,
    audio_url TEXT,
    image_url TEXT,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_quote_updated_at ON quote(updated_at);
CREATE TABLE favorite (quote_id VARCHAR(64) PRIMARY KEY REFERENCES quote(id), user_id VARCHAR(64), created_at BIGINT);
CREATE TABLE sync_metadata (id VARCHAR(64) PRIMARY KEY, last_cursor VARCHAR(255));
