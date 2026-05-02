package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> getAllUsers() {
        log.trace("Отправка всех пользователей");
        return users.values();
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        log.trace("Добавление пользователя");

        isBirthdayValid(user);
        isEmailValid(user);
        isLoginValid(user);

        setDefaultNicknameIfAbsent(user);
        user.setId(getNextId());
        users.put(user.getId(), user);

        log.trace("Возвращаем пользователя");
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        log.trace("Обновление пользователя");

        if (user.getId() == null) {
            throw new ValidateException("Id пользователя обязателен");
        }

        if (!users.containsKey(user.getId())) {
            log.error("Попытка обновить несуществующего пользователя с id {}", user.getId());
            throw new NotFoundException("User с id " + user.getId() + " не существует");
        }

        User updateUser = users.get(user.getId());

        if (user.getEmail() != null) {
            if (!user.getEmail().contains("@")) {
                throw new ValidateException("В почте должен присутствовать знак @");
            }
            updateUser.setEmail(user.getEmail());
        }

        if (user.getLogin() != null) {
            if (user.getLogin().isBlank() || user.getLogin().contains(" ")) {
                throw new ValidateException("Логин не может быть пустым или содержать пробелы");
            }
            updateUser.setLogin(user.getLogin());
        }

        if (user.getBirthday() != null) {
            if (user.getBirthday().isAfter(LocalDate.now())) {
                throw new ValidateException("Дата рождения не может быть в будущем");
            }
            updateUser.setBirthday(user.getBirthday());
        }

        if (user.getName() != null && !user.getName().isBlank()) {
            updateUser.setName(user.getName());
        }

        return updateUser;
    }

    private void setDefaultNicknameIfAbsent(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private void isEmailValid(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            log.error("Ошибка валидации: email без @ - {}", user.getEmail());
            throw new ValidateException("В почте должен присутствовать знак @");
        }
    }

    private void isLoginValid(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.error("Ошибка валидации: некорректный логин {}", user.getLogin());
            throw new ValidateException("Логин не может быть пустым или содержать пробелы");
        }
    }

    private void isBirthdayValid(User user) {
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации: дата рождения в будущем: {}", user.getBirthday());
            throw new ValidateException("Дата рождения не может быть в будущем");
        }
    }

    private Long getNextId() {
        log.trace("Генерация нового id");
        Long currentId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        Long nextId = currentId + 1;
        log.debug("Сгенерирован id {}", nextId);
        return nextId;
    }
}