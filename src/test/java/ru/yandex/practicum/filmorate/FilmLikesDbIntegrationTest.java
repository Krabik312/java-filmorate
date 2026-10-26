
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
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        UserRowMapper.class,
        FilmDbStorage.class,
        FilmRowMapper.class,
        GenreRowMapper.class
})
class FilmLikesDbIntegrationTest {

    @Autowired
    private UserDbStorage users;

    @Autowired
    private FilmDbStorage films;

    @Test
    void likePersistsInDatabase() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("test-user");
        user.setBirthday(LocalDate.of(1995, 1, 1));

        user = users.addUser(user);

        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        film = films.addFilm(film);

        FilmService service = new FilmService(films, users);

        service.addLike(film.getId(), user.getId());
        service.addLike(film.getId(), user.getId());

        assertThat(
                films.getFilmById(film.getId())
                        .orElseThrow()
                        .getLikes()
        ).containsExactly(user.getId());

        assertThat(service.getPopularFilmsByLike(1))
                .extracting(Film::getId)
                .containsExactly(film.getId());

        service.deleteLike(film.getId(), user.getId());

        assertThat(
                films.getFilmById(film.getId())
                        .orElseThrow()
                        .getLikes()
        ).isEmpty();
    }
}
