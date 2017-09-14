package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.PinDmdEditor.TabMode;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.test.Util;
import com.rinke.solutions.pinball.util.ApplicationProperties;
import com.rinke.solutions.pinball.util.RecentMenuManager;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorTest {

	@InjectMocks
	PinDmdEditor uut;

	byte[] digest = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

	@Mock
	RecentMenuManager recentAnimationsMenuManager;

	@Mock
	Pin2DmdConnector connector;

	@Mock
	AnimationHandler animationHandler;

	@Mock
	Observer editAniObserver;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setup() throws Exception {
		uut.licManager.verify("src/test/resources/#3E002400164732.key");
	}

	@Test
	public void testReplaceExtensionTo() throws Exception {
		String newName = uut.replaceExtensionTo("ani", "foo.xml");
		assertThat(newName, equalTo("foo.ani"));
	}

	@Test
	public void testOnExportProjectSelectedWithFrameMapping() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		p.switchMode = SwitchMode.PALETTE;
		p.frameSeqName = "foo";

		// List<Frame> frames = new ArrayList<Frame>();
		// FrameSeq fs = new FrameSeq(frames, "foo");
		// uut.project.frameSeqMap.put("foo", fs);

		uut.project.palMappings.add(p);

		// there must also be an animation called "foo"
		CompiledAnimation ani = new CompiledAnimation(AnimationType.COMPILED, "foo", 0, 0, 0, 0, 0);
		ani.setDesc("foo");
		uut.scenes.put("foo", ani);
		// finally put some frame data into it
		List<Frame> aniFrames = ani.getRenderer().getFrames();
		byte[] plane2 = new byte[512];
		byte[] plane1 = new byte[512];
		for (int i = 0; i < 512; i += 2) {
			plane1[i] = (byte) 0xFF;
			plane1[i + 1] = (byte) i;
			plane2[i] = (byte) 0xFF;
		}

		Frame frame = new Frame(plane1, plane2);
		frame.delay = 0x77ee77ee;
		aniFrames.add(frame);
		uut.exportProject(filename, f -> new FileOutputStream(f), true);
		// System.out.println(filename);
		assertNull(Util.isBinaryIdentical(filename, "./src/test/resources/mappingWithSeq.dat"));
		assertNull(Util.isBinaryIdentical(uut.replaceExtensionTo("fsq", filename), "./src/test/resources/testSeq.fsq"));

	}

	@Test
	public void testOnExportProjectSelectedWithMapping() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		p.switchMode = SwitchMode.PALETTE;

		uut.project.palMappings.add(p);

		uut.exportProject(filename, f -> new FileOutputStream(f), true);

		// create a reference file and compare against
		assertNull(Util.isBinaryIdentical(filename, "./src/test/resources/palettesOneMapping.dat"));
	}

	@Test
	public void testOnExportProjectSelectedEmpty() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		uut.exportProject(filename, f -> new FileOutputStream(f), true);
		// System.out.println(filename);

		// create a reference file and compare against
		assertNull(Util.isBinaryIdentical(filename, "./src/test/resources/defaultPalettes.dat"));
	}

	@Test
	public void testOnImportProjectSelectedString() throws Exception {
		uut.aniAction = new AnimationActionHandler(uut, null);
		uut.importProject("./src/test/resources/test.xml");
		verify(recentAnimationsMenuManager).populateRecent(eq("./src/test/resources/drwho-dump.txt.gz"));
	}

	@Test
	public void testCheckForDuplicateKeyFrames() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		assertFalse(uut.checkForDuplicateKeyFrames(p));
		uut.project.palMappings.add(p);
		assertTrue(uut.checkForDuplicateKeyFrames(p));
	}

	@Test
	public void testOnUploadProjectSelected() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		p.switchMode = SwitchMode.PALETTE;
		uut.project.palMappings.add(p);
		uut.onUploadProjectSelected();

		verify(connector).transferFile(eq("pin2dmd.pal"), any(InputStream.class));
	}

	@Test
	public void testUpdateAnimationMapKey() throws Exception {
		Animation animation = new Animation(AnimationType.COMPILED, "foo", 0, 1, 0, 1, 1);
		animation.setDesc("new");
		uut.recordings.put("old", animation);

		uut.updateAnimationMapKey("old", "new", uut.recordings);
		assertTrue(uut.recordings.get("new") != null);
	}

	@Test
	public void testBuildRelFilename() throws Exception {
		String filename = uut.buildRelFilename("/foo/test/tes.dat", "foo.ani");
		assertEquals("/foo/test/foo.ani", filename);
		filename = uut.buildRelFilename("/foo/test/tes.dat", "/foo.ani");
		assertEquals("/foo.ani", filename);
	}

	@Test
	public void testBuildUniqueName() throws Exception {
		Animation animation = new Animation(AnimationType.COMPILED, "foo", 0, 1, 0, 1, 1);
		animation.setDesc("new");
		uut.recordings.put("Scene 1", animation);
		String actual = uut.buildUniqueName(uut.recordings);
		assertNotEquals("Scene 1", actual);
	}

	@Test
	public void testOnInvert() throws Exception {
		byte[] data = new byte[512];
		uut.dmd.setMask(data);
		uut.onInvert();
		assertEquals((byte) 0xFF, (byte) uut.dmd.getFrame().mask.data[0]);
	}

	@Test
	public void testFromLabel() throws Exception {
		assertEquals(TabMode.KEYFRAME, TabMode.fromLabel("KeyFrame"));
	}

	@Test
	public void testRefreshPin2DmdHost() throws Exception {
		String filename = "foo.properties";
		System.out.println("propfile: " + filename);
		new FileOutputStream(filename).close(); // touch file
		ApplicationProperties.setPropFile(filename);
		uut.refreshPin2DmdHost("foo");
		new File(filename).delete();
	}

	@Test
	public void testRenameSceneShouldAdjustKey() throws Exception {
		uut.renameScene("old", "new"); // test free run
		CompiledAnimation cani = new CompiledAnimation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		uut.scenes.put("old", cani);
		uut.renameScene("old", "new");
		assertTrue(uut.scenes.containsKey("new"));
	}

	@Test
	public void testRenameSceneShouldAdjustKeyframe() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.frameSeqName = "old";
		uut.project.palMappings.add(p);
		assertTrue(uut.renameScene("old", "new"));
		assertEquals("new", p.frameSeqName);
	}

	@Test
	public void testRenameSceneShouldAdjustBookmark() throws Exception {
		Set<Bookmark> set = new TreeSet<>();
		uut.project.bookmarksMap.put("foo", set);
		Bookmark bookmark = new Bookmark("old", 0);
		set.add(bookmark);
		boolean b = uut.renameScene("old", "new");
		assertEquals("new", set.iterator().next().name); // new bookmark
		assertTrue(b);
	}

	@Test
	public void testRenameRecordingShouldAdjustKey() throws Exception {
		uut.renameRecording("old", "new");
		Animation cani = new Animation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		uut.recordings.put("old", cani);
		uut.renameRecording("old", "new");
		assertTrue(uut.recordings.containsKey("new"));
	}

	@Test
	public void testRenameRecordingShouldAdjustKeyFrame() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.animationName = "old";
		uut.project.palMappings.add(p);
		uut.renameRecording("old", "new");
		assertEquals("new", p.animationName);
	}

	@Test
	public void testRenameRecordingShouldPoplateNameMap() throws Exception {
		Animation cani = new Animation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		uut.recordings.put("old", cani);
		uut.renameRecording("old", "new");
		assertTrue( uut.project.recordingNameMap.containsKey("old"));
		assertEquals("new", uut.project.recordingNameMap.get("old"));
	}

	@Test
	public void testOnSetScenePalette() throws Exception {
		CompiledAnimation cani = new CompiledAnimation(AnimationType.COMPILED, "old.txt", 0, 0, 0, 0, 0);
		cani.setDesc("scene1");
		uut.selectedScene.set(cani);
		RGB[] rgb = {};
		uut.activePalette = new Palette(rgb , 15, "foo");
		
		PalMapping p = new PalMapping(0, "foo");
		p.frameSeqName = "scene1";
		p.palIndex = 1;
		uut.project.palMappings.add(p);
		
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

}
