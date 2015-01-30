package com.rinke.solutions.pinball.renderer;

public class FrameSet {
	public int width;
	public int height;
	public byte[] frame1; // lower intensity
	public byte[] frame2; // higher intensity
	public FrameSet(int width, int height, byte[] frame1, byte[] frame2) {
		super();
		this.width = width;
		this.height = height;
		this.frame1 = frame1;
		this.frame2 = frame2;
	}
}
