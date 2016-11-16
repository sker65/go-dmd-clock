package com.rinke.solutions.pinball.animation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import com.rinke.solutions.io.HeatShrinkEncoder;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AniWriter {
	
	public static void writeToFile(List<Animation> anis, String filename, int version, List<Palette> palettes) {
		
		DataOutputStream os = null;
		try {
			log.info("writing animations to {}",filename);
			FileOutputStream fos = new FileOutputStream(filename);
			os = new DataOutputStream(fos);
			os.writeBytes("ANIM"); // magic header
			os.writeShort(version); // version
			log.info("writing version {}",version);
			os.writeShort(anis.size());
			log.info("writing {} animations", anis.size());
			int startOfIndex = 0;
			if( version >= 2 ) {
				startOfIndex = writeIndexPlaceholder(anis.size(),os);
			}
			int aniIndex = 0;
			int aniOffset[] = new int[anis.size()];
			for (Animation a : anis) {
				aniOffset[aniIndex] = os.size();
			    writeAnimation(os, a);

				DMD dmd = new DMD(128, 32);
				
				int numberOfFrames = a.getFrameCount(dmd);
				os.writeShort(numberOfFrames);
				
				if( version >= 2 ) {
					writePaletteAndColor(os, a, palettes);
				}
				if( version >= 3 ) {
					os.writeByte(a.getEditMode().ordinal());
				}
				
				a.restart();
				// write frames
				log.info("writing {} frames", numberOfFrames);
				for(int i = 0; i<numberOfFrames;i++) {
					dmd = new DMD(128, 32);
					os.writeShort(dmd.getFrameSizeInByte());
					
					Frame frame =  a.render(dmd,false);
					
					// number of planes (normally 2, and mask optionally)
					if( frame.planes.size()==3) {
						os.writeShort(a.getTransitionDelay());
						os.writeByte(3);
						// if mask, use plane type 0x6d
						os.writeByte(0x6d); // mask marker
						os.write(frame.planes.get(2).plane);
					} else {
						// delay is set per frame, equal delay is just one possibility
						os.writeShort(frame.delay);
						os.writeByte(frame.planes.size());
					}
					
					if( version < 3 ) {
						writePlanes(os, frame);
					} else {
						// for version 3 add optional compression
						boolean compress = ( frame.planes.size() > 4 );
						os.writeBoolean(compress);
						if( !compress ) {
							writePlanes(os, frame);
						} else {
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							DataOutputStream dos = new DataOutputStream(bos);
							writePlanes(dos, frame);
							dos.close();
							ByteArrayOutputStream b2 = new ByteArrayOutputStream();
							ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
							HeatShrinkEncoder enc = new HeatShrinkEncoder(10,5);
							enc.encode(bis, b2);
							os.writeInt(b2.size());
							os.write(b2.toByteArray());
						}
					}
					
				}
				aniIndex++;
			}
			if( version >= 2 ) rewriteIndex(aniOffset, os, fos, startOfIndex);
			os.close();
			log.info("done");
		} catch (IOException e) {
			log.error("problems when wrinting file {}", filename);
		} finally {
			if( os != null ) {
				try {
					os.close();
				} catch (IOException e) {
					log.error("problems when closing file {}", filename);
				}
			}
		}
	}

	private static void writePaletteAndColor(DataOutputStream os, Animation a, List<Palette> palettes) throws IOException {
		// write palette idx
		if( a.getPalIndex() <= 8 ) {
			log.info("writing pal index: {}",a.getPalIndex());
			os.writeShort(a.getPalIndex()); // standard palette is always 0 for now
			os.writeShort(0); // number of colors on custom palette is also 0
		} else {
			os.writeShort(Short.MAX_VALUE); // as custom pallette use index short max 
			log.info("writing pal index: {}",Short.MAX_VALUE);
			if( a.getPalIndex() < palettes.size() ) {
				Palette pal = palettes.get(a.getPalIndex());
				os.writeShort(pal.numberOfColors);
				log.info("writing {} custom colors",pal.numberOfColors);
				for(RGB col: pal.colors) {
					os.writeByte(col.red);
					os.writeByte(col.green);
					os.writeByte(col.blue);
				}
			} else {
				os.writeShort(0);
			}
		}
	}

	private static void writeAnimation(DataOutputStream os, Animation a) throws IOException {
		log.info("writing {}",a);
		// write meta data
		os.writeUTF(a.getDesc());
		// write transition name???
		//os.writeUTF();
		os.writeShort(a.getCycles());
		os.writeShort(a.getHoldCycles());
		// clock while animating
		os.writeShort(a.getClockFrom()-a.getStart());
		os.writeBoolean(a.isClockSmall());
		os.writeBoolean(a.isClockInFront());
		os.writeShort(a.getClockXOffset());
		os.writeShort(a.getClockYOffset());
		
		os.writeShort(a.getRefreshDelay());
		os.writeByte(a.getType().ordinal());
		
		os.writeByte(a.getFsk());
	}

	private static void writePlanes(DataOutputStream os, Frame r) throws IOException {
		// transform in target format
		//os.write(dmd.transformFrame1(frameSet.frame1));
		//os.write(dmd.transformFrame1(frameSet.frame2));
		for(int j = 0; j < r.planes.size(); j++) {
		    // plane type (normal bit depth)
		    os.writeByte(j);
		    os.write(r.planes.get(j).plane);
		}
	}

	private static void rewriteIndex(int[] aniOffset, DataOutputStream os,
			FileOutputStream fos, int startOfIndex) throws IOException {
		FileChannel channel = fos.getChannel();
		channel.position(startOfIndex);
		for (int i = 0; i < aniOffset.length; i++) {
			os.writeInt(aniOffset[i]);
		}
	}

	private static int writeIndexPlaceholder(int count, DataOutputStream os) throws IOException {
		int startOfIndex = os.size();
		for(int i = 0; i<count; i++) os.writeInt(0);
		return startOfIndex;
	}

}
