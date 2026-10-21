package ru.yandex.practicum.filmorate.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {

    @Null(
            groups = Marker.OnCreate.class,
            message = "Id не задается пользователем"
    )
    @NotNull(
            groups = Marker.OnUpdate.class,
            message = "Должен передаваться id"
    )
    private Long id;

    @Email(
            groups = {Marker.OnCreate.class, Marker.OnUpdate.class},
            message = "Некорректный email"
    )
    @NotBlank(
            groups = Marker.OnCreate.class,
            message = "Email не должен быть пустым или состоять из пробелов"
    )
    private String email;

    @NotNull(
            groups = Marker.OnCreate.class,
            message = "Должен передаваться login"
    )
    @NotBlank(
            groups = {Marker.OnCreate.class, Marker.OnUpdate.class},
            message = "Login не должен быть пустым или состоять из пробелов"
    )
    private String login;

    private String name;

    @NotNull(
            groups = Marker.OnCreate.class,
            message = "Должен передаваться birthday"
    )
    @PastOrPresent(
            groups = {Marker.OnCreate.class, Marker.OnUpdate.class},
            message = "День рождения не может быть в будущем"
    )
    private LocalDate birthday;

    @Valid
    private Set<Friendship> friendships = new HashSet<>();

}