
package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

import java.time.LocalDate;

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
            groups = {
                    Marker.OnCreate.class,
                    Marker.OnUpdate.class
            },
            message = "Некорректный email"
    )
    @NotBlank(
            groups = Marker.OnCreate.class,
            message = "Email не должен быть пустым или состоять из пробелов"
    )
    private String email;

    @NotBlank(
            groups = Marker.OnCreate.class,
            message = "Login не должен быть пустым или состоять из пробелов"
    )
    @Pattern(
            regexp = "\\S+",
            groups = Marker.OnUpdate.class,
            message = "Login не должен содержать пробелы"
    )
    private String login;

    private String name;

    @NotNull(
            groups = Marker.OnCreate.class,
            message = "Должен передаваться birthday"
    )
    @PastOrPresent(
            groups = {
                    Marker.OnCreate.class,
                    Marker.OnUpdate.class
            },
            message = "День рождения не может быть в будущем"
    )
    private LocalDate birthday;
}
