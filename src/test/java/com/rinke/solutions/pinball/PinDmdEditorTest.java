package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
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
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setup() throws Exception {
		// TODO remove and replace by real license file
		uut.licManager.verify("src/test/resources/#3E002400164732.key");
	}

	@Test
	public void testReplaceExtensionTo() throws Exception {
		String newName = uut.replaceExtensionTo("ani", "foo.xml");
		assertThat(newName, equalTo("foo.ani"));
	}

	@Test
	@Ignore
	public void testExportProjectWithFrameMapping() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[]{1,2,3,4};		
		p.switchMode = SwitchMode.PALETTE;
		p.frameSeqName = "foo";

		List<Frame> frames = new ArrayList<Frame>();
		FrameSeq fs = new FrameSeq(frames, "foo");
		uut.project.frameSeqMap.put("foo", fs);

		uut.project.palMappings.add(p);

		// there must also be an animation called "foo"
		Animation ani = new Animation(AnimationType.COMPILED, "foo", 0, 0, 0,
				0, 0);
		ani.setDesc("foo");
		uut.animations.put("foo", ani);
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
		uut.exportProject(filename, f->new FileOutputStream(f));
		// System.out.println(filename);
		assertNull(Util.isBinaryIdentical(filename,
				"./src/test/resources/mappingWithSeq.dat"));
		assertNull(Util.isBinaryIdentical(
				uut.replaceExtensionTo("fsq", filename),
				"./src/test/resources/testSeq.fsq"));

	}

	@Test
	@Ignore
	public void testExportProjectWithMapping() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[]{1,2,3,4};		
		p.switchMode = SwitchMode.PALETTE;

		uut.project.palMappings.add(p);

		uut.exportProject(filename, f->new FileOutputStream(f));

		// System.out.println(filename);

		// create a reference file and compare against
		assertNull(Util.isBinaryIdentical(filename,
				"./src/test/resources/palettesOneMapping.dat"));
	}

	@Test
	@Ignore
	public void testExportProjectEmpty() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		uut.exportProject(filename, f->new FileOutputStream(f));
		// System.out.println(filename);

		// create a reference file and compare against
		assertNull(Util.isBinaryIdentical(filename,
				"./src/test/resources/defaultPalettes.dat"));
	}

	@Test
	public void testImportProjectString() throws Exception {
		uut.importProject("./src/test/resources/test.xml");
		verify(recentAnimationsMenuManager).populateRecent(eq("./src/test/resources/drwho-dump.txt.gz"));
	}

	@Test
	public void testCheckOverride() throws Exception {
		List<Palette> palettes = new ArrayList<>();
		List<Palette> palettesImported = new ArrayList<>();
		String override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo(""));

		palettes.add(new Palette(Palette.defaultColors(), 0, "foo"));
		palettesImported.add(new Palette(Palette.defaultColors(), 1, "foo"));
		override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo(""));

		palettes.add(new Palette(Palette.defaultColors(), 2, "foo2"));
		palettesImported.add(new Palette(Palette.defaultColors(), 2, "foo2"));
		override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo("2, "));
	}

	@Test
	public void testImportPalettes() throws Exception {
		List<Palette> palettesImported = new ArrayList<Palette>();
		palettesImported.add(new Palette(Palette.defaultColors(), 0, "foo"));
		uut.importPalettes(palettesImported, false);
		uut.importPalettes(palettesImported, true);
	}

	@Test
	public void testCheckForDuplicateKeyFrames() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[]{1,2,3,4};
		assertFalse(uut.checkForDuplicateKeyFrames(p));
		uut.project.palMappings.add(p);
		assertTrue(uut.checkForDuplicateKeyFrames(p));
	}

	@Test
	public void testUploadProject() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[]{1,2,3,4};		
		p.switchMode = SwitchMode.PALETTE;
		uut.project.palMappings.add(p);
		uut.uploadProject();
		
		verify(connector).transferFile(eq("palettes.dat"), any(InputStream.class));
	}

}
