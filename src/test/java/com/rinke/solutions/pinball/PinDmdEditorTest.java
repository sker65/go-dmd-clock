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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.test.Util;
import com.rinke.solutions.pinball.util.RecentMenuManager;
import com.rinke.solutions.pinball.widget.DMDWidget;

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
	DMDWidget dmdWidget;
	
	@Mock
	DMDWidget previewDMD;
	
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

		List<Frame> frames = new ArrayList<Frame>();
		FrameSeq fs = new FrameSeq(frames, "foo");
		uut.project.frameSeqMap.put("foo", fs);

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
		assertEquals((byte)0xFF, (byte)uut.dmd.getFrame().mask.data[0]);
	}

}
