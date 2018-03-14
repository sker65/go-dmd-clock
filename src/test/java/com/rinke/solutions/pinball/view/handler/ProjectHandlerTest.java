package com.rinke.solutions.pinball.view.handler;

// if one imports * it will conflict with hamcrest
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.test.Util;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

import java.io.FileOutputStream;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ProjectHandlerTest {
	@Mock
	private FileChooserUtil fileChooserUtil;

	@Mock
	private MessageUtil messageUtil;
	
	@Mock LicenseManager licenseManager;
	
	private ProjectHandler uut;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	private ViewModel vm;

	private DMD dmd;
	
	@Before public void setup() {
		this.vm = new ViewModel();
		this.dmd = new DMD(128,32);
		vm.init(dmd, DmdSize.Size128x32, "foo", 10);
		uut = new ProjectHandler(vm);
		uut.fileHelper = new FileHelper();
		uut.aniAction = new AnimationActionHandler(vm);
		// inject mocks
		uut.messageUtil = messageUtil;
		uut.fileChooserUtil = fileChooserUtil;
		uut.licenseManager = licenseManager;
	}
	
	@Test
	public void testReplaceExtensionTo() throws Exception {
		String newName = uut.replaceExtensionTo("ani", "foo.xml");
		assertThat(newName, equalTo("foo.ani"));
	}

	@Test
	public void testBuildRelFilename() throws Exception {
		String filename = uut.buildRelFilename("/foo/test/tes.dat", "foo.ani");
		assertEquals("/foo/test/foo.ani", filename);
		filename = uut.buildRelFilename("/foo/test/tes.dat", "/foo.ani");
		assertEquals("/foo.ani", filename);
	}


	@Test
	public void testBareName() throws Exception {
		assertEquals("foo", uut.bareName("foo"));
		assertEquals("foo", uut.bareName("foo.xml"));
		assertEquals("foo", uut.bareName("/USER/foo.xml"));
	}
	
	@Test
	public final void testLoadProject() {
		uut.onLoadProject("src/test/resources/ex1.xml");
	}

	@Test
	public void testOnExportProjectSelectedWithMapping() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		p.switchMode = SwitchMode.PALETTE;

		vm.keyframes.put(p.name,p);
		
		//when(licenseManager.requireOneOf(cap))

		uut.onExportProject(filename, f -> new FileOutputStream(f), true);

		// create a reference file and compare against
		assertNull(Util.isBinaryIdentical(filename, "./src/test/resources/palettesOneMapping.dat"));
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

		vm.keyframes.put(p.name,p);

		// there must also be an animation called "foo"
		CompiledAnimation ani = new CompiledAnimation(AnimationType.COMPILED, "foo", 0, 0, 0, 0, 0);
		ani.setDesc("foo");
		vm.scenes.put("foo", ani);
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
		uut.onExportProject(filename, f -> new FileOutputStream(f), true);
		// System.out.println(filename);
		assertNull(Util.isBinaryIdentical(filename, "./src/test/resources/mappingWithSeq.dat"));
		assertNull(Util.isBinaryIdentical(uut.replaceExtensionTo("fsq", filename), "./src/test/resources/testSeq.fsq"));

	}

	@Test
	public void testOnExportProjectSelectedEmpty() throws Exception {

		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();

		uut.onExportProject(filename, f -> new FileOutputStream(f), true);
		// System.out.println(filename);

		// create a reference file and compare against
		assertNull(Util.isBinaryIdentical(filename, "./src/test/resources/defaultPalettes.dat"));
	}



}
