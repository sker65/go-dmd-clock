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

import com.rinke.solutions.pinball.view.model.ViewModel;

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

	public static void loadProject(String filename, ViewModel vm) {
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
					byte[] filenameData = new byte[64]; // ROM name
					reader.read(filenameData);
					
					int sizeheader = reader.readInt();
					log.debug("header size {}", sizeheader);
					int FWidth = reader.readInt(); // frame width
					int FHeight = reader.readInt(); // frame height
					int NFrames = reader.readInt(); // number of frames
					int NOColors = reader.readInt(); // Number of colors in palette of original ROM=nO
					log.debug("w,h: {},{}, frames: {}, colors: {}", FWidth,FHeight,NFrames,NOColors);
					if (NOColors == 16)
						From = FrameFormat.Gray4;
					else
						From = FrameFormat.Gray2;
					int NCColors = reader.readInt(); // Number of colors in palette of colorized ROM=nC
					int NCompMasks = reader.readInt(); // Number of dynamic masks=nM
					int NMovMasks = reader.readInt(); // Number of moving rects=nMR
					int NSprites = reader.readInt(); // Number of sprites=nS

					int[] HashCodes = new int[NFrames]; // uint[nF] hashcode/checksum
					for (int ti = 0; ti < NFrames; ti++)
						HashCodes[ti] = reader.readInt();
					
					byte[] ShapeCompMode = new byte[NFrames]; // UINT8[nF] FALSE - full comparison (all 4 colors) TRUE - shape mode (we just compare black 0 against all the 3 other colors as if it was 1 color)
					// HashCode take into account the ShapeCompMode parameter converting any '2' or '3' into a '1'
					reader.readFully(ShapeCompMode); 
					byte[] CompMaskID = new byte[NFrames]; // UINT8[nF] Comparison mask ID per frame (255 if no rectangle for this frame)
					reader.readFully(CompMaskID);
					byte[] MovRctID = new byte[NFrames]; // UINT8[nF] Horizontal moving comparison rectangle ID per frame (255 if no rectangle for this frame)
					reader.readFully(MovRctID);
					if (NCompMasks > 0) {
						byte[] CompMasks = new byte[NCompMasks * FHeight * FWidth]; // UINT8[nM*fW*fH] Mask for comparison
						reader.readFully(CompMasks);
					}
					if (NMovMasks > 0) {
						byte[] MovRcts = new byte[NMovMasks * FHeight * FWidth]; // UINT8[nMR*4] Rect for Moving Comparision rectangle [x,y,w,h]. The value (<MAX_DYNA_4COLS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content. 
						reader.readFully(MovRcts);
					}
					
					java.util.List<Palette> CPal = new ArrayList<>(); // UINT8[3*nC*nF] Palette for each colorized frames
					
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

					byte[] CFrames = new byte[NFrames * FHeight * FWidth]; // UINT8[nF*fW*fH] Colorized frames color indices
					reader.readFully(CFrames);
					byte[] DynaMasks = new byte[NFrames * FHeight * FWidth]; // UINT8[nF*fW*fH] Mask for dynamic content for each frame.  The value (<MAX_DYNA_4COLS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content.
					reader.readFully(DynaMasks);
					byte[] Dyna4Cols = new byte[NFrames * MAX_DYNA_4COLS_PER_FRAME * NOColors]; // UINT8[nF*MAX_DYNA_4COLS_PER_FRAME*nO] Color sets used to fill the dynamic content
					reader.readFully(Dyna4Cols);
					byte[] FrameSprites = new byte[NFrames * MAX_SPRITES_PER_FRAME]; // UINT8[nF*MAX_SPRITES_PER_FRAME] Sprite numbers to look for in this frame max=MAX_SPRITES_PER_FRAME
					reader.readFully(FrameSprites);
					byte[] SpriteDescriptionsO = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE]; // UINT8[nS*MAX_SPRITE_SIZE*MAX_SPRITE_SIZE] 4-or-16-color sprite original drawing (255 means this is a transparent=ignored pixel) for Comparison step
					byte[] SpriteDescriptionsC = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE]; // UINT8[nS*MAX_SPRITE_SIZE*MAX_SPRITE_SIZE] 64-color sprite for Colorization step
					for (int ti = 0; ti < NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE; ti++) {
						SpriteDescriptionsC[ti] = reader.readByte();
						SpriteDescriptionsO[ti] = reader.readByte();
					}
					byte[] ActiveFrames=new byte[NFrames]; // UINT8[nF] is the frame active (colorized or duration>16ms) or not
					reader.readFully(ActiveFrames);
					byte[] ColorRotations=new byte[NFrames*3*MAX_COLOR_ROTATIONS]; // UINT8[nF*3*MAX_COLOR_ROTATIONS] list of color rotation for each frame:
					// 1st byte is color # of the first color to rotate / 2nd byte id the number of colors to rotate / 3rd byte is the length in 10ms between each color switch
					reader.readFully(ColorRotations);
					// WARN in java there is no uint -> we use int instead
					int[] SpriteDetDwords = new int[NSprites * MAX_SPRITE_DETECT_AREAS]; // uint[nS*MAX_SPRITE_DETECT_AREAS] dword to quickly detect 4 consecutive distinctive pixels inside the original drawing of a sprite for optimized detection
					for (int ti = 0; ti < NSprites * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetDwords[ti] = reader.readInt();
					
					// in java there is no unsigned int or uint16 so we use normal int array, but read unsigned int
					int[] SpriteDetDwordPos = new int[NSprites * MAX_SPRITE_DETECT_AREAS]; // UINT16[nS*MAX_SPRITE_DETECT_AREAS] offset of the above dword in the sprite description
					for (int ti = 0; ti < NSprites * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetDwordPos[ti] = reader.readUnsignedShort();
					
					int [] SpriteDetAreas = new int[NSprites * 4 * MAX_SPRITE_DETECT_AREAS]; // UINT16[nS*4*MAX_SPRITE_DETECT_AREAS] rectangles (left, top, width, height) as areas to detect sprites (left=0xffff -> no zone)
					for (int ti = 0; ti < NSprites * 4 * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetAreas[ti] = reader.readUnsignedShort();
	
					reader.close();
					
					int planeSize = FWidth * FHeight / 8;
					
					List<Frame> frames = new ArrayList<>();
					int frameSize = FWidth*FHeight;

					CompiledAnimation dest = new CompiledAnimation(
							AnimationType.COMPILED, bareName(filename),
							0, NFrames, 1, 1, 0);
					dest.setMutable(true);
					dest.width = FWidth;
					dest.height = FHeight;
					dest.setClockFrom(Short.MAX_VALUE);
					
					for(int ID = 0; ID < NFrames; ID++) {
						RGB[] rgbFrame = new RGB[frameSize];
						RGB[] actCols = CPal.get(ID).colors;

						for(int ti = 0; ti < frameSize; ti++) {
							
							if (DynaMasks[ti+(ID*frameSize)] == -1) {
								rgbFrame[ti] = actCols[CFrames[ti+(ID*frameSize)]];
							}
							else {
								//frame[ti] = dyna4cols[IDfound * MAX_DYNA_4COLS_PER_FRAME * nocolors + dynacouche * nocolors + frame[ti]];
								rgbFrame[ti] = actCols[Dyna4Cols[(ID * MAX_DYNA_4COLS_PER_FRAME * NOColors) + DynaMasks[ID * frameSize + ti] * NOColors + CFrames[ti+(ID*frameSize)]] + (NOColors - 1)];
							}
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
					vm.scenes.put(dest.getDesc(), dest);
					
				} else {
					log.error("zip does not contain cRom file");
				}
			}
			zipFile.close();
		} catch( IOException e2) {
    	    log.error("error on load "+filename,e2);
    	    throw new RuntimeException("error on load "+filename, e2);
    	}
	}
}
