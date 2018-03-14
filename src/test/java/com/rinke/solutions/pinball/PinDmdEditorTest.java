package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.test.Util;
import com.rinke.solutions.pinball.util.RecentMenuManager;
import com.rinke.solutions.pinball.view.handler.CutCmdHandler;
import com.rinke.solutions.pinball.view.handler.ScenesCmdHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
@Ignore
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

	private ViewModel vm;
	
	CutCmdHandler cutCmdHandler;

/*	@Before
	public void setup() throws Exception {
		uut.licManager.verify("src/test/resources/#3E002400164732.key");
		vm = new ViewModel();
		uut.init();
//		uut.connector = this.connector; // 
		uut.v.recentAnimationsMenuManager = this.recentAnimationsMenuManager;
		cutCmdHandler = new CutCmdHandler(vm);
	}


	@Test 
	public void testOnImportProjectSelectedString() throws Exception {
		uut.aniAction = new AnimationActionHandler(uut);
		vm.dmdSize = DmdSize.Size128x32;
		uut.importProject("./src/test/resources/test.xml");
		verify(recentAnimationsMenuManager).populateRecent(eq("./src/test/resources/drwho-dump.txt.gz"));
	}

	@Test
	public void testOnUploadProjectSelected() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		p.switchMode = SwitchMode.PALETTE;
		uut.project.palMappings.add(p);
		
		// TODO test handler directly uut.onUploadProjectSelected();

		verify(connector).transferFile(eq("pin2dmd.pal"), any(InputStream.class));
	}

	@Test
	public void testFromLabel() throws Exception {
		assertEquals(TabMode.KEYFRAME, TabMode.fromLabel("KeyFrame"));
	}

//	@Test
//	public void testRefreshPin2DmdHost() throws Exception {
//		String filename = "foo.properties";
//		System.out.println("propfile: " + filename);
//		new FileOutputStream(filename).close(); // touch file
//		ApplicationProperties.setPropFile(filename);
//		uut.onPin2dmdAdressChanged(null, "foo");
//		new File(filename).delete();
//	}


*/

}
