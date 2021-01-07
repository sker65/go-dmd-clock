package com.rinke.solutions.pinball.renderer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;

@Slf4j
public class VPinMameRawRenderer extends Renderer {
	
	int planesPerFrame = 3;
	List<Plane> planes = new ArrayList<>();

	@Override
	public long getTimeCode(int actFrame) {
		return actFrame < frames.size() ? frames.get(actFrame).timecode : 0;
	}
	
	int color6planes_map[] = { 0, 1, 2, 2, 2, 2, 3 };
	int color8planes_map[] = { 0, 1, 2, 2, 2, 2, 2, 2, 3};
	int color12planes_map[] = { 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3 };
	
	
	void readImage(String filename, DMD dmd) {
		BufferedInputStream stream = null;
		int r = 0;
		int frameNo = 0;
		int lastTimecode = 0;
		int firstTimecode = 0;
		int currentTimecode;
		int width = 0;
		int height = 0;
		Frame res = null;
		try {
			stream = getInputStream(filename);
			byte[] header = new byte[5];
			// read header 
			r = stream.read(header);
			if( r != 5 || !validateHeader(header)) throw new RuntimeException("raw header expected");
			width = stream.read();
			height = stream.read();
			planesPerFrame = stream.read();
			int bytesPerPlane = width*height / 8;
			int vmax = 0;
			int planeIdx = 0;
			while (true) {
				byte[] ts = new byte[4];
				r = stream.read(ts);
				if( r == -1 ) break;
				// little endian
				currentTimecode = ( ((int)ts[3] & 0xFF ) <<24 ) | ( ((int)ts[2] & 0xFF)<<16 ) | ( ((int)ts[1] & 0xFF) <<8 ) | ( ( (int)ts[0] & 0xFF) <<0 );
				for( int i = 0; i <planesPerFrame; i++) {
					// read planes
					byte[] buf = new byte[bytesPerPlane];
					r = stream.read(buf);
					Plane p = new Plane((byte)i, buf);
					planes.add(p);
					if( r == -1 || r < bytesPerPlane ) break;
				}
				res = new Frame(
					new byte[dmd.getPlaneSize()],
					new byte[dmd.getPlaneSize()],
					new byte[dmd.getPlaneSize()],
					new byte[dmd.getPlaneSize()]
					);

				// aggregate to frame
				for( int pix = 0; pix < width*height; pix++) {
					int bit = (pix % 8);
					int byteIdx = pix / 8;
					int mask = (0b10000000 >> bit);
					int v = 0;
					for( int i = 0; i<planesPerFrame; i++) {
						if (planesPerFrame<6)
							v += ( planes.get(planeIdx+i).data[byteIdx] >> bit ) & 1;
						else
							v += ( planes.get(planeIdx+i).data[byteIdx] >> 7-bit ) & 1;
					}
					switch (planesPerFrame){
						case 6:
						v = color6planes_map[v];
						break;
						case 8:
						v = color8planes_map[v];
						break;
						case 12:
						v = color12planes_map[v];
						break;
						default:
						break;
					}
					if( v > vmax ) vmax = v;
					if( (v & 1) != 0 ) 
						res.planes.get(0).data[byteIdx] |= mask;
					if( (v & 2) != 0 )
						res.planes.get(1).data[byteIdx] |= mask;
					if( (v & 4) != 0 )
						res.planes.get(2).data[byteIdx] |= mask;
					if( (v & 8) != 0 )
						res.planes.get(3).data[byteIdx] |= mask;
				}
				if (planesPerFrame<6)
					for( int i = 0; i <planesPerFrame; i++) {
						planes.get(planeIdx+i).data = Frame.transform(planes.get(planeIdx+i).data); // transform plane data (reverse bit order)
					}
				res.timecode = currentTimecode -firstTimecode;
				if( firstTimecode == 0 ) firstTimecode = currentTimecode; // offset basis for timecodes
				if( lastTimecode != 0) {
					frames.get(frameNo - 1).delay = currentTimecode - lastTimecode;
				}
				lastTimecode = currentTimecode;
				frames.add(res);
				planeIdx += planesPerFrame;
				frameNo++;
				notify(50, "reading "+bareName(filename)+"@"+frameNo);
			}
			// check maximum value for v
			// if never ever more than 3 reduce number of planes
			if( vmax <= 3 ) reducePlanes(frames,2);
		} catch (IOException e) {
			throw new RuntimeException("error reading", e);
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		this.maxFrame = frameNo;
	}

	private boolean validateHeader(byte[] header) {
		return Arrays.equals( new byte[]{0x52, 0x41, 0x57, 0x00, 0x01}, header);
	}

	private void reducePlanes(List<Frame> frames, int maxNumberOfPlanes) {
		for (Frame frame : frames) {
			List<Plane> planes = frame.planes;
			while( planes.size() > maxNumberOfPlanes ) {
				planes.remove(planes.size()-1);
			}
		}
		
	}

	public List<Plane> getPlanes() {
		return planes;
	}

	public int getPlanesPerFrame() {
		return planesPerFrame;
	}

}
