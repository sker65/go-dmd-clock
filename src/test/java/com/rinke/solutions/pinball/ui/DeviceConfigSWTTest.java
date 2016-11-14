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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.test.Util;

@RunWith(MockitoJUnitRunner.class)
public class DeviceConfigSWTTest {

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	@Mock
	FileChooser fileChooserMock;
	
	Shell shell;
	
	DeviceConfig uut;
	
	@Before
	public void setup() {
		shell = displayHelper.createShell();
		uut  = new DeviceConfig(shell) {

			@Override
			protected FileChooser createFileChooser(Shell shell, int flags) {
				return fileChooserMock;
			}
			
		};
	}
	
	@Test
	public void testSave() throws Exception {
		uut.createContents();
		uut.save();
	}
	
	@Test
	public void testCreateContents() throws Exception {
		uut.createContents();
	}
	
	@Test
	public void testWriteDeviceConfig() throws Exception {
		String newFile = testFolder.newFile().getAbsolutePath();
		uut.writeDeviceConfig(newFile, 2, 3, true, 2);
		assertNull( Util.isBinaryIdentical(newFile, "./src/test/resources/pin2dmd.dat"));
	}



}
