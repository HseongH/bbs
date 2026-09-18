CREATE TABLE member (
    id          BIGSERIAL    PRIMARY KEY,
    subject     VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_member_subject UNIQUE (subject)
);

COMMENT ON COLUMN member.subject IS 'Keycloak 사용자 식별자(sub)';
