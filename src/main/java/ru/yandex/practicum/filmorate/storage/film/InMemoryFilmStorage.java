package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();

    private static final LocalDate FIRST_FILM = LocalDate.of(1895, 12, 28);
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public Collection<Film> getAllFilms() {
        log.trace("Отправка всех фильмов");
        return films.values();
    }

    @Override
    public Film addFilm(Film film) {
        log.trace("Добавление фильма");

        if (film.getReleaseDate().isBefore(FIRST_FILM)) {
            throw new ValidateException("Дата релиза должна быть не раньше " + FIRST_FILM.format(dtf)
                    + "ваша дата: " + film.getReleaseDate());
        }

        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }


    @Override
    public Film updateFilm(Film film) {
        log.trace("Обновление фильма");

        if (!films.containsKey(film.getId())) {
            log.error("Попытка обновить несуществующий фильм с id {}", film.getId());
            throw new NotFoundException("Фильма с id " + film.getId() + " не существует");
        }

        Film updateFilm = films.get(film.getId());

        if (film.getDuration() != null) {
            updateFilm.setDuration(film.getDuration());
        }

        if (film.getName() != null) {
            updateFilm.setName(film.getName());
        }

        if (film.getDescription() != null) {
            updateFilm.setDescription(film.getDescription());
        }

        if (film.getReleaseDate() != null) {
            if (film.getReleaseDate().isBefore(FIRST_FILM)) {
                throw new ValidateException("Дата релиза должна быть не раньше " + FIRST_FILM.format(dtf)
                        + "ваша дата: " + film.getReleaseDate());
            }
            updateFilm.setReleaseDate(film.getReleaseDate());
        }

        return updateFilm;
    }

    public Optional<Film> getFilmById(Long filmId) {
        Film film = films.get(filmId);
        return Optional.ofNullable(film);
    }


    private Long getNextId() {

        Long currentId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        Long nextId = currentId + 1;
        log.debug("Сгенерирован фильм с id {}", nextId);
        return nextId;
    }
}
