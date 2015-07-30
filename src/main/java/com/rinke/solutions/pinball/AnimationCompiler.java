package com.rinke.solutions.pinball;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class that compiles an animation into a binary file 
 * @author sr
 */
public class AnimationCompiler {

	private static Logger LOG = LoggerFactory.getLogger(AnimationCompiler.class); 
	
	public static List<Animation> readFromCompiledFile(String filename) {
		List<Animation> anis = new ArrayList<>();
		LOG.info("reading animations from {}",filename);
		DataInputStream is = null;
		try {
			is = new DataInputStream(new FileInputStream(filename));
			byte[] magic = new byte[4];
			is.read(magic); //
			is.readShort(); // version
			int count = is.readShort();
			LOG.info("reading {} animations from {}",count, filename);
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
				AnimationType type = AnimationType.values()[is.readByte()];
				int fsk = is.readByte();
				
				// read complied animations
				CompiledAnimation a = new CompiledAnimation(type, filename, 0, 0, 1, cycles, holdCycles);
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
				int i = 0;
				while(frames>0) {
					int size = is.readShort(); // framesize in byte
					int delay = is.readShort();
					int numberOfPlanes = is.readByte();
					a.addFrame(new Frame(delay,128,32));
					Plane mask = null;
					while( numberOfPlanes>0) {
						byte[] f1 = new byte[size];
						byte marker = is.readByte(); // type of plane
						is.readFully(f1);
						Plane p = new Plane(marker, f1);
						if( marker <=1 ) a.addPlane(i, p );
						else mask = p;
						numberOfPlanes--;
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
	
	public static void main(String[] args)  {
//		List<Animation> anis = AnimationFactory.buildAnimations("logo.properties");
//		String filename = "logo.ani";
		List<Animation> anis = AnimationFactory.buildAnimations("animations.properties");
		String filename = "foo.ani";
		writeToCompiledFile(anis, filename);
	}

	public static void writeToCompiledFile(List<Animation> anis, String filename) {
		DataOutputStream os = null;
		try {
			LOG.info("writing animations to {}",filename);
			os = new DataOutputStream(new FileOutputStream(filename));
			os.writeBytes("ANIM"); // magic header

			os.writeShort(1); // version
			
			os.writeShort(anis.size());
			LOG.info("writing {} animations", anis.size());
			for (Animation a : anis) {
			    LOG.info("writing {}",a);
				// write meta data
				os.writeUTF(a.getDesc());
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
						os.writeByte(2);
					}
					
					// if mask, use plane type 0x6d
					
					// transform in target format
					//os.write(dmd.transformFrame1(frameSet.frame1));
					//os.write(dmd.transformFrame1(frameSet.frame2));
					// plane type (normal bit depth)
					os.writeByte(0);
					os.write(r.planes.get(0).plane);
					// plane type (normal bit depth)
					os.writeByte(1);
					os.write(r.planes.get(1).plane);
				}
			}
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
	
}
