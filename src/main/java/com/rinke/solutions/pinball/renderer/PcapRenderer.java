package com.rinke.solutions.pinball.renderer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Frame;
import com.rinke.solutions.pinball.renderer.Pcap.Header;
import com.rinke.solutions.pinball.renderer.Pcap.Paket;

public class PcapRenderer extends Renderer {

    private static Logger LOG = LoggerFactory.getLogger(PcapRenderer.class); 

	List<Frame> frames = new ArrayList<>();

	@Override
	public Frame convert(String filename, DMD dmd, int frameNo) {
		if (frames.isEmpty())
			readImage(filename, dmd);
		return frames.get(frameNo);
	}
	
    private void readImage(String filename, DMD dmd) {
    	LittleEndianDataInputStream stream = null;
    	try {
    		if( filename.endsWith(".pcap.gz")) {
    			stream = new LittleEndianDataInputStream(new GZIPInputStream(new FileInputStream(filename)));
    		} else if( filename.endsWith(".pcap")) {
    			stream = new LittleEndianDataInputStream(new FileInputStream(filename));
    		} else {
    			throw new RuntimeException( "bad file type / file extension. *.pcap or *.pcap.gz expected");
    		}
    		
    		Pcap pcap = new Pcap(stream);
    		Header header = pcap.readHeader();
    		System.out.println(header);
    		Paket p;
    		long lastTimestamp = 0;
    		while( ( p = pcap.readPaket()) != null ) {
    			byte[] data = new byte[p.incLen];
    			stream.read(data);
    			int offset = findPinDmdMagicOffset(data);
    			Frame res = new Frame(dmd.getWidth(), dmd.getHeight(), 
    					Frame.transform(data, offset+4, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+4+512, dmd.getFrameSizeInByte()));
    			res.delay = lastTimestamp == 0 ? 0 : (int) (p.getTimestampInMillis() - lastTimestamp);
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
