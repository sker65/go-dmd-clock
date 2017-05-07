package com.rinke.solutions.pinball;

import java.io.File;

import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.swt.SWTDispatcher;
import com.rinke.solutions.pinball.util.ApplicationProperties;
import com.rinke.solutions.pinball.util.MessageUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class AutosaveHandler implements Runnable {
	
	@Autowired
	PinDmdEditor editor;
	MessageUtil messageUtil;
	SWTDispatcher dispatcher;

	private long nextAutoSave;

	public AutosaveHandler(MessageUtil messageUtil, SWTDispatcher dis) {
		super();
		this.messageUtil = messageUtil;
		this.dispatcher = dis;
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
		dispatcher.timerExec(300*1000, this);
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
			int res = messageUtil.warn("auto save file found", "auto save file from last run found. Should it be restored?");
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

	public void setEditor(PinDmdEditor editor) {
		 this.editor = editor;
	}

}
