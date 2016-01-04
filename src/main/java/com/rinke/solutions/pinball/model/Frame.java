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
		this.timecode = frame.delay;
		for(Plane p: frame.planes) {
		    byte[] transformed = com.rinke.solutions.pinball.animation.Frame.transform(p.plane);
			planes.add(transformed);
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
		if( !planes.isEmpty() ) {
            os.writeShort(planes.get(0).length);
		}
		for(byte[] data : planes) {
			os.write(data);
		}
	}
	
}
