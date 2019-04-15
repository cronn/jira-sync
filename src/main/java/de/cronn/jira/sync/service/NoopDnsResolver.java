package de.cronn.jira.sync.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.conn.DnsResolver;

class NoopDnsResolver implements DnsResolver {

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
		InetAddress inetAddress = InetAddress.getByAddress(host, loopbackAddress.getAddress());
		return new InetAddress[] { inetAddress };
	}

}
