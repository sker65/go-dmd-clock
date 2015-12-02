package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FrameSequence implements Model {

	public String name;
	public List<Frame> frames = new ArrayList<>();
	
	@Override
	public void writeTo(DataOutputStream os) throws IOException {
		os.writeUTF(name);
		os.writeShort(frames.size());
		for (Frame frame : frames) {
			frame.writeTo(os);
		}
	}

	@Override
	public String toString() {
		return "FrameSequence [name=" + name + ", frames=" + frames + "]";
	}

	public FrameSequence(String name, List<Frame> frames) {
		super();
		this.name = name;
		this.frames = frames;
	}

}
