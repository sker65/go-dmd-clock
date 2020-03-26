package com.rinke.solutions.pinball.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;

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

	@Test
	public void testBuildFrameBuffer() throws Exception {
		uut.buildFrameBuffer(1536, 1, 2);
	}

	@Test
	public void testFromMapping() throws Exception {
		PalMapping k = new PalMapping(1, "foo");
		k.digest = new byte[]{1,2,3,4};
		uut.fromMapping(k);
	}

	@Test
	public void testFromPalette() throws Exception {
		Palette palette = Palette.getDefaultPalettes().get(0);
		uut.fromPalette(palette );
	}

	@Test
	public void testInstallLicense() throws Exception {
		uut.installLicense("src/test/resources/#3E002400164732.key");
	}

	@Test
	public void testUploadPalette() throws Exception {
		Palette palette = Palette.getDefaultPalettes().get(0);
		uut.upload(palette);
	}

	@Test
	public void testSendFrame() throws Exception {
		Frame frame = new Frame();
		frame.planes.add(new Plane((byte) 1, new byte[512]));
		frame.planes.add(new Plane((byte) 2, new byte[512]));
		uut.sendFrame(frame);
	}

	@Test
	public void testSendFrameWithBigFrame() throws Exception {
		Frame frame = new Frame();
		frame.planes.add(new Plane((byte) 1, new byte[1536]));
		frame.planes.add(new Plane((byte) 2, new byte[1536]));
		uut.setDmdSize(DmdSize.Size192x64);
		uut.sendFrame(frame);
	}

	@Test
	public void testSwitchToPal() throws Exception {
		uut.switchToPal(1);
	}

	@Test
	public void testSwitchToMode() throws Exception {
		uut.switchToMode(0);
	}

}
