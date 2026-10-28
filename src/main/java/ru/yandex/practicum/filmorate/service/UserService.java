
package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
public class UserService {

    private final UserStorage userStorage;
    private final FriendshipDbStorage friendshipStorage;

    public UserService(
            @Qualifier("userDbStorage") UserStorage userStorage,
            FriendshipDbStorage friendshipStorage
    ) {
        this.userStorage = userStorage;
        this.friendshipStorage = friendshipStorage;
    }

    public User getUserById(Long userId) {
        return getUserOrThrow(userId);
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User addUser(User user) {
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        return userStorage.updateUser(user);
    }

    public void addFriend(Long userId, Long friendId) {
        getUserOrThrow(userId);
        getUserOrThrow(friendId);

        if (userId.equals(friendId)) {
            throw new ValidateException(
                    "Нельзя добавить самого себя в друзья"
            );
        }

        friendshipStorage.addFriend(userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        getUserOrThrow(userId);
        getUserOrThrow(friendId);

        friendshipStorage.deleteFriend(userId, friendId);
    }

    public List<User> getAllFriends(Long userId) {
        getUserOrThrow(userId);

        return friendshipStorage.getFriends(userId);
    }

    public List<User> getAllCommonFriends(
            Long userId,
            Long otherUserId
    ) {
        getUserOrThrow(userId);
        getUserOrThrow(otherUserId);

        return friendshipStorage.getCommonFriends(
                userId,
                otherUserId
        );
    }

    private User getUserOrThrow(Long id) {
        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id: " + id + " не найден"
                ));
    }
}
