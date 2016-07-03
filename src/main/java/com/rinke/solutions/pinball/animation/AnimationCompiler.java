package com.rinke.solutions.pinball.animation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

/**
 * class that compiles an animation into a binary file 
 * @author sr
 */
public class AnimationCompiler {

	private static Logger LOG = LoggerFactory.getLogger(AnimationCompiler.class);
	private int startOfIndex;
	
	public int unsignedByte(byte b) {
	    return b & 0xFF;
	}

	
	public List<Animation> readFromCompiledFile(String filename) {
		List<Animation> anis = new ArrayList<>();
		LOG.info("reading animations from {}",filename);
		DataInputStream is = null;
		short version = 0;
		try {
			is = new DataInputStream(new FileInputStream(filename));
			byte[] magic = new byte[4];
			is.read(magic); //
			version = is.readShort(); // version
			int count = is.readShort();
			LOG.info("reading {} animations from {}",count, filename);
			if( version >= 2 ) {
				// skip index of animations
				for( int i=0; i< count; i++) is.readInt();
			}
			while(count>0) {
				String desc = is.readUTF();
				int cycles = is.readShort();
				int holdCycles = is.readShort();
				int clockFrom = is.readShort();
				boolean clockSmall = is.readBoolean();
				boolean front = is.readBoolean();
				int clockXOffset = is.readShort();
				int clockYOffset = is.readShort();
				int refreshDelay = is.readShort();
				is.readByte(); // ignore type (when reread its always compiled)
				int fsk = is.readByte();
				
				// read complied animations
				CompiledAnimation a = new CompiledAnimation(AnimationType.COMPILED, filename, 0, 0, 1, cycles, holdCycles);
				a.setRefreshDelay(refreshDelay);
				a.setClockFrom(clockFrom);
				a.setClockSmall(clockSmall);
				a.setClockXOffset(clockXOffset);
				a.setClockYOffset(clockYOffset);
				a.setClockInFront(front);
				a.setFsk(fsk);
				a.setDesc(desc);
				a.setBasePath(filename);
				
				int frames = is.readShort();
				if( frames < 0 ) frames += 65536;

				if( version >= 2 ) {
					a.setPalIndex(is.readShort());
					int numberOfColors = is.readShort();
					if( numberOfColors > 0) {
						RGB[] rgb = new RGB[numberOfColors];
						for( int i = 0; i < numberOfColors; i++) {
							rgb[i] = new RGB(
									unsignedByte(is.readByte()),
									unsignedByte(is.readByte()),
									unsignedByte(is.readByte()));
						}
						a.setAniColors(rgb);
					}
				}

				LOG.info("reading {} frames for {}",frames, desc);
				int i = 0;
				while(frames>0) {
					int size = is.readShort(); // framesize in byte
					int delay = is.readShort();
					int numberOfPlanes = is.readByte();
					int np = numberOfPlanes;
					Frame f = new Frame();
					f.delay = delay;
					a.addFrame(f);
					Plane mask = null;
					while( np>0) {
						byte[] f1 = new byte[size];
						byte marker = is.readByte(); // type of plane
						is.readFully(f1);
						Plane p = new Plane(marker, f1);
						if( marker < numberOfPlanes ) a.addPlane(i, p );
						else mask = p;
						np--;
					}
					// mask plane is the last in list but first in file
					if( mask != null ) {
					    a.addPlane(i, mask );
					    if( a.getTransitionFrom()==0) a.setTransitionFrom(i);
					}
					i++;
					frames--;
				}
				count--;
                LOG.info("reading {}",a);
				anis.add(a);
			}
		} catch (IOException e) {
			LOG.error("problems when reading file {}", filename,e);
		} finally {
			if( is != null ) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.error("problems when closing file {}", filename);
				}
			}
		}
		LOG.info("successful read {} anis", anis.size());
		return anis;
	}

	public void writeToCompiledFile(List<Animation> anis, String filename, int version, List<Palette> palettes) {
		DataOutputStream os = null;
		try {
			LOG.info("writing animations to {}",filename);
			FileOutputStream fos = new FileOutputStream(filename);
			os = new DataOutputStream(fos);
			os.writeBytes("ANIM"); // magic header
			os.writeShort(version); // version
			os.writeShort(anis.size());
			LOG.info("writing {} animations", anis.size());
			if( version >= 2 ) {
				writeIndexPlaceholder(anis.size(),os);
			}
			int aniIndex = 0;
			int aniOffset[] = new int[anis.size()];
			for (Animation a : anis) {
				aniOffset[aniIndex] = os.size();
			    LOG.info("writing {}",a);
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

				DMD dmd = new DMD(128, 32);
				
				int count = a.getFrameCount(dmd);
				os.writeShort(count);
				if( version >= 2 ) {
					// write palette idx
					if( a.getPalIndex() <= 8 ) {
						os.writeShort(a.getPalIndex()); // standard palette is always 0 for now
						os.writeShort(0); // number of colors on custom palette is also 0
					} else {
						os.writeShort(Short.MAX_VALUE); // as custom pallette use index short max 
						if( a.getPalIndex() < palettes.size() ) {
							Palette pal = palettes.get(a.getPalIndex());
							os.writeShort(pal.numberOfColors);
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
				
				a.restart();
				// write frames
				LOG.info("writing {} frames", count);
				for(int i = 0; i<count;i++) {
					dmd = new DMD(128, 32);
					os.writeShort(dmd.getFrameSizeInByte());
					Frame r =  a.render(dmd,false);
					
					// number of planes (normally 2, and mask optionally)
					if( r.planes.size()==3) {
						os.writeShort(a.getTransitionDelay());
						os.writeByte(3);
						os.writeByte(0x6d); // mask marker
						os.write(r.planes.get(2).plane);
					} else {
						// delay is set per frame, equal delay is just one possibility
						os.writeShort(r.delay);
						os.writeByte(r.planes.size());
					}
					
					// if mask, use plane type 0x6d
					
					// transform in target format
					//os.write(dmd.transformFrame1(frameSet.frame1));
					//os.write(dmd.transformFrame1(frameSet.frame2));
					for(int j = 0; j < r.planes.size(); j++) {
	                    // plane type (normal bit depth)
	                    os.writeByte(j);
	                    os.write(r.planes.get(j).plane);
					}
				}
				aniIndex++;
			}
			if( version >= 2 ) rewriteIndex(aniOffset, os, fos);
			os.close();
			LOG.info("done");
		} catch (IOException e) {
			LOG.error("problems when wrinting file {}", filename);
		} finally {
			if( os != null ) {
				try {
					os.close();
				} catch (IOException e) {
					LOG.error("problems when closing file {}", filename);
				}
			}
		}
	}

	private void rewriteIndex(int[] aniOffset, DataOutputStream os,
			FileOutputStream fos) throws IOException {
		FileChannel channel = fos.getChannel();
		channel.position(this.startOfIndex);
		for (int i = 0; i < aniOffset.length; i++) {
			os.writeInt(aniOffset[i]);
		}
	}

	private void writeIndexPlaceholder(int count, DataOutputStream os) throws IOException {
		startOfIndex = os.size();
		for(int i = 0; i<count; i++) os.writeInt(0);
	}
	
}
