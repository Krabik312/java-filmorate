
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        UserRowMapper.class,
        FriendshipDbStorage.class
})
class UserDbStorageIntegrationTest {

    @Autowired
    private UserDbStorage users;

    @Autowired
    private FriendshipDbStorage friendships;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users, friendships);
    }

    private User newUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    @Test
    void addUserGeneratesIdAndDefaultName() {
        User created = users.addUser(newUser("first"));
        User second = users.addUser(newUser("second"));

        assertThat(created.getId()).isPositive();
        assertThat(second.getId()).isGreaterThan(created.getId());

        assertThat(users.getUserById(created.getId()))
                .isPresent()
                .get()
                .extracting(User::getName)
                .isEqualTo("first");
    }

    @Test
    void getAllUsersReturnsCreatedUsers() {
        users.addUser(newUser("alpha"));
        users.addUser(newUser("beta"));

        assertThat(users.getAllUsers())
                .extracting(User::getLogin)
                .containsExactly("alpha", "beta");
    }

    @Test
    void updateUserChangesFields() {
        User created = users.addUser(newUser("initial"));

        User changes = new User();
        changes.setId(created.getId());
        changes.setEmail("edited@example.com");
        changes.setName("Edited name");

        User updated = users.updateUser(changes);

        assertThat(updated.getEmail())
                .isEqualTo("edited@example.com");

        assertThat(updated.getName())
                .isEqualTo("Edited name");

        assertThat(updated.getLogin())
                .isEqualTo("initial");
    }

    @Test
    void unknownUserReturnsEmpty() {
        assertThat(users.getUserById(999L)).isEmpty();
    }

    @Test
    void updateUnknownUserThrowsException() {
        User changes = newUser("missing");
        changes.setId(999L);

        assertThatThrownBy(() -> users.updateUser(changes))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void loginWithWhitespaceIsRejected() {
        assertThatThrownBy(
                () -> users.addUser(newUser("bad login"))
        ).isInstanceOf(ValidateException.class);
    }

    @Test
    void friendshipIsOneDirectional() {
        User a = users.addUser(newUser("alice"));
        User b = users.addUser(newUser("bob"));

        service.addFriend(a.getId(), b.getId());

        assertThat(service.getAllFriends(a.getId()))
                .extracting(User::getId)
                .containsExactly(b.getId());

        assertThat(service.getAllFriends(b.getId()))
                .isEmpty();
    }

    @Test
    void duplicateFriendshipDoesNotCreateDuplicates() {
        User a = users.addUser(newUser("alice"));
        User b = users.addUser(newUser("bob"));

        service.addFriend(a.getId(), b.getId());
        service.addFriend(a.getId(), b.getId());

        assertThat(service.getAllFriends(a.getId()))
                .hasSize(1);
    }

    @Test
    void deleteFriendOnlyRemovesForwardLink() {
        User a = users.addUser(newUser("alice"));
        User b = users.addUser(newUser("bob"));

        service.addFriend(a.getId(), b.getId());
        service.addFriend(b.getId(), a.getId());

        service.deleteFriend(a.getId(), b.getId());

        assertThat(service.getAllFriends(a.getId()))
                .isEmpty();

        assertThat(service.getAllFriends(b.getId()))
                .extracting(User::getId)
                .containsExactly(a.getId());
    }

    @Test
    void commonFriendsReturnsIntersection() {
        User a = users.addUser(newUser("alice"));
        User b = users.addUser(newUser("bob"));
        User common = users.addUser(newUser("common"));
        User other = users.addUser(newUser("other"));

        service.addFriend(a.getId(), common.getId());
        service.addFriend(a.getId(), other.getId());
        service.addFriend(b.getId(), common.getId());

        assertThat(
                service.getAllCommonFriends(a.getId(), b.getId())
        ).extracting(User::getId)
                .containsExactly(common.getId());
    }

    @Test
    void selfFriendshipIsRejected() {
        User user = users.addUser(newUser("alice"));

        assertThatThrownBy(
                () -> service.addFriend(user.getId(), user.getId())
        ).isInstanceOf(ValidateException.class);
    }

    @Test
    void missingFriendIsRejected() {
        User user = users.addUser(newUser("alice"));

        assertThatThrownBy(
                () -> service.addFriend(user.getId(), 999L)
        ).isInstanceOf(NotFoundException.class);
    }
}
