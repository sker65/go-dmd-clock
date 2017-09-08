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
	public static final String PIN2DMD_ADRESS_PROP_KEY = "pin2dmdAdress";
	public static final String PIN2DMD_DMDSIZE_PROP_KEY = "dmdSize";
	public static final String GODMD_ENABLED_PROP_KEY = "godmdEnabled";
	public static final String AUTOSAVE_INTERVAL = "autosaveInterval";
	public static final String AUTOSAVE = "autosave";
	public static final String AUTOKEYFRAME = "autoKeyframeWhenCut";
	public static final String NOOFPLANES = "noOfPlanesWhenCutting";
	public static final String OLDEXPORT = "oldexport";
	public static final String ADDPALWHENCUT = "addPalWhenCut";

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

	String getFilename() {
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
		log.debug("setting prop {} to '{}'", key, value);
		String old = getInstance().props.getProperty(key);
		if( !value.equals(old) ) {
			log.info("value for prop {} changed {} -> {}", key, old, value);
			getInstance().props.put(key, value);
			getInstance().save();
		}
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
		log.debug("get prop {} = '{}' ", key, val);
		return val;
	}

	/** mainly for testing purpose */
	public static void setPropFile(String filename) {
		getInstance().propertiesFilename = filename;
		
	}

	public static boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public static boolean getBoolean(String key, boolean defaultVal) {
		String val = get(key);
		return val!=null?Boolean.parseBoolean(val):defaultVal;	
	}
	
	public static int getInteger(String key) {
		return getInteger(key, 0);
	}

	public static int getInteger(String key, int defaultVal) {
		String val = get(key);
		return val!=null?Integer.parseInt(val):defaultVal;	
	}

	public static void put(String key, int val) {
		put(key, Integer.toString(val));
	}

	public static void put(String key, boolean val) {
		put(key, Boolean.toString(val));
	}
	
}
