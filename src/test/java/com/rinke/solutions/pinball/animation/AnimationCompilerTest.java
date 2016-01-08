package com.rinke.solutions.pinball.animation;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class AnimationCompilerTest {

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testReadFromCompiledFile() throws Exception {
		AnimationCompiler.readFromCompiledFile("./src/test/resources/test.ani");
	}

	@Test
	public void testWriteToCompiledFile() throws Exception {
		List<Animation> list = AnimationCompiler.readFromCompiledFile("./src/test/resources/test.ani");
		String filename = testFolder.newFile().getAbsolutePath();
		AnimationCompiler.writeToCompiledFile(list, filename);
	}

}
