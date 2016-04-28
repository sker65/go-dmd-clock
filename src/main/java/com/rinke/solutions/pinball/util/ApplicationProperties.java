package com.rinke.solutions.pinball.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationProperties {
	private static final String PIN2DMD_PROPERTIES = "pin2dmd.properties";

	private static ApplicationProperties theInstance;

	public static synchronized ApplicationProperties getInstance() {
		if (theInstance == null) {
			theInstance = new ApplicationProperties();
			theInstance.load();
		}
		return theInstance;
	}

	private ApplicationProperties() {
		super();
	}

	private Properties props = new Properties();

	public void load() {
		String homeDir = System.getProperty("user.home");
		try {
			props.load(new FileInputStream(homeDir + File.separator
					+ PIN2DMD_PROPERTIES));
		} catch (Exception e) {
			log.warn("problems loading / verifing " + PIN2DMD_PROPERTIES
					+ " from " + homeDir);
		}
	}
	
	public static Properties getProperties() {
		return getInstance().props;
	}
	
	public static void put(String key, String value) {
		getInstance().props.put(key, value);
		getInstance().save();
	}

	public void save() {
		String homeDir = System.getProperty("user.home");
		try {
			props.store(new FileOutputStream(homeDir + File.separator
					+ PIN2DMD_PROPERTIES), "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String get(String key) {
		return getInstance().props.getProperty(key);
	}

}
