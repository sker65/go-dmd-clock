package com.rinke.solutions.pinball;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenCVLoader {
	private static boolean loaded = false;

	public static void load() {
		if (!loaded) {
			log.info("loading openCV libs");
			Loader.load(opencv_java.class);
			loaded = true;
		}
	}
}
