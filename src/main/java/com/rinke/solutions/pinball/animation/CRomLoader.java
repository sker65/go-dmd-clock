// Serum file reader
// See https://github.com/zesinger/libserum 

package com.rinke.solutions.pinball.animation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameLink;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

import com.rinke.solutions.pinball.view.model.ViewModel;

import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

class cRom
{
	// header
	public byte[]		name; // ROM name (no .zip, no path, example: afm_113b)
	public int			fWidth;	// Frame width=fW
	public int			fHeight;	// Frame height=fH
	public int			nFrames;	// Number of frames=nF
	public int			noColors;	// Number of colors in palette of original ROM=noC
	public int			ncColors;	// Number of colors in palette of colorized ROM=nC
	public int			nCompMasks; // Number of dynamic masks=nM
	public int			nMovMasks; // Number of moving rects=nMR
	public int			nSprites; // Number of sprites=nS (max 255)
	// data
	// part for comparison
	public int[]		HashCode;	// UINT32[nF] hashcode/checksum
	public byte[]		CompMaskID;	// UINT8[nF] Comparison mask ID per frame (255 if no rectangle for this frame)
	public byte[]		ShapeCompMode;	// UINT8[nF] FALSE - full comparison (all 4 colors) TRUE - shape mode (we just compare black 0 against all the 3 other colors as if it was 1 color)
								// HashCode take into account the ShapeCompMode parameter converting any '2' or '3' into a '1'
	public byte[]		MovRctID;	// UINT8[nF] Horizontal moving comparison rectangle ID per frame (255 if no rectangle for this frame)
	public byte[]		CompMasks;	// UINT8[nM*fW*fH] Mask for comparison
	public byte[]		MovRcts; // UINT8[nMR*4] Rect for Moving Comparision rectangle [x,y,w,h]. The value (<MAX_DYNA_SETS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content.
	// part for colorization
	public byte[]		cPal;		// UINT8[3*nC*nF] Palette for each colorized frames
	public byte[]		cFrames;	// UINT8[nF*fW*fH] Colorized frames color indices, if this frame has sprites, it is the colorized frame of the static scene, with no sprite
	public byte[]		DynaMasks;	// UINT8[nF*fW*fH] Mask for dynamic content for each frame.  The value (<MAX_DYNA_SETS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content.
	public byte[]		Dyna4Cols;  // UINT8[nF*MAX_DYNA_SETS_PER_FRAME*noC] Color sets used to fill the dynamic content
	public byte[]		FrameSprites; // UINT8[nF*MAX_SPRITES_PER_FRAME] Sprite numbers to look for in this frame max=MAX_SPRITES_PER_FRAME 255 if no sprite
	public short[] 		SpriteDescriptions; // UINT16[nS*MAX_SPRITE_SIZE*MAX_SPRITE_SIZE] Sprite drawing on 2 bytes per pixel:
									// - the first is the 4-or-16-color sprite original drawing (255 means this is a transparent=ignored pixel) for Comparison step
								    // - the second is the 64-color sprite for Colorization step
	public byte[]		ColorRotations; // UINT8[3*MAX_COLOR_ROTATION*nF] 3 bytes per rotation: 1- first color; 2- last color; 3- number of 10ms between two rotations
	public int[]		SpriteDetAreas; // UINT16[nS*4*MAX_SPRITE_DETECT_AREAS] rectangles (left, top, width, height) as areas to detect sprites (left=0xffff -> no zone)
	public int[]		SpriteDetDwords; // UINT32[nS*MAX_SPRITE_DETECT_AREAS] dword to quickly detect 4 consecutive distinctive pixels inside the original drawing of a sprite for optimized detection
	public int[]		SpriteDetDwordPos; // UINT16[nS*MAX_SPRITE_DETECT_AREAS] offset of the above dword in the sprite description
	public int[]		TriggerID; // UINT32[nF] does this frame triggers any event ID, 0xFFFFFFFF if not
};

