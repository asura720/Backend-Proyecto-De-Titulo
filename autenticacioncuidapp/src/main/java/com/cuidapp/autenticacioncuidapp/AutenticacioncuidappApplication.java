package com.cuidapp.autenticacioncuidapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AutenticacioncuidappApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutenticacioncuidappApplication.class, args);
	}

}
