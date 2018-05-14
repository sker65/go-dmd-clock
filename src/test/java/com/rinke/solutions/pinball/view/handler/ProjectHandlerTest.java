package com.rinke.solutions.pinball.view.handler;

// if one imports * it will conflict with hamcrest
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
//import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.File;
import java.io.FileReader;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.AniReader;
import com.rinke.solutions.pinball.animation.Animation;
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
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ProjectHandlerTest extends HandlerTest {
	@Mock
	private FileChooserUtil fileChooserUtil;

	@Mock
	private MessageUtil messageUtil;
	
	@Mock LicenseManager licenseManager;
	
	@InjectMocks
	private ProjectHandler uut = new ProjectHandler(vm);
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before public void setup() {
		uut.fileHelper = new FileHelper();
		uut.aniAction = new AnimationActionHandler(vm);
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
		assertEquals("pin2dmd", uut.bareName(null));
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

	@Test 
	public void testOnImportProject() throws Exception {
		uut.importProject("./src/test/resources/test.xml");
	}

	@Test
	public void testOnImportProjectWithChoose() throws Exception {
		when(fileChooserUtil.choose(anyInt(), eq(null), any(String[].class), any(String[].class)))
			.thenReturn("src/test/resources/test.xml");
		uut.onImportProject();
	}

	@Test
	public void testOnDmdSizeChanged() throws Exception {
		uut.onDmdSizeChanged(DmdSize.Size128x32, DmdSize.Size192x64);
	}

	@Test
	public void testOnLoadProject() throws Exception {
		uut.onLoadProject();
	}

	@Test
	public void testOnLoadProjectWithName() throws Exception {
		when(fileChooserUtil.choose(anyInt(), eq(null), any(String[].class), any(String[].class)))
			.thenReturn("src/test/resources/ex1.xml");
		uut.onLoadProject();
	}

	@Test
	public void testOnExportRealPinProject() throws Exception {
		File tempFile = testFolder.newFile("test.dat");
		when(fileChooserUtil.choose(anyInt(), anyString(), any(String[].class), any(String[].class)))
		.thenReturn(tempFile.getAbsolutePath());
		vm.setProjectFilename("foo"); 
		uut.onExportRealPinProject();
	}

	@Test
	public void testOnExportVirtualPinProject() throws Exception {
		File tempFile = testFolder.newFile("test.dat");
		when(fileChooserUtil.choose(anyInt(), anyString(), any(String[].class), any(String[].class)))
		.thenReturn(tempFile.getAbsolutePath());
		
		vm.setProjectFilename("foo"); 
		uut.onExportVirtualPinProject();
	}

	@Test
	public void testOnExportVirtualPinProjectWithData() throws Exception {
		File tempFile = testFolder.newFile("test.dat");
		when(fileChooserUtil.choose(anyInt(), anyString(), any(String[].class), any(String[].class)))
		.thenReturn(tempFile.getAbsolutePath());
		
		vm.setProjectFilename("foo"); 
		List<Animation> anis = AniReader.readFromFile("src/test/resources/ex1.ani");
		vm.scenes.put("sc1", (CompiledAnimation) anis.get(0));
		PalMapping k = new PalMapping(1, "foo");
		k.switchMode = SwitchMode.ADD;
		k.frameSeqName = "sc1";
		vm.keyframes.put("kf1", k );
		uut.onExportVirtualPinProject();
	}

	@Test
	public void testOnSaveProject() throws Exception {
		uut.onSaveProject();
	}

	@Test
	public void testSaveProject() throws Exception {
		String filename = testFolder.newFile("test.xml").getAbsolutePath();
		uut.saveProject(filename);
	}

	@Test
	public void testSaveProjectWithBackup() throws Exception {
		String filename = testFolder.newFile("test.xml").getAbsolutePath();
		uut.backup = true;
		uut.saveProject(filename );
	}

	@Test
	public final void testLoadAndSaveProject() throws Exception {
		String tempFile = testFolder.newFile("ex1.xml").getPath();
		uut.onLoadProject("./src/test/resources/ex1.xml");
		uut.saveProject(tempFile);
		XMLUnit.setIgnoreWhitespace(true);
		assertXMLEqual(new FileReader("./src/test/resources/ex1.xml"), new FileReader(tempFile));
		Animation ani = CompiledAnimation.read(testFolder.getRoot()+"/ex1.ani").get(0);
		Animation ani2 = CompiledAnimation.read("./src/test/resources/ex1.ani").get(0);
		compare(ani,ani2);
		assertNull(Util.isBinaryIdentical(testFolder.getRoot()+"/ex1.ani", "./src/test/resources/ex1.ani"));
	}

	@SuppressWarnings("deprecation")
	private void compare(Animation ani, Animation ani2) {
		int i = ani.getStart();
		ani.restart(); ani2.restart();
		DMD dmd = new DMD(DmdSize.Size128x32);
		while( i < ani.end ) {
			Frame f = ani.render(dmd, false);
			Frame f2 = ani2.render(dmd, false);
			if( !EqualsBuilder.reflectionEquals(f, f2, false) ) fail("frame different @ "+i);
			if( ani.getRefreshDelay() != ani2.getRefreshDelay() ) fail( "delay at "+i);
			i++;
		}
		if( ani.getAniColors() != null && ani2.getAniColors() == null ) fail("different colors set");
		if( ani2.getAniColors() != null && ani.getAniColors() == null ) fail("different colors set");
		
		if( ani.getAniColors().length != ani2.getAniColors().length ) fail("different number of colors");
		for (int j = 0; j < ani.getAniColors().length; j++) {
			if( !ani.getAniColors()[j].equals(ani2.getAniColors()[j])) fail("different color @"+j);
		}
		boolean eq = EqualsBuilder.reflectionEquals(ani, ani2, "basePath", "name", "frames", "renderer");
		if( !eq ) fail( "not equal");
	}

	@Test
	public void testGetUniqueName() throws Exception {
		assertEquals("foo",uut.getUniqueName("foo", Arrays.asList("xxx")));
		assertEquals("foo_1",uut.getUniqueName("foo", Arrays.asList("foo")));
	}


}
