
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        FilmDbStorage.class,
        UserDbStorage.class,
        FilmRowMapper.class,
        GenreRowMapper.class,
        UserRowMapper.class
})
class FilmDbStorageIntegrationTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    private Film createFilm(String name, int mpaId, int... genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание фильма");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(mpaId);
        film.setMpa(mpa);

        Set<Genre> genres = new LinkedHashSet<>();

        for (int genreId : genreIds) {
            Genre genre = new Genre();
            genre.setId(genreId);
            genres.add(genre);
        }

        film.setGenres(genres);

        return film;
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        return userStorage.addUser(user);
    }

    @Test
    void addFilmSavesMainFieldsAndMpa() {
        Film film = createFilm("Первый фильм", 3);

        Film saved = filmStorage.addFilm(film);

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getName()).isEqualTo("Первый фильм");
        assertThat(saved.getDescription()).isEqualTo("Описание фильма");
        assertThat(saved.getReleaseDate())
                .isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(saved.getDuration()).isEqualTo(120);
        assertThat(saved.getMpa().getId()).isEqualTo(3);
        assertThat(saved.getMpa().getName()).isEqualTo("PG-13");
    }

    @Test
    void addFilmSavesGenres() {
        Film saved = filmStorage.addFilm(
                createFilm("Фильм с жанрами", 1, 1, 2)
        );

        assertThat(saved.getGenres())
                .extracting(Genre::getId)
                .containsExactly(1, 2);
    }

    @Test
    void getFilmByIdReturnsFilmWithDetails() {
        Film saved = filmStorage.addFilm(
                createFilm("Поиск фильма", 2, 3, 6)
        );

        User user = createUser("viewer");
        filmStorage.addLike(saved.getId(), user.getId());

        Film found = filmStorage.getFilmById(saved.getId())
                .orElseThrow();

        assertThat(found.getName()).isEqualTo("Поиск фильма");
        assertThat(found.getMpa().getName()).isEqualTo("PG");

        assertThat(found.getGenres())
                .extracting(Genre::getId)
                .containsExactly(3, 6);

        assertThat(found.getLikes())
                .containsExactly(user.getId());
    }

    @Test
    void getFilmByIdReturnsEmptyForMissingFilm() {
        assertThat(filmStorage.getFilmById(999L)).isEmpty();
    }

    @Test
    void getAllFilmsReturnsEveryFilmWithDetails() {
        Film first = filmStorage.addFilm(
                createFilm("Первый", 1, 1)
        );

        Film second = filmStorage.addFilm(
                createFilm("Второй", 2, 2, 4)
        );

        User user = createUser("liker");
        filmStorage.addLike(second.getId(), user.getId());

        List<Film> all = List.copyOf(filmStorage.getAllFilms());

        assertThat(all).hasSize(2);

        assertThat(all)
                .extracting(Film::getId)
                .containsExactly(first.getId(), second.getId());

        assertThat(all.get(0).getGenres())
                .extracting(Genre::getId)
                .containsExactly(1);

        assertThat(all.get(1).getGenres())
                .extracting(Genre::getId)
                .containsExactly(2, 4);

        assertThat(all.get(1).getLikes())
                .containsExactly(user.getId());
    }

    @Test
    void getAllFilmsReturnsEmptyWhenNoFilmsExist() {
        assertThat(filmStorage.getAllFilms()).isEmpty();
    }

    @Test
    void updateFilmChangesFieldsAndGenres() {
        Film original = filmStorage.addFilm(
                createFilm("Старое название", 1, 1, 2)
        );

        Film changes = createFilm("Новое название", 4, 5);
        changes.setId(original.getId());
        changes.setDescription("Новое описание");
        changes.setDuration(150);

        Film updated = filmStorage.updateFilm(changes);

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getName()).isEqualTo("Новое название");
        assertThat(updated.getDescription()).isEqualTo("Новое описание");
        assertThat(updated.getDuration()).isEqualTo(150);
        assertThat(updated.getMpa().getId()).isEqualTo(4);

        assertThat(updated.getGenres())
                .extracting(Genre::getId)
                .containsExactly(5);
    }

    @Test
    void updateFilmCanRemoveAllGenres() {
        Film original = filmStorage.addFilm(
                createFilm("Фильм", 1, 1, 2)
        );

        Film changes = createFilm("Фильм", 1);
        changes.setId(original.getId());

        Film updated = filmStorage.updateFilm(changes);

        assertThat(updated.getGenres()).isEmpty();
    }

    @Test
    void updateFilmPreservesLikes() {
        Film original = filmStorage.addFilm(
                createFilm("Фильм", 1, 1)
        );

        User user = createUser("viewer");
        filmStorage.addLike(original.getId(), user.getId());

        Film changes = createFilm("Новое название", 2, 2);
        changes.setId(original.getId());

        Film updated = filmStorage.updateFilm(changes);

        assertThat(updated.getLikes()).contains(user.getId());
    }

    @Test
    void updateMissingFilmThrowsNotFoundException() {
        Film film = createFilm("Несуществующий", 1);
        film.setId(999L);

        assertThatThrownBy(() -> filmStorage.updateFilm(film))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addLikeIsIdempotent() {
        Film film = filmStorage.addFilm(
                createFilm("Фильм", 1)
        );

        User user = createUser("viewer");

        filmStorage.addLike(film.getId(), user.getId());
        filmStorage.addLike(film.getId(), user.getId());

        Film found = filmStorage.getFilmById(film.getId())
                .orElseThrow();

        assertThat(found.getLikes())
                .containsExactly(user.getId());
    }

    @Test
    void deleteLikeRemovesOnlySpecifiedLike() {
        Film film = filmStorage.addFilm(
                createFilm("Фильм", 1)
        );

        User first = createUser("first");
        User second = createUser("second");

        filmStorage.addLike(film.getId(), first.getId());
        filmStorage.addLike(film.getId(), second.getId());

        filmStorage.deleteLike(film.getId(), first.getId());

        Film found = filmStorage.getFilmById(film.getId())
                .orElseThrow();

        assertThat(found.getLikes())
                .containsExactly(second.getId());
    }

    @Test
    void deleteMissingLikeDoesNotFail() {
        Film film = filmStorage.addFilm(
                createFilm("Фильм", 1)
        );

        User user = createUser("viewer");

        assertThatCode(
                () -> filmStorage.deleteLike(film.getId(), user.getId())
        ).doesNotThrowAnyException();
    }

    @Test
    void popularFilmsAreSortedByLikeCount() {
        Film first = filmStorage.addFilm(
                createFilm("Один лайк", 1)
        );

        Film second = filmStorage.addFilm(
                createFilm("Два лайка", 1, 2)
        );

        Film third = filmStorage.addFilm(
                createFilm("Без лайков", 1)
        );

        User user1 = createUser("user1");
        User user2 = createUser("user2");

        filmStorage.addLike(first.getId(), user1.getId());

        filmStorage.addLike(second.getId(), user1.getId());
        filmStorage.addLike(second.getId(), user2.getId());

        List<Film> popular = filmStorage.getPopularFilmsByLike(3);

        assertThat(popular)
                .extracting(Film::getId)
                .containsExactly(
                        second.getId(),
                        first.getId(),
                        third.getId()
                );

        assertThat(popular.get(0).getGenres())
                .extracting(Genre::getId)
                .containsExactly(2);

        assertThat(popular.get(0).getLikes()).hasSize(2);
    }

    @Test
    void popularFilmsRespectLimit() {
        filmStorage.addFilm(createFilm("Первый", 1));
        filmStorage.addFilm(createFilm("Второй", 1));
        filmStorage.addFilm(createFilm("Третий", 1));

        assertThat(filmStorage.getPopularFilmsByLike(2))
                .hasSize(2);
    }

    @Test
    void popularFilmsRejectInvalidCount() {
        assertThatThrownBy(
                () -> filmStorage.getPopularFilmsByLike(0)
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
                () -> filmStorage.getPopularFilmsByLike(null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void popularFilmsReturnEmptyWhenDatabaseIsEmpty() {
        assertThat(filmStorage.getPopularFilmsByLike(10))
                .isEmpty();
    }
}
