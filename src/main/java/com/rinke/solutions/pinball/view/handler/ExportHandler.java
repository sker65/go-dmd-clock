package com.rinke.solutions.pinball.view.handler;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class ExportHandler extends AbstractCommandHandler {

	@Autowired MessageUtil messageUtil;
	@Autowired FileChooserUtil fileChooserUtil;
	// @Autowired 
	LicenseManager licenseManager;
	
	@Autowired
	BeanFactory beanFactory;
	
	//@Autowired ProjectHandler projectHandler;
	
	@Value(key=Config.OLDEXPORT)
	boolean useOldExport;

	public ExportHandler(ViewModel vm) {
		super(vm);
	}

	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}

	public void xxonExportGoDMD() {
		messageUtil.warn("not implemented", "sorry but this is not yet implemented");
	}
	
	public void xxonExportGif() {
	}

	
}