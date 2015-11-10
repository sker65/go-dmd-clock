package com.rinke.solutions.pinball.renderer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.renderer.Pcap.Paket;

public class Pcap {

	private LittleEndianDataInputStream stream;

	public Pcap(LittleEndianDataInputStream stream) {
		this.stream = stream;
	}
	
	public Header readHeader() throws IOException {
		Header header = new Header();
		header.read(stream);
		return header;
	}
	
	public static class Header {
		public byte[] magic = new byte[4];
		public int versionMajor;
		public int versionMinor;
		public long gmtOffset;
		public long flags;
		public long snaplen;
		public long network;

		public void read(LittleEndianDataInputStream stream) throws IOException {
			stream.read(magic);
			versionMajor = stream.readShort();
			versionMinor = stream.readShort();
			gmtOffset = stream.readInt();
			flags = stream.readInt();
			snaplen = stream.readInt();
			network = stream.readInt();
		}

		@Override
		public String toString() {
			return "Header [magic=" + Arrays.toString(magic)
					+ ", versionMajor=" + versionMajor + ", versionMinor="
					+ versionMinor + ", gmtOffset=" + gmtOffset + ", flags="
					+ flags + ", snaplen=" + snaplen + ", network=" + network
					+ "]";
		}
	}
	
	public static class Paket {
		public int sec;
		public int usec;
		public int incLen;
		public int orgLen;

		public void read(LittleEndianDataInputStream stream) throws IOException {
			sec = stream.readInt();
			usec = stream.readInt();
			incLen = stream.readInt();
			orgLen = stream.readInt();
		}

		@Override
		public String toString() {
			return "Paket [sec=" + sec + ", usec=" + usec + ", incLen="
					+ incLen + ", orgLen=" + orgLen + "]";
		}
		
	}

	public Paket readPaket() throws IOException {
		if( stream.available() >0 ) {
			Paket paket = new Paket();
			paket.read(stream);
			return paket;
		} else {
			return null;
		}
	}
	

}
