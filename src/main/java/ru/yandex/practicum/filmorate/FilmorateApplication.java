package ru.yandex.practicum.filmorate;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ch.qos.logback.classic.Logger;


@SpringBootApplication
public class FilmorateApplication {

	private static final Logger log = (Logger) LoggerFactory.getLogger(FilmorateApplication.class);

	public static void main(String[] args) {
		//уровень логгирования задается в properties
		log.trace("Запуск сервера");
		SpringApplication.run(FilmorateApplication.class, args);
	}

}
