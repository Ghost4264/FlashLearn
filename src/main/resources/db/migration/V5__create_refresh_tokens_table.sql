-- Refresh токены для JWT аутентификации
CREATE TABLE refresh_tokens
(
    id         BIGSERIAL    NOT NULL,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
