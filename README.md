# java-filmorate
Template repository for Filmorate project.



DB for Filmorate:
![image](filmorate_DB.png)

## Основные SQL-запросы

Ниже представлены основные запросы для анализа данных в системе фильмов и пользователей.

```sql
-- 1. Топ фильмов по лайкам
SELECT 
    f.name,
    COUNT(fl.user_id) AS likes_count
FROM Film f
LEFT JOIN Film_Likes fl ON f.id = fl.film_id
GROUP BY f.id, f.name
ORDER BY likes_count DESC;

-- 2. Подтверждённые дружеские связи
SELECT 
    fr.requester_id,
    fr.addressee_id
FROM Friendship fr
WHERE fr.status = 'confirmed';

-- 3. Фильмы по жанру (пример: Comedy)
SELECT 
    f.name AS film_name,
    g.name AS genre
FROM Film f
JOIN Film_Genre fg ON f.id = fg.film_id
JOIN Genre g ON fg.genre_id = g.genre_id
WHERE g.name = 'Comedy';
