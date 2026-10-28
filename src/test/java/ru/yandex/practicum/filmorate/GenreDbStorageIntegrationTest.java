
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        GenreDbStorage.class,
        GenreRowMapper.class
})
class GenreDbStorageIntegrationTest {

    @Autowired
    private GenreDbStorage genreStorage;

    @Test
    void getAllGenresReturnsSixGenres() {
        assertThat(genreStorage.getAllGenres())
                .hasSize(6)
                .extracting(Genre::getId)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void getAllGenresReturnsCorrectNames() {
        assertThat(genreStorage.getAllGenres())
                .extracting(Genre::getName)
                .containsExactly(
                        "Комедия",
                        "Драма",
                        "Мультфильм",
                        "Триллер",
                        "Документальный",
                        "Боевик"
                );
    }

    @Test
    void getGenreReturnsCorrectGenre() {
        Genre genre = genreStorage.getGenre(1L).orElseThrow();

        assertThat(genre.getId()).isEqualTo(1);
        assertThat(genre.getName()).isEqualTo("Комедия");
    }

    @Test
    void getGenreReturnsEmptyForUnknownId() {
        assertThat(genreStorage.getGenre(999L)).isEmpty();
    }
}
