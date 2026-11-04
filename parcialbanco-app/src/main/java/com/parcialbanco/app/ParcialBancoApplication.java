package com.parcialbanco.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Unico punto de entrada de la aplicacion (puerto 8081). */
@SpringBootApplication
public class ParcialBancoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParcialBancoApplication.class, args);
    }
}
