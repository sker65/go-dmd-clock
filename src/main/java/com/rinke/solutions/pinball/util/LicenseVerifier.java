package com.rinke.solutions.pinball.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.Arrays;

public class LicenseVerifier {
	
	public static class VerifyResult {
		
		public VerifyResult(boolean result, long c) {
			this.success = result;
			this.capabilities = c;
		}
		public boolean success;
		public long capabilities;
		@Override
		public String toString() {
			return "VerifyResult [success=" + success + ", capabilities="
					+ capabilities + "]";
		}
	}

	public static void main(String[] args) throws Exception {
		LicenseVerifier verifier = new LicenseVerifier();
		VerifyResult verify = verifier.verify("#38003C00085140.key");
		System.out.println(verify);
	}

	public VerifyResult verify(String filename) throws Exception {

		byte[] hash = new byte[32];
		
		byte[] sig = IOUtils.toByteArray(new FileInputStream(filename) );
		
		PublicKey publicKey = load(pkHex, "secp256k1");
		Signature ecdsaVerify = Signature.getInstance("NONEwithECDSA");
		ecdsaVerify.initVerify(publicKey);
		
		// extract basename
		byte[] basename = FilenameUtils.getBaseName(filename).getBytes("ASCII");
		// UID + feature = 19 byte
		for(int i = 0; i<15; i++) {
			hash[i] = basename[i];
		}
		for(int i = 0; i<4; i++) {
			hash[i+15] = sig[i+64];
		}
		
		ecdsaVerify.update(hash);

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ASN1OutputStream aOut = new ASN1OutputStream(bOut);

		ASN1Integer[] array = {
				new ASN1Integer(new BigInteger(Arrays.copyOfRange(sig, 0, 32))),
				new ASN1Integer(new BigInteger(Arrays.copyOfRange(sig, 32, 64))) };

		DERSequence seq = new DERSequence(array);
		
		aOut.writeObject(seq);
		aOut.close();
		byte[] sigDat = bOut.toByteArray();
		
		boolean result = ecdsaVerify.verify(sigDat);
		//System.out.println("verify result: " + result);
		long cap = 0;
		for(int i = 0; i<4; i++) {
			cap <<= 8;
			cap += sig[i+64];
		}
		return new VerifyResult(result,cap);
	}

	String swapLsb(String in) {
		StringBuilder out = new StringBuilder(in.length());
		for (int i = 62; i >= 0; i -= 2) {
			// byte b =
			out.append(in.substring(i, i + 2));
		}
		if (in.length() > 64) {
			for (int i = 126; i >= 64; i -= 2) {
				out.append(in.substring(i, i + 2));
			}
		}
		return out.toString();
	}

	String pkHex = "fffb2a01aea91799293f07c862614b8782e5eae97739e499a79d8fd6bb816ba939ea4ab2ed72d57633f8098d1cde8930f61f686505bc1d7a0463d53d8eb39d5f";

	public PublicKey load(String hexPubKeyXY, String curveName)
			throws Exception {
		String hexX = hexPubKeyXY.substring(0, 64);
		String hexY = hexPubKeyXY.substring(64);
		ECPoint point = new ECPoint(new BigInteger(hexX, 16), new BigInteger(
				hexY, 16));

		AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
		parameters.init(new ECGenParameterSpec(curveName));
		ECParameterSpec ecParameters = parameters
				.getParameterSpec(ECParameterSpec.class);

		ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, ecParameters);

		return KeyFactory.getInstance("EC").generatePublic(pubKeySpec);

	}

	public byte[] toByte(String hex) {
		if (hex == null)
			return null;
		hex = hex.replaceAll("\\s", "");
		byte[] buffer = null;
		if (hex.length() % 2 != 0) {
			hex = "0" + hex;
		}
		int len = hex.length() / 2;
		buffer = new byte[len];
		for (int i = 0; i < len; i++) {
			buffer[i] = (byte) Integer.parseInt(
					hex.substring(i * 2, i * 2 + 2), 16);
		}
		return buffer;
	}

}
