WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, cloned_from_id
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM decks
    WHERE cloned_from_id IS NOT NULL
)
DELETE FROM decks d
USING ranked r
WHERE d.id = r.id
  AND r.rn > 1;
