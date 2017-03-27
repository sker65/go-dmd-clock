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
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AniWriter extends Worker {
	
	private List<Palette> palettes;
	private int version;
	private String filename;
	private List<Animation> anis;
	private Map<String,Integer> offsetMap;
	
	private static final int MASK_MARKER = 0x6D;
	
	public AniWriter(List<Animation> anis, String filename, int version, List<Palette> palettes, ProgressEventListener progressEvt) {
		this.anis = anis;
		this.filename = filename;
		this.version = version;
		this.palettes = palettes;
		setProgressEvt(progressEvt);
	}

	public static void writeToFile(List<Animation> anis, String filename, int version, List<Palette> palettes) {
		new AniWriter(anis,filename,version,palettes, null).run();
	}

	public void run() {
		int planesCompressed = 0;
		int planesRaw = 0;
		DataOutputStream os = null;
		try {
			log.info("writing animations to {}",filename);
			notify(0,"writing animations to " + filename);
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
			offsetMap = new HashMap<>();
			int aniProgressInc = 100 / anis.size();
			for (Animation a : anis) {
				aniOffset[aniIndex] = os.size();
				offsetMap.put(a.getDesc(), os.size());
			    writeAnimation(os, a);

				DMD dmd = new DMD(128, 32);
				
				int numberOfFrames = a.getFrameCount(dmd);
				os.writeShort(numberOfFrames);
				
				if( version >= 2 ) {
					writePaletteAndColor(os, a);
				}
				if( version >= 3 ) {
					os.writeByte(a.getEditMode().ordinal());
				}
				int preserveAct = a.getActFrame();
				a.restart();
				// write frames
				log.info("writing {} frames", numberOfFrames);
				for(int i = 0; i<numberOfFrames;i++) {
					dmd = new DMD(128, 32);
					os.writeShort(dmd.getFrameSizeInByte());
					notify(aniIndex*aniProgressInc + (int)((float)i/numberOfFrames * aniProgressInc), "writing animation "+a.getDesc());
					Frame frame =  a.render(dmd,false);

					// delay is set per frame, equal delay is just one possibility
					os.writeShort(frame.delay);
					os.writeByte(frame.planes.size());
					
					if( version < 3 ) {
						writePlanes(os, frame);
					} else {
						// for version 3 add optional compression
						boolean compress = ( frame.planes.size() > 4 );
						os.writeBoolean(compress);
						if( !compress ) {
							int size = writePlanes(os, frame);
							planesCompressed += size;
							planesRaw += size;
						} else {
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							DataOutputStream dos = new DataOutputStream(bos);
							writePlanes(dos, frame);
							dos.flush();
							dos.close();
							ByteArrayOutputStream b2 = new ByteArrayOutputStream();
							ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
							HeatShrinkEncoder enc = new HeatShrinkEncoder(10,5);
							enc.encode(bis, b2);
							//log.info("writing compressed planes: {} / {}", b2.size(), bos.size());
							planesCompressed += b2.size();
							planesRaw += bos.size();
							os.writeInt(b2.size());
							os.write(b2.toByteArray());
						}
					}
					if( cancelRequested ) break;
				}
				a.setActFrame(preserveAct);
				if( cancelRequested ) break;
				aniIndex++;
				notify(aniIndex*aniProgressInc, "animation "+a.getDesc()+" written");
			}
			if( version >= 2 ) rewriteIndex(aniOffset, os, fos, startOfIndex);
			os.close();
			log.info("frame compression {}/{} = {}", planesRaw,planesCompressed, (float)planesCompressed/(float)planesRaw);
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
		int maskPlane = searchMaskPlane(r.planes);
		if( maskPlane >= 0) {
			os.writeByte(MASK_MARKER);
		    os.write(r.planes.get(maskPlane).plane);
		}
		for(int j = 0; j < r.planes.size(); j++) {
		    // plane type (normal bit depth)
			if( j != maskPlane) {
			    os.writeByte(j);
			    os.write(r.planes.get(j).plane);
			}
		}
		return os.size()-start;
	}

	private int searchMaskPlane(List<Plane> planes) {
		for (int i = 0; i < planes.size(); i++) {
			if( planes.get(i).marker == MASK_MARKER ) return i;
		}
		return -1;
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

}
