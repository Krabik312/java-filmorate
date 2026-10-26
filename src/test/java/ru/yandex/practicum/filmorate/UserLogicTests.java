package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.marker.Marker;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserLogicTests {

    private Validator validator;
    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        userStorage = new InMemoryUserStorage();
    }

    private User validUser() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
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
    void userCreateWithFutureBirthdayHasViolation() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user, Marker.OnCreate.class, Default.class);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("День рождения не может быть в будущем");
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
    void getUserByIdReturnsStoredUserAsOptional() {
        User created = userStorage.addUser(validUser());

        assertThat(userStorage.getUserById(created.getId())).isPresent();
        assertThat(userStorage.getUserById(created.getId()).get()).isSameAs(created);
    }

    @Test
    void getUserByIdMissingReturnsEmptyOptional() {
        assertThat(userStorage.getUserById(999L)).isEmpty();
    }


}
