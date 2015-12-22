package com.rinke.solutions.pinball;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

public class PinDmdEditorTest {
	
	PinDmdEditor uut = new PinDmdEditor();
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public void testReplaceExtensionTo() throws Exception {
		String newName = uut.replaceExtensionTo("ani", "foo.xml");
		assertThat(newName, equalTo("foo.ani"));
	}

	@Test
	public void testExportProjectString() throws Exception {
		File tempFile = testFolder.newFile("test.dat");
		String filename = tempFile.getAbsolutePath();
		uut.exportProject(filename);
		
		assertTrue(new File(filename).exists());

		// create a reference file and compare against
		
	}

}