class cRP
{
	// Header
	public byte[]		name; // ROM name (no .zip, no path, example: afm_113b)
	public byte[]		oFrames;	// UINT8[nF*fW*fH] Original frames (TXT converted to byte '2'->2)
	public int[]		activeColSet; // 4-or-16-color sets active
	public byte[]		ColSets; // the 4-or-16-color sets
	public byte			acColSet; // current 4-or-16-color set
	public byte			preColSet; // first 4-or-16-color set displayed in the dialogbox
	public byte[]		nameColSet; // caption of the colsets
	public int			DrawColMode; // 0- 1 col mode, 1- 4 color set mode, 2- gradient mode
	public byte			Draw_Mode;	// in colorization mode: 0- point, 1- line, 2- rect, 3- circle, 4- fill
	public int			Mask_Sel_Mode; // in comparison mode: 0- point, 1- rectangle, 2- magic wand
	public int			Fill_Mode; // FALSE- empty, TRUE- filled
	public byte[]		Mask_Names; // the names of the dynamic masks
	public int			nSections; // number of sections in the frames
	public int[]		Section_Firsts; // first frame of each section
	public byte[][]		Section_Names; // Names of the sections
	public byte[]		Sprite_Names; // Names of the sprites
	public int[]		Sprite_Col_From_Frame; // Which frame is used for the palette of the sprite colors
	public byte[]		Sprite_Edit_Colors; // Which color are used to edit this sprite
	public int[]		FrameDuration; // UINT32[nF] duration of the frame
	public byte[]		SaveDir; // char[260] save directory
	public short[]		SpriteRect; // UINT16[255*4] rectangle where to find the sprite in the frame Sprite_Col_From_Frame
	public int[]		SpriteRectMirror; // BOOL[255*2] has the initial rectangle been mirrored horizontally or/and vertically
};

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
	private static int MAX_COL_SETS = 64;// max number of 4-color sets to remember
	private static int MAX_MASKS = 64; // max number of comparison masks for comparison
	private static int SIZE_MASK_NAME = 32; // size of dyna mask names
	private static int MAX_SECTIONS = 512; // maximum number of frame sections
	private static int SIZE_SECTION_NAMES = 32; // size of section names
	

	private static cRom MycRom = null;
	private static cRP MycRP = null;
	
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
		dest.setProjectAnimation(true);
		return dest;
	}
	
	public static int unsignedByte(byte b) {
	    return b & 0xFF;
	}
	
	public static String getPrintableHash(byte[] p) {
		StringBuffer hexString = new StringBuffer();
		for (int j = 0; j < p.length; j++)
			hexString.append(String.format("%02X", p[j]));
		return hexString.toString();
	}
	
	public static String getEmptyHash() {
		return (MycRom.fWidth == 128 && MycRom.fHeight == 32) ? "B2AA7578" : "6C1CE17E";
	}

	public static void loadcRom(LittleEndianDataInputStream reader) {
		
		try { 
			
			MycRom.name = new byte[64]; // ROM name
			reader.read(MycRom.name);
			
			int sizeheader = reader.readInt();
			log.debug("header size {}", sizeheader);
			MycRom.fWidth = reader.readInt(); // frame width
			MycRom.fHeight = reader.readInt(); // frame height
			MycRom.nFrames= reader.readInt(); // number of frames
			MycRom.noColors = reader.readInt(); // Number of colors in palette of original ROM=nO
			log.debug("w,h: {},{}, frames: {}, colors: {}", MycRom.fWidth,MycRom.fHeight,MycRom.nFrames,MycRom.noColors);
			FrameFormat From;
			if (MycRom.noColors == 16)
				From = FrameFormat.Gray4;
			else
				From = FrameFormat.Gray2;
			MycRom.ncColors = reader.readInt(); // Number of colors in palette of colorized ROM=nC
			MycRom.nCompMasks = reader.readInt(); // Number of dynamic masks=nM
			MycRom.nMovMasks = reader.readInt(); // Number of moving rects=nMR
			MycRom.nSprites = reader.readInt(); // Number of sprites=nS
	
			MycRom.HashCode = new int[MycRom.nFrames]; // hashcode/checksum
			for (int ti = 0; ti < MycRom.nFrames; ti++)
				MycRom.HashCode[ti] = reader.readInt();
			
			MycRom.ShapeCompMode = new byte[MycRom.nFrames]; // FALSE - full comparison (all 4 colors) TRUE - shape mode (we just compare black 0 against all the 3 other colors as if it was 1 color)
			// HashCode take into account the ShapeCompMode parameter converting any '2' or '3' into a '1'
			reader.readFully(MycRom.ShapeCompMode); 
			MycRom.CompMaskID = new byte[MycRom.nFrames]; // Comparison mask ID per frame (255 if no rectangle for this frame)
			reader.readFully(MycRom.CompMaskID);
			MycRom.MovRctID = new byte[MycRom.nFrames]; // Horizontal moving comparison rectangle ID per frame (255 if no rectangle for this frame)
			reader.readFully(MycRom.MovRctID);
			MycRom.CompMasks = new byte[MycRom.nCompMasks * MycRom.fHeight * MycRom.fWidth]; // Mask for comparison
			if (MycRom.nCompMasks > 0) {
				reader.readFully(MycRom.CompMasks);
			}
			MycRom.MovRcts = new byte[MycRom.nMovMasks * MycRom.fHeight * MycRom.fWidth]; // Rect for Moving Comparision rectangle [x,y,w,h]. The value (<MAX_DYNA_4COLS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content. 
			if (MycRom.nMovMasks > 0) {
				reader.readFully(MycRom.MovRcts);
			}
			MycRom.cPal = new byte[MycRom.nFrames * MycRom.ncColors * 3]; // palette data for each frame
			reader.readFully(MycRom.cPal);
			MycRom.cFrames = new byte[MycRom.nFrames * MycRom.fHeight * MycRom.fWidth]; // Colorized frames color indices
			reader.readFully(MycRom.cFrames);
			MycRom.DynaMasks = new byte[MycRom.nFrames * MycRom.fHeight * MycRom.fWidth]; // Mask for dynamic content for each frame.  The value (<MAX_DYNA_4COLS_PER_FRAME) points to a sequence of 4 colors in Dyna4Cols. 255 means not a dynamic content.
			reader.readFully(MycRom.DynaMasks);
			MycRom.Dyna4Cols = new byte[MycRom.nFrames * MAX_DYNA_4COLS_PER_FRAME * MycRom.noColors]; // Color sets used to fill the dynamic content
			reader.readFully(MycRom.Dyna4Cols);
			MycRom.FrameSprites = new byte[MycRom.nFrames * MAX_SPRITES_PER_FRAME]; // Sprite numbers to look for in this frame max=MAX_SPRITES_PER_FRAME
			reader.readFully(MycRom.FrameSprites);
			MycRom.SpriteDescriptions = new short[MycRom.nSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE]; // 4-or-16-color sprite original drawing (255 means this is a transparent=ignored pixel) for Comparison step
			for (int ti = 0; ti < MycRom.nSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE; ti++) {
				MycRom.SpriteDescriptions[ti] = reader.readShort();
			}
			byte[] ActiveFrames=new byte[MycRom.nFrames]; // is the frame active (colorized or duration>16ms) or not
			reader.readFully(ActiveFrames);
			MycRom.ColorRotations=new byte[MycRom.nFrames*3*MAX_COLOR_ROTATIONS]; // list of color rotation for each frame:
			// 1st byte is color # of the first color to rotate / 2nd byte id the number of colors to rotate / 3rd byte is the length in 10ms between each color switch
			reader.readFully(MycRom.ColorRotations);
			// WARN in java there is no uint -> we use int instead
			int[] SpriteDetDwords = new int[MycRom.nSprites * MAX_SPRITE_DETECT_AREAS]; // dword to quickly detect 4 consecutive distinctive pixels inside the original drawing of a sprite for optimized detection
			for (int ti = 0; ti < MycRom.nSprites * MAX_SPRITE_DETECT_AREAS; ti++)
				SpriteDetDwords[ti] = reader.readInt();
			
			// in java there is no unsigned int or uint16 so we use normal int array, but read unsigned int
			MycRom.SpriteDetDwordPos = new int[MycRom.nSprites * MAX_SPRITE_DETECT_AREAS]; // offset of the above dword in the sprite description
			for (int ti = 0; ti < MycRom.nSprites * MAX_SPRITE_DETECT_AREAS; ti++)
				MycRom.SpriteDetDwordPos[ti] = reader.readUnsignedShort();
			
			MycRom.SpriteDetAreas = new int[MycRom.nSprites * 4 * MAX_SPRITE_DETECT_AREAS]; // rectangles (left, top, width, height) as areas to detect sprites (left=0xffff -> no zone)
			for (int ti = 0; ti < MycRom.nSprites * 4 * MAX_SPRITE_DETECT_AREAS; ti++)
				MycRom.SpriteDetAreas[ti] = reader.readUnsignedShort();
			
			MycRom.TriggerID = new int[MycRom.nFrames];
			Arrays.fill(MycRom.TriggerID, (int)0xFFFFFFFF);
			if(sizeheader > 11 * 4) {
				for (int ti = 0; ti < MycRom.nFrames; ti++)
					MycRom.TriggerID[ti] = reader.readInt();
			}
			
		} catch( IOException e2) {
    	    log.error("error reading cRom");
    	    throw new RuntimeException("error reading cRom");
    	}
	}
	
