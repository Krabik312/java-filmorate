
package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.sql.PreparedStatement;
import java.util.*;

@Repository
public class FilmDbStorage implements FilmStorage {

    private static final String FIND_BY_ID_QUERY = """
            SELECT f.*, m.mpa_name
            FROM films AS f
            JOIN mpa AS m ON m.mpa_id = f.mpa_id
            WHERE f.id = ?
            """;

    private static final String FIND_ALL_FILMS = """
            SELECT f.*, m.mpa_name
            FROM films AS f
            JOIN mpa AS m ON m.mpa_id = f.mpa_id
            ORDER BY f.id
            """;

    private static final String FIND_GENRES_BY_FILM_ID = """
            SELECT g.genre_id, g.genre_name
            FROM film_genre AS fg
            JOIN genre AS g ON g.genre_id = fg.genre_id
            WHERE fg.film_id = ?
            ORDER BY g.genre_id
            """;

    private static final String FIND_LIKES_BY_FILM_ID = """
            SELECT user_id
            FROM film_likes
            WHERE film_id = ?
            """;

    private static final String FIND_GENRES_FOR_FILMS = """
            SELECT fg.film_id, g.genre_id, g.genre_name
            FROM film_genre AS fg
            JOIN genre AS g ON g.genre_id = fg.genre_id
            WHERE fg.film_id IN (:filmIds)
            ORDER BY fg.film_id, g.genre_id
            """;

    private static final String FIND_LIKES_FOR_FILMS = """
            SELECT film_id, user_id
            FROM film_likes
            WHERE film_id IN (:filmIds)
            """;

    private static final String FIND_POPULAR_FILMS = """
            SELECT f.*, m.mpa_name
            FROM films AS f
            JOIN mpa AS m ON f.mpa_id = m.mpa_id
            JOIN (
                SELECT f2.id, COUNT(fl.user_id) AS likes_count
                FROM films AS f2
                LEFT JOIN film_likes AS fl ON f2.id = fl.film_id
                GROUP BY f2.id
                ORDER BY likes_count DESC, f2.id
                LIMIT ?
            ) AS popular ON popular.id = f.id
            ORDER BY popular.likes_count DESC, f.id
            """;

    private static final String INSERT_FILM_QUERY = """
            INSERT INTO films (name, description, release_date, duration, mpa_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_FILM = """
            UPDATE films
            SET name = ?,
                description = ?,
                release_date = ?,
                duration = ?,
                mpa_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_GENRES =
            "DELETE FROM film_genre WHERE film_id = ?";

    private static final String ADD_GENRE =
            "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";

    private static final String ADD_LIKE = """
        MERGE INTO film_likes (film_id, user_id)
        KEY (film_id, user_id)
        VALUES (?, ?)
        """;

    private static final String DELETE_LIKE =
            "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final RowMapper<Film> filmMapper;
    private final RowMapper<Genre> genreMapper;

    public FilmDbStorage(
            JdbcTemplate jdbc,
            NamedParameterJdbcTemplate namedJdbc,
            RowMapper<Film> filmMapper,
            RowMapper<Genre> genreMapper
    ) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
        this.filmMapper = filmMapper;
        this.genreMapper = genreMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Film> getAllFilms() {
        List<Film> films = jdbc.query(FIND_ALL_FILMS, filmMapper);

        loadFilmDetails(films);

        return films;
    }

    @Override
    @Transactional
    public Film addFilm(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    INSERT_FILM_QUERY,
                    new String[]{"id"}
            );

            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, java.sql.Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());

            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "Не удалось получить ID созданного фильма"
            );
        }

        film.setId(generatedId.longValue());

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                jdbc.update(
                        ADD_GENRE,
                        film.getId(),
                        genre.getId()
                );
            }
        }

        return getFilmById(film.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Не удалось получить созданный фильм с ID " + film.getId()
                ));
    }

    @Override
    @Transactional
    public Film updateFilm(Film film) {
        int rowsUpdated = jdbc.update(
                UPDATE_FILM,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        if (rowsUpdated == 0) {
            throw new NotFoundException(
                    "Фильм с ID " + film.getId() + " не найден"
            );
        }

        jdbc.update(DELETE_GENRES, film.getId());

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                jdbc.update(
                        ADD_GENRE,
                        film.getId(),
                        genre.getId()
                );
            }
        }

        return getFilmById(film.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Не удалось получить обновлённый фильм с ID " + film.getId()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Film> getFilmById(Long filmId) {
        try {
            Film result = jdbc.queryForObject(
                    FIND_BY_ID_QUERY,
                    filmMapper,
                    filmId
            );

            Set<Genre> genres = new LinkedHashSet<>(
                    jdbc.query(
                            FIND_GENRES_BY_FILM_ID,
                            genreMapper,
                            filmId
                    )
            );

            Set<Long> likes = new HashSet<>(
                    jdbc.query(
                            FIND_LIKES_BY_FILM_ID,
                            (rs, rowNum) -> rs.getLong("user_id"),
                            filmId
                    )
            );

            if (result != null) {
                result.setGenres(genres);
                result.setLikes(likes);
            }

            return Optional.ofNullable(result);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void loadFilmDetails(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new HashMap<>();

        for (Film film : films) {
            film.setGenres(new LinkedHashSet<>());
            film.setLikes(new HashSet<>());
            filmsById.put(film.getId(), film);
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("filmIds", filmsById.keySet());

        namedJdbc.query(FIND_GENRES_FOR_FILMS, params, rs -> {
            long filmId = rs.getLong("film_id");

            Genre genre = genreMapper.mapRow(
                    rs,
                    rs.getRow() - 1
            );

            Film film = filmsById.get(filmId);

            if (film != null) {
                film.getGenres().add(genre);
            }
        });

        namedJdbc.query(FIND_LIKES_FOR_FILMS, params, rs -> {
            long filmId = rs.getLong("film_id");
            long userId = rs.getLong("user_id");

            Film film = filmsById.get(filmId);

            if (film != null) {
                film.getLikes().add(userId);
            }
        });
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbc.update(ADD_LIKE, filmId, userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        jdbc.update(DELETE_LIKE, filmId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Film> getPopularFilmsByLike(Integer count) {
        if (count == null || count <= 0) {
            throw new IllegalArgumentException(
                    "Количество фильмов должно быть положительным"
            );
        }

        List<Film> films = jdbc.query(
                FIND_POPULAR_FILMS,
                filmMapper,
                count
        );

        loadFilmDetails(films);

        return films;
    }
}
