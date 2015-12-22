package com.rinke.solutions.pinball;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.rinke.solutions.pinball.model.PalMapping;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

}
