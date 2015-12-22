package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.rinke.solutions.pinball.animation.Plane;

public class Frame implements Model {
	public long timecode;
	public List<byte[]> planes;
	
	public Frame(long timecode, List<byte[]> planes) {
		super();
		this.timecode = timecode;
		this.planes = planes;
	}

	//TODO remove different models
	public Frame(com.rinke.solutions.pinball.animation.Frame frame) {
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
		os.writeLong(timecode);
		os.writeShort(planes.size());
		for(byte[] data : planes) {
			os.write(data);
		}
	}
	
}
