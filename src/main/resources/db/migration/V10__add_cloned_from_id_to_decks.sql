ALTER TABLE decks
    ADD COLUMN cloned_from_id BIGINT REFERENCES decks(id) ON DELETE SET NULL;

CREATE INDEX idx_decks_cloned_from_id ON decks (cloned_from_id) WHERE cloned_from_id IS NOT NULL;
