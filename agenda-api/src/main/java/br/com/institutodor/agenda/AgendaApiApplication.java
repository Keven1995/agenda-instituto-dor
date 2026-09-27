package br.com.institutodor.agenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgendaApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgendaApiApplication.class, args);
    }
}
