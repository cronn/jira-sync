package de.cronn.jira.sync;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(JiraSyncApplication.class);

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer mapperBuilderCustomizer() {
		return mapperBuilder -> {
			mapperBuilder.serializationInclusion(Include.NON_NULL);
			mapperBuilder.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		};
	}

	@PostConstruct
	public void checkFileEncoding() {
		Charset defaultCharset = Charset.defaultCharset();
		Charset expectedCharset = StandardCharsets.UTF_8;
		if (!defaultCharset.equals(expectedCharset)) {
			log.warn("Default charset is '{}'. Consider switching environment to '{}'", defaultCharset, expectedCharset);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(JiraSyncApplication.class, args);
	}
}
