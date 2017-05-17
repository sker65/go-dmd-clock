package com.rinke.solutions.pinball.view.handler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.AniActionHandler;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.View;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class MenuHandler extends ViewHandler {
	
	@Autowired
	private Shell shell;
	
	@Autowired
	private BeanFactory beanFactory;
	
	@Autowired
	private MessageUtil messageUtil;
	
	@Autowired
	AniActionHandler aniActionHandler;
	
	@Value
	private boolean nodirty; // if set ignore dirty check
	
	public MenuHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model, d);
	}

	/**
	 * check if dirty.
	 * 
	 * @return true, if not dirty or if user decides to ignore dirtyness (or
	 *         global ignore flag is set via cmdline)
	 */
	public boolean couldQuit() {
		if (model.dirty && !nodirty) {
			int res = messageUtil.warn(SWT.ICON_WARNING | SWT.OK | SWT.CANCEL, "Unsaved Changes", "There are unsaved changes in project. Proceed?");
			return (res == SWT.OK);
		} else {
			return true;
		}
	}

	
	public void onAbout() {
		log.info("onAbout");
		View about = beanFactory.getBeanOfType(View.class, "about");
		about.open();
	}
	
	public void onDeviceConfiguration() {
		log.info("onDeviceConfiguration");
		View deviceConfig = beanFactory.getBeanOfType(View.class, "deviceConfig");
		deviceConfig.open();
	}
	
	public void onConfiguration() {
		log.info("onConfiguration");
		View configDialog = beanFactory.getBeanOfType(View.class, "configDialog");
		configDialog.open();
	}
	
	public void onQuit() {
		log.info("onQuit");
		// dirty check
		if( couldQuit())
			shell.close();
	}
	
	public void onLoadAniWithFC(boolean append) {
		aniActionHandler.onLoadAniWithFC(append);
	}

}
