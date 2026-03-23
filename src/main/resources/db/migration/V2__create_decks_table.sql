CREATE TABLE decks
(
    id          BIGSERIAL    NOT NULL,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_decks PRIMARY KEY (id),
    CONSTRAINT fk_decks_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_decks_user_id ON decks (user_id);
CREATE INDEX idx_decks_public ON decks (is_public) WHERE is_public = TRUE;
