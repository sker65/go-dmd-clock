package com.rinke.solutions.pinball.util;

import java.io.File;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;

public class ApplicationPropertiesTest {

	Random rand = new Random();
	String file;
	
	@Before
	public void setUp() throws Exception {
		file = Integer.toHexString(rand.nextInt(1000000))+".dat";
		ApplicationProperties.setPropFile(file);
	}
	
	@After
	public void tearDown() throws Exception {
		String filename = ApplicationProperties.getInstance().getFilename();
		new File(filename).deleteOnExit();
	}

	@Test
	public void testPut() throws Exception {
		ApplicationProperties.put("foo", "bar");
	}

}
