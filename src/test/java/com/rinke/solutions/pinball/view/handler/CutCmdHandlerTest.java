package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class CutCmdHandlerTest extends HandlerTest {
	@Mock
	private BookmarkHandler bookmarkHandler;

	@Mock
	private PaletteHandler paletteHandler;
	
	@InjectMocks
	private CutCmdHandler uut = new CutCmdHandler(vm);
	
	@Before
	public void setup() {
		vm.setSelectedScene(getScene("foo"));
	}

	@Test
	public void testBuildUniqueName() throws Exception {
		Animation animation = getScene("foo");
		vm.recordings.put("Scene 1", animation);
		String actual = uut.buildUniqueName(vm.recordings);
		assertNotEquals("Scene 1", actual);
	}

	@Test
	public void testCutScene() throws Exception {
		CompiledAnimation animation = getScene("foO");
		uut.cutScene(animation, 0, 0, "1");
	}

	@Test
	public void testCutSceneWithAddPal() throws Exception {
		uut.addPalWhenCut = true;
		CompiledAnimation animation = getScene("foO");
		uut.cutScene(animation, 0, 0, "1");
		verify(paletteHandler, times(1)).copyPalettePlaneUpgrade();
	}

	@Test
	public void testOnSelectedRecordingChanged() throws Exception {
		uut.onSelectedRecordingChanged(null, getScene("as"));
	}

	@Test
	public void testOnSelectedSceneChanged() throws Exception {
		uut.onSelectedSceneChanged(null, getScene("f"));
	}

	@Test
	public void testOnSelectedFrameChanged() throws Exception {
		uut.onSelectedFrameChanged(0, 1);
	}

	@Test
	public void testGetSourceAnimation() throws Exception {
		uut.getSourceAnimation();
	}

	@Test
	public void testOnMarkStart() throws Exception {
		uut.onMarkStart();
	}

	@Test
	public void testOnMarkEnd() throws Exception {
		uut.onMarkEnd();
	}

	@Test
	public void testOnCutScene() throws Exception {
		uut.onCutScene();
	}


}
