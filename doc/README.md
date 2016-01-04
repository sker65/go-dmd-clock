# File Formats

## pin2dmd.dat

This file contains just the default device mode, default palette, custom smart dmd signature (if any) and optional custom display timing parameters

Offset     | Length | Description
---------- | ------ | :-----------
0 | 1 Byte | device mode. 0: pinmame rgb, 1: pinmame mono, 2: wpc, 3: stern
1 | 1 Byte | number (or index) of default palette
2 | 8 Byte | custom smart dmd signature to use
10 | 10 Byte | custom timing parameters. 5 x 16bit uints for timing, first overall period, 4x periods for 4 subframe brightnesses

## palettes.dat
This file contains custom palette definition and key frame definitions for palette switches or insertion of replacement frames.

As there are a couple of sections in that file, each section is described separately.
 


> Written with [StackEdit](https://stackedit.io/).