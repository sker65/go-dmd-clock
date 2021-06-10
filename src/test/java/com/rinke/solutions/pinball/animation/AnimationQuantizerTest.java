package com.rinke.solutions.pinball.animation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class AnimationQuantizerTest {
	
	AnimationQuantizer uut;
	private ViewModel vm;
	private AnimationActionHandler handler;

	@Before
	public void setUp() throws Exception {
		uut = new AnimationQuantizer();
		vm = new ViewModel();
		vm.dmdSize = DmdSize.Size128x32;
		vm.srcDmdSize = DmdSize.Size128x32;
		handler = new AnimationActionHandler(vm);
	}

	@Test
	public void testQuantize() throws Exception {
		Animation ani = handler.loadAni("./src/test/resources/term32.rgb.gz", false, false, null).get(0);
		CompiledAnimation scene = ani.cutScene(0, 1, 24);
		Palette pal = Palette.getDefaultPalettes().get(0);
		CompiledAnimation qAni = uut.quantize("foo", scene, pal, 4);
		assertEquals(scene.frames.get(0).delay, qAni.frames.get(0).delay);
		assertEquals(scene.frames.get(0).timecode, qAni.frames.get(0).timecode);
		assertEquals(scene.width, qAni.width);
		assertEquals(scene.height, qAni.height);
	}

	@Test
	public void testQuantizeWithMask() throws Exception {
		Animation ani = handler.loadAni("./src/test/resources/term32.rgb.gz", false, false, null).get(0);
		CompiledAnimation scene = ani.cutScene(0, 1, 24);
		scene.frames.get(0).mask = new Mask(vm.dmdSize.planeSize);
		Palette pal = Palette.getDefaultPalettes().get(0);
		uut.quantize("foo", scene, pal, 4);
	}

}
