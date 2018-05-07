package com.rinke.solutions.pinball.renderer;

import java.io.FileInputStream;
import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;

public class DMDFRenderer extends Renderer {

	DmdSize size = DmdSize.Size128x32;
	
	public Frame convert(String filename, DMD dmd, int frameNo) {
		LittleEndianDataInputStream is = null;
		try {
			is = new LittleEndianDataInputStream(new FileInputStream(filename));
			
			byte[] header = new byte[8];
			is.read(header );
			// TODO verify header
			int width = is.readShort();
			int height = is.readShort();
			int maxFrames = is.readShort();
			if( frameNo <= maxFrames ) {
				is.skip( ( width*height + 2)*frameNo );
			}

			int w = is.readShort();
			if( w == width ) {
				byte[] f1 = new byte[dmd.getPlaneSizeInByte()];
				byte[] f2 = new byte[dmd.getPlaneSizeInByte()];
				byte[] buf = new byte[width*height];
				is.read(buf);
				for( int y = 0; y<height; y++) {
					for( int x = 0; x<width; x++) {
						if( buf[y*width+x] == 1 ) {
							f1[y*dmd.getBytesPerRow() + x / 8] |= (size.width >> (x % 8));
						} else if( buf[y*width+x] == 2 ) {
							f2[y*dmd.getBytesPerRow() + x / 8] |= (size.width >> (x % 8));
						} else if( buf[y*width+x] == 4 ) {
							f1[y*dmd.getBytesPerRow() + x / 8] |= (size.width >> (x % 8));
							f2[y*dmd.getBytesPerRow() + x / 8] |= (size.width >> (x % 8));
						}
					}
				}
				//dmd.setFrames(f1, f2);
				return new Frame(f1, f2);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if( is != null ) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}


}
