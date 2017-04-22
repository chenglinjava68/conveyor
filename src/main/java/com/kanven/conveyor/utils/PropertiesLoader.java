package com.kanven.conveyor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author kanven
 *
 */
public class PropertiesLoader {

	public static Properties loadProperties(String path) throws IOException {
		InputStream in = ClassLoader.getSystemResourceAsStream(path);
		Properties properties = new Properties();
		properties.load(in);
		return properties;
	}

	public static Properties loadProperties(String path, Class<?> clazz)
			throws IOException {
		InputStream in = clazz.getClassLoader().getResourceAsStream(path);
		Properties properties = new Properties();
		properties.load(in);
		return properties;
	}

}
