package com.rinke.solutions.pinball.view.handler;

import java.io.File;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.MainView;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.handler.ProjectHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
@Bean
public class AutosaveHandler extends AbstractCommandHandler implements Runnable {
	
	private long nextAutoSave;
	
	@Autowired private ProjectHandler projectHandler;
	@Autowired private MessageUtil messageUtil;
	
	@Value int autosaveInterval;
	@Value boolean autosave;
	
	@Setter private MainView mainView;

	public AutosaveHandler(ViewModel vm) {
		super(vm);
		nextAutoSave = System.currentTimeMillis() + autosaveInterval*60*1000;
	}
	
	String getFilename() {
		String homeDir = System.getProperty("user.home");
		String filename = homeDir + File.separator + "pin2dmd-autosave.xml";
		return filename;
	}
	
	@Override
	public void run() {
		long now = System.currentTimeMillis();
		long interval = autosaveInterval*60*1000;
		if( autosave && now > nextAutoSave ) {
			doAutoSave();
			nextAutoSave = now + interval;
		}
		// dafür brauchts ein interface SWTDispatcher
		mainView.timerExec(300*1000, this);
	}

	void doAutoSave() {
		if( autosave ) {
			String filename = getFilename();
			log.info("auto save to {}", filename);
			projectHandler.saveProject(filename, false);
		}
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
			int res = messageUtil.warn(SWT.ICON_WARNING | SWT.YES | SWT.NO,
					"auto save file found",
					"auto save file from last run found. Should it be restored?");
			if( res == SWT.YES ) {
				projectHandler.onLoadProject(getFilename());
			} else {
				onDeleteAutosaveFiles();
			}
		}
	}
	
	public void onAutoSave() {
		doAutoSave();
	}
	
	public void onDeleteAutosaveFiles() {
		log.info("deleting auto saved files (if any)");
		String filename = getFilename();
		File f = new File(filename);
		f.delete();
		filename = replaceExtensionTo("ani", filename);
		f = new File(filename);
		f.delete();
	}

}