public static void loadcRP(LittleEndianDataInputStream reader) {
		
		try { 
			
			MycRP.name = new byte[64]; // ROM name
			reader.read(MycRP.name);
			MycRP.oFrames=new byte[MycRom.nFrames * MycRom.fWidth * MycRom.fHeight];
			reader.readFully(MycRP.oFrames);
			MycRP.activeColSet = new int[MAX_COL_SETS];
			for (int ti = 0; ti < MAX_COL_SETS; ti++)
				MycRP.activeColSet[ti] = reader.readInt();
			MycRP.ColSets = new byte[MAX_COL_SETS * 16];
			reader.readFully(MycRP.ColSets);
			MycRP.acColSet = reader.readByte();
			MycRP.preColSet = reader.readByte();
			MycRP.nameColSet = new byte[MAX_COL_SETS * 64];
			reader.readFully(MycRP.nameColSet);
			MycRP.DrawColMode = reader.readInt();
			if (MycRP.DrawColMode == 2) MycRP.DrawColMode = 0;
			MycRP.Draw_Mode = reader.readByte();
			MycRP.Mask_Sel_Mode = reader.readInt();
			MycRP.Fill_Mode = reader.readInt();
			MycRP.Mask_Names = new byte[MAX_MASKS * SIZE_MASK_NAME];
			reader.readFully(MycRP.Mask_Names);
			MycRP.nSections = reader.readInt();
			MycRP.Section_Firsts = new int[MAX_SECTIONS];
			for (int ti = 0; ti < MAX_SECTIONS; ti++)
				MycRP.Section_Firsts[ti] = reader.readInt();
			MycRP.Section_Names = new byte[MAX_SECTIONS] [SIZE_SECTION_NAMES];
			for (int ti = 0; ti < MAX_SECTIONS; ti++)
				reader.readFully(MycRP.Section_Names[ti]);
			MycRP.Sprite_Names = new byte[255 * SIZE_SECTION_NAMES];
			reader.readFully(MycRP.Sprite_Names);
			MycRP.Sprite_Col_From_Frame = new int[255];
			for (int ti = 0; ti < 255; ti++)
				MycRP.Sprite_Col_From_Frame[ti] = reader.readInt();
			MycRP.FrameDuration=new int[MycRom.nFrames];
			for (int ti = 0; ti < MycRom.nFrames; ti++)
				MycRP.FrameDuration[ti] = reader.readInt();
			MycRP.Sprite_Edit_Colors = new byte[16*255];
			reader.readFully(MycRP.Sprite_Edit_Colors);
			MycRP.SaveDir = new byte[260];
			reader.readFully(MycRP.SaveDir);
			MycRP.SpriteRect = new short[4 * 255];
			for (int ti = 0; ti < (4 * 255); ti++)
				MycRP.SpriteRect[ti] = reader.readShort();
			MycRP.SpriteRectMirror = new int[2 * 255];
			for (int ti = 0; ti < (2 * 255); ti++)
				MycRP.SpriteRectMirror[ti] = reader.readInt();
			

		} catch( IOException e2) {
    	    log.error("error reading cRP");
    	    throw new RuntimeException("error reading cRP");
    	}
	}
	
	public static void loadProject(String filename, ViewModel vm) {

		InputStream cRomStream = null;
		InputStream cRPStream = null;
		
		Animation recordingAni = null;
		
		try { 
			
			log.debug("opening file {}", filename);
			if (filename.toLowerCase().endsWith(".crz")) {
				ZipFile zipFile = new ZipFile(filename);
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
	
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.getName().toLowerCase().endsWith(".crom")) {
						log.debug("found cRom file {} in cRZ", entry.getName());
						cRomStream = zipFile.getInputStream(entry);
						break;
					}
				}
				MycRom = new cRom();
			} else if(filename.toLowerCase().endsWith(".crom")) {
				MycRom = new cRom();
				File cRomFile = new File(filename);
				cRomStream = new FileInputStream(cRomFile);
				MycRP = new cRP();
				String cRPfilename = filename.substring(0, filename.lastIndexOf('.')) + ".crp";
				File cRPFile = new File(cRPfilename);
				cRPStream = new FileInputStream(cRPFile);
			} else {
				return;
			}
			
			LittleEndianDataInputStream cRomReader = new LittleEndianDataInputStream (cRomStream);
			loadcRom(cRomReader);
			cRomReader.close();
			
			if (MycRP != null) {
				LittleEndianDataInputStream cRPReader = new LittleEndianDataInputStream (cRPStream);
				loadcRP(cRPReader);
				cRPReader.close();
			}
			// now create dump file from cRP
			GZIPOutputStream gos = null;

	        File myGzipFile = new File(filename.substring(0, filename.indexOf('.')) + ".txt.gz");
	        gos = new GZIPOutputStream(new FileOutputStream(myGzipFile));
	        
	        int tick = (int)System.currentTimeMillis();
	        InputStream is = null;
	        byte[] buffer = new byte[1024];
	        int len;
	        int nFrames = 1;
	        if (MycRP != null)
	        	nFrames = MycRom.nFrames;
	        for (int kk = 0; kk < nFrames; kk++) {
	        	String tickStr = String.format("0x%08x", tick);
		        is = new ByteArrayInputStream(tickStr.getBytes());
		        buffer = new byte[1024];
		        while ((len = is.read(buffer)) != -1) {
		            gos.write(buffer, 0, len);
		        }
		        gos.write(0x0D);
                gos.write(0x0A);
	        	for (int jj = 0; jj < MycRom.fHeight; jj++) {
	                for (int ii = 0; ii < MycRom.fWidth; ii++)
	                {
	                	byte col = 0x00;
	                	if (MycRP != null)
	                		col = MycRP.oFrames[(kk * MycRom.fWidth * MycRom.fHeight) + jj * MycRom.fWidth + ii];
	                	String str = String.format("%01x", col);
	    		        is = new ByteArrayInputStream(str.getBytes());
	    		        buffer = new byte[1024];
	    		        while ((len = is.read(buffer)) != -1) {
	    		            gos.write(buffer, 0, len);
	    		        }
	                }
	                gos.write(0x0D);
	                gos.write(0x0A);
	            }
	        	if (MycRP != null)
	        		tick = tick + MycRP.FrameDuration[kk];
                gos.write(0x0D);
                gos.write(0x0A);
	        }
	        gos.close();
	        String basefile = FilenameUtils.getBaseName(filename.substring(0, filename.indexOf('.')) + ".txt.gz") + "." + FilenameUtils.getExtension(filename.substring(0, filename.indexOf('.')) + ".txt.gz");
	        vm.inputFiles.add(basefile);
	        recordingAni = AnimationFactory.buildAnimationFromFile(filename.substring(0, filename.indexOf('.')) + ".txt.gz", AnimationType.MAME, vm.numberOfColors);
	        vm.recordings.put(recordingAni.getDesc(), recordingAni);
	        vm.setSelectedRecording(recordingAni);
	        if (MycRP != null) {
        		vm.bookmarksMap = new HashMap<String, Set<Bookmark>>();
    			Set<Bookmark> set = new TreeSet<Bookmark>();
    			vm.bookmarksMap.put(recordingAni.getDesc(),set);
	        	for(int sects = 0; sects < MycRP.nSections; sects++) {
        			String bookmarkName = new String(MycRP.Section_Names[sects]).split("\0")[0];
    	    		set.add(new Bookmark(bookmarkName, MycRP.Section_Firsts[sects]));
	    		}
	    		vm.bookmarks.replaceAll(set);
	        }

		} catch( IOException e2) {
		    log.error("error on load "+filename,e2);
		    throw new RuntimeException("error on load "+filename, e2);
		}

		CompiledAnimation destRGB = createAni(MycRom.fWidth, MycRom.fHeight, bareName(filename) + "_RGB");
		CompiledAnimation dest = createAni(MycRom.fWidth, MycRom.fHeight, "0");
		CompiledAnimation dest6planes = createAni(MycRom.fWidth, MycRom.fHeight, bareName(filename) + "_6planes");

		int palIdx = 0;
		int sceneIdx = 0;
		
		RGB[] actCols = new RGB[MycRom.ncColors];

		vm.masks.clear();
		
		for (int i = 0; i < MycRom.nCompMasks; i++) {
			Mask dmask = new Mask(MycRom.fWidth*MycRom.fHeight/8);
			dmask.data = createDMask(MycRom.CompMasks,i,MycRom.fWidth, MycRom.fHeight);
			vm.masks.add(i, dmask);
			if (MycRP != null && Arrays.hashCode(dmask.data) != REPLACEMASK && Arrays.hashCode(dmask.data) != COLMASKMASK)
				vm.masks.get(i).locked = true;
			vm.setMaxNumberOfMasks(i);
		}
		
		vm.paletteMap.clear();

		String sectName = null;
		
		for(int ID = 0; ID < MycRom.nFrames; ID++) {
			
			// log.debug("processing frame {}", ID);
			
			RGB[] rgbFrame = new RGB[MycRom.fWidth*MycRom.fHeight];
			byte[] frame = new byte[MycRom.fWidth*MycRom.fHeight];
			
			if (MycRP != null) {
				for(int sects = 0; sects < MycRP.nSections; sects++) {
					if (MycRP.Section_Firsts[sects] == ID) {
						sectName = new String(MycRP.Section_Names[sects]).split("\0")[0];
						break;
					}
				}
			}
			
			RGB[] cols = new RGB[MycRom.ncColors];
			for( int i = 0; i < MycRom.ncColors; i++) {
				cols[i] = new RGB(
						unsignedByte(MycRom.cPal[(ID * MycRom.ncColors * 3) + (i * 3)]),
						unsignedByte(MycRom.cPal[(ID * MycRom.ncColors * 3) + (i * 3) + 1]),
						unsignedByte(MycRom.cPal[(ID * MycRom.ncColors * 3) + (i * 3) + 2])
						);
			}
			String name = "new" + UUID.randomUUID().toString().substring(0, 4);
			Palette cPalette = new Palette(cols, ID, name);

			if ((Arrays.hashCode(actCols) != Arrays.hashCode(cPalette.colors)) && (ID != 0)) {
				dest.end = dest.frames.size()-1;
				if (sectName == null) {
					dest.setDesc("scene_"+Integer.toString(sceneIdx));
				} else {
					dest.setDesc(sectName + "_" + Integer.toString(sceneIdx));
				}
				if (dest.frames.size() != 0) {
					dest.setRecordingLink(new RecordingLink(dest.frames.get(0).frameLink.recordingName , dest.frames.get(0).frameLink.frame));
					vm.scenes.put(dest.getDesc(), dest);
					if (MycRP != null) {
						for (int i = 0; i < dest.frames.size(); i++) {
							PalMapping palMapping = new PalMapping(dest.getPalIndex(), dest.getDesc());
							palMapping.switchMode = SwitchMode.LAYEREDREPLACE;
							palMapping.frameIndex = ID - dest.frames.size() + i;
							palMapping.setDigest(dest.frames.get(i).crc32);
							if (MycRom.CompMaskID[ID - dest.frames.size() + i] != -1) {
								palMapping.withMask = true;
								palMapping.maskNumber = MycRom.CompMaskID[(ID - dest.frames.size() + i)];
							}
							palMapping.frameSeqName = dest.getDesc();
							palMapping.name = dest.getDesc() + "_" +Integer.toString(i);

							boolean duplicate = false;
							if (!Arrays.equals(palMapping.crc32,new byte[]{0,0,0,0})) {
								for (PalMapping p : vm.keyframes.values()) {
									if (Arrays.equals(p.crc32, palMapping.crc32)) {
										duplicate = true;
										break;
									}
								}
							}
							
							if (!Arrays.equals(dest.frames.get(i).crc32,new byte[]{0,0,0,0}) && !duplicate) {
								vm.keyframes.put(dest.getDesc() + "_" + Integer.toString(i), palMapping);
							}
						}
					}
					sceneIdx++;
					dest = createAni(MycRom.fWidth, MycRom.fHeight, "scene_"+Integer.toString(sceneIdx));
					dest.setPalIndex(palIdx);
				}
			}
			
			actCols = cPalette.colors;

			int colVal = 0;
			int maxColVal = 0;
			
			for(int ti = 0; ti < MycRom.fWidth*MycRom.fHeight; ti++) {
				if (MycRom.DynaMasks[ti+(ID*MycRom.fWidth*MycRom.fHeight)] == -1) {
					colVal = MycRom.cFrames[ti+(ID*MycRom.fWidth*MycRom.fHeight)]; 
				}
				else {
					colVal = MycRom.Dyna4Cols[ID * MAX_DYNA_4COLS_PER_FRAME * MycRom.noColors + MycRom.DynaMasks[ID * MycRom.fWidth*MycRom.fHeight + ti] * MycRom.noColors + (MycRom.noColors - 1)];
					if (MycRP != null) {
						if (colVal > (MycRom.noColors - 1))
							colVal = colVal - ((MycRom.noColors - 1) - MycRP.oFrames[ti+(ID*MycRom.fWidth*MycRom.fHeight)]);
						else
							colVal = MycRP.oFrames[ti+(ID*MycRom.fWidth*MycRom.fHeight)];
					}
				}
				if (colVal > maxColVal) maxColVal = colVal;
				rgbFrame[ti] = actCols[colVal];
				frame[ti] = (byte) (colVal & 0xFF);
			}
			
			Mask lmask = new Mask(MycRom.fWidth*MycRom.fHeight/8);
			lmask.data = createLMask(MycRom.DynaMasks,ID,MycRom.fWidth, MycRom.fHeight);
			
			Mask dmask = new Mask(MycRom.fWidth*MycRom.fHeight/8);
			
			if(MycRom.CompMaskID[ID] != -1) {

				dmask.data = createDMask(MycRom.CompMasks,MycRom.CompMaskID[ID],MycRom.fWidth, MycRom.fHeight);
				
				boolean dMaskExists = false;
				for (int i = 0; i < dest.getMasks().size();i++) {
					if(Arrays.hashCode(dmask.data) == Arrays.hashCode(dest.getMask(i).data)) {
                    	dMaskExists = true;
                    	break;
                    }
                }
				if (!dMaskExists) {
					dest.getMasks().add(dmask);
					dest.lockMask(dest.getMasks().size()-1);
				}

			}
			
			Frame f = createFrame(frame,MycRom.fWidth,MycRom.fHeight,(int)(Math.log(MycRom.ncColors) / Math.log(2)));
			f.mask = lmask;
			if (recordingAni != null) {
				DMD tmp = new DMD(MycRom.fWidth, MycRom.fHeight);
				Frame recframe = recordingAni.render(ID, tmp, true);
				recframe.setMask(dmask);
				List<byte[]> hashes = recframe.getHashes();
				int idx = hashes.size();
				while (idx > 0) {
					idx--;
					String hash = getPrintableHash(hashes.get(idx));
					if (!hash.startsWith(getEmptyHash()))
						break;
				}
				String hash = getPrintableHash(hashes.get(idx));
				if (!hash.startsWith(getEmptyHash()))
					f.setHash(hashes.get(idx));
				else 
					f.crc32 = new byte[] {0,0,0,0};
			}
			
			Frame fRGB = createRGBFrame(rgbFrame,MycRom.fWidth,MycRom.fHeight);

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
            	Palette newPalette = new Palette(cPalette.colors, palIdx, cPalette.name);
            	vm.paletteMap.put(palIdx, newPalette);
            	dest.setPalIndex(palIdx);
            	palIdx++;
            } 
            
            if (MycRP == null) {
            	fRGB.delay = 15;
            	f.delay = 15;
            	f.frameLink = new FrameLink(FilenameUtils.getBaseName(filename),0);
			} else {
				fRGB.delay = MycRP.FrameDuration[ID];
            	f.delay = MycRP.FrameDuration[ID];
            	f.frameLink = new FrameLink(FilenameUtils.getBaseName(filename),ID);
			}
			
			if (maxColVal > MycRom.noColors - 1) { // only add frame if colorized
				destRGB.frames.add(fRGB);
				int i = 0;
				for (i = 0; i < dest.frames.size(); i++) {
					if (Arrays.equals(f.crc32,dest.frames.get(i).crc32))
						break;
				}
				if (i == dest.frames.size()) {
					dest.frames.add(f);
				} else {
					f.crc32 = new byte[] {0,0,0,0};
					dest.frames.add(f);
				}
			}
			dest6planes.frames.add(f);
		}
		
		
		if (dest.frames.size() != 0) {
			dest.end = dest.frames.size()-1;
			dest.setDesc("scene_"+Integer.toString(sceneIdx));
			dest.setRecordingLink(new RecordingLink(dest.frames.get(0).frameLink.recordingName , dest.frames.get(0).frameLink.frame));
			vm.scenes.put(dest.getDesc(), dest);
		}

		destRGB.end = destRGB.frames.size()-1;
		destRGB.setDesc(bareName(filename)+"_RGB");
		vm.scenes.put(destRGB.getDesc(), destRGB);
		
		dest6planes.end = dest6planes.frames.size()-1;
		dest6planes.setDesc(bareName(filename)+"_6planes");
		vm.scenes.put(dest6planes.getDesc(), dest6planes);
			
	}
}
