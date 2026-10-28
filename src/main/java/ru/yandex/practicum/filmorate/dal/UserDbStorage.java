
package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Optional;

@Repository
public class UserDbStorage implements UserStorage {

    private static final String FIND_ALL_USERS = """
            SELECT id, email, login, name, birthday
            FROM users
            ORDER BY id
            """;

    private static final String FIND_USER_BY_ID = """
            SELECT id, email, login, name, birthday
            FROM users
            WHERE id = ?
            """;

    private static final String INSERT_USER = """
            INSERT INTO users (email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """;

    private static final String UPDATE_USER = """
            UPDATE users
            SET email = ?, login = ?, name = ?, birthday = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbc;
    private final RowMapper<User> userMapper;

    public UserDbStorage(JdbcTemplate jdbc, RowMapper<User> userMapper) {
        this.jdbc = jdbc;
        this.userMapper = userMapper;
    }

    @Override
    public Collection<User> getAllUsers() {
        return jdbc.query(FIND_ALL_USERS, userMapper);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(FIND_USER_BY_ID, userMapper, id)
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User addUser(User user) {
        validateLogin(user.getLogin());

        String name = user.getName() == null || user.getName().isBlank()
                ? user.getLogin()
                : user.getName();

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    INSERT_USER,
                    new String[]{"id"}
            );

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, name);
            ps.setDate(4, java.sql.Date.valueOf(user.getBirthday()));

            return ps;
        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                    "Не удалось получить ID созданного пользователя"
            );
        }

        user.setId(id.longValue());
        user.setName(name);

        return getUserById(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Созданный пользователь не найден"
                ));
    }

    @Override
    public User updateUser(User changes) {
        if (changes.getId() == null) {
            throw new ValidateException("Id пользователя обязателен");
        }

        User current = getUserById(changes.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + changes.getId() + " не найден"
                ));

        if (changes.getEmail() != null) {
            current.setEmail(changes.getEmail());
        }

        if (changes.getLogin() != null) {
            validateLogin(changes.getLogin());
            current.setLogin(changes.getLogin());
        }

        if (changes.getBirthday() != null) {
            current.setBirthday(changes.getBirthday());
        }

        if (changes.getName() != null && !changes.getName().isBlank()) {
            current.setName(changes.getName());
        }

        int updated = jdbc.update(
                UPDATE_USER,
                current.getEmail(),
                current.getLogin(),
                current.getName(),
                current.getBirthday(),
                current.getId()
        );

        if (updated == 0) {
            throw new NotFoundException(
                    "Пользователь с id " + changes.getId() + " не найден"
            );
        }

        return current;
    }

    private void validateLogin(String login) {
        if (login == null || login.isBlank()
                || login.chars().anyMatch(Character::isWhitespace)) {
            throw new ValidateException(
                    "Логин не может быть пустым или содержать пробелы"
            );
        }
    }
}
