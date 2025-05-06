package com.agms.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootTest
class AgmsBackendApplicationTests {
	@DynamicPropertySource
	static void loadEnvironmentVariables(DynamicPropertyRegistry registry) {

		try {
			Dotenv dotenv = Dotenv.configure()
					.filename("env.local")
					.load();

			// Set system properties for Spring to use
			dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to load environment variables", e);
		}
	}

	@Test
	void contextLoads() {
		// This test verifies that the Spring context loads successfully
	}
}
