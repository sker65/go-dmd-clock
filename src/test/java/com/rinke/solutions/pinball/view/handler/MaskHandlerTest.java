package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Mask;

@RunWith(MockitoJUnitRunner.class)
public class MaskHandlerTest extends HandlerTest  {
	
	@Mock HashCmdHandler hashCmdHandler;
	@Mock DrawCmdHandler drawCmdHandler;

	@InjectMocks
	MaskHandler uut = new MaskHandler(vm);
	
	Mask mask = new Mask(vm.dmdSize.planeSize);
	
	@Before
	public void setup() {
		uut.animationHandler = new AnimationHandler(vm, null, dmd);
	}

	@Test
	public void testOnInvertMask() throws Exception {
		vm.setSelectedScene(getScene("foo"));
		vm.setSelectedEditMode(EditMode.FIXED);
		mask.data[0] = 0;
		vm.dmd.setMask(mask);
		uut.onInvertMask();
		assertEquals((byte) 0xFF, (byte) vm.dmd.getFrame().mask.data[0]);
		assertEquals((byte) 0x00, (byte) vm.dmd.getFrame().mask.data[1]);
	}

	@Test
	public void testOnDeleteColMaskd() throws Exception {
		vm.setSelectedScene(getScene("foo"));
		vm.setSelectedEditMode(EditMode.FIXED);
		mask.data[0] = 0;
		vm.dmd.setMask(mask);
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
		vm.setSelectedScene(getScene("foo"));
		vm.setSelectedEditMode(EditMode.FIXED);
		mask.data[0] = 0;
		vm.dmd.setMask(mask);
		uut.onDeleteColMask();
	}

	@Test
	public void testOnSelectedMaskChanged() throws Exception {
		vm.setSelectedEditMode(EditMode.REPLACE);
		uut.onSelectedMaskNumberChanged(0, 1);
	}

	@Test
	public void testOnSelectedMaskChangedLayered() throws Exception {
		vm.setSelectedScene(getScene("foo"));
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.onSelectedMaskNumberChanged(0, 1);
	}

	@Test
	public void testOnLayerMaskActiveChanged() throws Exception {
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.onLayerMaskActiveChanged(false, true);
	}

	@Test
	public void testCommitMaskIfNeeded() throws Exception {
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		uut.commitMaskIfNeeded(false);
	}
	
	@Test
	public void testCommitMaskIfNeededWithMask() throws Exception {
		vm.setSelectedEditMode(EditMode.LAYEREDCOL);
		vm.setSelectedScene(getScene("foo"));
		vm.dmd.setMask(mask);
		vm.dmd.addUndoBuffer();
		uut.commitMaskIfNeeded(false);
		assertTrue( vm.dirty );
	}

	@Test
	public void testCommitMaskIfNeededWithLockedMask() throws Exception {
		vm.setSelectedEditMode(EditMode.FIXED);
		vm.setSelectedScene(getScene("foo"));
		vm.dmd.setMask(mask);
		vm.dmd.addUndoBuffer();
		vm.masks.get(0).locked = true;
		uut.commitMaskIfNeeded(false);
		assertFalse( vm.dirty );
	}

}
