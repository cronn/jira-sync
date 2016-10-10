package de.cronn.jira.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class JiraSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(JiraSyncApplication.class, args);
	}
}
