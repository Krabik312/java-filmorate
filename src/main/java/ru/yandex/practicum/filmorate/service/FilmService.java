package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class FilmService {

    private static final LocalDate dateStartValidation = LocalDate.of(1895, 12, 28);
    private static final LocalDate dateFinishValidation = LocalDate.now();

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }


    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film addFilm(Film film) {
        isFilmDateValid(film);
        isIdGenreValid(film);
        isMpaValid(film);
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        isFilmDateValid(film);
        isIdGenreValid(film);
        isMpaValid(film);
        return filmStorage.updateFilm(film);

    }

    public Film getFilmById(Long filmId) {
        return getFilmOrThrow(filmId);
    }

    public void addLike(Long filmId, Long userId) {
        if (userStorage.getUserById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        getFilmOrThrow(filmId);

        filmStorage.addLike(filmId, userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        if (userStorage.getUserById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        getFilmOrThrow(filmId);

        filmStorage.deleteLike(filmId, userId);
    }

    public List<Film> getPopularFilmsByLike(Integer count) {
        return filmStorage.getPopularFilmsByLike(count);
    }

    private Film getFilmOrThrow(Long filmId) {
        return filmStorage.getFilmById(filmId).orElseThrow(() ->
                new NotFoundException("Фильм с id: " + filmId + " не найден")
        );
    }

    private void isFilmDateValid(Film film) {
        if (film.getReleaseDate().isBefore(dateStartValidation) ||
                film.getReleaseDate().isAfter(dateFinishValidation)) {
            throw new ValidateException("Неверная дата выхода фильма");
        }
    }

    private void isIdGenreValid(Film film) {
        for (Genre genre : film.getGenres()) {
            if (genre.getId() < 1 || genre.getId() > 6) {
                throw new NotFoundException("Такого жанра не существует");
            }
        }
    }

    private void isMpaValid(Film film) {
        if (film.getMpa().getId() < 1 || film.getMpa().getId() > 5) {
            throw new NotFoundException("Такого рейтинга не существует");
        }
    }
}
