package com.rinke.solutions.pinball.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.BeanFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class Config implements BeanFactory.PropertyProvider {

	private String propertiesFilename;
	public static final String PIN2DMD_ADRESS = "pin2dmdAdress";
	public static final String COLOR_ACCURACY = "colorAccuracy";
	public static final String DMDSIZE = "dmdSize";
	public static final String NOOFCOLORS = "numberOfColors";
	public static final String GODMD_ENABLED_PROP_KEY = "godmdEnabled";
	public static final String AUTOSAVE_INTERVAL = "autosaveInterval";
	public static final String AUTOSAVE = "autosave";
	public static final String AUTOKEYFRAME = "autoKeyframeWhenCut";
	public static final String NOOFPLANES = "noOfPlanesWhenCutting";
	public static final String OLDEXPORT = "oldexport";
	public static final String ADDPALWHENCUT = "addPalWhenCut";
	public static final String CREATEBOOKCUT= "createBookmarkAfterCut";
	public static final String BACKUP = "backup";
	public static final String GODMD_EXPORT_PATH = "goDmdExportPath";
	public static final String NO_QUIT_WARNING = "noQuitWarning";
	public static final String NO_EXPORT_WARNING = "noExportWarning";

	public Config() {
		super();
	}

	public Config(String file) {
		super();
		this.propertiesFilename = file;
	}

	private Properties props = new Properties();

	String getFilename() {
		if( propertiesFilename != null ) return propertiesFilename;
		String homeDir = System.getProperty("user.home");
		String propertiesFilename = homeDir + File.separator + "pin2dmd.properties";
		return propertiesFilename;
	}
	
	public void init() {
		load();
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

	public  Properties getProperties() {
		return props;
	}

	public  void put(String key, String value) {
		log.debug("setting prop {} to '{}'", key, value);
		String old = props.getProperty(key);
		if( !value.equals(old) ) {
			log.info("value for prop {} changed {} -> {}", key, old, value);
			props.put(key, value);
			save();
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

	public  String get(String key) {
		String val = props.getProperty(key);
		log.debug("get prop {} = '{}' ", key, val);
		return val;
	}

	/** mainly for testing purpose */
	public  void setPropFile(String filename) {
		propertiesFilename = filename;
		
	}

	public  boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public  boolean getBoolean(String key, boolean defaultVal) {
		String val = get(key);
		return val!=null?Boolean.parseBoolean(val):defaultVal;	
	}
	
	public  int getInteger(String key) {
		return getInteger(key, 0);
	}

	public  int getInteger(String key, int defaultVal) {
		String val = get(key);
		return val!=null?Integer.parseInt(val):defaultVal;	
	}

	public  void put(String key, int val) {
		put(key, Integer.toString(val));
	}

	public  void put(String key, boolean val) {
		put(key, Boolean.toString(val));
	}

	@Override
	public String getProperty(String key) {
		return props.getProperty(key);
	}
	
}