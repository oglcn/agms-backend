package com.agms.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgmsBackendApplication {

	public static void main(String[] args) {
		// Load environment variables from .env.local in the root directory
		Dotenv dotenv = Dotenv.configure()
				.filename(".env.local")
				.load();

		// Set system properties for Spring to use
		dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

		SpringApplication.run(AgmsBackendApplication.class, args);
	}

}
