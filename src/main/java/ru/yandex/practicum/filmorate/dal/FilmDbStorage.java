
package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
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


    private final JdbcTemplate jdbc;
    private final RowMapper<Film> filmMapper;
    private final FilmGenreDbStorage genreStorage;
    private final FilmLikeDbStorage likeStorage;

    public FilmDbStorage(
            JdbcTemplate jdbc,
            RowMapper<Film> filmMapper,
            FilmGenreDbStorage genreStorage,
            FilmLikeDbStorage likeStorage
    ) {
        this.jdbc = jdbc;
        this.filmMapper = filmMapper;
        this.genreStorage = genreStorage;
        this.likeStorage = likeStorage;
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

        genreStorage.addGenres(film.getId(), film.getGenres());

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

        genreStorage.replaceGenres(film.getId(), film.getGenres());

        return getFilmById(film.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Не удалось получить обновлённый фильм с ID " + film.getId()
                ));
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Film> getFilmById(Long filmId) {
        try {
            Film film = jdbc.queryForObject(
                    FIND_BY_ID_QUERY,
                    filmMapper,
                    filmId
            );

            if (film != null) {
                film.setGenres(
                        genreStorage.getGenresByFilmId(filmId)
                );

                film.setLikes(
                        likeStorage.getLikesByFilmId(filmId)
                );
            }

            return Optional.ofNullable(film);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    private void loadFilmDetails(List<Film> films) {
        genreStorage.loadGenres(films);
        likeStorage.loadLikes(films);
    }


    @Override
    public void addLike(Long filmId, Long userId) {
        likeStorage.addLike(filmId, userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        likeStorage.deleteLike(filmId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Film> getPopularFilmsByLike(Integer count) {
        List<Film> films = jdbc.query(
                FIND_POPULAR_FILMS,
                filmMapper,
                count
        );

        loadFilmDetails(films);

        return films;
    }
}
