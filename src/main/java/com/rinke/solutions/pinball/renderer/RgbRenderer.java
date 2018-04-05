package com.rinke.solutions.pinball.renderer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

@Slf4j
public class RgbRenderer extends Renderer {

	@Override
	public long getTimeCode(int actFrame) {
		return actFrame < frames.size() ? frames.get(actFrame).timecode : 0;
	}
	
	Frame createFrame( DMD dmd ) {
		Frame res = new Frame();
		// for true color create 24 planes
		for( int i = 0; i<24; i++)
			res.planes.add(new Plane((byte)i, new byte[dmd.getPlaneSizeInByte()]));
		return res;
	}

	void readImage(String filename, DMD dmd) {
		BufferedInputStream stream = null;
		int frameNo = 0;
		int timecode = 0;
		long lastTimeStamp = 0;
		try {
			stream = new BufferedInputStream(new GZIPInputStream(
							new FileInputStream(new File(filename))));
			
			Frame res = createFrame(dmd);

			while ( true ) {
				byte[] ts = new byte[4];
				stream.read(ts);
				long newTs = (ts[3]<<24) + (ts[2] << 16) + (ts[1] << 8) + ts[0];
				if (frameNo > 0 && lastTimeStamp > 0) {
					//System.out.println(newTs+":"+(newTs - lastTimeStamp));
					frames.get(frameNo - 1).delay = (int) (newTs - lastTimeStamp);
					timecode += (newTs - lastTimeStamp);
					res.timecode = timecode;
				}
				lastTimeStamp = newTs;
				int frameSize = dmd.getWidth()*dmd.getHeight()*3;
				byte[] rgb = new byte[frameSize];
				int read = stream.read(rgb);
				if( read == frameSize ) {
					for( int x = 0; x < dmd.getWidth(); x++ ) {
						for( int y = 0; y < dmd.getHeight(); y++ ) {
							int r = rgb[(x+y*dmd.getWidth())*3];
							int g = rgb[(x+y*dmd.getWidth())*3+1];
							int b = rgb[(x+y*dmd.getWidth())*3+2];
							int v = (r<<16) + (g<<8) + b;
							int bit = (x % 8);
							int mask = (0b10000000 >> bit);
							int o = (x >> 3);
							for( int k = 0; k < 24; k++) {
								if( (v & (1 << k)) != 0 ) 
									res.planes.get(k).data[y*dmd.getBytesPerRow() + o] |= mask;
							}
						}
					}
					frames.add(res);
					frameNo++;
					res = createFrame(dmd);
				} else {
					break;
				}
			// check maximum value for v
			// if never ever more than 3 reduce number of planes
			} // while
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

	int hex2int(char ch) {
		if( ch >= '0' && ch <= '9') return ch - '0';
		if( ch >= 'A' && ch <= 'F') return ch -'A' + 10;
		if( ch >= 'a' && ch <= 'f') return ch -'a' + 10;
		return 0;
	}

	/*private void inc(Map<Integer, Integer> map, int v) {
		if( map.containsKey(v)) {
			map.put(v, map.get(v)+1);
		} else {
			map.put(v, 1);
		}
		
	}*/

}
