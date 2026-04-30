package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.*;
import ru.yandex.practicum.filmorate.controller.*;
import ru.yandex.practicum.filmorate.exceptions.*;
import ru.yandex.practicum.filmorate.model.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmorateApplicationTests {

    private UserController userController;
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        filmController = new FilmController();
    }


    @Nested
    class UserTests {

        private User createValidUser() {
            User user = new User();
            user.setEmail("test@mail.com");
            user.setLogin("login");
            user.setName("name");
            user.setBirthday(LocalDate.of(2000, 1, 1));
            return user;
        }


        @Test
        void shouldThrow_whenEmailWithoutAt() {
            User user = createValidUser();
            user.setEmail("invalid");

            assertThrows(ValidateException.class,
                    () -> userController.isUserValid(user));
        }

        @Test
        void shouldThrow_whenEmailNull() {
            User user = createValidUser();
            user.setEmail(null);

            assertThrows(NullPointerException.class,
                    () -> userController.isUserValid(user));
        }

        @Test
        void shouldThrow_whenLoginBlank() {
            User user = createValidUser();
            user.setLogin(" ");

            assertThrows(ValidateException.class,
                    () -> userController.isUserValid(user));
        }

        @Test
        void shouldThrow_whenLoginContainsSpaces() {
            User user = createValidUser();
            user.setLogin("log in");

            assertThrows(ValidateException.class,
                    () -> userController.isUserValid(user));
        }

        @Test
        void shouldThrow_whenBirthdayInFuture() {
            User user = createValidUser();
            user.setBirthday(LocalDate.now().plusDays(1));

            assertThrows(ValidateException.class,
                    () -> userController.isUserValid(user));
        }

        @Test
        void shouldPass_whenBirthdayToday() {
            User user = createValidUser();
            user.setBirthday(LocalDate.now());

            assertDoesNotThrow(() -> userController.isUserValid(user));
        }

        @Test
        void shouldAddUser_andGenerateId() {
            User user = createValidUser();

            User saved = userController.addUser(user);

            assertNotNull(saved.getId());
            assertEquals(1, userController.getAllUsers().size());
        }

        @Test
        void shouldSetNameFromLogin_whenNameBlank() {
            User user = createValidUser();
            user.setName(" ");

            User saved = userController.addUser(user);

            assertEquals("login", saved.getName());
        }

        @Test
        void shouldIncrementIds() {
            userController.addUser(createValidUser());
            userController.addUser(createValidUser());

            assertEquals(2, userController.getAllUsers().size());
        }

        @Test
        void shouldStoreCorrectUserData() {
            User user = createValidUser();

            userController.addUser(user);

            User stored = userController.getAllUsers().iterator().next();

            assertEquals("login", stored.getLogin());
        }

        @Test
        void shouldUpdateUserName() {
            User saved = userController.addUser(createValidUser());

            saved.setName("updated");

            userController.updateUser(saved);

            User updated = userController.getAllUsers().iterator().next();

            assertEquals("updated", updated.getName());
        }

        @Test
        void shouldThrow_whenUpdateNonExistingUser() {
            User user = createValidUser();
            user.setId(999L);

            assertThrows(NotFoundException.class,
                    () -> userController.updateUser(user));
        }

        @Test
        void shouldNotChangeIdOnUpdate() {
            User saved = userController.addUser(createValidUser());
            Long id = saved.getId();

            saved.setName("updated");
            userController.updateUser(saved);

            User updated = userController.getAllUsers().iterator().next();

            assertEquals(id, updated.getId());
        }
    }

    @Nested
    class FilmTests {

        private Film createValidFilm() {
            Film film = new Film();
            film.setName("Film");
            film.setDescription("Description");
            film.setReleaseDate(LocalDate.of(2000, 1, 1));
            film.setDuration(100);
            return film;
        }

        @Test
        void shouldThrow_whenNameBlank() {
            Film film = createValidFilm();
            film.setName(" ");

            assertThrows(ValidateException.class,
                    () -> filmController.isValidFilm(film));
        }

        @Test
        void shouldThrow_whenDescriptionTooLong() {
            Film film = createValidFilm();
            film.setDescription("a".repeat(201));

            assertThrows(ValidateException.class,
                    () -> filmController.isValidFilm(film));
        }

        @Test
        void shouldPass_whenDescriptionExactly200() {
            Film film = createValidFilm();
            film.setDescription("a".repeat(200));

            assertDoesNotThrow(() -> filmController.isValidFilm(film));
        }

        @Test
        void shouldThrow_whenReleaseDateTooEarly() {
            Film film = createValidFilm();
            film.setReleaseDate(LocalDate.of(1800, 1, 1));

            assertThrows(ValidateException.class,
                    () -> filmController.isValidFilm(film));
        }

        @Test
        void shouldPass_whenReleaseDateBoundary() {
            Film film = createValidFilm();
            film.setReleaseDate(LocalDate.of(1895, 12, 28));

            assertDoesNotThrow(() -> filmController.isValidFilm(film));
        }

        @Test
        void shouldThrow_whenDurationZero() {
            Film film = createValidFilm();
            film.setDuration(0);

            assertThrows(ValidateException.class,
                    () -> filmController.isValidFilm(film));
        }

        @Test
        void shouldPass_whenDurationOne() {
            Film film = createValidFilm();
            film.setDuration(1);

            assertDoesNotThrow(() -> filmController.isValidFilm(film));
        }

        @Test
        void shouldAddFilm_andGenerateId() {
            Film film = createValidFilm();

            Film saved = filmController.addFilm(film);

            assertNotNull(saved.getId());
            assertEquals(1, filmController.getAllFilms().size());
        }

        @Test
        void shouldStoreFilmCorrectly() {
            Film film = createValidFilm();

            filmController.addFilm(film);

            Film stored = filmController.getAllFilms().iterator().next();

            assertEquals("Film", stored.getName());
        }

        @Test
        void shouldIncrementFilmIds() {
            filmController.addFilm(createValidFilm());
            filmController.addFilm(createValidFilm());

            assertEquals(2, filmController.getAllFilms().size());
        }

        @Test
        void shouldUpdateFilmName() {
            Film saved = filmController.addFilm(createValidFilm());

            saved.setName("Updated");

            filmController.updateFilm(saved);

            Film updated = filmController.getAllFilms().iterator().next();

            assertEquals("Updated", updated.getName());
        }

        @Test
        void shouldThrow_whenUpdateNonExistingFilm() {
            Film film = createValidFilm();
            film.setId(999L);

            assertThrows(NotFoundException.class,
                    () -> filmController.updateFilm(film));
        }
    }
}