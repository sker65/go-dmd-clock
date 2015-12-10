package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FrameMapping implements Model {
    public byte digest[];
    public Palette palette;
    public List<Frame> frames;
	public FrameMapping(byte[] digest, Palette palette, List<Frame> frames) {
		super();
		this.digest = digest;
		this.palette = palette;
		this.frames = frames;
	}
	@Override
	public String toString() {
		return "FrameMapping [digest=" + Arrays.toString(digest) + ", palette="
				+ palette + ", frames=" + frames + "]";
	}
	
	public void writeTo(DataOutputStream os) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
