package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public Collection<User> getAllUsers() {
        log.trace("Отправка всех пользователей");
        return users.values();
    }

    @Override
    public User addUser(User user) {
        log.trace("Добавление пользователя");

        isLoginValid(user);

        setDefaultNicknameIfAbsent(user);
        user.setId(getNextId());
        users.put(user.getId(), user);

        log.trace("Возвращаем пользователя");
        return user;
    }

    @Override
    public User updateUser(User user) {
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
            updateUser.setEmail(user.getEmail());
        }

        if (user.getLogin() != null) {
            updateUser.setLogin(user.getLogin());
        }

        if (user.getBirthday() != null) {
            updateUser.setBirthday(user.getBirthday());
        }

        if (user.getName() != null && !user.getName().isBlank()) {
            updateUser.setName(user.getName());
        }

        return updateUser;
    }

    @Override
    public User getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователя с id " + id + " не существует");
        }
        return user;
    }

    private void setDefaultNicknameIfAbsent(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private void isLoginValid(User user) {
        if (user.getLogin().contains(" ")) {
            log.error("Ошибка валидации: некорректный логин {}", user.getLogin());
            throw new ValidateException("Логин не может содержать пробелы");
        }
    }


    @Override
    public Long getNextId() {
        log.trace("Генерация нового id");
        Long currentId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        Long nextId = currentId + 1;
        log.debug("Сгенерирован пользователь с id {}", nextId);
        return nextId;
    }
}
