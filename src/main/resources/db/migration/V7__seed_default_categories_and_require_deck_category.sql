INSERT INTO categories (user_id, name, created_at)
SELECT u.id, c.name, NOW()
FROM users u
CROSS JOIN (
    VALUES
        ('Java'),
        ('Языки'),
        ('Kotlin'),
        ('Docker'),
        ('Git'),
        ('Разное')
) AS c(name)
ON CONFLICT (user_id, name) DO NOTHING;

UPDATE decks d
SET category_id = c.id
FROM categories c
WHERE d.category_id IS NULL
  AND c.user_id = d.user_id
  AND c.name = 'Разное';

-- 3) Делаем категорию обязательной
ALTER TABLE decks
    ALTER COLUMN category_id SET NOT NULL;
