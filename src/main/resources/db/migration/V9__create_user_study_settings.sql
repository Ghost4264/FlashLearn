CREATE TABLE user_study_settings
(
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT        NOT NULL UNIQUE,
    new_cards_per_session INTEGER       NOT NULL DEFAULT 20,
    interval_modifier     DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_study_settings_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

INSERT INTO user_study_settings (user_id, new_cards_per_session, interval_modifier, created_at, updated_at)
SELECT u.id, 20, 1.0, NOW(), NOW()
FROM users u
ON CONFLICT (user_id) DO NOTHING;
