SELECT user_id,
       cloned_from_id,
       COUNT(*) AS clones_count,
       ARRAY_AGG(id ORDER BY created_at DESC, id DESC) AS deck_ids_newest_first
FROM decks
WHERE cloned_from_id IS NOT NULL
GROUP BY user_id, cloned_from_id
HAVING COUNT(*) > 1
ORDER BY clones_count DESC, user_id, cloned_from_id;
