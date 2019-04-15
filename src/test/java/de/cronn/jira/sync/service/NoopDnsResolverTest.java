package de.cronn.jira.sync.service;

import static org.assertj.core.api.Assertions.*;

import java.net.InetAddress;

import org.apache.http.conn.DnsResolver;
import org.junit.Test;

public class NoopDnsResolverTest {

	@Test
	public void testResolveSomeHost() throws Exception {
		DnsResolver dnsResolver = new NoopDnsResolver();

		InetAddress[] inetAddresses = dnsResolver.resolve("some.host");
		assertThat(inetAddresses)
			.extracting(InetAddress::getHostName)
			.containsExactly("some.host");
	}

}
