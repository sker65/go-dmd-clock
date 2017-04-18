package com.rinke.solutions.pinball.renderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;

// als parameter in der Steuerdatei sollten
// die helligkeits schwellen angebbar sein

public class VPinMameRenderer extends Renderer {

	private static Logger LOG = LoggerFactory.getLogger(VPinMameRenderer.class);

	@Override
	public long getTimeCode(int actFrame) {
		return actFrame < frames.size() ? frames.get(actFrame).timecode : 0;
	}

	void readImage(String filename, DMD dmd) {
		BufferedReader stream = null;
		int frameNo = 0;
		int timecode = 0;
		long lastTimeStamp = 0;
		try {
			stream = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(new File(filename)))));
			String line = stream.readLine();
			Frame res = new Frame(
					new byte[dmd.getPlaneSizeInByte()],
					new byte[dmd.getPlaneSizeInByte()],
					new byte[dmd.getPlaneSizeInByte()],
					new byte[dmd.getPlaneSizeInByte()]);

			int j = 0;
			int vmax = 0;
			Map<Integer,Integer> count1 = new HashMap<>();
			Map<Integer,Integer> count2 = new HashMap<>();
			while (line != null) {
				if (line.startsWith("0x")) {
					long newTs = Long.parseLong(line.substring(2), 16);
					if (frameNo > 0 && lastTimeStamp > 0) {
						frames.get(frameNo - 1).delay = (int) (newTs - lastTimeStamp);
						timecode += (newTs - lastTimeStamp);
						res.timecode = timecode;
					}
					lastTimeStamp = newTs;
					line = stream.readLine();
					continue;
				}
				int lineLenght = line.length();
				if (lineLenght == 0) {
					frames.add(res);
					frameNo++;
					res = new Frame(
							new byte[dmd.getPlaneSizeInByte()],
							new byte[dmd.getPlaneSizeInByte()],
							new byte[dmd.getPlaneSizeInByte()],
							new byte[dmd.getPlaneSizeInByte()]
							);
					LOG.trace("reading frame: " + frameNo);
					j = 0;
					line = stream.readLine();
					continue;
				}
				
				for (int i = 0; i<line.length(); i++) {
					//char c = line.charAt(i);
					int k = i;
					if( lineLenght > 128){
						//v1 = Integer.parseInt(line.substring(i,i+1), 16);
						//inc(count1,v1);
						i++; // skip every other byte
						k >>= 1;
					}
					int bit = (k % 8);
					int b = k / 8;
					int mask = PinDmdEditor.DMD_WIDTH >> bit;
					int v = Integer.parseInt(line.substring(i,i+1), 16);
//					inc(count2,v);
					if( v > vmax ) vmax = v;
					if( (v & 1) != 0 )
						res.planes.get(0).plane[j + b] |= mask;
					if( (v & 2) != 0 )
						res.planes.get(1).plane[j + b] |= mask;
					if( (v & 4) != 0 )
						res.planes.get(2).plane[j + b] |= mask;
					if( (v & 8) != 0 )
						res.planes.get(3).plane[j + b] |= mask;
				}
				j += 16;
				line = stream.readLine();
			}
			// check maximum value for v
			// if never ever more than 3 reduce number of planes
			if( vmax <= 3 ) reducePlanes(frames,2);
		} catch (IOException e) {
			e.printStackTrace();
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

	private void inc(Map<Integer, Integer> map, int v) {
		if( map.containsKey(v)) {
			map.put(v, map.get(v)+1);
		} else {
			map.put(v, 1);
		}
		
	}

	private void reducePlanes(List<Frame> frames, int maxNumberOfPlanes) {
		for (Frame frame : frames) {
			List<Plane> planes = frame.planes;
			while( planes.size() > maxNumberOfPlanes ) {
				planes.remove(planes.size()-1);
			}
		}
		
	}

}
