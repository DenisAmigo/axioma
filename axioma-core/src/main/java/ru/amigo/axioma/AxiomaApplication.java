package ru.amigo.axioma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AxiomaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AxiomaApplication.class, args);
    }
}
