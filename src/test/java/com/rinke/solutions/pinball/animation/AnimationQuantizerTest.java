package com.rinke.solutions.pinball.animation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class AnimationQuantizerTest {
	
	AnimationQuantizer uut;

	@Before
	public void setUp() throws Exception {
		uut = new AnimationQuantizer();
	}

	@Test
	public void testQuantize() throws Exception {
		ViewModel vm = new ViewModel();
		vm.dmdSize = DmdSize.Size128x32;
		AnimationActionHandler handler = new AnimationActionHandler(vm);
		Animation ani = handler.loadAni("./src/test/resources/term32.rgb.gz", false, false).get(0);
		CompiledAnimation scene = ani.cutScene(0, 1, 24);
		Palette pal = Palette.getDefaultPalettes().get(0);
		uut.quantize("foo", scene, pal );
	}

}
