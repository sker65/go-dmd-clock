package com.rinke.solutions.pinball;

import static org.junit.Assert.assertNull;

import java.io.FileReader;

import org.custommonkey.xmlunit.XMLUnit;
import static org.custommonkey.xmlunit.XMLAssert.*;

import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.test.Util;
import com.rinke.solutions.pinball.util.RecentMenuManager;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorIOTest {
	
	@Mock
	Shell shell;
	
	@Mock
	RecentMenuManager recentAnimationsMenuManager;
	
	@Mock
	RecentMenuManager recentProjectsMenuManager;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	PinDmdEditor uut = new PinDmdEditor(){

		@Override
		protected void setupUIonProjectLoad() {}
		
	};

	@Before
	public void setUp() throws Exception {
		uut.licManager.verify("src/test/resources/#3E002400164732.key");
		uut.aniAction = new AnimationActionHandler(uut, shell);
		uut.recentAnimationsMenuManager = recentAnimationsMenuManager;
		uut.shell = shell;
		uut.recentProjectsMenuManager = recentProjectsMenuManager;
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
//		assertNull(Util.isBinaryIdentical(testFolder.getRoot()+"/ex1.ani", "./src/test/resources/ex1.ani"));
	}
	
}
