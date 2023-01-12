// Serum file reader
// See https://github.com/zesinger/libserum 

package com.rinke.solutions.pinball.animation;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.model.Scene;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CRomLoader {

	public enum FrameFormat {
		Gray4, Gray2
	}

	private static int MAX_DYNA_4COLS_PER_FRAME = 16; // max number of color sets for dynamic content for each frame
	private static int MAX_SPRITE_SIZE = 128; // maximum size of the sprites
	private static int MAX_SPRITES_PER_FRAME = 32; // maximum amount of sprites to look for per frame
	private static int MAX_COLOR_ROTATIONS = 8; // maximum amount of color rotations per frame
	private static int MAX_SPRITE_DETECT_AREAS = 4; // maximum number of areas to detect the sprite
	
	static String bareName(String filename) {
		String b = new File(filename).getName();
		int i = b.lastIndexOf('.');
		return i==-1?b:b.substring(0, i);
	}
	
	static Frame createRGBFrame( DMD dmd ) {
		Frame res = new Frame();
		// for true color create 24 planes
		for( int i = 0; i<24; i++)
			res.planes.add(new Plane((byte)i, new byte[dmd.getPlaneSize()]));
		return res;
	}
	
	public static int unsignedByte(byte b) {
	    return b & 0xFF;
	}

	public static Object loadProject(String filename) {
		Object res = null;
		try { 

			ZipFile zipFile = new ZipFile(filename);
	
			log.debug("opening file {}", filename);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
	
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				FrameFormat From;
				if (entry.getName().endsWith(".cRom")) {
					log.debug("found cRom file {}", entry.getName());
					InputStream stream = zipFile.getInputStream(entry);
					LittleEndianDataInputStream reader = new LittleEndianDataInputStream (stream);
	
					// now read everything see
					// https://github.com/zesinger/dmd-extensions/blob/16ac4b846452f24d5bfe693be3ff510ada973ece/LibDmd/Converter/Serum/Serum.cs
					byte[] filenameData = new byte[64];
					reader.read(filenameData);
					
					int sizeheader = reader.readInt();
					log.debug("header size {}", sizeheader);
					int FWidth = reader.readInt();
					int FHeight = reader.readInt();
					int NFrames = reader.readInt();
					int NOColors = reader.readInt();
					log.debug("w,h: {},{}, frames: {}, colors: {}", FWidth,FHeight,NFrames,NOColors);
					if (NOColors == 16)
						From = FrameFormat.Gray4;
					else
						From = FrameFormat.Gray2;
					int NCColors = reader.readInt();
					int NCompMasks = reader.readInt();
					int NMovMasks = reader.readInt();
					int NSprites = reader.readInt();

					int[] HashCodes = new int[NFrames];
					for (int ti = 0; ti < NFrames; ti++)
						HashCodes[ti] = reader.readInt();
					
					byte[] ShapeCompMode = new byte[NFrames];
					reader.readFully(ShapeCompMode);
					byte[] CompMaskID = new byte[NFrames];
					reader.readFully(CompMaskID);
					byte[] MovRctID = new byte[NFrames];
					reader.readFully(MovRctID);
					if (NCompMasks > 0) {
						byte[] CompMasks = new byte[NCompMasks * FHeight * FWidth];
						reader.readFully(CompMasks);
					}
					if (NMovMasks > 0) {
						byte[] MovRcts = new byte[NMovMasks * FHeight * FWidth];
						reader.readFully(MovRcts);
					}
					
					java.util.List<Palette> CPal = new ArrayList<>();
					
					for(int palidx = 0; palidx < NFrames; palidx++) {
						RGB[] cols = new RGB[NCColors];
						for( int i = 0; i < NCColors; i++) {
							cols[i] = new RGB(
									unsignedByte(reader.readByte()),
									unsignedByte(reader.readByte()),
									unsignedByte(reader.readByte()));
						}
						String name = "new" + UUID.randomUUID().toString().substring(0, 4);
						Palette newPalette = new Palette(cols, palidx, name);
						CPal.add(newPalette);
					}

					byte[] CFrames = new byte[NFrames * FHeight * FWidth];
					reader.readFully(CFrames);
					byte[] DynaMasks = new byte[NFrames * FHeight * FWidth];
					reader.readFully(DynaMasks);
					byte[] Dyna4Cols = new byte[NFrames * MAX_DYNA_4COLS_PER_FRAME * NOColors];
					reader.readFully(Dyna4Cols);
					byte[] FrameSprites = new byte[NFrames * MAX_SPRITES_PER_FRAME];
					reader.readFully(FrameSprites);
					byte[] SpriteDescriptionsO = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE];
					byte[] SpriteDescriptionsC = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE];
					for (int ti = 0; ti < NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE; ti++) {
						SpriteDescriptionsC[ti] = reader.readByte();
						SpriteDescriptionsO[ti] = reader.readByte();
					}
					byte[] ActiveFrames=new byte[NFrames];
					reader.readFully(ActiveFrames);
					byte[] ColorRotations=new byte[NFrames*3*MAX_COLOR_ROTATIONS];
					reader.readFully(ColorRotations);
					// WARN in java there is no uint -> we use int instead
					int[] SpriteDetDwords = new int[NSprites * MAX_SPRITE_DETECT_AREAS];
					for (int ti = 0; ti < NSprites * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetDwords[ti] = reader.readInt();
					
					// in java there is no unsigned int or uint16 so we use normal int array, but read unsigned int
					int[] SpriteDetDwordPos = new int[NSprites * MAX_SPRITE_DETECT_AREAS];
					for (int ti = 0; ti < NSprites * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetDwordPos[ti] = reader.readUnsignedShort();
					
					int [] SpriteDetAreas = new int[NSprites * 4 * MAX_SPRITE_DETECT_AREAS];
					for (int ti = 0; ti < NSprites * 4 * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetAreas[ti] = reader.readUnsignedShort();
	
					reader.close();
					
					int planeSize = FWidth * FHeight / 8;
					
					Project p = new Project();
					p.paletteMap.clear();
					p.name = bareName(filename);
					p.mask = new byte[planeSize];
					Arrays.fill(p.mask, (byte)0xFF);		// just for backwards comp. of older version of editor that expect something here
					p.height = FHeight;
					p.width = FWidth;
					p.srcHeight = FHeight;
					p.srcWidth = FWidth;
					p.planeSize = planeSize;
					p.version = 2;
					//p.paletteMap.putAll(vm.paletteMap);
					p.masks.clear();
					//p.masks.addAll(vm.masks);
					
					/*List<Animation> anis = new ArrayList<>();
					Animation ani = 
					CompiledAnimation cani = ani.cutScene(ani.start, ani.end, 0);
					anis.add(cani);*/

					/*p.paletteMap.put(palidx, newPalette);
					
					for (Palette pals : p.paletteMap.values()) {
						if (pals.sameColors(cols)) {
							
						}
					}*/
					

					
					List<Frame> frames = new ArrayList<>();
					int frameSize = FWidth*FHeight;

					CompiledAnimation dest = new CompiledAnimation(
							AnimationType.COMPILED, bareName(filename),
							0, NFrames, 2, 1, 0);
					dest.setMutable(true);
					dest.width = FWidth;
					dest.height = FHeight;
					dest.setClockFrom(Short.MAX_VALUE);
					
					for(int ti = 0; ti < NFrames; ti++) {
						RGB[] rgbFrame = new RGB[frameSize];
						RGB[] actCols = CPal.get(ti).colors;

						for(int i = 0; i < frameSize; i++) {
							rgbFrame[i] = actCols[CFrames[i+(ti*frameSize)]]; 
						}
						
						DMD dmd = new DMD(FWidth,FHeight);
						Frame f = createRGBFrame(dmd);
						
						for( int x = 0; x < FWidth; x++ ) {
							for( int y = 0; y < FHeight; y++ ) {
								int r = rgbFrame[(x+y*FWidth)].red & 0xff;
								int g = rgbFrame[(x+y*FWidth)].green & 0xff;
								int b = rgbFrame[(x+y*FWidth)].blue & 0xff;
								int v = (r<<16) + (g<<8) + b;
								int bit = (x % 8);
								int mask = (0b10000000 >> bit);
								int o = (x >> 3);
								for( int k = 0; k < 24; k++) {
									if( (v & (1 << k)) != 0 ) 
										f.planes.get(k).data[y*dmd.getBytesPerRow() + o] |= mask;
								}
							}
						}
						f.delay = 15;
						dest.frames.add(f);
					}
					dest.setDesc(bareName(filename));
					p.scenes = new ArrayList<Scene>();
					//p.scenes.put("foo",dest);
					return dest;
					
				} else {
					log.error("zip does not contain cRom file");
				}
			}
			zipFile.close();
		} catch( IOException e2) {
    	    log.error("error on load "+filename,e2);
    	    throw new RuntimeException("error on load "+filename, e2);
    	}
		return res;
	}
}
