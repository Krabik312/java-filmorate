package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

@RestController
@RequestMapping("/films")
public class FilmController {

    HashMap<Long, Film> films = new HashMap<>();

    private static final LocalDate FIRST_FILM = LocalDate.of(1895, 12, 28);
    public DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Logger log = (Logger) LoggerFactory.getLogger(FilmController.class);

    @GetMapping
    public Collection<Film> getAllFilms() {
        log.trace("Отправка всех фильмов");
        return films.values();
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        log.trace("Добавление фильма");
        if (isValidFilm(film)) {
            log.debug("Фильм прошел валидацию {}", film.toString());

            log.trace("Генерация id для фильма");
            film.setId(getNextId());

            log.trace("Сохранение фильма в хранилище");
            films.put(film.getId(), film);
        }
        log.trace("Возвращаем фильм");
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        log.trace("Обновление фильма");

        if (films.containsKey(film.getId())) {
            log.debug("Фильм найден для обновления {}", film.toString());

            if (isValidFilm(film)) {
                log.trace("Обновление фильма в хранилище");
                films.put(film.getId(), film);
            }

            log.trace("Возвращаем обновленный фильм");
            return film;
        }

        log.error("Попытка обновить несуществующий фильм с id {}", film.getId());
        throw new NotFoundException("Фильма с id " + film.getId() + " не существует");
    }

    public boolean isValidFilm(Film film) {
        log.trace("Валидация фильма {}", film);
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации: пустое название фильма");
            throw new ValidateException("Название не может быть пустым");
        }
        if (film.getDescription().length() > 200) {
            log.error("Ошибка валидации: слишком длинное описание, длина {}", film.getDescription().length());
            throw new ValidateException("Описание должно быть не длиннее 200 символов");
        }
        if (film.getReleaseDate().isBefore(FIRST_FILM)) {
            log.error("Ошибка валидации: дата релиза {} раньше допустимой {}",
                    film.getReleaseDate(), FIRST_FILM);
            throw new ValidateException("Дата релиза не должна быть раньше" + FIRST_FILM.format(dtf));
        }
        if (film.getDuration() < 1) {
            log.error("Ошибка валидации: некорректная длительность {}", film.getDuration());
            throw new ValidateException("Продолжительность фильма должна быть положительным числом");
        }
        log.debug("Фильм прошел валидацию {}", film);
        return true;
    }

    public Long getNextId() {
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