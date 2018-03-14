package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class RecordingsCmdHandlerTest extends HandlerTest {
	
	@Mock
	private AnimationHandler animationHandler;
	
	private RecordingsCmdHandler uut;

	@Before
	public void setUp() throws Exception {
		this.uut = new RecordingsCmdHandler(vm);
		this.uut.animationHandler = animationHandler;
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



}
