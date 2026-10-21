package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

@Data
public class Mpa {
    @NotNull(groups = {Marker.OnCreate.class, Marker.OnUpdate.class}, message = "id рейтинга должно быть указано")
    private Integer id;
    private String name;
}
