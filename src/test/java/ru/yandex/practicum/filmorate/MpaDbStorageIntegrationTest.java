
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.MpaDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        MpaDbStorage.class,
        MpaRowMapper.class
})
class MpaDbStorageIntegrationTest {

    @Autowired
    private MpaDbStorage mpaStorage;

    @Test
    void getAllMpaReturnsExactlyFiveRatings() {
        assertThat(mpaStorage.getAllMpa())
                .hasSize(5)
                .extracting(Mpa::getId)
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void getAllMpaReturnsAmericanRatingSystem() {
        assertThat(mpaStorage.getAllMpa())
                .extracting(Mpa::getName)
                .containsExactly(
                        "G",
                        "PG",
                        "PG-13",
                        "R",
                        "NC-17"
                );
    }

    @Test
    void getMpaReturnsCorrectRating() {
        Mpa mpa = mpaStorage.getMpa(3).orElseThrow();

        assertThat(mpa.getId()).isEqualTo(3);
        assertThat(mpa.getName()).isEqualTo("PG-13");
    }

    @Test
    void getMpaReturnsEmptyForUnknownId() {
        assertThat(mpaStorage.getMpa(999)).isEmpty();
    }
}
