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
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.util.MessageUtil;

@RunWith(MockitoJUnitRunner.class)
public class RecordingsCmdHandlerTest extends HandlerTest {
	
	@Mock AnimationHandler animationHandler;
	@Mock HashCmdHandler hashCmdHandler;
	@Mock MessageUtil messageUtil;
	
	@InjectMocks
	private RecordingsCmdHandler uut = new RecordingsCmdHandler(vm);

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testRenameRecordingShouldAdjustLink() throws Exception {
		uut.onRenameRecording("old", "new");
		Animation cani = new Animation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		vm.recordings.put("old", cani);
		CompiledAnimation scene = getScene("test");
		scene.setRecordingLink(new RecordingLink("old", 0));
		vm.scenes.put("scene", scene);
		uut.onRenameRecording("old", "new");
		assertEquals("new", scene.getRecordingLink().associatedRecordingName);
	}

	@Test
	public void testRenameRecordingShouldAdjustKey() throws Exception {
		uut.onRenameRecording("old", "new");
		Animation cani = new Animation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		vm.recordings.put("old", cani);
		uut.onRenameRecording("old", "new");
		assertTrue(vm.recordings.containsKey("new"));
	}

	@Test
	public void testRenameRecordingShouldAdjustKeyFrame() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.animationName = "old";
		vm.keyframes.put(p.name,p);
		uut.onRenameRecording("old", "new");
		assertEquals("new", p.animationName);
	}
	
	@Test
	public void testRenameRecordingShouldPoplateNameMap() throws Exception {
		Animation cani = new Animation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		vm.recordings.put("old", cani);
		uut.onRenameRecording("old", "new");
		assertTrue( vm.recordingNameMap.containsKey("old"));
		assertEquals("new", vm.recordingNameMap.get("old"));
	}

	@Test
	public void testOnSelectedRecordingChanged() throws Exception {
		uut.onSelectedRecordingChanged(null, getScene("foo"));
	}

	@Test
	public void testOnDeleteRecording() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		uut.onDeleteRecording();
	}

	@Test
	public void testOnDeleteRecordingWithKeyframe() throws Exception {
		vm.setSelectedRecording(getScene("foo"));
		PalMapping pal = new PalMapping(0, "pal");
		pal.animationName = "foo";
		vm.keyframes.put("k1", pal );
		uut.onDeleteRecording();
		verify(messageUtil).warn(anyString(), anyString());
	}

	@Test
	public void testOnSortRecording() throws Exception {
		uut.onSortRecording();
	}

}
