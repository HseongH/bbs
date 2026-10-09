CREATE TABLE comment (
    id                BIGSERIAL     PRIMARY KEY,
    post_id           BIGINT        NOT NULL REFERENCES post (id),
    author_id         BIGINT        NOT NULL REFERENCES member (id),
    body              VARCHAR(1000) NOT NULL,
    parent_comment_id BIGINT        REFERENCES comment (id),
    depth             SMALLINT      NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT ck_comment_depth CHECK (depth BETWEEN 0 AND 1)
);

CREATE INDEX idx_comment_active_by_post
    ON comment (post_id, created_at, id)
    WHERE deleted_at IS NULL;
