package com.rinke.solutions.pinball.util;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;

@Slf4j
public class ByteUtilTest {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testShiftLeft() {
		byte[] test = {(byte)0xff, (byte)0xff};
		for( int i = 0; i < 16; i++) {
			ByteUtil.shiftLeft(test, 1, false);
			log.info(toBitString(test));
		}
	}

	@Test
	public void testShiftLeft2() {
		byte[] test = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x0f, (byte)0xff};
		log.info(toBitString(test));
		ByteUtil.shiftLeft(test, 13, false);
		log.info(toBitString(test));
	}

	@Test
	public void testShiftLeft3() {
		byte[] test = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0, (byte)0x0f, (byte)0};
		log.info(toBitString(test));
		for( int i = 0; i < 16; i++) {
			ByteUtil.shiftLeft(test, 1, true);
			log.info(toBitString(test));
		}
	}

	@Test
	public void testShiftRicht3() {
		byte[] test = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0, (byte)0x0f, (byte)0};
		log.info(toBitString(test));
		for( int i = 0; i < 16; i++) {
			ByteUtil.shiftRight(test, 1, true);
			log.info(toBitString(test));
		}
	}

	@Test
	public void testShiftRight() {
		byte[] test = {(byte)0xff, (byte)0xff};
		for( int i = 0; i < 16; i++) {
			ByteUtil.shiftRight(test, 1, false);
			log.info(toBitString(test));
		}
	}

	@Test
	public void testShiftRight2() {
		byte[] test = {(byte)0xff, (byte)0x0f, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
		log.info(toBitString(test));
		ByteUtil.shiftRight(test, 13, false);
		log.info(toBitString(test));
	}

	private String toBitString(byte[] test) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < test.length; i++) {
			byte b = test[i];
			for( int j = 7; j >= 0; j--) {
				if( (b & (1<<j)) != 0) sb.append('*');
				else sb.append('.');
			}
		}
		return sb.toString();
	}

}
