package com.rinke.solutions.pinball.renderer.video;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Java2DFrameConverter {
	
	// original classes reside in org.bytedeco.javacv
	Object delegate = null;
	Class<?> clz;
	Class<?> frameClz;
	private Method convertMethod;
 
	public Java2DFrameConverter() {
		super();
		try {
			clz = Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
			frameClz = Class.forName("org.bytedeco.javacv.Frame");
			delegate = clz.newInstance();
			convertMethod = clz.getMethod("convert", frameClz);
		} catch (Exception e) {
			log.warn("video converter class not found", e);
		}
	}

	public BufferedImage convert(Object frame) {
		if( convertMethod!=null) {
			try {
				return (BufferedImage) convertMethod.invoke(delegate, frame);
			} catch (Exception e) {
				log.warn("call to convert failed.", e);
			} 
		}
		return null;
	}

}
