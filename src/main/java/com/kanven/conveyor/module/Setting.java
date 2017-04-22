package com.kanven.conveyor.module;

import java.io.IOException;
import java.util.Properties;

import com.kanven.conveyor.utils.PropertiesLoader;

public class Setting {

	private Properties properties;

	private final static String DEFAULT_SETTING_PATH = "conf/conveyor.properties";

	public Setting() throws IOException {
		this(DEFAULT_SETTING_PATH);
	}

	public Setting(String configPath) throws IOException {
		this.properties = PropertiesLoader.loadProperties(configPath);
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

}
