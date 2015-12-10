package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class Frame implements Model {
	public long timecode;
	public List<byte[]> planes;
	
	public Frame(long timecode, List<byte[]> planes) {
		super();
		this.timecode = timecode;
		this.planes = planes;
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
