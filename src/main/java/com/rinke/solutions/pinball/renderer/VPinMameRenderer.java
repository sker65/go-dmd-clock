package com.rinke.solutions.pinball.renderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Editor;
import com.rinke.solutions.pinball.Frame;

// als parameter in der Steuerdatei sollten
// die helligkeits schwellen angebbar sein

public class VPinMameRenderer extends Renderer {
    
    private static Logger LOG = LoggerFactory.getLogger(VPinMameRenderer.class); 

	List<Frame> frames = new ArrayList<>();

	@Override
	public Frame convert(String filename, DMD dmd, int frameNo) {

		if (frames.isEmpty())
			readImage(filename, dmd);
		return frames.get(frameNo);
	}

	private void readImage(String filename, DMD dmd) {
		BufferedReader stream = null;
		int frameNo = 0;
		long lastTimeStamp=0L;
		try {
			stream = new BufferedReader( new InputStreamReader(
					new GZIPInputStream(new FileInputStream(new File(filename)))));
			String line = stream.readLine();
			Frame res = new Frame(dmd.getWidth(), dmd.getHeight(), 
					new byte[dmd.getFrameSizeInByte()], new byte[dmd.getFrameSizeInByte()]);

			int j=0;
			while(line!=null) {
				if( line.startsWith("0x")) {
					long newTs = Long.parseLong(line.substring(2), 16);
					if( frameNo>0 && lastTimeStamp >0) {
					    frames.get(frameNo-1).delay = (int) (newTs - lastTimeStamp);
					}
					lastTimeStamp = newTs;
					line = stream.readLine();
					continue;
				}
				if( line.length()==0 ) {
					frames.add(res);
					frameNo++;
					res = new Frame(dmd.getWidth(), dmd.getHeight(), 
							new byte[dmd.getFrameSizeInByte()], new byte[dmd.getFrameSizeInByte()]);
					LOG.debug("reading frame: "+frameNo);
					j = 0;
					line = stream.readLine();
					continue;
				}
				for( int i=0; i<line.length();i++) {
					char c = line.charAt(i);
					int bit = (i % 8);
					int b = i/8;
					int mask = 128 >> bit;
					if( c == '1') {
						res.planes.get(0).plane[j+b] |= mask;
					} else if( c == '2') {
						res.planes.get(1).plane[j+b] |= mask; 
					} else if( c == '3') {
						res.planes.get(0).plane[j+b] |= mask;
						res.planes.get(1).plane[j+b] |= mask;
						
					}
				}
				j += 16;
				line = stream.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if( stream != null )
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		this.maxFrame = frameNo;
	}

	public static void main(String[] args) {
		Renderer renderer = new VPinMameRenderer();
		String base = "/home/sr/Downloads/Pinball/DMDpaint/";
		DMD dmd = new DMD(128, 32);
		renderer.convert(base + "ezgif-645182047.gif", dmd, 0);
	}

}
