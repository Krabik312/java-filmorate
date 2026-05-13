package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;


    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") @Positive Long userId) {
        return userService.getUserById(userId);
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    @Validated({Marker.OnCreate.class, Default.class})
    public User addUser(@Valid @RequestBody User user) {
        return userService.addUser(user);
    }

    @PutMapping
    @Validated({Marker.OnUpdate.class, Default.class})
    public User updateUser(@Valid @RequestBody User user) {
        return userService.updateUser(user);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable("id") @Positive Long userId, @PathVariable @Positive Long friendId) {
        userService.addFriend(userId, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void deleteFriend(@PathVariable("id") @Positive Long userId, @PathVariable @Positive Long friendId) {
        userService.deleteFriend(userId, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getAllFriends(@PathVariable("id") @Positive Long userId) {
        return userService.getAllFriends(userId);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getAllCommonFriends(@PathVariable("id") @Positive Long userId,
                                          @PathVariable("otherId") @Positive Long otherUserId) {
        return userService.getAllCommonFriends(userId, otherUserId);
    }

}