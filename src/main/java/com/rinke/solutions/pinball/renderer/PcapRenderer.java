package com.rinke.solutions.pinball.renderer;

import java.io.DataInputStream;
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
    		while( ( p = pcap.readPaket()) != null ) {
    			byte[] data = new byte[p.incLen];
    			stream.read(data);
    			System.out.println(p);
    		}
    		
    		
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
