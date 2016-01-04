# File Formats
All binary number are written in **big endian** format, which means most significant bytes first. Basic data types are binary numbers like int8, int16, int32 and int64 or just raw byte data.
For colors 3 byte rgb values are used. Frames stored are raw uncompressed pixel map, plane by plane, lowest significant plane first, left pixel is HSB (like PPM image format).

## pin2dmd.dat

This file contains just the default device mode, default palette, custom smart dmd signature (if any) and optional custom display timing parameters

Offset     | Length | Description
---------- | ------ | :-----------
0 | 1 byte | device mode. 0: pinmame rgb, 1: pinmame mono, 2: wpc, 3: stern
1 | 1 byte | number (or index) of default palette
2 | 8 bytes | custom smart dmd signature to use
10 | 10 bytes | custom timing parameters. 5 x 16bit uints for timing, first overall period, 4x periods for 4 subframe brightnesses

## palettes.dat
This file contains custom palette definition and key frame definitions for palette switches or insertion of replacement frames.

Name | Structure
:---- | :-----------
Version | int8 version of file (actually 1)
SeqOfPalettes | int16 number of palettes contained in this file
Palette.Index | int16 palette index
Palette.NoOfColors | int16 number of colors contained in palette
Palette.Type | int8 type of palette. 0: normal, 1: default (only one palette per file could be marked as default)
Palette.SeqOfRGB | sequence rgb values (3 bytes) of colors in this palette
SeqOfKeyFrameMappings |  int16 number of key frame mappings contained in this file
KeyFrameMapping.Hash | 16 bytes md5 hash of key frame
KeyFrameMapping.PaletteIndex | int16 palette index
KeyFrameMapping.Offset | int64 offset in fsq file for replacement frames seq (or 0 if just palette switching)
KeyFrameMapping.Duration | int16 duration until switch back to default palette (if 0 don't switch back at all)

## pin2dmd.fsq
This file holds all sets of replacement frames sequences that can be used in key frame mappings.

Name | Description
------ | :-----------
SeqOfFrameSequencenes | int16 number of frame sequences included in this file
SeqOfFrames | int16 number of frames contained in this sequence
Frame.Index | int32 delay in ms for this frame to be displayed
Frame.NoOfPlanes | int16 number of planes (or subframes)
Frame.SizeOfPlane | int16 size of each plane in bytes
Frame.PlaneData | planes * sizeOfPlane bytes frame data for all planes HSB first, LS plane first (PPM format)



> Written with [StackEdit](https://stackedit.io/).