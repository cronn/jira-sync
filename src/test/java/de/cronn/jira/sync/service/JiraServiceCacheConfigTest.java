package de.cronn.jira.sync.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import de.cronn.jira.sync.domain.JiraProject;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JiraServiceCacheConfigTest {

	@Autowired
	private CacheManager cacheManager;

	@Test
	public void testProjectsCache() throws Exception {
		Cache<Object, Object> cache = cacheManager.getCache(JiraServiceCacheConfig.CACHE_NAME_PROJECTS);
		assertThat(cache).isNotNull();
		List<String> key = Arrays.asList("key1", "key2");
		cache.put(key, new JiraProject("123", "foobar"));
		Object cachedProject = cache.get(key);
		assertThat(cachedProject).isNotNull();
	}

}