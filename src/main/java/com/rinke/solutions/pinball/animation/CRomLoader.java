// Serum file reader
// See https://github.com/zesinger/libserum 

package com.rinke.solutions.pinball.animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
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
	private static int REPLACEMASK = 1664589825; // hash for full mask
	private static int COLMASKMASK = 1425784833; // hash for empty mask
	
	static String bareName(String filename) {
		String b = new File(filename).getName();
		int i = b.lastIndexOf('.');
		return i==-1?b:b.substring(0, i);
	}
	
	static Frame createRGBFrame(RGB[] src, int width, int height) {
		Frame res = new Frame();
		DMD dmd = new DMD(width,height);
		
		// for true color create 24 planes
		for( int i = 0; i<24; i++)
			res.planes.add(new Plane((byte)i, new byte[dmd.getPlaneSize()]));
		
		for( int x = 0; x < width; x++ ) {
			for( int y = 0; y < height; y++ ) {
				int r = src[(x+y*width)].red & 0xff;
				int g = src[(x+y*width)].green & 0xff;
				int b = src[(x+y*width)].blue & 0xff;
				int v = (r<<16) + (g<<8) + b;
				int bit = (x % 8);
				int mask = (0b10000000 >> bit);
				int o = (x >> 3);
				for( int k = 0; k < 24; k++) {
					if( (v & (1 << k)) != 0 ) 
						res.planes.get(k).data[y*dmd.getBytesPerRow() + o] |= mask;
				}
			}
		}

		return res;
	}
	
	static Frame createFrame (byte[] src, int width, int height, int bitDepth) {
		Frame f = new Frame();
		for (int i = 0; i < bitDepth; i++)
			f.planes.add(new Plane((byte)i, new byte[width*height/8]));
		
		for( int pix = 0; pix < width*height; pix++) {
			int bit = (pix % 8);
			int byteIdx = pix / 8;
			int mask = (0b10000000 >> bit);
			int v = src[pix] ;
			for (int i = 0; i < bitDepth; i++) {
				if( (v & (int)(Math.pow(2,i))) != 0 ) 
					f.planes.get(i).data[byteIdx] |= mask;
			}
		}
		return f;
	}
	
	static byte[] createLMask (byte[] src, int ID, int width, int height) {
		byte[] dest = new byte[width*height/8];
		for( int pix = 0; pix < width*height; pix++) {
			int bit = (pix % 8);
			int byteIdx = pix / 8;
			int mask = (0b10000000 >> bit);
			int v = 1;
			if(src[pix + (ID * width * height)] != -1) v=0;
			if((v & 1) != 0 ) dest[byteIdx] |= mask;
		}
		return dest;
	}
	
	static byte[] createDMask (byte[] src, int ID, int width, int height) {
		byte[] dest = new byte[width*height/8];
		for( int pix = 0; pix < width*height; pix++) {
			int bit = (pix % 8);
			int byteIdx = pix / 8;
			int mask = (0b10000000 >> bit);
			int v = 1;
			if(src[pix + (ID * width * height)] != 0) v=0;
			if((v & 1) != 0 ) dest[byteIdx] |= mask;
		}
		return dest;
	}
	
	static CompiledAnimation createAni(int width, int height, String name) {
		CompiledAnimation dest = new CompiledAnimation(
				AnimationType.COMPILED, name,
				0, 1, 1, 1, 0);
		dest.setMutable(true);
		dest.width = width;
		dest.height = height;
		dest.setClockFrom(Short.MAX_VALUE);
		dest.setEditMode(EditMode.FIXED);
		return dest;
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

					int[] HashCodes = new int[NFrames]; // hashcode/checksum
					for (int ti = 0; ti < NFrames; ti++)
						HashCodes[ti] = reader.readInt();
					
					byte[] ShapeCompMode = new byte[NFrames]; // FALSE - full comparison (all 4 colors) TRUE - shape mode (we just compare black 0 against all the 3 other colors as if it was 1 color)
					// HashCode take into account the ShapeCompMode parameter converting any '2' or '3' into a '1'
					reader.readFully(ShapeCompMode); 
					byte[] CompMaskID = new byte[NFrames]; // Comparison mask ID per frame (255 if no rectangle for this frame)
					reader.readFully(CompMaskID);
					byte[] MovRctID = new byte[NFrames]; // Horizontal moving comparison rectangle ID per frame (255 if no rectangle for this frame)
					reader.readFully(MovRctID);
					byte[] CompMasks = new byte[NCompMasks * FHeight * FWidth]; // Mask for comparison
					if (NCompMasks > 0) {
						reader.readFully(CompMasks);
					}
					byte[] MovRcts = new byte[NMovMasks * FHeight * FWidth]; // Rect for Moving Comparision rectangle [x,y,w,h]. The value (<MAX_DYNA_4COLS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content. 
					if (NMovMasks > 0) {
						reader.readFully(MovRcts);
					}
					
					java.util.List<Palette> CPal = new ArrayList<>(); // Palette for each colorized frames
					
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

					byte[] CFrames = new byte[NFrames * FHeight * FWidth]; // Colorized frames color indices
					reader.readFully(CFrames);
					byte[] DynaMasks = new byte[NFrames * FHeight * FWidth]; // Mask for dynamic content for each frame.  The value (<MAX_DYNA_4COLS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content.
					reader.readFully(DynaMasks);
					byte[] Dyna4Cols = new byte[NFrames * MAX_DYNA_4COLS_PER_FRAME * NOColors]; // Color sets used to fill the dynamic content
					reader.readFully(Dyna4Cols);
					byte[] FrameSprites = new byte[NFrames * MAX_SPRITES_PER_FRAME]; // Sprite numbers to look for in this frame max=MAX_SPRITES_PER_FRAME
					reader.readFully(FrameSprites);
					byte[] SpriteDescriptionsO = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE]; // 4-or-16-color sprite original drawing (255 means this is a transparent=ignored pixel) for Comparison step
					byte[] SpriteDescriptionsC = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE]; //  64-color sprite for Colorization step
					for (int ti = 0; ti < NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE; ti++) {
						SpriteDescriptionsC[ti] = reader.readByte();
						SpriteDescriptionsO[ti] = reader.readByte();
					}
					byte[] ActiveFrames=new byte[NFrames]; // is the frame active (colorized or duration>16ms) or not
					reader.readFully(ActiveFrames);
					byte[] ColorRotations=new byte[NFrames*3*MAX_COLOR_ROTATIONS]; // list of color rotation for each frame:
					// 1st byte is color # of the first color to rotate / 2nd byte id the number of colors to rotate / 3rd byte is the length in 10ms between each color switch
					reader.readFully(ColorRotations);
					// WARN in java there is no uint -> we use int instead
					int[] SpriteDetDwords = new int[NSprites * MAX_SPRITE_DETECT_AREAS]; // dword to quickly detect 4 consecutive distinctive pixels inside the original drawing of a sprite for optimized detection
					for (int ti = 0; ti < NSprites * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetDwords[ti] = reader.readInt();
					
					// in java there is no unsigned int or uint16 so we use normal int array, but read unsigned int
					int[] SpriteDetDwordPos = new int[NSprites * MAX_SPRITE_DETECT_AREAS]; // offset of the above dword in the sprite description
					for (int ti = 0; ti < NSprites * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetDwordPos[ti] = reader.readUnsignedShort();
					
					int [] SpriteDetAreas = new int[NSprites * 4 * MAX_SPRITE_DETECT_AREAS]; // rectangles (left, top, width, height) as areas to detect sprites (left=0xffff -> no zone)
					for (int ti = 0; ti < NSprites * 4 * MAX_SPRITE_DETECT_AREAS; ti++)
						SpriteDetAreas[ti] = reader.readUnsignedShort();
					
					int triggerIDs[] = new int[NFrames];
					Arrays.fill(triggerIDs, (int)0xFFFFFFFF);
					if(sizeheader > 11 * 4) {
						for (int ti = 0; ti < NFrames; ti++)
							triggerIDs[ti] = reader.readInt();
					}
					
	
					reader.close();
					
					CompiledAnimation destRGB = createAni(FWidth, FHeight, bareName(filename) + "_RGB");
					CompiledAnimation dest = createAni(FWidth, FHeight, "0");
					CompiledAnimation dest6planes = createAni(FWidth, FHeight, bareName(filename) + "_6planes");

					int palIdx = 0;
					int sceneIdx = 0;
					int maskhash = 0;
					
					RGB[] actCols = new RGB[NCColors];
					
					vm.paletteMap.clear();
					
					for(int ID = 0; ID < NFrames; ID++) {
						
						RGB[] rgbFrame = new RGB[FWidth*FHeight];
						byte[] frame = new byte[FWidth*FHeight];

						if ((Arrays.hashCode(actCols) != Arrays.hashCode(CPal.get(ID).colors)) && (ID != 0)) {
							dest.end = dest.frames.size()-1;
							dest.setDesc("scene_"+Integer.toString(sceneIdx));
							if (dest.frames.size() != 0) {
								vm.scenes.put(dest.getDesc(), dest);
								sceneIdx++;
								dest = createAni(FWidth, FHeight, "scene_"+Integer.toString(sceneIdx));
								dest.setPalIndex(palIdx);
							}
						}
						
						actCols = CPal.get(ID).colors;

						int colVal = 0;
						int maxColVal = 0;
						
						for(int ti = 0; ti < FWidth*FHeight; ti++) {
							if (DynaMasks[ti+(ID*FWidth*FHeight)] == -1) {
								colVal = CFrames[ti+(ID*FWidth*FHeight)]; 
							}
							else {
								colVal = Dyna4Cols[(ID * MAX_DYNA_4COLS_PER_FRAME * NOColors) + DynaMasks[ID * FWidth*FHeight + ti] * NOColors + CFrames[ti+(ID*FWidth*FHeight)]];
								if (CFrames[ti+(ID*FWidth*FHeight)] == 0) // make dynamic area visible
									colVal += NOColors - 1;
							}
							if (colVal > maxColVal) maxColVal = colVal;
							rgbFrame[ti] = actCols[colVal];
							frame[ti] = (byte) (colVal & 0xFF);
						}
						
						Mask lmask = new Mask(FWidth*FHeight/8);
						lmask.data = createLMask(DynaMasks,ID,FWidth, FHeight);
						maskhash = Arrays.hashCode(lmask.data);
						
						if(CompMaskID[ID] != -1) {
							Mask dmask = new Mask(FWidth*FHeight/8);
							dmask.data = createDMask(CompMasks,CompMaskID[ID],FWidth, FHeight);
							
							boolean dMaskExists = false;
							for (int i = 0; i < dest.getMasks().size();i++) {
								if(Arrays.hashCode(dmask.data) == Arrays.hashCode(dest.getMask(i).data)) {
		                        	dMaskExists = true;
		                        	break;
		                        }
		                    }
							if (!dMaskExists)
								dest.getMasks().add(dmask);
						}
						
						Frame f = createFrame(frame,FWidth,FHeight,(int)(Math.log(NCColors) / Math.log(2)));
						f.mask = lmask;
						Frame fRGB = createRGBFrame(rgbFrame,FWidth,FHeight);

						if (maskhash == REPLACEMASK && dest.getEditMode() == EditMode.FIXED)
							dest.setEditMode(EditMode.REPLACE);
						else if (maskhash == COLMASKMASK && dest.getEditMode() == EditMode.FIXED)
							dest.setEditMode(EditMode.COLMASK);
						else if (maskhash != REPLACEMASK && maskhash != COLMASKMASK)
							dest.setEditMode(EditMode.LAYEREDREPLACE);
						
						// only add palette if not already in the project
						boolean palExists = false;
	                    for (Palette pals : vm.paletteMap.values()) {
	                        if (pals.sameColors(actCols)) {
	                        	dest.setPalIndex(pals.index);
	                        	palExists = true;
	                        	break;
	                        }
	                    }
	                    if (!palExists) {
	                    	Palette newPalette = new Palette(CPal.get(ID).colors, palIdx, CPal.get(ID).name);
	                    	vm.paletteMap.put(palIdx, newPalette);
	                    	dest.setPalIndex(palIdx);
	                    	palIdx++;
	                    } 

						fRGB.delay = 15;
						f.delay = 15;
						
						if (maxColVal > NOColors - 1) { // only add frame if colorized
							destRGB.frames.add(fRGB);
							dest.frames.add(f);
						}
						
						dest6planes.frames.add(f);
					}
					
					destRGB.end = destRGB.frames.size()-1;
					destRGB.setDesc(bareName(filename)+"_RGB");
					vm.scenes.put(destRGB.getDesc(), destRGB);
					
					dest6planes.end = dest6planes.frames.size()-1;
					dest6planes.setDesc(bareName(filename)+"_6planes");
					vm.scenes.put(dest6planes.getDesc(), dest6planes);
					
					Palette newPalette = new Palette(CPal.get(NFrames-1).colors, palIdx, CPal.get(NFrames-1).name);
                	vm.paletteMap.put(palIdx, newPalette);
                	dest.setPalIndex(palIdx);
					dest.end = dest.frames.size()-1;
					dest.setDesc("scene_"+Integer.toString(sceneIdx));
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
