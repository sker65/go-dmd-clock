package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FrameSeq implements Model {
    public byte digest[];
    public Palette palette;
    public List<Frame> frames;
    public String name;
	
    public FrameSeq(byte[] digest, Palette palette, List<Frame> frames, String name) {
		super();
		this.digest = digest;
		this.palette = palette;
		this.frames = frames;
		this.name = name;
	}

	public void writeTo(DataOutputStream os) throws IOException {
        os.write(digest);
        palette.writeTo(os);
        os.writeShort(frames.size());
        for (Frame frame : frames) {
            frame.writeTo(os);
        }
	}

    @Override
    public String toString() {
        return "FrameSeq [digest=" + Arrays.toString(digest) + ", palette=" + palette + ", frames=" + frames + ", name=" + name
                + "]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
