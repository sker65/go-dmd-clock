package com.rinke.solutions.pinball.animation;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;

public class AniWriterTest {
	
	AniWriter uut;

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	private int planeSize = 512;
	
	@Before
	public void setUp() throws Exception {
	}
	
	protected CompiledAnimation getScene(String name) {
		String n = name!=null?name:"scene";
		CompiledAnimation r = new CompiledAnimation(AnimationType.COMPILED, n, 0, 0, 0, 0, 0);
		Frame frame = new Frame(new byte[planeSize], new byte[planeSize]);
		r.frames.add(frame);
		r.frames.add(new Frame(frame));
		r.setDesc(n);
		return r;
	}

	@Test
	public void testInnerRun() throws Exception {
		String filename = tmpFolder.newFile("test.ani").getAbsolutePath();
		CompiledAnimation scene = getScene("foo");
		scene.getMasks().add( new Mask(512));
		scene.setRecordingLink(new RecordingLink("foo", 12));
		List<Animation> anis = Arrays.asList(scene);
		uut = new AniWriter(anis , filename , 6, getDefaultPaletteMap(Palette.getDefaultPalettes()), null);
		uut.innerRun();
		
		List<Animation> an = AniReader.read(filename);
		CompiledAnimation a = (CompiledAnimation) an.get(0);
		assertEquals(1, a.getMasks().size());
		assertEquals("foo", a.getRecordingLink().associatedRecordingName);
	}

	private Map<Integer, Palette> getDefaultPaletteMap(List<Palette> palettes) {
		Map<Integer, Palette> m = new HashMap<Integer, Palette>();
		palettes.stream().forEach(p->m.put(p.index,p));
		return m;
	}

}
