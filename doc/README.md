# File Formats
All binary number are written in **big endian** format, which means most significant bytes first. Basic data types are binary numbers like int8, int16 and int32 or just raw byte data.
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
 ------ | :-----------
 File | int8 version of file (actually 1)
 SeqOfPalettes | int16 number of palettes contained in this file
 Palette | int16 palette index
 - | int16 number of colors contained in palette
 - | int8 type of palette. 0: normal, 1: default (only one palette per file could be marked as default)
 - | rgb value (3 bytes) of colors in this palette
 SeqOfKeyFrameMappings |  int16 number of key frame mappings contained in this file
 

## pin2dmd.fsq
This file holds all sets of replacement frames sequences that can be used in key frame mappings.

Offset     | Length | Description
---------- | ------ | :-----------
0	| 2 bytes | int16 number of frame sequences included in this file
2.0 | 2 bytes | int16 number of frames contained in this sequence
2.2.0 | 4 bytes | int32 delay in ms for this frame to be displayed
2.2.4 | 2 bytes | int16 number of planes (or subframes)
2.2.6 | 2 bytes | int16 size of each plane in bytes
2.2.8 | planes * sizeOfPlane | frame data for all planes HSB first, LS plane first (PPM format)
2.2.X | ... | repeated for all frames in this sequence
2.X | ... | repeated for all sequences in this file


> Written with [StackEdit](https://stackedit.io/).