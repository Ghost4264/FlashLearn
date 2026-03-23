-- Прогресс повторения карточек по алгоритму SM-2
CREATE TABLE review_progress
(
    id             BIGSERIAL NOT NULL,
    user_id        BIGINT    NOT NULL,
    card_id        BIGINT    NOT NULL,
    ease_factor    FLOAT     NOT NULL DEFAULT 2.5,  -- коэффициент лёгкости (мин 1.3)
    interval_days  INT       NOT NULL DEFAULT 1,    -- интервал до следующего повторения
    repetitions    INT       NOT NULL DEFAULT 0,    -- кол-во успешных повторений подряд
    next_review_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_review_at TIMESTAMP,

    CONSTRAINT pk_review_progress PRIMARY KEY (id),
    CONSTRAINT uq_review_user_card UNIQUE (user_id, card_id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_card FOREIGN KEY (card_id)
        REFERENCES cards (id) ON DELETE CASCADE
);

-- Главный индекс: какие карточки пора повторять сегодня
CREATE INDEX idx_review_due ON review_progress (user_id, next_review_at);
