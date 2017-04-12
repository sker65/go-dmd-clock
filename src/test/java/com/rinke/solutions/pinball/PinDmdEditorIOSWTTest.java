package com.rinke.solutions.pinball;

import static org.junit.Assert.assertNull;

import java.io.FileReader;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.custommonkey.xmlunit.XMLUnit;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
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
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.test.Util;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.RecentMenuManager;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorIOSWTTest {
	
	@Mock
	Shell shell;
	
	@Mock
	RecentMenuManager recentAnimationsMenuManager;
	
	@Mock
	RecentMenuManager recentProjectsMenuManager;
	
	@Mock
	MenuItem menuItemMock;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	PinDmdEditor uut = new PinDmdEditor(){

		@Override
		protected void setupUIonProjectLoad() {}
		
	};

	@Before
	public void setUp() throws Exception {
		uut.licManager.verify("src/test/resources/#3E002400164732.key");
		uut.aniAction = new AnimationActionHandler(uut, shell) {

			@Override
			protected Progress getProgress() {
				return null;
			}
			
		};
		uut.recentAnimationsMenuManager = recentAnimationsMenuManager;
		uut.shell = shell;
		uut.recentProjectsMenuManager = recentProjectsMenuManager;
		uut.mntmSaveProject = menuItemMock;
	}

	@Test
	public final void testLoadProject() {
		uut.loadProject("src/test/resources/ex1.xml");
	}

	@Test
	public final void testLoadAndSaveProject() throws Exception {
		String tempFile = testFolder.newFile("ex1.xml").getPath();
		uut.loadProject("./src/test/resources/ex1.xml");
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
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
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
	
}
