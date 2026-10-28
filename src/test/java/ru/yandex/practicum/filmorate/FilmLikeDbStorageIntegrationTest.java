
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmGenreDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmLikeDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        FilmDbStorage.class,
        FilmGenreDbStorage.class,
        FilmLikeDbStorage.class,
        UserDbStorage.class,
        FilmRowMapper.class,
        GenreRowMapper.class,
        UserRowMapper.class
})
class FilmLikeDbStorageIntegrationTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private FilmLikeDbStorage likeStorage;

    @Autowired
    private UserDbStorage userStorage;

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

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        return userStorage.addUser(user);
    }

    @Test
    void addLikeSavesLike() {
        Film film = createFilm();
        User user = createUser("user1");

        likeStorage.addLike(film.getId(), user.getId());

        assertThat(likeStorage.getLikesByFilmId(film.getId()))
                .containsExactly(user.getId());
    }

    @Test
    void addLikeDoesNotCreateDuplicates() {
        Film film = createFilm();
        User user = createUser("user1");

        likeStorage.addLike(film.getId(), user.getId());
        likeStorage.addLike(film.getId(), user.getId());

        assertThat(likeStorage.getLikesByFilmId(film.getId()))
                .containsExactly(user.getId());
    }

    @Test
    void deleteLikeRemovesSpecifiedLike() {
        Film film = createFilm();

        User first = createUser("user1");
        User second = createUser("user2");

        likeStorage.addLike(film.getId(), first.getId());
        likeStorage.addLike(film.getId(), second.getId());

        likeStorage.deleteLike(film.getId(), first.getId());

        assertThat(likeStorage.getLikesByFilmId(film.getId()))
                .containsExactly(second.getId());
    }

    @Test
    void deleteLikeWhenAbsentDoesNotFail() {
        Film film = createFilm();
        User user = createUser("user1");

        assertThatCode(
                () -> likeStorage.deleteLike(
                        film.getId(),
                        user.getId()
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void getLikesByFilmIdReturnsEmptyWhenNoLikes() {
        Film film = createFilm();

        assertThat(likeStorage.getLikesByFilmId(film.getId()))
                .isEmpty();
    }

    @Test
    void getLikesByFilmIdReturnsAllLikes() {
        Film film = createFilm();

        User first = createUser("user1");
        User second = createUser("user2");

        likeStorage.addLike(film.getId(), first.getId());
        likeStorage.addLike(film.getId(), second.getId());

        assertThat(likeStorage.getLikesByFilmId(film.getId()))
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId()
                );
    }

    @Test
    void loadLikesLoadsLikesForSeveralFilms() {
        Film firstFilm = createFilm();
        Film secondFilm = createFilm();

        User firstUser = createUser("user1");
        User secondUser = createUser("user2");

        likeStorage.addLike(firstFilm.getId(), firstUser.getId());
        likeStorage.addLike(firstFilm.getId(), secondUser.getId());
        likeStorage.addLike(secondFilm.getId(), secondUser.getId());

        Film firstLoaded = filmStorage
                .getFilmById(firstFilm.getId())
                .orElseThrow();

        Film secondLoaded = filmStorage
                .getFilmById(secondFilm.getId())
                .orElseThrow();

        // Метод должен самостоятельно заполнить коллекции лайков.
        firstLoaded.setLikes(null);
        secondLoaded.setLikes(null);

        List<Film> films = new ArrayList<>(
                List.of(firstLoaded, secondLoaded)
        );

        likeStorage.loadLikes(films);

        assertThat(firstLoaded.getLikes())
                .containsExactlyInAnyOrder(
                        firstUser.getId(),
                        secondUser.getId()
                );

        assertThat(secondLoaded.getLikes())
                .containsExactly(secondUser.getId());
    }

    @Test
    void loadLikesHandlesEmptyCollection() {
        assertThatCode(
                () -> likeStorage.loadLikes(List.of())
        ).doesNotThrowAnyException();
    }
}
