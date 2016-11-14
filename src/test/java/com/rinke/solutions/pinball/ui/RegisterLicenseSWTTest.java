package com.rinke.solutions.pinball.ui;


import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.Image;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManager.VerifyResult;
import com.rinke.solutions.pinball.model.Palette;

public class RegisterLicenseSWTTest {
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	private RegisterLicense uut;

	private LicenseManager licenseManager;
	
	@Before
	public void setUp() throws Exception {
		uut = new RegisterLicense(displayHelper.createShell());
		uut.createContents();
		licenseManager = Mockito.mock(LicenseManager.class);
		uut.licManager = licenseManager;
	}

	@Test
	public void testSave() throws Exception {
		uut.filename = "foo.properties";
		VerifyResult ver = new VerifyResult(true, 0);
		when(licenseManager.getLicense()).thenReturn(ver);
		uut.save();
	}
	
	@Test
	public void testLoad() throws Exception {
		when(licenseManager.getLicenseFile()).thenReturn("foo");
		uut.load();
	}


}
