CREATE TABLE cards
(
    id         BIGSERIAL NOT NULL,
    deck_id    BIGINT    NOT NULL,
    front      TEXT      NOT NULL,
    back       TEXT      NOT NULL,
    hint       TEXT,
    position   INT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT fk_cards_deck FOREIGN KEY (deck_id)
        REFERENCES decks (id) ON DELETE CASCADE
);

CREATE INDEX idx_cards_deck_id ON cards (deck_id);
