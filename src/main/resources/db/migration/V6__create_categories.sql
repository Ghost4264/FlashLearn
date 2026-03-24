CREATE TABLE categories
(
    id         BIGSERIAL    NOT NULL,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_user_category_name UNIQUE (user_id, name),
    CONSTRAINT fk_category_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_category_user ON categories (user_id);

ALTER TABLE decks
    ADD COLUMN category_id BIGINT;

ALTER TABLE decks
    ADD CONSTRAINT fk_deck_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE SET NULL;

CREATE INDEX idx_deck_category ON decks (category_id);
