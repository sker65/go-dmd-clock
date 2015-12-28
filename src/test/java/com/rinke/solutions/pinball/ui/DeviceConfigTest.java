package com.rinke.solutions.pinball.ui;

import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fappel.swt.DisplayHelper;

public class DeviceConfigTest {

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	Shell shell;
	
	DeviceConfig uut;
	
	@Before
	public void setup() {
		shell = displayHelper.createShell();
		uut  = new DeviceConfig(shell);
	}
	
	@Test
	public void testWriteDeviceConfig() throws Exception {
		String newFile = testFolder.newFile().getAbsolutePath();
		uut.writeDeviceConfig(newFile, 2, 3);
		assertNull( isBinaryIdentical(newFile, "./src/test/resources/pin2dmd.dat"));
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
