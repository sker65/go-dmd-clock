package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bouncycastle.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.MessageUtil;

@RunWith(MockitoJUnitRunner.class)
public class KeyframeHandlerTest extends HandlerTest {
	
	@Mock
	MessageUtil messageUtil;
	
	@Mock
	HashCmdHandler hashCmdHandler;
	@Mock
	MaskHandler maskHandler;
	
	@InjectMocks
	KeyframeHandler uut = new KeyframeHandler(vm);

	Mask mask = new Mask(vm.dmdSize.planeSize);
	
	@Before
	public void setup() {
		vm.hashes.add(new byte[] { 1, 2, 3, 4 });
		vm.selectedRecording = getScene("foo");
	}

	@Test
	public void testCheckForDuplicateKeyFrames() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		assertFalse(uut.checkForDuplicateKeyFrames(p));
		vm.keyframes.put(p.name,p);
		assertTrue(uut.checkForDuplicateKeyFrames(p));
	}

	@Test
	public void testOnSelectedKeyFrameChanged() throws Exception {
		PalMapping p = getKeyframe();
		CompiledAnimation rec = getScene("drwho-dump");
		vm.recordings.put("drwho-dump",rec);
		uut.onSelectedKeyFrameChanged(null, p);
		assertEquals(vm.selectedRecording, rec);
	}

	@Test
	public void testOnSelectedKeyFrameChangedWithMask() throws Exception {
		PalMapping p = getKeyframe();
		p.withMask = true;
		uut.onSelectedKeyFrameChanged(null, p);
	}

	@Test
	public void testOnSelectedKeyFrameChangedToNull() throws Exception {
		uut.onSelectedKeyFrameChanged(getKeyframe(), null);
	}

	@Test
	public void testOnSelectedKeyFrameChangedWithScene() throws Exception {
		PalMapping p = getKeyframe();
		p.frameSeqName = "scene 0";
		p.palIndex = 4;
		CompiledAnimation rec = getScene("drwho-dump");
		CompiledAnimation scene = getScene("scene 0");
		scene.setPalIndex(5);
		vm.recordings.put("drwho-dump",rec);
		vm.scenes.put("scene 0",scene);
		uut.onSelectedKeyFrameChanged(null, p);
		assertEquals(vm.selectedRecording, rec);
		assertEquals(vm.selectedFrameSeq, scene);
		assertEquals(4, vm.selectedPalette.index);
	}

	@Test
	public void testOnSelectedKeyFrameChangedWithEvent() throws Exception {
		PalMapping p = getKeyframe();
		p.switchMode = SwitchMode.EVENT;
		p.durationInMillis = 258;
		uut.onSelectedKeyFrameChanged(null, p);
		assertEquals(1, vm.selectedSpinnerDeviceId);
		assertEquals(2, vm.selectedSpinnerEventId);
		assertEquals(258, vm.duration);
	}

	PalMapping getKeyframe() {
		PalMapping p = new PalMapping(0,"foo");
		p.animationName = "drwho-dump";
		p.frameIndex = 0;
		p.switchMode = SwitchMode.PALETTE;
		return p;
	}

	@Test
	public void testOnSortKeyFrames() throws Exception {
		uut.onSortKeyFrames();
	}

	@Test
	public void testOnSetKeyframePalette() throws Exception {
		vm.selectedKeyFrame = getKeyframe();
		vm.selectedPalette = Palette.getDefaultPalettes().get(1);
		uut.onSetKeyframePalette();
		assertEquals(1, vm.selectedKeyFrame.palIndex);
	}

	@Test
	public void testCheckReleaseMask() throws Exception {
		PalMapping k = getKeyframe();
		vm.keyframes.put("kf1",k);
		k = getKeyframe();
		k.withMask = true;
		k.maskNumber = 1;
		vm.keyframes.put("kf2",k);
		vm.masks.get(0).locked = true;
		vm.masks.get(1).locked = true;
		
		vm.showMask = true;
		vm.selectedMaskNumber = 0;
		
		uut.checkReleaseMask();
		assertFalse(vm.masks.get(0).locked);
	}

	@Test
	public void testOnDeleteKeyframe() throws Exception {
		vm.selectedKeyFrame = getKeyframe();
		uut.onDeleteKeyframe();
	}
		
	@Test
	public void testOnAddKeyframeWithSelectedSequenceAndReplace() throws Exception {
		CompiledAnimation ani = getScene("foo");
		vm.setSelectedRecording(ani);
		vm.setSelectedFrameSeq(ani);
		uut.onAddKeyframe(SwitchMode.REPLACE);
	}

	@Test
	public void testOnAddKeyframeWithSelectedSequenceAndColMask() throws Exception {
		CompiledAnimation ani = getScene("foo");
		vm.setSelectedRecording(ani);
		vm.setSelectedFrameSeq(ani);
		uut.onAddKeyframe(SwitchMode.ADD);
	}

	@Test
	public void testOnAddKeyFrameWithReplace() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		uut.onAddKeyframe(SwitchMode.REPLACE);
		assertEquals(1, vm.keyframes.size());
	}

	@Test
	public void testOnAddKeyFrameWithSitchModeFromScene() throws Exception {
		CompiledAnimation scene = getScene("foo");
		vm.setSelectedRecording(ani);
		scene.setEditMode(EditMode.COLMASK_FOLLOW);
		vm.setSelectedFrameSeq(scene);
		uut.onAddKeyframe(null);
		PalMapping k = getFristKeyframe();
		assertEquals(SwitchMode.FOLLOW, k.switchMode);
	}

	@Test
	public void testOnAddKeyFrameWithSitchModeFromSceneLayered() throws Exception {
		CompiledAnimation scene = getScene("foo");
		vm.setSelectedRecording(ani);
		scene.setEditMode(EditMode.LAYEREDCOL);
		vm.setSelectedFrameSeq(scene);
		vm.setSelectedScene(scene);
		uut.onAddKeyframe(null);
		PalMapping k = getFristKeyframe();
		assertEquals(SwitchMode.LAYEREDCOL, k.switchMode);
	}

	@Test
	public void testOnAddKeyFrameWithFollow() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		uut.onAddKeyframe(SwitchMode.FOLLOW);
		PalMapping k = getFristKeyframe();
	}

	@Test
	public void testOnAddKeyFrameWithEvent() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		vm.setSelectedSpinnerDeviceId(1);
		uut.onAddKeyframe(SwitchMode.EVENT);
		PalMapping k = getFristKeyframe();
		assertEquals(256,k.durationInMillis);
		assertEquals(SwitchMode.EVENT, k.switchMode);
		assertNull(k.frameSeqName);
	}
	
	PalMapping getFristKeyframe() {
		assertEquals(1, vm.keyframes.size());
		return vm.keyframes.values().iterator().next();
	}

	@Test
	public void testOnAddKeyFrameWithMask() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		vm.showMask = true;
		vm.dmd.setMask(mask);
		uut.onAddKeyframe(SwitchMode.ADD);
		PalMapping k = getFristKeyframe();
		assertEquals(true, k.withMask);
		assertEquals(SwitchMode.ADD, k.switchMode);
		verify( maskHandler, atLeastOnce()).commitMaskIfNeeded(true);
	}

	@Test
	public void testOnSelectedSpinnerEventIdChanged() throws Exception {
		vm.selectedKeyFrame = getKeyframe();
		vm.selectedKeyFrame.switchMode = SwitchMode.EVENT;
		uut.onSelectedSpinnerEventIdChanged(0, 1);
		assertEquals(1, vm.duration);
	}

	@Test
	public void testOnSelectedSpinnerEventIdChangedWithoutSelection() throws Exception {
		uut.onSelectedSpinnerEventIdChanged(0, 1);
		assertEquals(0, vm.duration);
	}

	@Test
	public void testOnSelectedSpinnerDeviceIdChanged() throws Exception {
		vm.selectedKeyFrame = getKeyframe();
		vm.selectedKeyFrame.switchMode = SwitchMode.EVENT;
		uut.onSelectedSpinnerDeviceIdChanged(0, 1);
		assertEquals(256, vm.duration);
	}

	@Test
	public void testOnSelectedSpinnerDeviceIdChangedWithoutSelection() throws Exception {
		uut.onSelectedSpinnerDeviceIdChanged(0, 1);
		assertEquals(0, vm.duration);
	}

	@Test
	public void testGetName() throws Exception {
		String n = uut.getName(SwitchMode.PALETTE, null);
		assertEquals("KeyFrame 1", n);
	}

	@Test
	public void testGetNameWithDuplicate() throws Exception {
		vm.keyframes.put("KeyFrame 1", getKeyframe());
		CompiledAnimation scene = getScene("1");
		String n = uut.getName(SwitchMode.ADD, scene);
		assertEquals("KeyFrame 1 1", n);	}

}
