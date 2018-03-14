package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class ScenesCmdHandlerTest {
	@Mock
	private AnimationHandler animationHandler;
	//@InjectMocks
	private ScenesCmdHandler uut;
	private ViewModel vm;

	@Before
	public void setUp() throws Exception {
		this.vm = new ViewModel();
		this.uut = new ScenesCmdHandler(vm);
		this.uut.animationHandler = animationHandler;
	}
	
	@Test
	public void testRenameSceneShouldAdjustKey() throws Exception {
		uut.onRenameScene("old", "new"); // test free run with no scenes
		CompiledAnimation cani = new CompiledAnimation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		vm.scenes.put("old", cani);
		uut.onRenameScene("old", "new");
		assertTrue(vm.scenes.containsKey("new"));
	}

	@Test
	public void testRenameSceneShouldAdjustKeyframe() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.frameSeqName = "old";
		vm.keyframes.put(p.name,p);
		uut.onRenameScene("old", "new");
		assertEquals("new", p.frameSeqName);
	}
	
	@Test
	public void testRenameSceneShouldAdjustBookmark() throws Exception {
		Set<Bookmark> set = new TreeSet<>();
		vm.bookmarksMap.put("foo", set);
		Bookmark bookmark = new Bookmark("old", 0);
		set.add(bookmark);
		uut.onRenameScene("old", "new");
		assertEquals("new", set.iterator().next().name); // new bookmark
	}

	@Test
	public void testOnSetScenePalette() throws Exception {
		CompiledAnimation cani = new CompiledAnimation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		cani.setDesc("scene1");
		vm.setSelectedScene(cani);
		RGB[] rgb = {};
		vm.setSelectedPalette(new Palette(rgb , 15, "foo"));
		
		PalMapping p = new PalMapping(0, "foo");
		p.frameSeqName = "scene1";
		p.palIndex = 1;
		vm.keyframes.put(p.name,p);
		
		uut.onSetScenePalette();
		assertEquals(15,cani.getPalIndex());
		// also check keyframe
		assertEquals(1,p.palIndex);
		
		p.switchMode = SwitchMode.PALETTE;
		uut.onSetScenePalette();
		assertEquals(1,p.palIndex);
		
		p.switchMode = SwitchMode.REPLACE;
		uut.onSetScenePalette();
		assertEquals(15,p.palIndex);
	}


	@Test
	public void testOnDeleteScene() throws Exception {
//		throw new RuntimeException("not yet implemented");
	}

	@Test
	public void testOnSortScenes() throws Exception {
//		throw new RuntimeException("not yet implemented");
	}

	@Test
	public void testUpdateBookmarkNames() throws Exception {
//		throw new RuntimeException("not yet implemented");
	}

	@Test
	public void testOnRenameScene() throws Exception {
//		throw new RuntimeException("not yet implemented");
	}

}
