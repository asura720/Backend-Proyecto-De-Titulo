package com.cuidapp.notificacionescuidapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotificacionescuidappApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificacionescuidappApplication.class, args);
	}

}
