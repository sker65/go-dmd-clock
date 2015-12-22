package com.rinke.solutions.pinball.renderer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Frame;
import com.rinke.solutions.pinball.renderer.Pcap.Header;
import com.rinke.solutions.pinball.renderer.Pcap.Paket;

public class PcapRenderer extends Renderer {

    private static Logger LOG = LoggerFactory.getLogger(PcapRenderer.class); 

    protected void readImage(String filename, DMD dmd) {
    	LittleEndianDataInputStream stream = null;
    	long bufSize = new File(filename).length();
    	try {
    		if( filename.endsWith(".pcap.gz")) {
    			stream = new LittleEndianDataInputStream(new GZIPInputStream(
    			        new FileInputStream(filename),(int)bufSize));
    		} else if( filename.endsWith(".pcap")) {
    			stream = new LittleEndianDataInputStream(new BufferedInputStream(
    			        new FileInputStream(filename)));
    		} else {
    			throw new RuntimeException( "bad file type / file extension. *.pcap or *.pcap.gz expected");
    		}
    		
    		Pcap pcap = new Pcap(stream);
    		Header header = pcap.readHeader();
    		System.out.println(header);
    		Paket p;
    		long lastTimestamp = 0;
    		long tc = 0;
    		while( ( p = pcap.readPaket()) != null ) {
    			byte[] data = new byte[p.incLen];
    			stream.read(data);
    			int offset = findPinDmdMagicOffset(data);
    			Frame res = new Frame(dmd.getWidth(), dmd.getHeight(), 
    					Frame.transform(data, offset+4, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+4+512, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+4+1024, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+4+1536, dmd.getFrameSizeInByte())
    					);
    			
    			//res = buildSummarizedFrame(dmd.getWidth(), dmd.getHeight(),data, offset+4);
    			
    			res.delay = lastTimestamp == 0 ? 0 : (int) (p.getTimestampInMillis() - lastTimestamp);
    			tc += res.delay;
    			res.timecode = tc;
    			if( res.delay > 1 ) {    			    
    				System.out.println("frame"+frames.size()+", delay: "+res.delay + " "+p);
    				frames.add(res);
    			}
				lastTimestamp = p.getTimestampInMillis();
    				//frameNo++;
    		}
    		this.maxFrame = frames.size();
    		
    	} catch(IOException e) {
			LOG.error("error on reading from stream for {}", filename, e);
    	} finally {
    		if( stream != null ) {
    			try {
					stream.close();
				} catch (IOException e) {
					LOG.error("error on closing stream for {}", filename, e);
				}
    		}
    	}
    	
	}

	/*private Frame buildSummarizedFrame(int width, int height, byte[] data, int offset) {
		Frame res = new Frame(width, height, null, null);
		int bytesPerPlane = width/8*height;
		for(int i = 0; i < bytesPerPlane; i++) {
			// 4 planes
			byte v1 = data[offset+i];
			byte v2 = data[offset+i+bytesPerPlane];
			byte v3 = data[offset+i+bytesPerPlane*2];
			byte v4 = data[offset+i+bytesPerPlane*3];
			for(int j = 0; j < 8; j++) {
				
			}
		}
		return res;
	}*/

	private int findPinDmdMagicOffset(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			if( data[i] == (byte)0x81 &&
					data[i+1] == (byte)0xC3 &&
					data[i+2] == (byte)0xE7 &&
					data[i+3] == 0x00
					) {
				return i;
			}	
		}
		return -1;
	}

	@Override
    public long getTimeCode(int actFrame) {
        return actFrame< frames.size() ? frames.get(actFrame).timecode:0;
    }
	
	public static void main(String[] args) {
		Renderer renderer = new PcapRenderer();
		String base = "./";
		DMD dmd = new DMD(128, 32);
		renderer.convert(base + "t2.pcap", dmd, 0);
	}



}
