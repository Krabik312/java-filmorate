
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmGenreDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmLikeDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        FilmDbStorage.class,
        FilmGenreDbStorage.class,
        FilmLikeDbStorage.class,
        FilmRowMapper.class,
        GenreRowMapper.class
})
class FilmGenreDbStorageIntegrationTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private FilmGenreDbStorage genreStorage;

    private Film createFilm() {
        Film film = new Film();
        film.setName("Тестовый фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        film.setGenres(new LinkedHashSet<>());
        return filmStorage.addFilm(film);
    }

    private Genre genre(int id) {
        Genre genre = new Genre();
        genre.setId(id);
        return genre;
    }

    @Test
    void addGenresSavesSeveralGenres() {
        Film film = createFilm();

        genreStorage.addGenres(
                film.getId(),
                new LinkedHashSet<>(List.of(genre(1), genre(2)))
        );

        assertThat(genreStorage.getGenresByFilmId(film.getId()))
                .extracting(Genre::getId)
                .containsExactly(1, 2);
    }

    @Test
    void addGenresWithEmptySetDoesNothing() {
        Film film = createFilm();

        genreStorage.addGenres(film.getId(), Set.of());

        assertThat(genreStorage.getGenresByFilmId(film.getId()))
                .isEmpty();
    }

    @Test
    void addGenresWithNullDoesNothing() {
        Film film = createFilm();

        assertThatCode(
                () -> genreStorage.addGenres(film.getId(), null)
        ).doesNotThrowAnyException();

        assertThat(genreStorage.getGenresByFilmId(film.getId()))
                .isEmpty();
    }

    @Test
    void replaceGenresReplacesOldGenres() {
        Film film = createFilm();

        genreStorage.addGenres(
                film.getId(),
                new LinkedHashSet<>(List.of(genre(1), genre(2)))
        );

        genreStorage.replaceGenres(
                film.getId(),
                new LinkedHashSet<>(List.of(genre(3), genre(4)))
        );

        assertThat(genreStorage.getGenresByFilmId(film.getId()))
                .extracting(Genre::getId)
                .containsExactly(3, 4);
    }

    @Test
    void replaceGenresWithEmptySetRemovesOldGenres() {
        Film film = createFilm();

        genreStorage.addGenres(
                film.getId(),
                Set.of(genre(1))
        );

        genreStorage.replaceGenres(film.getId(), Set.of());

        assertThat(genreStorage.getGenresByFilmId(film.getId()))
                .isEmpty();
    }

    @Test
    void getGenresByFilmIdReturnsEmptyForFilmWithoutGenres() {
        Film film = createFilm();

        assertThat(genreStorage.getGenresByFilmId(film.getId()))
                .isEmpty();
    }

    @Test
    void loadGenresLoadsGenresForSeveralFilms() {
        Film first = createFilm();
        Film second = createFilm();

        genreStorage.addGenres(
                first.getId(),
                Set.of(genre(1), genre(2))
        );
        genreStorage.addGenres(
                second.getId(),
                Set.of(genre(5))
        );

        Film firstLoaded = filmStorage.getFilmById(first.getId())
                .orElseThrow();
        Film secondLoaded = filmStorage.getFilmById(second.getId())
                .orElseThrow();

        // Проверяем, что DAO заменяет ранее заполненные коллекции.
        firstLoaded.setGenres(new LinkedHashSet<>());
        secondLoaded.setGenres(new LinkedHashSet<>());

        List<Film> films = new ArrayList<>(
                List.of(firstLoaded, secondLoaded)
        );

        genreStorage.loadGenres(films);

        assertThat(firstLoaded.getGenres())
                .extracting(Genre::getId)
                .containsExactlyInAnyOrder(1, 2);

        assertThat(secondLoaded.getGenres())
                .extracting(Genre::getId)
                .containsExactly(5);
    }

    @Test
    void loadGenresHandlesEmptyCollection() {
        assertThatCode(
                () -> genreStorage.loadGenres(List.of())
        ).doesNotThrowAnyException();
    }
}
