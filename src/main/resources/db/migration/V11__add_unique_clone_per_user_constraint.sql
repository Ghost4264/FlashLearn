WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, cloned_from_id
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM decks
    WHERE cloned_from_id IS NOT NULL
),
to_delete AS (
    SELECT id
    FROM ranked
    WHERE rn > 1
)
DELETE FROM decks d
WHERE d.id IN (SELECT id FROM to_delete);

CREATE UNIQUE INDEX ux_decks_user_cloned_from_not_null
    ON decks (user_id, cloned_from_id)
    WHERE cloned_from_id IS NOT NULL;
