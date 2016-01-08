package org.bytedeco.javacv;

/**
 * dummy class, just to make the compiler happy, when not using video
 * @author stefanri
 *
 */
public class FFmpegFrameGrabber {

	public FFmpegFrameGrabber(String name) {
	}

	public void start() {
	}

	public org.bytedeco.javacv.Frame grab() {
		return null;
	}

	public int getTimestamp() {
		return 0;
	}

	public void stop() {
	}

}
