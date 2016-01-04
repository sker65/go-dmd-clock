# File Formats

## pin2dmd.dat

This file contains just the default device mode, default palette, custom smart dmd signature (if any) and optional custom display timing parameters

Offset     | Length | Description
---------- | ------ | :-----------
0 | 1 Byte | device mode. 0: pinmame rgb, 1: pinmame mono, 2: wpc, 3: stern
1 | 1 Byte | number (or index) of default palette
2 | 8 Byte | custom smart dmd signature to use

