package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Override
    public void addLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException(
                        "Фильм с id " + filmId + " не найден"
                ));

        film.addLike(userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException(
                        "Фильм с id " + filmId + " не найден"
                ));

        film.deleteLike(userId);
    }

    @Override
    public List<Film> getPopularFilmsByLike(Integer count) {
        if (count == null || count <= 0) {
            throw new IllegalArgumentException(
                    "Количество фильмов должно быть положительным"
            );
        }

        return films.values().stream()
                .sorted(
                        Comparator.comparingInt(
                                        (Film film) -> film.getLikes().size()
                                ).reversed()
                                .thenComparing(Film::getId)
                )
                .limit(count)
                .toList();
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
