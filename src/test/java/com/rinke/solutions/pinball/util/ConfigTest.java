package com.rinke.solutions.pinball.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
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

public class ConfigTest {

	String file;
	Config uut;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		file = testFolder.newFile("test.property").getAbsolutePath();
		uut = new Config(file);
		uut.put("stringKey", "stringValue");
		uut.put("boolKey", true);
		uut.put("intKey", 4711);
	}
	
	private void writeTestData() throws IOException {
		PrintWriter writer = new PrintWriter(file);
		writer.append("# foo\n");
		writer.append("foo=1\n");
		writer.flush();
		writer.close();	
	}
	
	@Test
	public void testPut() throws Exception {
		uut.put("foo", "bar");
	}

	@Test
	public void testGet() throws Exception {
		assertEquals("stringValue", uut.get("stringKey"));
		assertNull( uut.get("unkownKey"));
	}

	@Test
	public void testGetBooleanString() throws Exception {
		assertTrue(uut.getBoolean("boolKey"));
		assertFalse(uut.getBoolean("unkownKey"));
	}

	@Test
	public void testGetIntegerStringInt() throws Exception {
		assertEquals(4711,uut.getInteger("intKey"));
		assertEquals(0,uut.getInteger("UnkwownKey"));
		assertEquals(23,uut.getInteger("UnkwownKey",23));
	}

	@Test
	public void testGetProperty() throws Exception {
		assertEquals("stringValue", uut.getProperty("stringKey"));
	}

	@Test
	public void testLoad() throws Exception {
		uut.setPropFile(file);
		writeTestData();
		uut.load();
		assertEquals(1,uut.getInteger("foo"));
	}

	@Test
	public void testGetFilename() throws Exception {
		Config c = new Config();
		c.getFilename();
	}

}
