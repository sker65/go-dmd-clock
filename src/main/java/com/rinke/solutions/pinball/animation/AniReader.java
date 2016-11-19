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
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

@Slf4j
public class AniReader {
	
	public static int unsignedByte(byte b) {
	    return b & 0xFF;
	}

	public static List<Animation> readFromFile(String filename) {
		List<Animation> anis = new ArrayList<>();
		log.info("reading animations from {}",filename);
		DataInputStream is = null;
		short version = 0;
		try {
			is = new DataInputStream(new FileInputStream(filename));
			byte[] magic = new byte[4];
			is.read(magic); //
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
		CompiledAnimation a = new CompiledAnimation(AnimationType.COMPILED, name, 0, 0, 1, cycles, holdCycles);
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
			int numberOfPlanes = is.readByte();
			Frame f = new Frame();
			f.delay = delay;
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
					foundMask = readPlanes(f, numberOfPlanes, size, is2);
				} else {
					foundMask = readPlanes(f, numberOfPlanes, size, is);
				}
			} else {
				foundMask = readPlanes(f, numberOfPlanes, size, is);
			}
			if( foundMask && a.getTransitionFrom()==0) a.setTransitionFrom(i);
			i++;
			frames--;
		}
	}


	private static boolean readPlanes(Frame f, int numberOfPlanes, int size, DataInputStream is) throws IOException {
		Plane mask = null;
		int np = numberOfPlanes;
		while( np>0) {
			byte[] f1 = new byte[size];
			byte marker = is.readByte(); // type of plane
			is.readFully(f1);
			Plane p = new Plane(marker, f1);
			if( marker < numberOfPlanes ) f.planes.add( p );
			else mask = p;
			np--;
		}
		// mask plane is the last in list but first in file
		if( mask != null ) {
		    f.planes.add( mask );
		    return true;
		}
		return false;
	}


}
