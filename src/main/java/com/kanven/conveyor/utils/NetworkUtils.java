package com.kanven.conveyor.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class NetworkUtils {

	private static final String ENV = "ENVIROMENT";

	private static final String LOCALHOST_IP = "127.0.0.1";

	private static final String EMPTY_IP = "0.0.0.0";

	private static final Pattern IP_PATTERN = Pattern.compile("[0-9]{1,3}(\\.[0-9]{1,3}){3,}");

	public static String getHostName() {
		String env = System.getenv(ENV);
		if (StringUtils.isBlank(env)) {
			env = System.getProperty(ENV);
		}
		if (StringUtils.isBlank(env)) {
			try {
				InetAddress address = InetAddress.getLocalHost();
				env = address.getHostName();
			} catch (UnknownHostException e) {
			}
		}
		return env;
	}

	public static String getHostIp() {
		String ip = null;
		try {
			InetAddress address = InetAddress.getLocalHost();
			ip = address.getHostAddress();
		} catch (UnknownHostException e) {
		}
		return ip;
	}

	public static boolean isValidHostAddress(InetAddress address) {
		if (address == null || address.isLoopbackAddress())
			return false;
		String name = address.getHostAddress();
		return (name != null && !EMPTY_IP.equals(name) && !LOCALHOST_IP.equals(name)
				&& IP_PATTERN.matcher(name).matches());
	}

	public static InetAddress getHostAddress() {
		InetAddress localAddress = null;
		try {
			localAddress = InetAddress.getLocalHost();
			if (isValidHostAddress(localAddress)) {
				return localAddress;
			}
		} catch (Throwable e) {
		}
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces != null) {
				while (interfaces.hasMoreElements()) {
					try {
						NetworkInterface network = interfaces.nextElement();
						Enumeration<InetAddress> addresses = network.getInetAddresses();
						if (addresses != null) {
							while (addresses.hasMoreElements()) {
								try {
									InetAddress address = addresses.nextElement();
									if (isValidHostAddress(address)) {
										return address;
									}
								} catch (Throwable e) {
								}
							}
						}
					} catch (Throwable e) {
					}
				}
			}
		} catch (Throwable e) {
		}
		return localAddress;
	}

}
