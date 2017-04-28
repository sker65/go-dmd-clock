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

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;

// als parameter in der Steuerdatei sollten
// die helligkeits schwellen angebbar sein
@Slf4j
public class VPinMameRenderer extends Renderer {

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
			//Map<Integer,Integer> count1 = new HashMap<>();
			//Map<Integer,Integer> count2 = new HashMap<>();
			while (line != null) {
				if (line.startsWith("0x")) {
					long newTs = Long.parseLong(line.substring(2), 16);	
					if (frameNo > 0 && lastTimeStamp > 0) {
						//System.out.println(newTs+":"+(newTs - lastTimeStamp));
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
					log.trace("reading frame: " + frameNo);
					j = 0;
					line = stream.readLine();
					continue;
				}
				for (int i = 0; i<line.length(); i++) {
					int k = i;
					if( lineLenght > dmd.getWidth()){
						//v1 = Integer.parseInt(line.substring(i,i+1), 16);
						//inc(count1,v1);
						i++; // skip every other byte
						k >>= 1;
					}
					int bit = (k % 8);
					int b = (k >> 3);
					int mask = (0b10000000 >> bit);
					int v = hex2int(line.charAt(i));
//					inc(count2,v);
					if( v > vmax ) vmax = v;
					if( (v & 1) != 0 ) 
						res.planes.get(0).data[j + b] |= mask;
					if( (v & 2) != 0 )
						res.planes.get(1).data[j + b] |= mask;
					if( (v & 4) != 0 )
						res.planes.get(2).data[j + b] |= mask;
					if( (v & 8) != 0 )
						res.planes.get(3).data[j + b] |= mask;
				}
				j += dmd.getBytesPerRow();
				line = stream.readLine();
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

	private int hex2int(char ch) {
		int r = ch - '0';
		if( ch >= 'A' ) r -= 'A' - '0';
		if( ch >= 'a' ) r -= 'a' - 'A';
		return r;
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
