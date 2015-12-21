package com.rinke.solutions.pinball;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PinDmdEditorTest {
	
	PinDmdEditor uut = new PinDmdEditor();

	@Test
	public void testReplaceExtensionTo() throws Exception {
		String newName = uut.replaceExtensionTo("ani", "foo.xml");
		assertThat(newName, equalTo("foo.ani"));
	}

}
