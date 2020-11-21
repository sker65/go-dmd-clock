package com.rinke.solutions.pinball.animation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.rinke.solutions.io.HeatShrinkEncoder;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AniWriter extends Worker {
	
	public static final String ANIM = "ANIM";
	private Map<Integer,Palette> palettes;
	private int version;
	private String filename;
	private List<Animation> anis;
	private Map<String,Integer> offsetMap = new HashMap<>();
	private String header = ANIM;
	public boolean writeLinearPlane = false;
	
	public AniWriter(List<Animation> anis, String filename, int version, Map<Integer,Palette> palettes, ProgressEventListener progressEvt) {
		this.anis = anis;
		this.filename = filename;
		this.version = version;
		this.palettes = palettes;
		setProgressEvt(progressEvt);
	}

	public static void write(List<Animation> anis, String filename, int version, Map<Integer,Palette> palettes) {
		new AniWriter(anis,filename,version,palettes, null).run();
	}
	
	private int getPixel(int x, int y, Frame frame, int bytesPerRow) {
		byte mask = (byte) (0b10000000 >> (x % 8));
		int v = 0;
		for (int plane = 0; plane < frame.planes.size(); plane++) {
			v += (frame.planes.get(plane).data[x / 8 + y * bytesPerRow] & mask) != 0 ? (1 << plane) : 0;
		}
		return v;
	}

	public void innerRun() {
		int planesCompressed = 0;
		int planesRaw = 0;
		DataOutputStream os = null;
		try {
			log.info("writing animations to {}",filename);
			notify(0,"writing animations to " + filename);
			FileOutputStream fos = new FileOutputStream(filename);
			os = new DataOutputStream(fos);
			os.writeBytes(header); // magic header
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
			int aniProgressInc = 100 / anis.size();
			boolean atLeastOneCompressed = false;
			for (Animation a : anis) {
				aniOffset[aniIndex] = os.size();
				offsetMap.put(a.getDesc(), os.size());
			    writeAnimation(os, a);

				DMD dmd = new DMD(a.width,a.height);
				
				int numberOfFrames = a.getFrameCount(dmd);
				os.writeShort(numberOfFrames);
				
				if( version >= 2 ) {
					writePaletteAndColor(os, a);
				}
				if( version >= 3 ) {
					os.writeByte(a.getEditMode().ordinal());
				}
				if( version >= 4 ) {
					os.writeShort(a.width);
					os.writeShort(a.height);
				}
				if( version >= 5 ) {
					// write layered masks
					List<Mask> masks = a.getMasks();
					int noOfMasks = masks!=null ? masks.size() : 0;
					os.writeShort(noOfMasks);
					for( int i = 0; i < noOfMasks; i++) {
						os.writeBoolean(masks.get(i).locked);
						os.writeShort( masks.get(i).data.length);
						os.write( masks.get(i).data);
					}
				}
				if( version >= 6 ) { // write recording link
					boolean hasLink = (a instanceof CompiledAnimation && ((CompiledAnimation) a).getRecordingLink() != null);
					if( hasLink ) {
						CompiledAnimation c = (CompiledAnimation) a;
						os.writeByte(1);
						os.writeUTF(c.getRecordingLink().associatedRecordingName);
						os.writeInt(c.getRecordingLink().startFrame);
					} else {
						os.writeByte(0);
					}
				}

				int preserveAct = a.getActFrame();
				a.restart();
				
				// write frames
				log.info("writing {} frames", numberOfFrames);
				for(int i = 0; i<numberOfFrames;i++) {
					dmd = new DMD(a.width,a.height);
					int planeSize = dmd.getPlaneSize();
					if( writeLinearPlane ) {
						planeSize = a.width*a.height; // one byte per pixel in just one plane
					}
					os.writeShort(planeSize);
					
					notify(aniIndex*aniProgressInc + (int)((float)i/numberOfFrames * aniProgressInc), "writing animation "+a.getDesc());
					Frame frame =  a.render(dmd,false);

					// delay is set per frame, equal delay is just one possibility
					os.writeShort(frame.delay);
					if(version >= 4 ) {
						os.write(frame.crc32);
					}
					int numberOfPlanes = frame.hasMask()?frame.planes.size()+1:frame.planes.size();
					if( writeLinearPlane ) {
						numberOfPlanes = 1;
					}
					os.writeByte(numberOfPlanes);
					
					if( version < 3 ) {
						writePlanes(os, frame);
					} else {
						// for indexed color with goDMD we optionally choose a one byte per pixel model with
						// up to 256 colors in palette (up to 8 planes)
						// plane marker in this case will be 0xFF linear
						if( writeLinearPlane ) {
							int bytesPerRow = dmd.getBytesPerRow();
							int w = dmd.getWidth();
							int h = dmd.getHeight();
							byte[] data = new byte[planeSize];
							// create 8 plane zero fill
							for(int x = 0; x < w; x++) {
								for(int y = 0; y < h; y++) {
									data[x+y*w] = (byte) getPixel(x, y, frame, bytesPerRow);
								}
							}
							Frame linearFrame = new Frame();
							linearFrame.delay = frame.delay;
							//int planeSize = dmd.getPlaneSize();
							//for( int j = 0; j < 8; j++) {
							//	byte[] sdata = new byte[planeSize];
							//	System.arraycopy(data, planeSize*j, sdata, 0, planeSize);
							//	linearFrame.planes.add(new Plane((byte)0xff, sdata));
							//}
							linearFrame.planes.add(new Plane((byte)0xAA, data));
							frame = linearFrame;
						}
						// for version 3 add optional compression
						boolean compress = ( frame.planes.size() > 5 ) || frame.planes.get(0).data.length>1024; // 4 planes and mask will not compressed
						os.writeBoolean(compress);
						if( !compress ) {
							int size = writePlanes(os, frame);
							planesCompressed += size;
							planesRaw += size;
						} else {
							atLeastOneCompressed = true;
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							DataOutputStream dos = new DataOutputStream(bos);
							writePlanes(dos, frame);
							dos.flush();
							dos.close();
							ByteArrayOutputStream b2 = new ByteArrayOutputStream();
							ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
							HeatShrinkEncoder enc = new HeatShrinkEncoder(10,5);
							enc.encode(bis, b2);
							log.info("writing {} compressed planes: {} / {}", frame.planes.size(), b2.size(), bos.size());
							planesCompressed += b2.size();
							planesRaw += bos.size();
							os.writeInt(b2.size());
							os.write(b2.toByteArray());
						}
					}
					if( cancelRequested ) {
						log.warn("cancel requested, leaving write loop");
						break;
					}
				}
				a.setActFrame(preserveAct);
				if( cancelRequested ) {
					log.warn("cancel requested, leaving ani loop");
					break;
				}
				aniIndex++;
				notify(aniIndex*aniProgressInc, "animation "+a.getDesc()+" written");
			}
			if( version >= 2 ) rewriteIndex(aniOffset, os, fos, startOfIndex);
			os.close();
			if( atLeastOneCompressed ) log.info("frame compression {}/{} = {}", planesRaw,planesCompressed, (float)planesCompressed/(float)planesRaw);
			log.info("done");
		} catch (IOException e) {
			log.error("problems when wrinting file {}", filename);
		} finally {
			IOUtils.closeQuietly(os);
		}
	}

	private void writePaletteAndColor(DataOutputStream os, Animation a) throws IOException {
		// write palette idx
		if( a.getPalIndex() <= 8 ) {
			log.info("writing pal index: {}",a.getPalIndex());
			os.writeShort(a.getPalIndex()); // standard palette is always 0 for now
			os.writeShort(0); // number of colors on custom palette is also 0
		} else {
			os.writeShort(a.getPalIndex()); // as custom pallette use index short max 
			log.info("writing pal index: {}",Short.MAX_VALUE);
			if( palettes.containsKey(a.getPalIndex()) ) {
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

	private void writeAnimation(DataOutputStream os, Animation a) throws IOException {
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

	private int writePlanes(DataOutputStream os, Frame r) throws IOException {
		// ensure that if mask plane is contained, it is written first
		int start = os.size();
		if( r.hasMask()) {
			os.writeByte(Plane.xMASK);
		    os.write(r.mask.data);
		}
		for(int j = 0; j < r.planes.size(); j++) {
		    os.writeByte(j);
		    os.write(r.planes.get(j).data);
		}
		return os.size()-start;
	}

	private void rewriteIndex(int[] aniOffset, DataOutputStream os,
			FileOutputStream fos, int startOfIndex) throws IOException {
		FileChannel channel = fos.getChannel();
		channel.position(startOfIndex);
		for (int i = 0; i < aniOffset.length; i++) {
			os.writeInt(aniOffset[i]);
		}
	}

	private int writeIndexPlaceholder(int count, DataOutputStream os) throws IOException {
		int startOfIndex = os.size();
		for(int i = 0; i<count; i++) os.writeInt(0);
		return startOfIndex;
	}

	public Map<String, Integer> getOffsetMap() {
		return offsetMap;
	}

	public void setHeader(String header) {
		this.header = header;
	}

}
