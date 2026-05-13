package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Data
public class Film {

    @Null(groups = Marker.OnCreate.class, message = "Id не задается пользователем")
    @NotNull(groups = Marker.OnUpdate.class, message = "Должен быть id")
    private Long id;
    @NotBlank(message = "Имя не может быть null или пустым")
    private String name;
    @Size(max = 200, message = "Максимальная длина описания 200 символов")
    @NotNull(groups = Marker.OnCreate.class, message = "Описание не должно быть пустым")
    private String description;
    @NotNull(groups = Marker.OnCreate.class, message = "Дата выхода должна быть указана")
    private LocalDate releaseDate;
    @Min(value = 1, message = "Продолжительность фильма не может быть меньше 1й минуты")
    @NotNull(groups = Marker.OnCreate.class, message = "Длительность должна быть указана")
    private Integer duration;
    private Set<Long> likes = new HashSet<>();

    public void addLike(Long userId) {
        likes.add(userId);
    }

    public void deleteLike(Long userId) {
        likes.remove(userId);
    }
}
