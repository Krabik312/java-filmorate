package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;


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
        User user = getUserOrThrow(userId);
        User friendUser = getUserOrThrow(friendId);

        user.addFriend(friendUser.getId());
        friendUser.addFriend(user.getId());

    }

    public void deleteFriend(Long userId, Long friendId) {
        User user = getUserOrThrow(userId);
        User friendUser = getUserOrThrow(friendId);

        user.deleteFriend(friendUser.getId());
        friendUser.deleteFriend(user.getId());

    }

    public List<User> getAllFriends(Long userId) {
        User user = getUserOrThrow(userId);
        return user.getFriends().stream()
                .map(this::getUserOrThrow)
                .toList();
    }

    public List<User> getAllCommonFriends(Long userId, Long otherUserId) {
        User user = getUserOrThrow(userId);
        User otherUser = getUserOrThrow(otherUserId);
        List<User> commonFriends = user.getFriends().stream()
                .filter(id -> otherUser.getFriends().contains(id))
                .map(this::getUserOrThrow)
                .toList();
        return commonFriends;
    }

    private User getUserOrThrow(Long id) {
        return userStorage.getUserById(id)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id: " + id + " не найден")
                );
    }
}
