package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

@Data
public class Genre {
    @NotNull(groups = {Marker.OnCreate.class, Marker.OnUpdate.class}, message = "id жанра должен быть указан")
    private Integer id;
    private String name;
}
