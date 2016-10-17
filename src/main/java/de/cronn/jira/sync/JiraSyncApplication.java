package de.cronn.jira.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.SerializationFeature;

@SpringBootApplication
@EnableCaching
public class JiraSyncApplication {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer mapperBuilderCustomizer() {
		return mapperBuilder -> {
			mapperBuilder.serializationInclusion(Include.NON_NULL);
			mapperBuilder.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(JiraSyncApplication.class, args);
	}
}
