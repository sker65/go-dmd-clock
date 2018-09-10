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
			public void testRead() throws Exception {
				AniReader.read("./src/test/resources/test.ani");
			}

	@Test
	public void testWriteToCompiledFile() throws Exception {
		List<Animation> list = AniReader.read("./src/test/resources/test.ani");
		String filename = testFolder.newFile().getAbsolutePath();
		AniWriter.write(list, filename, 1, Collections.emptyMap());
	}

}
