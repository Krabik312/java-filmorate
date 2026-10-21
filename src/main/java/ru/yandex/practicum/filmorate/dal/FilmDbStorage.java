package ru.yandex.practicum.filmorate.dal;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.sql.PreparedStatement;
import java.util.*;

@Repository
public class FilmDbStorage implements FilmStorage {

    private static final String FIND_BY_ID_QUERY = "SELECT * FROM films AS f JOIN mpa AS m ON m.mpa_id = f.mpa_id" +
            "  WHERE f.id = ?";

    private static final String FIND_ALL_FILMS = "SELECT * FROM films AS f JOIN mpa AS m ON m.mpa_id = f.mpa_id";

    private static final String FIND_ALL_GENRES_BY_FILMS = "SELECT * FROM film_genre AS fg" +
            " JOIN genre AS g ON fg.genre_id = g.genre_id ORDER BY fg.film_id, g.genre_id";

    private static final String FIND_ALL_LIKES_BY_FILMS = "SELECT * FROM film_likes ORDER BY film_id";

    private static final String FIND_GENRES_BY_FILM_ID = "SELECT fg.genre_id, g.genre_name FROM film_genre AS fg " +
            "JOIN genre AS g ON g.genre_id = fg.genre_id WHERE fg.film_id = ? ORDER BY g.genre_id";

    private static final String FIND_LIKES_BY_FILM_ID = "SELECT user_id FROM film_likes WHERE film_id = ?";

    private static final String UPDATE_FILM = "UPDATE films SET name = ?, description = ?, release_date = ?," +
            " duration = ?, mpa_id = ? WHERE id = ?";

    private static final String INSERT_FILM_QUERY = """
        INSERT INTO films (name, description, release_date, duration, mpa_id)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String DELETE_GENRES = "DELETE FROM film_genre WHERE film_id = ?";

    private static final String ADD_GENRE = "INSERT INTO film_genre(film_id, genre_id) VALUES(?,?)";

    private final JdbcTemplate jdbc;
    private final RowMapper<Film> filmMapper;
    private final RowMapper<Genre> genreMapper;

    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> filmMapper, RowMapper<Genre> genreMapper){
        this.jdbc = jdbc;
        this.filmMapper = filmMapper;
        this.genreMapper = genreMapper;
    }

    @Override
    public Collection<Film> getAllFilms() {
        List<Film> films = jdbc.query(FIND_ALL_FILMS, filmMapper);
        HashMap<Long, Film> filmsById = new HashMap<>();
        for (Film film : films){
            filmsById.put(film.getId(), film);
        }

        jdbc.query(FIND_ALL_GENRES_BY_FILMS, rs -> {
            long filmId = rs.getLong("film_id");

            Genre genre = genreMapper.mapRow(rs, rs.getRow() - 1);

            Film film = filmsById.get(filmId);

            if (film != null && film.getGenres() != null) {
                film.getGenres().add(genre);
            }
        });

        jdbc.query(FIND_ALL_LIKES_BY_FILMS, rs -> {
            long filmId = rs.getLong("film_id");
            long userId = rs.getLong("user_id");

            Film film = filmsById.get(filmId);

            if (film != null && film.getLikes() != null) {
                film.getLikes().add(userId);
            }
        });

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
            throw new IllegalStateException("Не удалось получить ID созданного фильма");
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
        int rowsUpdated = jdbc.update(UPDATE_FILM, film.getName(), film.getDescription(),
                film.getReleaseDate(), film.getDuration(), film.getMpa().getId(), film.getId());
        if (rowsUpdated == 0){
            throw new NotFoundException("такой фильм не найден");
        }
        jdbc.update(DELETE_GENRES, film.getId());
        if (film.getGenres() != null){
            for (Genre genre : film.getGenres()){
                jdbc.update(ADD_GENRE, film.getId(), genre.getId());
            }
        }

        return getFilmById(film.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Не удалось получить созданный фильм с ID " + film.getId()
                ));
    }

    @Override
    public Optional<Film> getFilmById(Long filmId) {
        try {
            Film result  = jdbc.queryForObject(FIND_BY_ID_QUERY, filmMapper, filmId);
            Set<Genre> genres = new LinkedHashSet<>(
                    jdbc.query(FIND_GENRES_BY_FILM_ID, genreMapper, filmId)
            );
            Set<Long> likes = new HashSet<>(jdbc.query(
                    FIND_LIKES_BY_FILM_ID,
                    (rs, rowNum) -> rs.getLong("user_id"),
                    filmId
            ));
            if (result != null) {
                result.setGenres(genres);
                result.setLikes(likes);
            }
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
