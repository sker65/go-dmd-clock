package com.rinke.solutions.pinball.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rinke.solutions.pinball.util.LicenseVerifier.VerifyResult;


public class LicenseVerifierTest {
	
	LicenseVerifier uut = new LicenseVerifier();

	@Test
	public void testVerify() throws Exception {
		String filename = "src/test/resources/#38003C00085140.key";
		VerifyResult verifyResult = uut.verify(filename);
		assertTrue( verifyResult.success );
	}

}
