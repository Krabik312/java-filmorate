
package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Repository
public class MpaDbStorage {

    private static final String GET_MPA_BY_ID =
            "SELECT * FROM mpa WHERE mpa_id = ?";

    private static final String GET_ALL_MPA =
            "SELECT * FROM mpa ORDER BY mpa_id";

    private final JdbcTemplate jdbc;
    private final RowMapper<Mpa> mpaRowMapper;

    public MpaDbStorage(JdbcTemplate jdbc, MpaRowMapper mpaRowMapper) {
        this.jdbc = jdbc;
        this.mpaRowMapper = mpaRowMapper;
    }

    public Optional<Mpa> getMpa(Integer id) {
        try {
            Mpa mpa = jdbc.queryForObject(
                    GET_MPA_BY_ID,
                    mpaRowMapper,
                    id
            );

            return Optional.ofNullable(mpa);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Mpa> getAllMpa() {
        return jdbc.query(GET_ALL_MPA, mpaRowMapper);
    }
}
