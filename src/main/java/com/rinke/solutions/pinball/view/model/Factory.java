package com.rinke.solutions.pinball.view.model;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManagerFactory;

@Bean
public class Factory {

	@Bean
	public LicenseManager createLicenseManager() {
		return LicenseManagerFactory.getInstance();
	}
}
