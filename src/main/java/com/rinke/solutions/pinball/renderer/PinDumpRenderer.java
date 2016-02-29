package com.rinke.solutions.pinball.renderer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DeviceMode;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.renderer.Pcap.Header;
import com.rinke.solutions.pinball.renderer.Pcap.Paket;

public class PinDumpRenderer extends Renderer {

    private static Logger LOG = LoggerFactory.getLogger(PinDumpRenderer.class); 

    void readImage(String filename, DMD dmd) {
    	InputStream stream = null;
    	long bufSize = new File(filename).length();
    	try {
    		if( filename.endsWith(".dump.gz")) {
    			stream = new GZIPInputStream(
    			        new FileInputStream(filename),(int)bufSize);
    		} else if( filename.endsWith(".dump")) {
    			stream = new BufferedInputStream(
    			        new FileInputStream(filename));
    		} else {
    			throw new RuntimeException( "bad file type / file extension. *.dump or *.dump.gz expected");
    		}
    		
    		int lastTimestamp = 0;
    		DeviceMode deviceMode = DeviceMode.forOrdinal(stream.read());
    		byte[] tcBuffer = new byte[4];
    		int tc = 0;
    		while( stream.available() > 0 ) {
    			stream.read(tcBuffer);
    			tc = tcBuffer[0] << 24 + tcBuffer[1] << 16 + tcBuffer[2] << 8 +tcBuffer[3]; 
    			int numberOfFrames = 4;
    			int buflen = 512*numberOfFrames;
    			byte[] data = new byte[buflen];
    			stream.read(data);
    			int offset = 0;
    			Frame res = new Frame( 
    					Frame.transform(data, offset, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+512, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+1024, dmd.getFrameSizeInByte()),
    					Frame.transform(data, offset+1536, dmd.getFrameSizeInByte())
    					);
    			
    			//res = buildSummarizedFrame(dmd.getWidth(), dmd.getHeight(),data, offset+4);
    			
    			res.delay = lastTimestamp == 0 ? 0 : (int) (tc - lastTimestamp);
    			tc += res.delay;
    			res.timecode = tc;
    			if( res.delay > 1 ) {    			    
    				//System.out.println("frame"+frames.size()+", delay: "+res.delay + " "+p);
    				LOG.debug("Frame {}", res);
    				frames.add(res);
    			}
				lastTimestamp = tc;
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

	@Override
    public long getTimeCode(int actFrame) {
        return actFrame< frames.size() ? frames.get(actFrame).timecode:0;
    }
	
	public static void main(String[] args) {
		Renderer renderer = new PinDumpRenderer();
		String base = "./";
		DMD dmd = new DMD(128, 32);
		renderer.convert(base + "t2.pcap", dmd, 0);
	}



}
