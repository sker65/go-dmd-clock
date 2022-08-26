package com.rinke.solutions.pinball.renderer.video;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

/**
 * dummy class, just to make the compiler happy, when not using video
 * @author stefanri
 *
 */
@Slf4j
public class FFmpegFrameGrabber {
	
	Object delegate = null;
	Class<?> clz;
	Class<?> frameClz;
	private Method startMethod;
	private Method stopMethod;
	private Method grabMethod;
	private Method getTimestampMethod;
	private Method getLengthInFramesMethod;
	private Method getImageHeightMethod;
	private Method getImageWidthMethod;
	private Method releaseMethod;


	public FFmpegFrameGrabber(String name) {
		try {
			clz = Class.forName("org.bytedeco.javacv.FFmpegFrameGrabber");
			Constructor<?> ctor = clz.getDeclaredConstructor(String.class);
			delegate = ctor.newInstance(name);
			startMethod = clz.getDeclaredMethod("start");
			stopMethod = clz.getDeclaredMethod("stop");
			grabMethod = clz.getDeclaredMethod("grab");
			getTimestampMethod = clz.getMethod("getTimestamp");
			getLengthInFramesMethod = clz.getMethod("getLengthInFrames");
			getImageHeightMethod = clz.getMethod("getImageHeight");
			getImageWidthMethod = clz.getMethod("getImageWidth");
			//releaseMethod = clz.getMethod("release");
		} catch (Exception e) {
			log.warn("error loading classes",e);
		}
	}

	public void release() {
		try {
			if( releaseMethod!= null ) releaseMethod.invoke(delegate);
			log.info("releasing grabber");
		} catch (Exception e) {
			log.warn("error calling release",e);
		}
	}

	public void start() {
		try {
			startMethod.invoke(delegate);
			log.info("start grabber");
		} catch (Exception e) {
			log.warn("error calling start",e);
		}
	}
	
	public int getImageWidth() {
		try {
			return (int) getImageWidthMethod.invoke(delegate);
		} catch (Exception e) {
			log.warn("error calling getImageWidth",e);
			return 0;
		}
	}
	
	public int getImageHeight() {
		try {
			return (int) getImageHeightMethod.invoke(delegate);
		} catch (Exception e) {
			log.warn("error calling getImageHeight",e);
			return 0;
		}
	}
	
	public int getLengthInFrames() {
		try {
			return (int) getLengthInFramesMethod.invoke(delegate);
		} catch (Exception e) {
			log.warn("error calling getLengthInFrames",e);
			return 0;
		}
	}

	public Object grab() {
		try {
			return grabMethod.invoke(delegate);
		} catch (Exception e) {
			log.warn("error calling grab",e);
			return null;
		}
	}

	public long getTimestamp() {
		try {
			return (long) getTimestampMethod.invoke(delegate);
		} catch (Exception e) {
			log.warn("error calling start",e);
			return 0;
		}
	}

	public void stop() {
		try {
			stopMethod.invoke(delegate);
			log.info("stopping grabber");
		} catch (Exception e) {
			log.warn("error calling stop",e);
		}
	}

	public boolean containsData(Object frame) {
		Class<?> frameClz = frame.getClass();
		try {
			Field field = frameClz.getField("image");
			return field.get(frame) != null;
		} catch (Exception e) {
		}
		return false;
	}

}
