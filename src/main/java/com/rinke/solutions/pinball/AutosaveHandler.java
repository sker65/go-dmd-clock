package com.rinke.solutions.pinball;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.util.ApplicationProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutosaveHandler implements Runnable {
	
	private Display display;
	private Shell shell;
	private PinDmdEditor editor;
	private long nextAutoSave;

	public AutosaveHandler(Display display, Shell sh, PinDmdEditor editor) {
		super();
		this.display = display;
		this.shell = sh;
		this.editor = editor;
		nextAutoSave = System.currentTimeMillis() + ApplicationProperties.getInteger(ApplicationProperties.AUTOSAVE_INTERVAL)*60*1000;
	}
	
	String getFilename() {
		String homeDir = System.getProperty("user.home");
		String filename = homeDir + File.separator + "pin2dmd-autosave.xml";
		return filename;
	}

	@Override
	public void run() {
		long now = System.currentTimeMillis();
		long interval = ApplicationProperties.getInteger(ApplicationProperties.AUTOSAVE_INTERVAL)*60*1000;
		if( ApplicationProperties.getBoolean(ApplicationProperties.AUTOSAVE) && now > nextAutoSave ) {
			doAutoSave();
			nextAutoSave = now + interval;
		}
		display.timerExec(300*1000, this);
	}

	private void doAutoSave() {
		String filename = getFilename();
		log.info("auto save to {}", filename);
		editor.saveProject(filename);
	}
	
	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}
		
	public void checkAutoSaveAtStartup() {
		File f = new File(getFilename());
		if( f.exists() ) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
			messageBox.setText("auto save file found");
			messageBox.setMessage("auto save file from last run found. Should it be restored?");
			int res = messageBox.open();
			if( res == SWT.YES ) {
				editor.loadProject(getFilename());
			} else {
				deleteAutosaveFiles();
			}
		}
	}
	
	public void deleteAutosaveFiles() {
		log.info("deleting auto saved files (if any)");
		String filename = getFilename();
		File f = new File(filename);
		f.delete();
		filename = replaceExtensionTo("ani", filename);
		f = new File(filename);
		f.delete();
	}

}
