package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.ui.EditLinkView;
import com.rinke.solutions.pinball.util.MessageUtil;

@RunWith(MockitoJUnitRunner.class)
public class ScenesCmdHandlerTest extends HandlerTest {
	@Mock
	private AnimationHandler animationHandler;
	
	@Mock DrawCmdHandler drawCmdHandler;
	@Mock MaskHandler maskHandler;
	@Mock MessageUtil messageUtil;
	@Mock EditLinkView editLink;

	@InjectMocks
	private ScenesCmdHandler uut = new ScenesCmdHandler(vm);

	@Before
	public void setUp() throws Exception {
		// setup some hashes
		vm.hashes.add( new byte[]{0,1,2,3});
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
		CompiledAnimation cani = getScene("old.txt");
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
		uut.onDeleteScene();
	}

	@Test
	public void testOnDeleteSceneWithRefInKeyframe() throws Exception {
		CompiledAnimation scene = getScene("foo");
		vm.scenes.put("foo", scene);
		vm.setSelectedScene(scene);
		PalMapping palMapping = new PalMapping(1, "k1");
		palMapping.frameSeqName = "foo";
		vm.keyframes.put("k1", palMapping );
		uut.onDeleteScene();
		verify(messageUtil).warn(anyString(), anyString());
		assertTrue( vm.scenes.containsKey("foo")); // its reference therefore will not deleted
	}

	@Test
	public void testOnDeleteSceneWithSelected() throws Exception {
		CompiledAnimation scene = getScene("foo");
		vm.scenes.put("foo", scene);
		vm.setSelectedScene(scene);
		uut.onDeleteScene();
		assertFalse( vm.scenes.containsKey("foo"));
	}

	@Test
	public void testOnSortScenes() throws Exception {
		uut.onSortScenes();
	}

	@Test
	public void testUpdateBookmarkNames() throws Exception {
		Set<Bookmark> bm = new HashSet<Bookmark>();
		vm.bookmarksMap.put("old", bm );
		uut.updateBookmarkNames("old", "new");
		assertFalse( vm.bookmarksMap.containsKey("new"));
	}

	@Test
	public void testOnRenameScene() throws Exception {
		uut.onRenameScene("old", "new");
	}

	@Test
	public void testOnSelectedSceneChanged() throws Exception {
		CompiledAnimation nextScene = getScene("foo");
		uut.onSelectedSceneChanged(null , nextScene);
		assertEquals(vm.hashVal,"");
	}

	@Test
	public void testOnSelectedSceneChangedWithRecordingLink() throws Exception {
		CompiledAnimation nextScene = getScene("foo");
		RecordingLink rl = new RecordingLink("rl-foo", 4711);
		nextScene.setRecordingLink(rl);
		uut.onSelectedSceneChanged(null , nextScene);
		assertEquals(vm.linkVal,"rl-foo:4711");
	}

	@Test
	public void testOnSelectedSceneChangedWithHash() throws Exception {
		CompiledAnimation nextScene = getScene("foo");
		nextScene.setEditMode(EditMode.LAYEREDCOL);
		Frame frame = nextScene.getActualFrame();
		frame.crc32 = new byte[]{1,2,3,4};
		uut.onSelectedSceneChanged(null , nextScene);
		assertEquals("01020304",vm.hashVal);
	}

	@Test
	public void testOnSelectedSceneChangedWithOldScene() throws Exception {
		CompiledAnimation oldScene = getScene("old");
		CompiledAnimation nextScene = getScene(null);
		uut.onSelectedSceneChanged(oldScene , nextScene);
	}

	@Test
	public void testOnSelectedSceneChangedWithNextSceneNull() throws Exception {
		CompiledAnimation oldScene = getScene("old");
		uut.onSelectedSceneChanged(oldScene , null);
		assertEquals(vm.hashVal,"");
	}

	@Test
	public void testOnUnlockSceneMasks() throws Exception {
		vm.selectedScene = getScene("foo");
		when(messageUtil.warn(eq(0), anyString(), anyString(), anyString(), anyObject(), eq(2))).thenReturn(2);
		uut.onUnlockSceneMasks();
	}

	@Test
	public void testOnEditLink() throws Exception {
		vm.selectedScene = getScene("foo");
		uut.onEditLink();
	}

	@Test
	public void testOnEditLinkWithSetting() throws Exception {
		vm.selectedScene = getScene("foo");
		when(editLink.okClicked()).thenReturn(true);
		RecordingLink rl = new RecordingLink("foo", 1234);
		when(editLink.getRecordingLink()).thenReturn(rl);
		uut.onEditLink();
		assertThat( vm.selectedScene.getRecordingLink().associatedRecordingName, is("foo") );
	}
	
}