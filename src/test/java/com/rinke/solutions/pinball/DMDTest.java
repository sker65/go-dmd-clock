package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.almondtools.conmatch.datatypes.PrimitiveArrayMatcher.*;

import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.model.Frame;

public class DMDTest implements Observer {
	
	DMD dmd;
	private int height = 32;
	private int width = 128;
	private byte[] plane;
	private Frame frame;
	private boolean notified;
	private byte[] framePlane0;
	private byte[] framePlane1;
	
	@Before
	public void setup() {
		Random rand = new Random();
		dmd = new DMD(width,height);
		plane = new byte[dmd.getPlaneSizeInByte()];
		framePlane0 = new byte[512];
		rand.nextBytes(framePlane0);
		framePlane1 = new byte[512];
		rand.nextBytes(framePlane1);

		frame = new Frame(framePlane0, framePlane1);
		dmd.addObserver(this);
		this.notified = false;
	}

	@Test
	public void testCopyLastBuffer() throws Exception {
		dmd.setPixel(0, 0, 1);
		dmd.addUndoBuffer();
		dmd.copyLastBuffer();
		assertThat(dmd.getPixel(0, 0), is(1));
	}

	@Test
	public void testAddUndoBuffer() throws Exception {
		dmd.addUndoBuffer();
		assertThat(dmd.canUndo(), is(true));
	}

	@Test
	public void testUndo() throws Exception {
		dmd.undo();
		dmd.addUndoBuffer();
		dmd.setPixel(0, 0, 1);
		dmd.undo();
		assertThat(dmd.getPixel(0, 0), is(0));
	}

	@Test
	public void testRedo() throws Exception {
		dmd.addUndoBuffer();
		dmd.setPixel(0, 0, 1);
		dmd.undo();
		assertThat(dmd.getPixel(0, 0), is(0));
		dmd.redo();
		assertThat(dmd.getPixel(0, 0), is(1));
	}

	@Test
	public void testCanUndo() throws Exception {
		dmd.canUndo();
	}

	@Test
	public void testSetPixel() throws Exception {
		dmd.setPixel(0, 0, 1);
		assertThat( dmd.getPixel(0, 0), is(1));
		assertThat( dmd.getFrame().planes.get(0).data[0], is( (byte)0x80 ));
	}

	@Test
	public void testGetPixelWithoutMask() throws Exception {
		dmd.getPixelWithoutMask(0, 0);
	}

	@Test
	public void testGetPixelIntInt() throws Exception {
		dmd.getPixel(0, 0);
	}

	@Test
	public void testWriteOr() throws Exception {
		dmd.writeOr(frame);
	}

	@Test
	public void testCopy() throws Exception {
		DMD src = new DMD(width, height);
		dmd.copy(0, 0, src , false, false);
	}

	@Test
	public void testFill() throws Exception {
		byte[] target = new byte[dmd.getPlaneSizeInByte()];
		Arrays.fill(target, (byte)1);
		dmd.fill((byte) 1);
		assertThat(dmd.getFrame().planes.get(0).data, byteArrayContaining(target));
		dmd.setMask(plane);
		dmd.fill((byte) 1);
		assertThat(dmd.getFrame().mask.data, byteArrayContaining(target));
	}

	@Test
	public void testSetMaskPixel() throws Exception {
		dmd.setMaskPixel(0, 0, true);
		dmd.setMask(plane);
		dmd.setMaskPixel(0, 0, true);
		assertThat(dmd.getFrame().mask.data[0], equalTo((byte)0x80));
	}

	@Test
	public void testRemoveMask() throws Exception {
		dmd.setMask(plane);
		dmd.removeMask();
		assertThat( dmd.getFrame().mask, is(nullValue()));
		dmd.removeMask();
	}

	@Test
	public void testSetFrame() throws Exception {
		dmd.setFrame(frame);
		assertThat(dmd.getFrame().planes.get(0).data, byteArrayContaining(framePlane0));
		assertThat(this.notified, is(true));
	}

	@Override
	public void update(Observable o, Object arg) {
		this.notified = true;
	}

	@Test
	public void testClear() throws Exception {
		dmd.clear();
		assertThat(dmd.getFrame().planes.get(0).data, byteArrayContaining(new byte[dmd.getPlaneSizeInByte()]));
	}

}
