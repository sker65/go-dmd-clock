package com.rinke.solutions.pinball.animation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;

import com.rinke.solutions.io.HeatShrinkDecoder;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameLink;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

@Slf4j
public class AniReader {
	
	public short version = 0;
	
	public static int unsignedByte(byte b) {
	    return b & 0xFF;
	}

	public List<Animation> read(String filename) {
		List<Animation> anis = new ArrayList<>();
		log.info("reading animations from {}",filename);
		DataInputStream is = null;
		try {
			is = new DataInputStream(new FileInputStream(filename));
			byte[] magic = new byte[4];
			is.read(magic);
			if( !AniWriter.ANIM.equals(new String(magic, "UTF-8")) ) {
				throw new RuntimeException("bad file format: " + filename);
			}
			version = is.readShort(); // version
			log.info("version is {}",version);
			int numberOfAnimations = is.readShort();
			log.info("reading {} animations from {}",numberOfAnimations, filename);
			if( version >= 2 ) {
				// skip index of animations
				skipIndex(is, numberOfAnimations);
			}
			while(numberOfAnimations>0) {
				CompiledAnimation a = readAnimation(is, filename);
				
				int frames = is.readShort();
				if( frames < 0 ) frames += 65536;

				if( version >= 2 ) {
					readPalettesAndColors(is, a);
				} // version 2
				if( version >= 3 ) {
					byte editMode = is.readByte();
					a.setEditMode(EditMode.fromOrdinal(editMode));
				}
				if( version >= 4 ) {
					int width = is.readShort();
					int height = is.readShort();
					a.setDimension(width, height);
				}
				if( version >= 5 ) {
					int noOfMasks = is.readShort();
					List<Mask> masks = a.getMasks();
					for( int i = 0; i < noOfMasks; i++) {
						boolean locked = is.readBoolean();
						int length = is.readShort();
						byte[] data = new byte[length];
						is.read(data);
						masks.add(new Mask(data,locked));
					}
				}
				if( version >= 6 ) { // read recording link
					if( is.readByte() == 1 ) {
						String name = is.readUTF();
						int startFrame = is.readInt();
						a.setRecordingLink(new RecordingLink(name, startFrame));
					}
				}
				readFrames( is, a, frames, version );
				numberOfAnimations--;
                log.info("reading {}",a);
				anis.add(a);
			}
		} catch (IOException e) {
			log.error("problems when reading file {}", filename,e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		log.info("successful read {} anis", anis.size());
		return anis;
	}


	private static void skipIndex(DataInputStream is, int numberOfAnimations) throws IOException {
		for( int i=0; i< numberOfAnimations; i++) is.readInt();
	}

	private static CompiledAnimation readAnimation(DataInputStream is, String name) throws IOException {
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
		CompiledAnimation a = new CompiledAnimation(AnimationType.COMPILED, name, 0, 0, 1, cycles, holdCycles, 128, 32);
		a.setRefreshDelay(refreshDelay);
		a.setClockFrom(clockFrom);
		a.setClockSmall(clockSmall);
		a.setClockXOffset(clockXOffset);
		a.setClockYOffset(clockYOffset);
		a.setClockInFront(front);
		a.setFsk(fsk);
		a.setDesc(desc);
		a.setBasePath(name);
		log.info("reading {}",a);
		return a;
	}


	private static void readPalettesAndColors(DataInputStream is, CompiledAnimation a) throws IOException {
		a.setPalIndex(is.readShort());
		log.info("reading pal index {}",a.getPalIndex());
		int numberOfColors = is.readShort();
		log.info("reading {} custom colors", numberOfColors);
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


	private static void readFrames(DataInputStream is, CompiledAnimation a, int frames, int version) throws IOException {
		log.info("reading {} frames for {}",frames, a.getDesc());
		int i = 0;
		while(frames>0) {
			int size = is.readShort(); // framesize in byte
			int delay = is.readShort();
			byte[] crc32 = new byte[4];
			if(version >= 4 ) {
				is.read(crc32);
			}
			int numberOfPlanes = is.readByte();
			Frame f = new Frame();
			f.delay = delay;
			f.setHash(crc32);
			a.addFrame(f);
			boolean foundMask = false;
			if( version >= 3 ) {
				boolean compressed = is.readBoolean();
				if( compressed ) {
					int compressedSize = is.readInt();
					byte[] inBuffer = new byte[compressedSize];
					is.read(inBuffer);
					ByteArrayOutputStream b2 = new ByteArrayOutputStream();
					ByteArrayInputStream bis = new ByteArrayInputStream(inBuffer);
					HeatShrinkDecoder dec = new HeatShrinkDecoder(10,5,1024);
					dec.decode(bis, b2);
					DataInputStream is2 = new DataInputStream(new ByteArrayInputStream(b2.toByteArray()));
					foundMask = readPlanes(f, numberOfPlanes, size, is2, version);
				} else {
					foundMask = readPlanes(f, numberOfPlanes, size, is, version);
				}
			} else {
				foundMask = readPlanes(f, numberOfPlanes, size, is, version);
			}
			if( version >= 7 ) {
				byte hasLink = is.readByte();
				if( hasLink == 1) {
					f.frameLink = new FrameLink(is.readUTF(), is.readInt());
				}
			}
			if( version >= 8 ) {
				f.keyFrame = is.readBoolean();
			}
			if( foundMask && a.getTransitionFrom()==0) a.setTransitionFrom(i);
			i++;
			frames--;
		}
	}

	/**
	 * read planes
	 * @param f
	 * @param numberOfPlanes
	 * @param size
	 * @param is
	 * @param version
	 * @return true if mask was found
	 * @throws IOException
	 */
	private static boolean readPlanes(Frame f, int numberOfPlanes, int size, DataInputStream is, int version) throws IOException {
		int np = numberOfPlanes;
		while( np>0) {
			byte[] f1 = new byte[size];
			byte marker = is.readByte(); // type of plane
			is.readFully(f1);
			Plane p = new Plane(marker, f1);
			if( marker < numberOfPlanes ) f.planes.add( p );
			else f.mask = new Mask(p.data, false);
			np--;
		}
		// mask plane is always the first in the list
		return f.hasMask();
	}


}
