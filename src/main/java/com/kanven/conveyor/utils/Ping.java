package com.kanven.conveyor.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ping {

	private static final Logger log = LoggerFactory.getLogger(Ping.class);

	public static boolean ping(String host, int port) {
		SocketAddress address = new InetSocketAddress(host, port);
		return ping(address);
	}

	public static boolean ping(SocketAddress address) {
		Socket socket = new Socket();
		try {
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(3000);
			socket.setSoLinger(false, -1);
			socket.connect(address);
			return true;
		} catch (IOException e) {
			log.error("socker连接异常！", e);
			return false;
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

}
