package com.rinke.solutions.pinball.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationProperties {

	private String propertiesFilename = "pin2dmd.properties";

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

	private String getFilename() {
		String homeDir = System.getProperty("user.home");
		String filename = homeDir + File.separator + propertiesFilename;
		return filename;
	}

	public void load() {
		String filename = getFilename();
		try {
			props.load(new FileInputStream(filename));
			log.info("loaded properties from {}", filename);
		} catch( FileNotFoundException e ) {
			log.info("no property file {} found", filename );
		} catch (Exception e) {
			log.warn("problems loading {} from ", filename, e);
		}
	}

	public static Properties getProperties() {
		return getInstance().props;
	}

	public static void put(String key, String value) {
		log.info("setting prop {} to '{}'", key, value);
		getInstance().props.put(key, value);
		getInstance().save();
	}

	public void save() {
		String filename = getFilename();
		try {
			props.store(new FileOutputStream(filename), "");
		} catch (IOException e) {
			log.error("storing {}", filename);
			throw new RuntimeException(e);
		}
	}

	public static String get(String key) {
		String val = getInstance().props.getProperty(key);
		log.info("get prop {} = '{}' ", key, val);
		return val;
	}

	/** mainly for testing purpose */
	public static void setPropFile(String filename) {
		getInstance().propertiesFilename = filename;
		
	}

}
