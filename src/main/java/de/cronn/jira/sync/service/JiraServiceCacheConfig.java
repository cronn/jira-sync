package de.cronn.jira.sync.service;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.ehcache.jsr107.Eh107Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

@Configuration
public class JiraServiceCacheConfig {

	private static final Logger log = LoggerFactory.getLogger(JiraServiceCacheConfig.class);

	static final String CACHE_NAME_PRIORITIES = "priorities";
	static final String CACHE_NAME_SERVER_INFO = "serverInfo";
	static final String CACHE_NAME_MYSELF = "myself";
	static final String CACHE_NAME_PROJECTS = "projects";
	static final String CACHE_NAME_VERSIONS = "versions";
	static final String CACHE_NAME_RESOLUTIONS = "resolutions";

	private static final Duration ONE_HOUR = Duration.of(1, TimeUnit.HOURS);
	private static final Duration THIRTY_SECONDS = Duration.of(30, TimeUnit.SECONDS);

	@Bean
	@Primary
	public CacheManager ehCacheManager(ApplicationContext context) throws Exception {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		Resource configLocation = context.getResource("classpath:ehcache.xml");
		CacheManager cacheManager = cachingProvider.getCacheManager(configLocation.getURI(),
			cachingProvider.getDefaultClassLoader(), new Properties());
		createCache(cacheManager, CACHE_NAME_PROJECTS, ONE_HOUR, true);
		createCache(cacheManager, CACHE_NAME_PRIORITIES, ONE_HOUR, true);
		createCache(cacheManager, CACHE_NAME_RESOLUTIONS, ONE_HOUR, true);
		createCache(cacheManager, CACHE_NAME_VERSIONS, ONE_HOUR, true);
		createCache(cacheManager, CACHE_NAME_MYSELF, ONE_HOUR, false);
		createCache(cacheManager, CACHE_NAME_SERVER_INFO, THIRTY_SECONDS, false);
		return cacheManager;
	}

	private void createCache(CacheManager cacheManager, String cacheName, Duration timeToLive, boolean persistentCache) {
		ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.heap(100);
		if (persistentCache) {
			resourcePoolsBuilder = resourcePoolsBuilder.disk(1, MemoryUnit.MB, true);
		}
		CacheConfiguration<Object, Object> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class,
			resourcePoolsBuilder)
			.withExpiry(Expirations.timeToLiveExpiration(timeToLive))
			.build();

		for (String cache : cacheManager.getCacheNames()) {
			if (cache.equals(cacheName)) {
				log.warn("cache '{}' already exists. skipping creation", cacheName);
				return;
			}
		}

		cacheManager.createCache(cacheName, Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));
	}

}
