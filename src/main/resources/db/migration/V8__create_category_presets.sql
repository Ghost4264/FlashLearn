CREATE TABLE category_presets
(
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_category_presets PRIMARY KEY (id),
    CONSTRAINT uq_category_preset_name UNIQUE (name)
);

INSERT INTO category_presets (name, created_at)
VALUES
    ('Java', NOW()),
    ('Языки', NOW()),
    ('Kotlin', NOW()),
    ('Docker', NOW()),
    ('Git', NOW()),
    ('Разное', NOW())
ON CONFLICT (name) DO NOTHING;
