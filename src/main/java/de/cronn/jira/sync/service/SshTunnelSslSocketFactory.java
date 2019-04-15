package de.cronn.jira.sync.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import de.cronn.proxy.ssh.SshProxy;

public class SshTunnelSslSocketFactory implements LayeredConnectionSocketFactory {

	private final SshProxy sshProxy;
	private final LayeredConnectionSocketFactory connectionSocketFactory;
	private final String sshJumpHost;

	SshTunnelSslSocketFactory(SshProxy sshProxy, LayeredConnectionSocketFactory connectionSocketFactory, String sshJumpHost) {
		this.sshProxy = sshProxy;
		this.connectionSocketFactory = connectionSocketFactory;
		this.sshJumpHost = sshJumpHost;
	}

	@Override
	public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException {
		return connectionSocketFactory.createLayeredSocket(socket, target, port, context);
	}

	@Override
	public Socket createSocket(HttpContext context) throws IOException {
		return connectionSocketFactory.createSocket(context);
	}

	@Override
	public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
		int localPort = sshProxy.connect(sshJumpHost, remoteAddress.getHostName(), remoteAddress.getPort());
		InetSocketAddress proxiedRemoteAddress = new InetSocketAddress(SshProxy.LOCALHOST, localPort);
		return connectionSocketFactory.connectSocket(connectTimeout, sock, host, proxiedRemoteAddress, localAddress, context);
	}
}
