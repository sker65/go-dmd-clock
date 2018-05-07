package com.rinke.solutions.pinball.util;

import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionUtil {
	public synchronized static String getVersion() {
		
		String className = VersionUtil.class.getSimpleName() + ".class";
		String classPath = VersionUtil.class.getResource(className).toString();
		if (!classPath.startsWith("jar")) {
		  // Class not from JAR
		  return "9.9.9.9";
		}
		String version = "";
		
		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + 
		    "/META-INF/MANIFEST.MF";
		
	    // try to load from maven properties first
	    try {
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			Attributes attr = manifest.getMainAttributes();
	            version = "Version: " + attr.getValue("version");
	            version += "\nat: " + attr.getValue("buildNumber");
	            version += "\nBuild: " + attr.getValue("drone");
	    } catch (Exception e) {
	        // ignore
	    }

	    // fallback to using Java API
	    if (version == null) {
	        Package aPackage = VersionUtil.class.getPackage();
	        if (aPackage != null) {
	            version = aPackage.getImplementationVersion();
	            if (version == null) {
	                version = aPackage.getSpecificationVersion();
	            }
	        }
	    }

	    if (version == null) {
	        // we could not compute the version so use a blank
	        version = "";
	    }

	    return version;
	} 

}
