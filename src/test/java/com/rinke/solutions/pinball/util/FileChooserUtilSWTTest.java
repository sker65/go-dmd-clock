package com.rinke.solutions.pinball.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FileChooserUtilSWTTest {

	private FileChooserUtil uut;

	@Before
	public void setUp() throws Exception {
		uut = new FileChooserUtil();
	}

	@Test
	public void testBasename() throws Exception {
		assertEquals("base.xml", uut.basename("/foo/base.xml"));
	}

}
