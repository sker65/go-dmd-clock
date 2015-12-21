package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FrameSeq implements Model {

    public List<Frame> frames;
    public String name;
	
    public FrameSeq(List<Frame> frames, String name) {
		super();
		this.frames = frames;
		this.name = name;
	}

	public void writeTo(DataOutputStream os) throws IOException {
        os.writeShort(frames.size());
        for (Frame frame : frames) {
            frame.writeTo(os);
        }
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	@Override
	public String toString() {
		return "FrameSeq [frames=" + frames + ", name=" + name + "]";
	}

}
