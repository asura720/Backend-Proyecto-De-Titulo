package com.cuidapp.medicamentoscuidapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicamentoscuidappApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicamentoscuidappApplication.class, args);
	}

}
