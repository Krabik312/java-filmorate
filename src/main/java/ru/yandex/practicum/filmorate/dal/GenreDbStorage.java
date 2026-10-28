package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

@Repository
public class GenreDbStorage {

    private static final String GET_GENRE_BY_ID = "SELECT * FROM genre WHERE genre_id = ?";

    private static final String GET_ALL_GENRES = "SELECT * FROM genre ORDER BY genre_id";


    private final JdbcTemplate jdbc;
    private final RowMapper<Genre> genreRowMapper;

    public GenreDbStorage(JdbcTemplate jdbc, GenreRowMapper genreRowMapper) {
        this.jdbc = jdbc;
        this.genreRowMapper = genreRowMapper;
    }

    public Optional<Genre> getGenre(Long id) {
        try {
            Genre genre = jdbc.queryForObject(GET_GENRE_BY_ID, genreRowMapper, id);

            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Genre> getAllGenres() {
        return jdbc.query(GET_ALL_GENRES, genreRowMapper);
    }
}
