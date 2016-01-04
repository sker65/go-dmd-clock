# File Formats
All binary number are written in **big endian** format, which means most significant bytes first. Basic data types are binary numbers like int8, int16, int32 and int64 or just raw byte data.
For colors 3 byte rgb values are used. Frames stored are raw uncompressed pixel map, plane by plane, lowest significant plane first, left pixel is HSB (like PPM image format).
For repeated structures there is a SeqOf... description given, which is always started by the number of data strutures that follows.

## pin2dmd.dat

This file contains just the default device mode, default palette, custom smart dmd signature (if any) and optional custom display timing parameters

> pin2dmd.dat | Type | Description
>  :----- | --- | :-----------
 DeviceMode | int8 | 0: pinmame rgb, 1: pinmame mono, 2: wpc, 3: stern
 DefaultPaletteIndex | int8 | number (or index) of default palette
 CustomSmartDMDSig | 8 bytes | custom smart dmd signature to use

> > Timing | Type | Description
> >  :----- | --- | :-----------
> >  Total | int16  |custom timing parameters: complete duty period
DutyPlane0 | int16  |custom timing parameters: duty period plane 0
DutyPlane1 | int16  |custom timing parameters: duty period plane 1
DutyPlane2 | int16  |custom timing parameters: duty period plane 2
DutyPlane3 | int16  |custom timing parameters: duty period plane 3
 

## palettes.dat
This file contains custom palette definition and key frame definitions for palette switches or insertion of replacement frames.

> palettes.dat | Type | Description
>  :----- | --- | :-----------
> Version | int8 | version of file (actually 1)
> SeqOfPalettes | int16 | number of palettes contained in this file

> > Palette | Type | Description
> >  :----- | --- | :-----------
> > Index | int16 | palette index
> > NoOfColors | int16 | number of colors contained in palette
> > Type | int8 | type of palette. 0: normal, 1: default (only one palette per file could be marked as default)
> > SeqOfRGB | n * 3 bytes | sequence rgb values (3 bytes) of colors in this palette

> &nbsp; | &nbsp; | &nbsp; 
>  :----- | --- | :-----------
> SeqOfKeyFrameMappings |  int16 | number of key frame mappings contained in this file

> > KeyFrameMapping | Type | Description
> >  :----- | --- | :-----------
> > Hash | 16 bytes | md5 hash of key frame
> > PaletteIndex | int16 | palette index
> > Offset | int64 | offset in fsq file for replacement frames seq (or 0 if just palette switching)
> > Duration | int16 | duration until switch back to default palette (if 0 don't switch back at all)

## pin2dmd.fsq
This file holds all sets of replacement frames sequences that can be used in key frame mappings.

> pin2dmd.fsq | Type | Description
> :----- | --- | :-----------
> SeqOfFrameSequencenes | int16 | number of frame sequences included in this file

> > FrameSequence | Type | Description
> > :----- | --- | :-----------
> > SeqOfFrames | int16 | number of frames contained in this sequence

> > > Frame | Type | Description
> > > :----- | --- | :-----------
> > > Index | int32 | delay in ms for this frame to be displayed
NoOfPlanes | int16 | number of planes (or subframes)
SizeOfPlane | int16 | size of each plane in bytes
PlaneData | planes * sizeOfPlane bytes | frame data for all planes HSB first, LS plane first (PPM format)

--- 

> Written with [StackEdit](https://stackedit.io/).