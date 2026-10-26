package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;

@RequiredArgsConstructor
@Service
public class GenreService {

    private final GenreDbStorage genreDbStorage;

    public Collection<Genre> getAllGenres() {
        return genreDbStorage.getAllGenres();
    }

    public Genre getGenreById(Long genreId) {
        return genreDbStorage.getGenre(genreId)
                .orElseThrow(() -> new NotFoundException("Жанр с id " + genreId + " не найден"));
    }
}
