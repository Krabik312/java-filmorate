package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film addFilm(Film film) {
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        return filmStorage.updateFilm(film);
    }

    public Film getFilmById(Long filmId) {
        return getFilmOrThrow(filmId);
    }

    public void addLike(Long filmId, Long userId) {
        if (userStorage.getUserById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        getFilmOrThrow(filmId).addLike(userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        if (userStorage.getUserById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        getFilmOrThrow(filmId).deleteLike(userId);
    }

    public List<Film> getPopularFilmsByLike(Integer count) {
        List<Film> popularFilms = filmStorage.getAllFilms().stream()
                .sorted((f1, f2) -> Integer.compare(
                        f2.getLikes().size(),
                        f1.getLikes().size()
                ))
                .limit(count)
                .toList();
        return popularFilms;
    }

    private Film getFilmOrThrow(Long filmId) {
        return filmStorage.getFilmById(filmId).orElseThrow(() ->
                new NotFoundException("Фильм с id: " + filmId + " не найден")
        );
    }
}
