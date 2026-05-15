package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.marker.Marker;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilmLogicTests {

    private Validator validator;
    private InMemoryUserStorage userStorage;
    private InMemoryFilmStorage filmStorage;
    private FilmService filmService;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        userStorage = new InMemoryUserStorage();
        filmStorage = new InMemoryFilmStorage();
        filmService = new FilmService(filmStorage, userStorage);
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Nice film");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    private User validUser() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.addUser(user);
    }

    private Film persistFilm(String name, int likesCount) {
        Film film = validFilm();
        film.setName(name);
        for (long i = 1; i <= likesCount; i++) {
            film.addLike(i);
        }
        return filmStorage.addFilm(film);
    }

    @Test
    void validFilmCreateHasNoViolations() {
        assertThat(validator.validate(validFilm(), Marker.OnCreate.class, Default.class)).isEmpty();
    }

    @Test
    void filmCreateWithNullIdHasNoViolations() {
        Film film = validFilm();
        film.setId(null);

        assertThat(validator.validate(film, Marker.OnCreate.class, Default.class)).isEmpty();
    }

    @Test
    void filmCreateWithNonNullIdHasViolation() {
        Film film = validFilm();
        film.setId(1L);

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Id не задается пользователем");
    }

    @Test
    void filmCreateWithBlankNameHasViolation() {
        Film film = validFilm();
        film.setName(" ");

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Имя не может быть null или пустым");
    }

    @Test
    void filmCreateWithLongDescriptionHasViolation() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Максимальная длина описания 200 символов");
    }

    @Test
    void filmCreateWithNullReleaseDateHasViolation() {
        Film film = validFilm();
        film.setReleaseDate(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Дата выхода должна быть указана");
    }

    @Test
    void filmCreateWithNullDurationHasViolation() {
        Film film = validFilm();
        film.setDuration(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Длительность должна быть указана");
    }

    @Test
    void filmCreateWithZeroDurationHasViolation() {
        Film film = validFilm();
        film.setDuration(0);

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Продолжительность фильма не может быть меньше 1й минуты");
    }

    @Test
    void filmUpdateWithValidDataHasNoViolations() {
        Film film = validFilm();
        film.setId(1L);

        assertThat(validator.validate(film, Marker.OnUpdate.class, Default.class)).isEmpty();
    }

    @Test
    void filmUpdateWithNullIdHasViolation() {
        Film film = validFilm();
        film.setId(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film, Marker.OnUpdate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Должен быть id");
    }

    @Test
    void addFilmSetsIdAndSecondFilmIncrementsId() {
        Film first = filmStorage.addFilm(validFilm());
        Film second = validFilm();
        second.setName("Film 2");

        Film createdSecond = filmStorage.addFilm(second);

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(createdSecond.getId()).isEqualTo(2L);
    }

    @Test
    void addFilmWithTooEarlyReleaseThrowsValidateException() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThatThrownBy(() -> filmStorage.addFilm(film))
                .isInstanceOf(ValidateException.class)
                .hasMessageContaining("Дата релиза должна быть не раньше");
    }

    @Test
    void updateFilmChangesExistingFields() {
        Film created = filmStorage.addFilm(validFilm());

        Film update = new Film();
        update.setId(created.getId());
        update.setName("New Film");
        update.setDescription("New description");
        update.setReleaseDate(LocalDate.of(2001, 1, 1));
        update.setDuration(150);

        Film updated = filmStorage.updateFilm(update);

        assertThat(updated.getName()).isEqualTo("New Film");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getReleaseDate()).isEqualTo(LocalDate.of(2001, 1, 1));
        assertThat(updated.getDuration()).isEqualTo(150);
    }

    @Test
    void updateFilmWithUnknownIdThrowsNotFoundException() {
        Film update = validFilm();
        update.setId(999L);

        assertThatThrownBy(() -> filmStorage.updateFilm(update))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильма с id 999");
    }

    @Test
    void getFilmByIdReturnsStoredFilmAsOptional() {
        Film created = filmStorage.addFilm(validFilm());

        assertThat(filmStorage.getFilmById(created.getId())).isPresent();
        assertThat(filmStorage.getFilmById(created.getId()).get()).isSameAs(created);
    }

    @Test
    void getFilmByIdMissingReturnsEmptyOptional() {
        assertThat(filmStorage.getFilmById(999L)).isEmpty();
    }

    @Test
    void addLikeAddsUserToFilmLikes() {
        User user = userStorage.addUser(validUser());
        Film film = filmStorage.addFilm(validFilm());

        filmService.addLike(film.getId(), user.getId());

        assertThat(film.getLikes()).contains(user.getId());
    }

    @Test
    void addLikeDoesNotDuplicateUserLike() {
        User user = userStorage.addUser(validUser());
        Film film = filmStorage.addFilm(validFilm());

        filmService.addLike(film.getId(), user.getId());
        filmService.addLike(film.getId(), user.getId());

        assertThat(film.getLikes()).hasSize(1);
    }

    @Test
    void deleteLikeRemovesUserFromFilmLikes() {
        User user = userStorage.addUser(validUser());
        Film film = filmStorage.addFilm(validFilm());

        filmService.addLike(film.getId(), user.getId());
        filmService.deleteLike(film.getId(), user.getId());

        assertThat(film.getLikes()).doesNotContain(user.getId());
    }

    @Test
    void getPopularFilmsSortsByLikesAndLimitsCount() {
        Film film1 = persistFilm("Film 1", 1);
        Film film2 = persistFilm("Film 2", 3);
        Film film3 = persistFilm("Film 3", 2);

        List<Film> popular = filmService.getPopularFilmsByLike(2);

        assertThat(popular).extracting(Film::getId)
                .containsExactly(film2.getId(), film3.getId());
    }

    @Test
    void addLikeWithMissingFilmThrowsNotFoundException() {
        User user = validUser();

        assertThatThrownBy(() -> filmService.addLike(999L, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id");
    }

    @Test
    void addLikeWithMissingUserThrowsNotFoundException() {
        Film film = filmStorage.addFilm(validFilm());

        assertThatThrownBy(() -> filmService.addLike(film.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id: 999");
    }
}
