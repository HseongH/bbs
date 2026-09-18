CREATE TABLE post (
    id          BIGSERIAL      PRIMARY KEY,
    title       VARCHAR(100)   NOT NULL,
    content     VARCHAR(10000) NOT NULL,
    author_id   BIGINT         NOT NULL REFERENCES member (id),
    view_count  BIGINT         NOT NULL DEFAULT 0,
    like_count  BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_post_active_created_at
    ON post (created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
