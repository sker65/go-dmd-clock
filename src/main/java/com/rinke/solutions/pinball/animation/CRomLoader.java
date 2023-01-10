package com.rinke.solutions.pinball.animation;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.LittleEndianDataInputStream;

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

	public static void main(String[] args) throws Exception {
		ZipFile zipFile = new ZipFile(args[0]);

		log.debug("opening file {}", args[0]);
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
				int NSprites = 0;
				if (sizeheader >= 8 * 4 /* sizeof(uint) */) {
					NSprites = reader.readInt();
				} else
					NSprites = 0;

				int[] HashCodes = new int[NFrames];
				for (int ti = 0; ti < NFrames; ti++)
					HashCodes[ti] = reader.readInt();
				byte[] ShapeCompMode = new byte[NFrames];
				reader.read(ShapeCompMode);
				byte[] CompMaskID = new byte[NFrames];
				reader.read(CompMaskID);
				byte[] MovRctID = new byte[NFrames];
				reader.read(MovRctID);
				if (NCompMasks > 0) {
					byte[] CompMasks = new byte[NCompMasks * FHeight * FWidth];
					reader.read(CompMasks);
				}
				if (NMovMasks > 0) {
					byte[] MovRcts = new byte[NMovMasks * FHeight * FWidth];
					reader.read(MovRcts);
				}
				byte[] CPal = new byte[NFrames * 3 * NCColors];
				reader.read(CPal);
				byte[] CFrames = new byte[NFrames * FHeight * FWidth];
				reader.read(CFrames);
				byte[] DynaMasks = new byte[NFrames * FHeight * FWidth];
				reader.read(DynaMasks);
				byte[] Dyna4Cols = new byte[NFrames * MAX_DYNA_4COLS_PER_FRAME * NOColors];
				reader.read(Dyna4Cols);
				byte[] FrameSprites = new byte[NFrames * MAX_SPRITES_PER_FRAME];
				reader.read(FrameSprites);
				byte[] SpriteDescriptionsO = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE];
				byte[] SpriteDescriptionsC = new byte[NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE];
				for (int ti = 0; ti < NSprites * MAX_SPRITE_SIZE * MAX_SPRITE_SIZE; ti++) {
					SpriteDescriptionsC[ti] = reader.readByte();
					SpriteDescriptionsO[ti] = reader.readByte();
				}

				reader.close();
			} else {
				log.error("zip does not contain cRom file");
			}
		}
		zipFile.close();
	}

}
