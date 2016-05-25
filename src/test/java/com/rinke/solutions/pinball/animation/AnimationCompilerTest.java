package com.rinke.solutions.pinball.animation;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class AnimationCompilerTest {
	
	AnimationCompiler uut = new AnimationCompiler();

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testReadFromCompiledFile() throws Exception {
		uut.readFromCompiledFile("./src/test/resources/test.ani");
	}

	@Test
	public void testWriteToCompiledFile() throws Exception {
		List<Animation> list = uut.readFromCompiledFile("./src/test/resources/test.ani");
		String filename = testFolder.newFile().getAbsolutePath();
		uut.writeToCompiledFile(list, filename, 1, Collections.emptyList());
	}

}
