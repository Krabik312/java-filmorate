package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();

    private static final LocalDate FIRST_FILM = LocalDate.of(1895, 12, 28);
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @GetMapping
    public Collection<Film> getAllFilms() {
        log.trace("Отправка всех фильмов");
        return films.values();
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        log.trace("Добавление фильма");

        validateFilm(film);

        film.setId(getNextId());
        films.put(film.getId(), film);

        log.trace("Возвращаем фильм");
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        log.trace("Обновление фильма");

        if (film.getId() == null) {
            throw new ValidateException("Id фильма обязателен");
        }

        if (!films.containsKey(film.getId())) {
            log.error("Попытка обновить несуществующий фильм с id {}", film.getId());
            throw new NotFoundException("Фильма с id " + film.getId() + " не существует");
        }

        Film updateFilm = films.get(film.getId());

        if (film.getDuration() > 0) {
            updateFilm.setDuration(film.getDuration());
        } else {
            throw new ValidateException("Длительность не может быть меньше 1");
        }

        if (film.getName() != null) {
            if (film.getName().isBlank()) {
                throw new ValidateException("Название не может быть пустым");
            }
            updateFilm.setName(film.getName());
        }

        if (film.getDescription() != null) {
            if (film.getDescription().length() > 200) {
                throw new ValidateException("Описание не должно быть длиннее 200 символов");
            }
            updateFilm.setDescription(film.getDescription());
        }

        if (film.getReleaseDate() != null) {
            if (film.getReleaseDate().isBefore(FIRST_FILM)) {
                throw new ValidateException("Дата релиза не должна быть не раньше " + FIRST_FILM.format(dtf));
            }
            updateFilm.setReleaseDate(film.getReleaseDate());
        }

        return updateFilm;
    }

    private void validateFilm(Film film) {
        log.trace("Валидация фильма {}", film);

        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidateException("Название не может быть пустым");
        }

        if (film.getDescription() == null || film.getDescription().length() > 200) {
            throw new ValidateException("Описание должно быть не длиннее 200 символов");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FIRST_FILM)) {
            throw new ValidateException("Дата релиза не раньше " + FIRST_FILM.format(dtf));
        }

        if (film.getDuration() < 1) {
            throw new ValidateException("Продолжительность должна быть положительной");
        }
    }

    private Long getNextId() {
        log.trace("Генерация нового id для фильма");

        Long currentId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        Long nextId = currentId + 1;
        log.debug("Сгенерирован id {}", nextId);
        return nextId;
    }
}