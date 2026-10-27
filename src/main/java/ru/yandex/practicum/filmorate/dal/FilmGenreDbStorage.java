
package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Repository
public class FilmGenreDbStorage {

    private static final String ADD_GENRE = """
            INSERT INTO film_genre (film_id, genre_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_GENRES =
            "DELETE FROM film_genre WHERE film_id = ?";

    private static final String FIND_GENRES_BY_FILM_ID = """
            SELECT g.genre_id, g.genre_name
            FROM film_genre AS fg
            JOIN genre AS g ON g.genre_id = fg.genre_id
            WHERE fg.film_id = ?
            ORDER BY g.genre_id
            """;

    private static final String FIND_GENRES_FOR_FILMS = """
            SELECT fg.film_id, g.genre_id, g.genre_name
            FROM film_genre AS fg
            JOIN genre AS g ON g.genre_id = fg.genre_id
            WHERE fg.film_id IN (:filmIds)
            ORDER BY fg.film_id, g.genre_id
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final RowMapper<Genre> genreMapper;

    public FilmGenreDbStorage(
            JdbcTemplate jdbc,
            NamedParameterJdbcTemplate namedJdbc,
            RowMapper<Genre> genreMapper
    ) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
        this.genreMapper = genreMapper;
    }

    public void addGenres(Long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        jdbc.batchUpdate(
                ADD_GENRE,
                genres,
                genres.size(),
                (ps, genre) -> {
                    ps.setLong(1, filmId);
                    ps.setInt(2, genre.getId());
                }
        );
    }

    public void replaceGenres(Long filmId, Set<Genre> genres) {
        jdbc.update(DELETE_GENRES, filmId);
        addGenres(filmId, genres);
    }

    public Set<Genre> getGenresByFilmId(Long filmId) {
        return new LinkedHashSet<>(
                jdbc.query(
                        FIND_GENRES_BY_FILM_ID,
                        genreMapper,
                        filmId
                )
        );
    }

    public void loadGenres(Collection<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new HashMap<>();

        for (Film film : films) {
            film.setGenres(new LinkedHashSet<>());
            filmsById.put(film.getId(), film);
        }

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("filmIds", filmsById.keySet());

        namedJdbc.query(FIND_GENRES_FOR_FILMS, params, rs -> {
            Film film = filmsById.get(rs.getLong("film_id"));

            if (film != null) {
                Genre genre = genreMapper.mapRow(
                        rs,
                        rs.getRow() - 1
                );

                film.getGenres().add(genre);
            }
        });
    }
}
