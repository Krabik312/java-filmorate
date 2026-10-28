
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        FriendshipDbStorage.class,
        UserDbStorage.class,
        UserRowMapper.class
})
class FriendshipDbStorageIntegrationTest {

    @Autowired
    private FriendshipDbStorage friendshipStorage;

    @Autowired
    private UserDbStorage userStorage;

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        return userStorage.addUser(user);
    }

    @Test
    void addFriendCreatesOneDirectionalFriendship() {
        User alice = createUser("alice");
        User bob = createUser("bob");

        friendshipStorage.addFriend(alice.getId(), bob.getId());

        assertThat(friendshipStorage.getFriends(alice.getId()))
                .extracting(User::getId)
                .containsExactly(bob.getId());

        assertThat(friendshipStorage.getFriends(bob.getId()))
                .isEmpty();
    }

    @Test
    void addFriendDoesNotCreateDuplicates() {
        User alice = createUser("alice");
        User bob = createUser("bob");

        friendshipStorage.addFriend(alice.getId(), bob.getId());
        friendshipStorage.addFriend(alice.getId(), bob.getId());

        assertThat(friendshipStorage.getFriends(alice.getId()))
                .hasSize(1);
    }

    @Test
    void deleteFriendRemovesSpecifiedDirectionOnly() {
        User alice = createUser("alice");
        User bob = createUser("bob");

        friendshipStorage.addFriend(alice.getId(), bob.getId());
        friendshipStorage.addFriend(bob.getId(), alice.getId());

        friendshipStorage.deleteFriend(alice.getId(), bob.getId());

        assertThat(friendshipStorage.getFriends(alice.getId()))
                .isEmpty();

        assertThat(friendshipStorage.getFriends(bob.getId()))
                .extracting(User::getId)
                .containsExactly(alice.getId());
    }

    @Test
    void deleteNonexistentFriendshipDoesNotFail() {
        User alice = createUser("alice");
        User bob = createUser("bob");

        assertThatCode(
                () -> friendshipStorage.deleteFriend(
                        alice.getId(),
                        bob.getId()
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void getFriendsReturnsUsersInIdOrder() {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User charlie = createUser("charlie");

        friendshipStorage.addFriend(alice.getId(), charlie.getId());
        friendshipStorage.addFriend(alice.getId(), bob.getId());

        assertThat(friendshipStorage.getFriends(alice.getId()))
                .extracting(User::getId)
                .containsExactly(bob.getId(), charlie.getId());
    }

    @Test
    void getFriendsReturnsEmptyForUserWithoutFriends() {
        User alice = createUser("alice");

        assertThat(friendshipStorage.getFriends(alice.getId()))
                .isEmpty();
    }

    @Test
    void getCommonFriendsReturnsOnlyIntersection() {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User common = createUser("common");
        User onlyAlice = createUser("onlyalice");

        friendshipStorage.addFriend(alice.getId(), common.getId());
        friendshipStorage.addFriend(alice.getId(), onlyAlice.getId());
        friendshipStorage.addFriend(bob.getId(), common.getId());

        assertThat(
                friendshipStorage.getCommonFriends(
                        alice.getId(),
                        bob.getId()
                )
        ).extracting(User::getId)
                .containsExactly(common.getId());
    }

    @Test
    void getCommonFriendsReturnsEmptyWhenNoneExist() {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User charlie = createUser("charlie");

        friendshipStorage.addFriend(alice.getId(), charlie.getId());

        assertThat(
                friendshipStorage.getCommonFriends(
                        alice.getId(),
                        bob.getId()
                )
        ).isEmpty();
    }
}
