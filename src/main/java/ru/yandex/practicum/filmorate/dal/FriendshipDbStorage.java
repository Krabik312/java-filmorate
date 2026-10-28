
package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

@Repository
public class FriendshipDbStorage {

    private static final String ADD_FRIEND = """
            MERGE INTO friendship (
                requester_id,
                addressee_id,
                status
            )
            KEY (requester_id, addressee_id)
            VALUES (?, ?, 'UNCONFIRMED')
            """;

    private static final String DELETE_FRIEND = """
            DELETE FROM friendship
            WHERE requester_id = ? AND addressee_id = ?
            """;

    private static final String FIND_FRIENDS = """
            SELECT u.id, u.email, u.login, u.name, u.birthday
            FROM friendship AS fr
            JOIN users AS u ON u.id = fr.addressee_id
            WHERE fr.requester_id = ?
            ORDER BY u.id
            """;

    private static final String FIND_COMMON_FRIENDS = """
            SELECT u.id, u.email, u.login, u.name, u.birthday
            FROM friendship AS first_fr
            JOIN friendship AS second_fr
                ON first_fr.addressee_id = second_fr.addressee_id
            JOIN users AS u ON u.id = first_fr.addressee_id
            WHERE first_fr.requester_id = ?
              AND second_fr.requester_id = ?
            ORDER BY u.id
            """;

    private final JdbcTemplate jdbc;
    private final RowMapper<User> userMapper;

    public FriendshipDbStorage(
            JdbcTemplate jdbc,
            RowMapper<User> userMapper
    ) {
        this.jdbc = jdbc;
        this.userMapper = userMapper;
    }

    public void addFriend(Long userId, Long friendId) {
        jdbc.update(ADD_FRIEND, userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        jdbc.update(DELETE_FRIEND, userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        return jdbc.query(FIND_FRIENDS, userMapper, userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        return jdbc.query(
                FIND_COMMON_FRIENDS,
                userMapper,
                userId,
                otherUserId
        );
    }
}
