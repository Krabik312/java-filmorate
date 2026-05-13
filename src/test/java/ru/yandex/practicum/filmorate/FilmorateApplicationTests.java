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
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.marker.Marker;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilmorateApplicationTests {

    private Validator validator;
    private InMemoryUserStorage userStorage;
    private InMemoryFilmStorage filmStorage;
    private UserService userService;
    private FilmService filmService;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        userStorage = new InMemoryUserStorage();
        filmStorage = new InMemoryFilmStorage();
        userService = new UserService(userStorage);
        filmService = new FilmService(filmStorage, userStorage);
    }

    private User validUser() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Nice film");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    private User persistUser(String login) {
        User user = validUser();
        user.setLogin(login);
        user.setEmail(login + "@yandex.ru");
        user.setName(login);
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
    void validUserCreateHasNoViolations() {
        assertThat(validator.validate(validUser(), Marker.OnCreate.class, Default.class)).isEmpty();
    }

    @Test
    void userCreateWithNullIdHasNoViolations() {
        User user = validUser();
        user.setId(null);

        assertThat(validator.validate(user, Marker.OnCreate.class, Default.class)).isEmpty();
    }

    @Test
    void userCreateWithNonNullIdHasViolation() {
        User user = validUser();
        user.setId(10L);

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Id не задается пользователем");
    }

    @Test
    void userCreateWithBlankEmailHasViolation() {
        User user = validUser();
        user.setEmail("   ");

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Email не должен быть пустым или состоять из пробелов");
    }

    @Test
    void userCreateWithInvalidEmailHasViolation() {
        User user = validUser();
        user.setEmail("not-an-email");

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Некорректный email");
    }

    @Test
    void userCreateWithBlankLoginHasViolation() {
        User user = validUser();
        user.setLogin(" ");

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Login не должен быть пустым или состоять из пробелов");
    }

    @Test
    void userUpdateWithValidDataHasNoViolations() {
        User user = validUser();
        user.setId(1L);

        assertThat(validator.validate(user, Marker.OnUpdate.class, Default.class)).isEmpty();
    }

    @Test
    void userUpdateWithNullIdHasViolation() {
        User user = validUser();
        user.setId(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnUpdate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Должен передаваться id");
    }

    @Test
    void filmCreateWithValidDataHasNoViolations() {
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
    void userCreateWithFutureBirthdayHasViolation() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("День рождения не может быть в будущем");
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
    void addUserSetsIdAndDefaultName() {
        User user = validUser();
        user.setName("   ");

        User created = userStorage.addUser(user);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getName()).isEqualTo(created.getLogin());
    }

    @Test
    void addSecondUserIncrementsId() {
        User first = userStorage.addUser(validUser());
        User second = validUser();
        second.setLogin("second");
        second.setEmail("second@yandex.ru");

        User createdSecond = userStorage.addUser(second);

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(createdSecond.getId()).isEqualTo(2L);
    }

    @Test
    void addUserWithSpacesInLoginThrowsValidateException() {
        User user = validUser();
        user.setLogin("bad login");

        assertThatThrownBy(() -> userStorage.addUser(user))
                .isInstanceOf(ValidateException.class)
                .hasMessage("Логин не может содержать пробелы");
    }

    @Test
    void updateUserChangesExistingFields() {
        User created = userStorage.addUser(validUser());

        User update = new User();
        update.setId(created.getId());
        update.setEmail("new@yandex.ru");
        update.setLogin("newlogin");
        update.setBirthday(LocalDate.of(1991, 2, 2));
        update.setName("New Name");

        User updated = userStorage.updateUser(update);

        assertThat(updated.getEmail()).isEqualTo("new@yandex.ru");
        assertThat(updated.getLogin()).isEqualTo("newlogin");
        assertThat(updated.getBirthday()).isEqualTo(LocalDate.of(1991, 2, 2));
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    void updateUserWithNullIdThrowsValidateException() {
        User update = validUser();
        update.setId(null);

        assertThatThrownBy(() -> userStorage.updateUser(update))
                .isInstanceOf(ValidateException.class)
                .hasMessage("Id пользователя обязателен");
    }

    @Test
    void updateUserWithUnknownIdThrowsNotFoundException() {
        User update = validUser();
        update.setId(999L);

        assertThatThrownBy(() -> userStorage.updateUser(update))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User с id 999");
    }

    @Test
    void getUserByIdReturnsStoredUser() {
        User created = userStorage.addUser(validUser());

        assertThat(userStorage.getUserById(created.getId())).isSameAs(created);
    }

    @Test
    void getUserByIdMissingThrowsNotFoundException() {
        assertThatThrownBy(() -> userStorage.getUserById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователя с id 999");
    }

    @Test
    void getNextUserIdWorks() {
        assertThat(userStorage.getNextId()).isEqualTo(1L);

        userStorage.addUser(validUser());

        assertThat(userStorage.getNextId()).isEqualTo(2L);
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
    void getFilmByIdReturnsStoredFilm() {
        Film created = filmStorage.addFilm(validFilm());

        assertThat(filmStorage.getFilmById(created.getId())).isSameAs(created);
    }

    @Test
    void getFilmByIdMissingThrowsNotFoundException() {
        assertThatThrownBy(() -> filmStorage.getFilmById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id: 999");
    }

    @Test
    void getNextFilmIdWorks() {
        assertThat(filmStorage.getNextId()).isEqualTo(1L);

        filmStorage.addFilm(validFilm());

        assertThat(filmStorage.getNextId()).isEqualTo(2L);
    }

    @Test
    void addFriendCreatesMutualLink() {
        User user = userStorage.addUser(validUser());
        User friend = persistUser("friend");

        userService.addFriend(user.getId(), friend.getId());

        assertThat(user.getFriends()).contains(friend.getId());
        assertThat(friend.getFriends()).contains(user.getId());
    }

    @Test
    void deleteFriendRemovesMutualLink() {
        User user = userStorage.addUser(validUser());
        User friend = persistUser("friend");

        userService.addFriend(user.getId(), friend.getId());
        userService.deleteFriend(user.getId(), friend.getId());

        assertThat(user.getFriends()).doesNotContain(friend.getId());
        assertThat(friend.getFriends()).doesNotContain(user.getId());
    }

    @Test
    void getAllFriendsReturnsStoredUsers() {
        User user = userStorage.addUser(validUser());
        User friend1 = persistUser("friend1");
        User friend2 = persistUser("friend2");

        userService.addFriend(user.getId(), friend1.getId());
        userService.addFriend(user.getId(), friend2.getId());

        List<User> friends = userService.getAllFriends(user.getId());

        assertThat(friends).extracting(User::getId)
                .containsExactlyInAnyOrder(friend1.getId(), friend2.getId());
    }

    @Test
    void getAllCommonFriendsReturnsIntersection() {
        User user1 = userStorage.addUser(validUser());
        User user2 = persistUser("user2");
        User common = persistUser("common");
        User other = persistUser("other");

        userService.addFriend(user1.getId(), common.getId());
        userService.addFriend(user1.getId(), other.getId());
        userService.addFriend(user2.getId(), common.getId());

        List<User> commonFriends = userService.getAllCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends).extracting(User::getId)
                .containsExactly(common.getId());
    }

    @Test
    void addFriendWithMissingUserThrowsNotFoundException() {
        User friend = userStorage.addUser(validUser());

        assertThatThrownBy(() -> userService.addFriend(999L, friend.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователя с id 999");
    }

    @Test
    void addFriendWithMissingFriendThrowsNotFoundException() {
        User user = userStorage.addUser(validUser());

        assertThatThrownBy(() -> userService.addFriend(user.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователя с id 999");
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
        User user = userStorage.addUser(validUser());

        assertThatThrownBy(() -> filmService.addLike(999L, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id: 999");
    }

    @Test
    void addLikeWithMissingUserThrowsNotFoundException() {
        Film film = filmStorage.addFilm(validFilm());

        assertThatThrownBy(() -> filmService.addLike(film.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователя с id 999");
    }
}
