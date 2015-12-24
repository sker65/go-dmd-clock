package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;

public class PinDmdEditorTest {
	
	PinDmdEditor uut = new PinDmdEditor();
	
	byte[] digest = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public void testReplaceExtensionTo() throws Exception {
		String newName = uut.replaceExtensionTo("ani", "foo.xml");
		assertThat(newName, equalTo("foo.ani"));
	}
	
	@Test
	public void testExportProjectWithFrameMapping() throws Exception {
		
		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();
		
		PalMapping p = new PalMapping(0, "foo");
		p.digest = digest;
		p.frameSeqName = "foo";
		
		List<Frame> frames = new ArrayList<Frame>();
		FrameSeq fs = new FrameSeq(frames , "foo");
		uut.project.frameSeqMap.put("foo", fs );
		
		uut.project.palMappings.add(p);
		
		// there must also be an animation called "foo"
		Animation ani = new Animation(AnimationType.COMPILED,"foo",0,0,0,0,0);
		ani.setDesc("foo");
		uut.animations.put("foo", ani);
		// finally put some frame data into it
		List<com.rinke.solutions.pinball.animation.Frame> aniFrames = ani.getRenderer().getFrames();
		byte[] plane2 = new byte[512];
		byte[] plane1 = new byte[512];
		for(int i = 0; i <512; i+=2) {
			plane1[i] = (byte)0xFF;
			plane1[i+1] = (byte)i;
			plane2[i] = (byte)0xFF;
		}
		com.rinke.solutions.pinball.animation.Frame frame = 
				new com.rinke.solutions.pinball.animation.Frame(128, 32, plane1, plane2);
		frame.timecode = 0x77ee77ee;
		aniFrames.add(frame);
		uut.exportProject(filename);
		//System.out.println(filename);
		assertNull( isBinaryIdentical( filename, "./src/test/resources/mappingWithSeq.dat"));
		assertNull( isBinaryIdentical( uut.replaceExtensionTo("fsq", filename), "./src/test/resources/testSeq.fsq"));
		
	}

	@Test
	public void testExportProjectWithMapping() throws Exception {
		
		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();
		
		PalMapping p = new PalMapping(0, "foo");
		p.digest = digest;
		
		uut.project.palMappings.add(p );
		
		uut.exportProject(filename);
		
		//System.out.println(filename);

		// create a reference file and compare against
		assertNull( isBinaryIdentical( filename, "./src/test/resources/palettesOneMapping.dat"));
	}

	@Test
	public void testExportProjectEmpty() throws Exception {
		
		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();
		
		uut.exportProject(filename);
		//System.out.println(filename);

		// create a reference file and compare against
		assertNull( isBinaryIdentical( filename, "./src/test/resources/defaultPalettes.dat"));
	}

	private String isBinaryIdentical(String filename, String filename2) throws IOException {
		byte[] b1 = IOUtils.toByteArray(new FileInputStream(filename));
		byte[] b2 = IOUtils.toByteArray(new FileInputStream(filename2));
		if( b1.length != b2.length ) return String.format("different lenth %d : %d", b1.length, b2.length);
		for( int i = 0; i < b1.length; i++) {
			if( b1[i] != b2[i] ) return String.format("files differ at %d", i);
		}
		return null;
	}

	@Test
	public void testImportProjectString() throws Exception {
		uut.importProject("./src/test/resources/test.xml");
	}

	@Test
	public void testCheckOverride() throws Exception {
		List<Palette> palettes = new ArrayList<>();
		List<Palette> palettesImported = new ArrayList<>();
		String override = uut.checkOverride(palettes , palettesImported );
		assertThat(override,equalTo(""));
		
		palettes.add( new Palette(Palette.defaultColors(), 0, "foo"));
		palettesImported.add( new Palette(Palette.defaultColors(), 1, "foo"));
		override = uut.checkOverride(palettes , palettesImported );
		assertThat(override,equalTo(""));

		palettes.add( new Palette(Palette.defaultColors(), 2, "foo2"));
		palettesImported.add( new Palette(Palette.defaultColors(), 2, "foo2"));
		override = uut.checkOverride(palettes , palettesImported );
		assertThat(override,equalTo("2, "));
	}

	@Test
	public void testImportPalettes() throws Exception {
		List<Palette> palettesImported = new ArrayList<Palette>();
		palettesImported.add( new Palette(Palette.defaultColors(), 0, "foo"));
		uut.importPalettes(palettesImported , false);
		uut.importPalettes(palettesImported , true);
	}

}
