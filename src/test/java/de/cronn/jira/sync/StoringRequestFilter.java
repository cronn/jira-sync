package de.cronn.jira.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class StoringRequestFilter extends OncePerRequestFilter {
	private static final int PAYLOAD_LIMIT = 10_000;
	private Map<String, String> requests = new LinkedHashMap<>();

	public StoringRequestFilter() {
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
		filterChain.doFilter(wrapper, response);
		String payload = getPayload(wrapper);
		requests.put(createKey(wrapper.getMethod(), wrapper.getRequestURI()), payload);
	}

	private String createKey(String method, String requestURI) {
		return method + " " + requestURI;
	}

	private String getPayload(ContentCachingRequestWrapper wrapper) {
		String payload = null;
		byte[] buf = wrapper.getContentAsByteArray();
		if (buf.length > 0) {
			int length = Math.min(buf.length, PAYLOAD_LIMIT);
			try {
				payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
			} catch (UnsupportedEncodingException ex) {
				payload = "[unknown]";
			}
		}
		return payload;
	}

	public String getPayload(HttpMethod method, String url) {
		return requests.get(createKey(method.name(), url));
	}

	public void clear() {
		requests.clear();
	}

}