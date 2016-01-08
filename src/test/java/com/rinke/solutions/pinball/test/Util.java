package com.rinke.solutions.pinball.test;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class Util {
	
	public static String isBinaryIdentical(String filename, String filename2) throws IOException {
		byte[] b1 = IOUtils.toByteArray(new FileInputStream(filename));
		byte[] b2 = IOUtils.toByteArray(new FileInputStream(filename2));
		if( b1.length != b2.length ) return String.format("different lenth %d : %d", b1.length, b2.length);
		for( int i = 0; i < b1.length; i++) {
			if( b1[i] != b2[i] ) return String.format("files differ at %d: b1: %s <-> b2: %s", i, getBytes(b1, i, 8), getBytes(b2,i,8));
		}
		return null;
	}

	private static String getBytes(byte[] b, int offset, int count) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while( count-- > 0 ) {
			sb.append( String.format(" %02X", b[offset+i]));
			i++;
		}
		return sb.toString();
	}

	
}
