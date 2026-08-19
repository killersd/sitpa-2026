package br.com.sitpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ponto de entrada do backend do SITPa 2.0. */
@SpringBootApplication
@EnableScheduling
public class SitpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SitpaApplication.class, args);
    }
}
