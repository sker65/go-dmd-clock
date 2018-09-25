package com.rinke.solutions.pinball;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.handler.HandlerTest;

@RunWith(MockitoJUnitRunner.class)
public class AnimationActionHandlerTest extends HandlerTest{

	@Mock MessageUtil messageUtil;
	@Mock FileChooserUtil fileChooserUtil;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@InjectMocks
	AnimationActionHandler uut = new AnimationActionHandler(vm);
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testOnSaveAniWithFC() throws Exception {
		uut.onSaveAniWithFC(2);
	}

	@Test
	public void testStoreAnimations() throws Exception {
		Collection<Animation> anis = new ArrayList<>();
		uut.storeAnimations(anis, testFolder.newFile("foo.ani").getAbsolutePath(), 1, true);
	}

	@Test
	public void testStoreAnimationsWithFile() throws Exception {
		Collection<Animation> anis = new ArrayList<>();
		anis.add(getScene("foo"));
		uut.storeAnimations(anis, testFolder.newFile("foo.ani").getAbsolutePath(), 1, true);
	}

	@Test
	public void testLoadAni() throws Exception {
		uut.loadAni("./src/test/resources/ex1.ani", false, false, null);
	}

	@Test
	public void testLoadAniGifWithSmallPalette() throws Exception {
		uut.loadAni("./src/test/resources/renderer/ezgif-645182047.gif", true, true, null);
		assertEquals(10, vm.paletteMap.size() );
		assertEquals(16, vm.paletteMap.get(9).numberOfColors);
	}

	@Test
	public void testLoadAniGifBigSmallPalette() throws Exception {
		uut.loadAni("./src/test/resources/renderer/wave-ball-preloader.gif", true, true, null);
		assertEquals(10, vm.paletteMap.size() );
		assertEquals(16, vm.paletteMap.get(9).numberOfColors);
	}

	@Test
	public void testLoadAniWithDump() throws Exception {
		uut.loadAni("./src/test/resources/drwho-dump.txt.gz", false, false, null);
	}

	@Test
	public void testLoadAniWithRgb() throws Exception {
		uut.loadAni("./src/test/resources/term32.rgb.gz", false, false, null);
	}

	@Test
	public void testPopulateAni() throws Exception {
		Map<String, CompiledAnimation> anis = new HashMap<>();
		uut.populateAni(getScene("foo"), anis );
	}

	@Test
	public void testPopulateAniWithCollision() throws Exception {
		Map<String, CompiledAnimation> anis = new HashMap<>();
		uut.populateAni(getScene("foo"), anis );
		uut.populateAni(getScene("foo"), anis );
		assertTrue(anis.containsKey("foo-0"));
	}

	@Test
	public void testPopulatePalette() throws Exception {
		Map<Integer, Palette> palettes = new HashMap<>();
		uut.populatePalette(getScene("foo"), palettes );
	}

	@Test
	public void testOnLoadAniWithFC() throws Exception {
		uut.onLoadAniWithFC(false);
	}

	@Test
	public void testOnSaveSingleAniWithFC() throws Exception {
		vm.selectedScene =  getScene("foo");
		String tmpFile = testFolder.newFile("foo.ani").getAbsolutePath();
		when( fileChooserUtil.choose(eq(SWT.SAVE), any(String.class), any(String[].class), any(String[].class) ) ).thenReturn(tmpFile );
		uut.onSaveSingleAniWithFC(1);
	}

}
