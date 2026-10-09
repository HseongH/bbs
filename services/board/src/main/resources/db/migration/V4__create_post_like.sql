CREATE TABLE post_like (
    id         BIGSERIAL   PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES post (id),
    member_id  BIGINT      NOT NULL REFERENCES member (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_post_like UNIQUE (post_id, member_id)
);
