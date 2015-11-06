package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class Frame implements Model {
	public int repeatCount;
	public List<byte[]> planes;
	
	public Frame(int repeatCount, List<byte[]> planes) {
		super();
		this.repeatCount = repeatCount;
		this.planes = planes;
	}

	@Override
	public String toString() {
		return "Frame [repeatCount=" + repeatCount + ", planes=" + planes + "]";
	}

	@Override
	public void writeTo(DataOutputStream os) throws IOException {
		os.writeShort(repeatCount);
		os.writeShort(planes.size());
		for(byte[] data : planes) {
			os.write(data);
		}
	}
	
}
