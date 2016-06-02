package com.rinke.solutions.pinball.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class Pin2DmdConnectorTest {

	public static class Pin2DmdTestConnector extends Pin2DmdConnector {

		private byte[] data;
		private ByteArrayInputStream bis;
		private ByteArrayOutputStream bos;
		
		public Pin2DmdTestConnector(String address, byte[] data) {
			super(address);
			this.data = data;
			this.bis = new ByteArrayInputStream(data);
			this.bos = new ByteArrayOutputStream();
		}

		@Override
		protected byte[] receive(ConnectionHandle usb, int len) {
			byte[] b = new byte[len];
			try {
				bis.read(b);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return b;
		}

		@Override
		protected void send(byte[] res, ConnectionHandle usb) {
			try {
				this.bos.write(res);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public ConnectionHandle connect(String address) {
			return null;
		}

		@Override
		public void release(ConnectionHandle usb) {
		}
	};
		
	Pin2DmdTestConnector uut;
	
	@Before
	public void setUp() throws Exception {
		byte[] data = new byte[20];
		uut = new Pin2DmdTestConnector("", data );
	}

	@Test
	public void testTransferFile() throws Exception {
		InputStream is = new FileInputStream("src/test/resources/defaultPalettes.dat");
		uut.transferFile("foo", is );
	}

}
