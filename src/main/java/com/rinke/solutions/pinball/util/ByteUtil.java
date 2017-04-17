package com.rinke.solutions.pinball.util;

public class ByteUtil {
	
	/**
	 * Shifts input byte array len bits left.This method will alter the input byte array.
	 * @param positivePadding 
	 */
	public static byte[] shiftLeft(byte[] data, int len, boolean positivePadding) {
	    int word_size = (len / 8) + 1;
	    int shift = len % 8;
	    byte carry_mask = (byte) ((1 << shift) - 1);
	    int offset = word_size - 1;
	    for (int i = 0; i < data.length; i++) {
	        int src_index = i+offset;
	        if (src_index >= data.length) {
	            data[i] = (byte) (positivePadding ? -1 : 0);
	        } else {
	            byte src = data[src_index];
	            byte dst = (byte) (src << shift);
	            if( src_index == data.length-1 && positivePadding ) {
	            	dst |= 255 >> (8-shift);
	            }
	            if (src_index+1 < data.length) {
	                dst |= data[src_index+1] >>> (8-shift) & carry_mask;
	            }
	            data[i] = dst;
	        }
	    }
	    return data;
	}
	
	public static byte[] shift(byte[] data, int shift, boolean positivePadding) {
		if( shift>0 ) return shiftRight(data, shift, positivePadding);
		if( shift<0 ) return shiftLeft(data, -shift, positivePadding);
		return data;
	}
	
	public static byte[] shiftRight(byte[] data, int len, boolean positivePadding) {
	    int word_size = (len / 8) + 1;
	    int shift = len % 8;
	    byte carry_mask =(byte) (0xFF >>> shift);
	    carry_mask = (byte) ~carry_mask;
	    int offset = word_size - 1;
	    for (int i = data.length-1; i >= 0; i--) {
	        int src_index = i-offset;
	        if (src_index < 0) {
	            data[i] = (byte) (positivePadding ? -1 : 0);
	        } else {
	            byte src = data[src_index];
	            byte dst = (byte) (Byte.toUnsignedInt(src) >>> shift);
	            if( src_index == 0 && positivePadding ) {
	            	dst |= 255 << (8-shift);
	            }
	            if (src_index-1 >= 0 ) {
	                dst |= data[src_index-1] << (8-shift) & carry_mask;
	            }
	            data[i] = dst;
	        }
	    }
	    return data;
	}
}
