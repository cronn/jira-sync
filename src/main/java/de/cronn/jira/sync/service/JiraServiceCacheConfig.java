package de.cronn.jira.sync.service;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import de.cronn.jira.sync.config.CacheConfig;
import de.cronn.jira.sync.config.JiraSyncConfig;

@Configuration
public class JiraServiceCacheConfig {

	private static final Logger log = LoggerFactory.getLogger(JiraServiceCacheConfig.class);

	static final String CACHE_NAME_PRIORITIES = "priorities";
	static final String CACHE_NAME_SERVER_INFO = "serverInfo";
	static final String CACHE_NAME_MYSELF = "myself";
	static final String CACHE_NAME_USERS = "users";
	static final String CACHE_NAME_PROJECTS = "projects";
	static final String CACHE_NAME_VERSIONS = "versions";
	static final String CACHE_NAME_COMPONENTS = "components";
	static final String CACHE_NAME_RESOLUTIONS = "resolutions";
	static final String CACHE_NAME_FIELDS = "fields";
	static final String CACHE_NAME_REMOTE_LINKS = "remoteLinks";
	static final String CACHE_NAME_FIELD_ALLOWED_VALUES = "fieldAllowedValues";

	private static final Duration ONE_HOUR = Duration.of(1, TimeUnit.HOURS);
	private static final Duration THIRTY_SECONDS = Duration.of(30, TimeUnit.SECONDS);

	@Bean
	@Primary
	public CacheManager ehCacheManager(JiraSyncConfig jiraSyncConfig) throws Exception {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		EhcacheCachingProvider ehcacheCachingProvider = (EhcacheCachingProvider) cachingProvider;
		CacheConfig cacheConfig = jiraSyncConfig.getCache();
		CacheManager cacheManager = getCacheManager(ehcacheCachingProvider, cacheConfig);
		boolean persistentCache = cacheConfig.isPersistent();
		createCache(cacheManager, CACHE_NAME_PROJECTS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_PRIORITIES, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_RESOLUTIONS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_VERSIONS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_COMPONENTS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_FIELDS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_REMOTE_LINKS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_USERS, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_FIELD_ALLOWED_VALUES, ONE_HOUR, persistentCache);
		createCache(cacheManager, CACHE_NAME_MYSELF, ONE_HOUR, false);
		createCache(cacheManager, CACHE_NAME_SERVER_INFO, THIRTY_SECONDS, false);
		return cacheManager;
	}

	private CacheManager getCacheManager(EhcacheCachingProvider cachingProvider, CacheConfig cacheConfig) {
		URI uri = cachingProvider.getDefaultURI();
		DefaultConfiguration configuration = getCacheConfiguration(cachingProvider, cacheConfig);
		return cachingProvider.getCacheManager(uri, configuration);
	}

	private DefaultConfiguration getCacheConfiguration(EhcacheCachingProvider cachingProvider, CacheConfig cacheConfig) {
		if (cacheConfig.isPersistent()) {
			Path cacheDirectory = Paths.get(cacheConfig.getDirectory());
			log.info("setting up persistent cache in {}", cacheDirectory.toAbsolutePath());
			DefaultPersistenceConfiguration persistenceConfiguration = new DefaultPersistenceConfiguration(cacheDirectory.toFile());
			return new DefaultConfiguration(cachingProvider.getDefaultClassLoader(), persistenceConfiguration);
		} else {
			log.info("setting up in-memory cache");
			return new DefaultConfiguration(cachingProvider.getDefaultClassLoader());
		}
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
