package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.util.MessageUtil;

@RunWith(MockitoJUnitRunner.class)
public class KeyframeHandlerTest extends HandlerTest {
	
	@Mock
	MessageUtil messageUtil;
	
	@Mock
	HashCmdHandler hashCmdHandler;
	
	@InjectMocks
	KeyframeHandler uut = new KeyframeHandler(vm);
	
	@Before
	public void setup() {
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
		uut.onSelectedKeyFrameChanged(null, p);
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
		uut.onSetKeyframePalette();
	}

	@Test
	public void testCheckReleaseMask() throws Exception {
		uut.checkReleaseMask();
	}

	@Test
	public void testOnDeleteKeyframe() throws Exception {
		uut.onDeleteKeyframe();
	}

	@Test
	public void testOnAddFrameSeq() throws Exception {
		uut.onAddFrameSeq(EditMode.REPLACE);
		verify(messageUtil).warn(anyString(), anyString());
	}

	@Test
	public void testOnAddFrameSeqWithSelectedKeyframe() throws Exception {
		CompiledAnimation ani = getScene("foo");
		vm.setSelectedRecording(ani);
		vm.setSelectedFrameSeq(ani);
		vm.setSelectedHashIndex(0);
		vm.hashes.add( new byte[]{0,1,2,3});
		uut.onAddFrameSeq(EditMode.REPLACE);
	}

	@Test
	public void testOnAddKeyFrame() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		vm.hashes.add( new byte[]{0,1,2,3});
		uut.onAddKeyFrame(SwitchMode.REPLACE);
		assertEquals(1, vm.keyframes.size());
	}

	@Test
	public void testOnAddKeyFrameWithMask() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		vm.hashes.add( new byte[]{0,1,2,3});
		vm.showMask = true;
		uut.onAddKeyFrame(SwitchMode.ADD);
		assertEquals(1, vm.keyframes.size());
		PalMapping keyframe = vm.keyframes.values().iterator().next();
		assertEquals(true, keyframe.withMask);
	}

	@Test
	public void testUpdateKeyFrameButtons() throws Exception {
		uut.updateKeyFrameButtons(getScene(""), getScene(""), 0);
	}

	@Test
	public void testOnSelectedSpinnerEventIdChanged() throws Exception {
		uut.onSelectedSpinnerEventIdChanged(0, 1);
	}

	@Test
	public void testOnSelectedSpinnerDeviceIdChanged() throws Exception {
		uut.onSelectedSpinnerDeviceIdChanged(0, 1);
	}

}
