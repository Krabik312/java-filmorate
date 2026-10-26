package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.marker.Marker;

@Data
public class Friendship {
    @NotNull(groups = {Marker.OnCreate.class, Marker.OnUpdate.class}, message = "id отправляющего запрос на дружбу должен быть указан")
    private Long requesterId;
    @NotNull(groups = {Marker.OnCreate.class, Marker.OnUpdate.class}, message = "id получающего запрос на дружбу должен быть указан")
    private Long addresseeId;
    @NotNull(groups = {Marker.OnCreate.class, Marker.OnUpdate.class}, message = "статус запроса на дружбу должен быть указан")
    private FriendshipStatus status;
}
