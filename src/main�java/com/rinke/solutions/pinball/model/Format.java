package com.rinke.solutions.pinball.model;

public enum Format {
	XML, JSON, BIN, DAT, UNKOWN;

    public static Format byFilename(String filename) {
        int i = filename.lastIndexOf('.');
        if( i != -1) {
            return Format.valueOf(filename.substring(i+1).toUpperCase());
        }
        return UNKOWN;
    }
}
