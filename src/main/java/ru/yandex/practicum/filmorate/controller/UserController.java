package ru.yandex.practicum.filmorate.controller;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ch.qos.logback.classic.Logger;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;

@RestController
@RequestMapping("/users")
public class UserController {

    HashMap<Long, User> users = new HashMap<>();
    private static final Logger log = (Logger) LoggerFactory.getLogger(UserController.class);


    @GetMapping
    public Collection<User> getAllUsers() {
        log.trace("Отправка всех пользователей");
        return users.values();
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        log.trace("Добавление пользователей");
        if (isUserValid(user)) {
            log.debug("Пользователь прошел валидацию {}", user.toString());
            log.trace("Проверка имени пользователя");
            if (user.getName() == null || user.getName().isBlank()) {
                log.debug("Устанавливаем вместо имени логин {}", user.toString());
                user.setName(user.getLogin());
            }
            log.trace("Устанавливаем id");
            user.setId(getNextId());
            log.trace("Добавляем пользователя в хранилище");
            users.put(user.getId(), user);
        }
        log.trace("Возвращаем пользователя");
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        log.trace("Обновление пользователя");
        if (users.containsKey(user.getId())) {
            log.debug("Пользователь найден для обновления {}", user.toString());
            log.trace("Проверка имени пользователя");
            if (user.getName() == null || user.getName().isBlank()) {
                log.debug("Имя пустое, устанавливаем логин {}", user.toString());
                user.setName(user.getLogin());
            }
            log.trace("Обновление пользователя в хранилище");
            users.put(user.getId(), user);
        } else {
            log.error("Попытка обновить несуществующего пользователя с id {}", user.getId());
            throw new NotFoundException("User с id " + user.getId() + " не существует");
        }
        log.trace("Возвращаем обновленного пользователя");
        return user;
    }


    public boolean isUserValid(User user) {
        log.trace("Валидация пользователя {}", user);
        if (!user.getEmail().contains("@")) {
            log.error("Ошибка валидации: email без @ - {}", user.getEmail());
            throw new ValidateException("В почте должен присутствовать знак @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.error("Ошибка валидации: некорректный логин {}", user.getLogin());
            throw new ValidateException("Логин не может быть пустым или содержать пробелы");
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации: дата рождения в будущем: {}", user.getBirthday());
            throw new ValidateException("Дата рождения не может быть в будущем");
        }
        log.debug("Пользователь прошел валидацию {}", user);
        return true;
    }

    public Long getNextId() {
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
