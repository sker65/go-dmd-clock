package com.rinke.solutions.pinball.widget;

import java.util.List;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.util.ByteUtil;

public class PasteTool extends DrawTool {

	private byte[] dataToPaste;
	private byte[] emptyMaskRow = new byte[PinDmdEditor.DMD_WIDTH/8];
	private byte[] emptyRow = new byte[PinDmdEditor.DMD_WIDTH/8];
	private boolean maskOnly;
	
	public PasteTool(int actualColor) {
		super(actualColor);
		Arrays.fill(emptyMaskRow, (byte) 0xFF);
		Arrays.fill(emptyRow, (byte) 0);
	}

	@Override
	public boolean mouseMove(int ix, int iy) {
		if( pressedButton >0 ) {
			int x = ix;// - x1;
			int y = iy;// - y1;
			dmd.copyLastBuffer();
			if( maskOnly ) {
				byte[] plane = copyShiftedPlane(x, y, 0, true);
				dmd.ensureMask(plane);
			} else {
				int mask = dmd.getDrawMask();
				Frame f = dmd.getFrame();
				int sPos = 0;
				for( int j = 0; j < f.planes.size(); j++) {
					if (((1 << j) & mask) != 0) {
						if( dataToPaste.length>= sPos+PinDmdEditor.PLANE_SIZE) {
							byte[] plane = copyShiftedPlane(x, y, sPos, false);
							System.arraycopy(plane, 0, f.planes.get(j).plane, 0, PinDmdEditor.PLANE_SIZE);
						}
						sPos += PinDmdEditor.PLANE_SIZE;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	// TODO correct handling of negative y (copy order)
	private byte[] copyShiftedPlane(int x, int y, int planeOffset, boolean filling) {
		byte[] plane = new byte[PinDmdEditor.PLANE_SIZE];
		System.arraycopy(dataToPaste, 0, plane, 0, Math.min(dataToPaste.length, plane.length));
		byte[] rowBytes = new byte[PinDmdEditor.DMD_WIDTH/8];
		int bytesPerRow = dmd.getBytesPerRow();
		for( int row = PinDmdEditor.DMD_HEIGHT-1; row>= 0; row--) {
			if( row + y >= 0 && row + y < PinDmdEditor.DMD_HEIGHT) {
				// copy shifted row
				System.arraycopy(plane, bytesPerRow*row, rowBytes, 0, bytesPerRow);
				ByteUtil.shift(rowBytes, x, filling);
				System.arraycopy(rowBytes, 0, plane, bytesPerRow*(row+y), bytesPerRow);
			} else {
//			if( row < y ){
				// fill row with 1 or 0
				System.arraycopy(filling ? emptyMaskRow : emptyRow, 0, plane, bytesPerRow*row, bytesPerRow);
			}
		}
		return plane;
	}

	@Override
	public boolean mouseUp(int x, int y) {
		return super.mouseUp(x, y);
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.addUndoBuffer();
		return true;
	}

	public byte[] getDataToPaste() {
		return dataToPaste;
	}

	public void setDataToPaste(byte[] dataToPaste) {
		 this.dataToPaste = dataToPaste;
	}

	public void setMaskOnly(boolean val) {
		this.maskOnly = val;
	}

}
