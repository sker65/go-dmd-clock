package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation.EditMode;

@RunWith(MockitoJUnitRunner.class)
public class MaskHandlerTest extends HandlerTest  {
	
	MaskHandler uut;
	
	@Mock HashCmdHandler hashCmdHandler;
	@Mock DrawCmdHandler drawCmdHandler;
	
	@Before
	public void setup() {
		uut = new MaskHandler(vm);
		uut.hashCmdHandler = hashCmdHandler;
		uut.drawCmdHandler = drawCmdHandler;
	}

	@Test
	public void testOnInvertMask() throws Exception {
		byte[] data = new byte[512];
		vm.dmd.setMask(data);
		uut.onInvertMask();
		assertEquals((byte) 0xFF, (byte) vm.dmd.getFrame().mask.data[0]);
	}

	@Test
	public void testOnDeleteColMaskd() throws Exception {
		uut.onDeleteColMask();
		assertTrue(vm.dmdDirty);
		// test filling of dmd in dmd itself
	}

	@Test
	public void testOnDetectionMaskActiveChanged() throws Exception {
		vm.setSelectedEditMode(EditMode.FIXED);
		uut.onDetectionMaskActiveChanged(false, true);
		verify(hashCmdHandler).updateHashes(anyObject());
		verify(drawCmdHandler).setDrawMaskByEditMode(eq(EditMode.FIXED));
	}

	@Test
	public void testOnDeleteColMask() throws Exception {
		uut.onDeleteColMask();
	}

	@Test
	public void testOnSelectedMaskChanged() throws Exception {
		vm.setSelectedEditMode(EditMode.REPLACE);
		uut.onSelectedMaskChanged(0, 1);
	}

	@Test
	public void testOnSelectedMaskChangedLayered() throws Exception {
		vm.setSelectedScene(getScene("foo"));
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.onSelectedMaskChanged(0, 1);
	}

	@Test
	public void testGetCurrentMask() throws Exception {
		vm.setSelectedEditMode(EditMode.REPLACE);
		uut.getCurrentMask();
	}

	@Test
	public void testGetCurrentMaskWithScene() throws Exception {
		vm.setSelectedScene(getScene("foo"));
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.getCurrentMask();
	}

	@Test
	public void testOnLayerMaskActiveChanged() throws Exception {
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.onLayerMaskActiveChanged(false, true);
	}

	@Test
	public void testCommitMaskIfNeeded() throws Exception {
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.commitMaskIfNeeded();
	}


}
