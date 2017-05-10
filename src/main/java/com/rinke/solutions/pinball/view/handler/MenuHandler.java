package com.rinke.solutions.pinball.view.handler;

import org.eclipse.swt.widgets.Shell;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.BeanFactory;
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
	
	public MenuHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model, d);
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
	}


}
