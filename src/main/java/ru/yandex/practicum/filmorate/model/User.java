package ru.yandex.practicum.filmorate.model;


import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    @Null(groups = Marker.OnCreate.class, message = "Id не задается пользователем")
    @NotNull(groups = Marker.OnUpdate.class, message = "Должен передаваться id")
    private Long id;
    @Email(message = "Некорректный email")
    @NotBlank(groups = Marker.OnCreate.class, message = "Email не должен быть пустым или состоять из пробелов")
    private String email;
    @NotNull(groups = Marker.OnCreate.class, message = "Должен передаваться login")
    @NotBlank(message = "Login не должен быть пустым или состоять из пробелов")
    private String login;
    private String name;
    @NotNull(groups = Marker.OnCreate.class, message = "Должен передаваться birthday")
    @PastOrPresent(message = "День рождения не может быть в будущем")
    private LocalDate birthday;
    private Set<Long> friends = new HashSet<>();

    public void addFriend(Long id) {
        friends.add(id);
    }

    public void deleteFriend(Long id) {
        friends.remove(id);
    }
}
