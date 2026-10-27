
package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Repository
public class FilmLikeDbStorage {

    private static final String ADD_LIKE = """
            MERGE INTO film_likes (film_id, user_id)
            KEY (film_id, user_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_LIKE =
            "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";

    private static final String FIND_LIKES_BY_FILM_ID = """
            SELECT user_id
            FROM film_likes
            WHERE film_id = ?
            """;

    private static final String FIND_LIKES_FOR_FILMS = """
            SELECT film_id, user_id
            FROM film_likes
            WHERE film_id IN (:filmIds)
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmLikeDbStorage(
            JdbcTemplate jdbc,
            NamedParameterJdbcTemplate namedJdbc
    ) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    public void addLike(Long filmId, Long userId) {
        jdbc.update(ADD_LIKE, filmId, userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        jdbc.update(DELETE_LIKE, filmId, userId);
    }

    public Set<Long> getLikesByFilmId(Long filmId) {
        return new HashSet<>(
                jdbc.query(
                        FIND_LIKES_BY_FILM_ID,
                        (rs, rowNum) -> rs.getLong("user_id"),
                        filmId
                )
        );
    }

    public void loadLikes(Collection<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new HashMap<>();

        for (Film film : films) {
            film.setLikes(new HashSet<>());
            filmsById.put(film.getId(), film);
        }

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("filmIds", filmsById.keySet());

        namedJdbc.query(FIND_LIKES_FOR_FILMS, params, rs -> {
            Film film = filmsById.get(rs.getLong("film_id"));

            if (film != null) {
                film.getLikes().add(rs.getLong("user_id"));
            }
        });
    }
}
