package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rinke.solutions.pinball.animation.Plane;

public class Frame implements Model {
	public int timecode;
	public List<byte[]> planes = new ArrayList<>();
	
	public Frame(int timecode, List<byte[]> planes) {
		super();
		this.timecode = timecode;
		this.planes = planes;
	}

	//TODO remove different models
	public Frame(com.rinke.solutions.pinball.animation.Frame frame) {
		this.timecode = frame.timecode;
		for(Plane p: frame.planes) {
			planes.add(Arrays.copyOf(p.plane, p.plane.length));
		}
	}

	@Override
	public String toString() {
		return "Frame [timecode=" + timecode + ", planes=" + planes + "]";
	}

	@Override
	public void writeTo(DataOutputStream os) throws IOException {
		os.writeInt(timecode);
		os.writeShort(planes.size());
		for(byte[] data : planes) {
			os.writeShort(data.length);
			os.write(data);
		}
	}
	
}
