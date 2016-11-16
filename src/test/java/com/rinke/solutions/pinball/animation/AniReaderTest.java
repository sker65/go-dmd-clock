package com.rinke.solutions.pinball.animation;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class AniReaderTest {
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
		public void testReadFromFile() throws Exception {
			AniReader.readFromFile("./src/test/resources/test.ani");
		}

	@Test
	public void testWriteToCompiledFile() throws Exception {
		List<Animation> list = AniReader.readFromFile("./src/test/resources/test.ani");
		String filename = testFolder.newFile().getAbsolutePath();
		AniWriter.writeToFile(list, filename, 1, Collections.emptyList());
	}

}
