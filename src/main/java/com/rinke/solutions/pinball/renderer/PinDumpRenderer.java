package com.rinke.solutions.pinball.renderer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.LittleEndianDataInputStream;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DeviceMode;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.renderer.Pcap.Header;
import com.rinke.solutions.pinball.renderer.Pcap.Paket;

public class PinDumpRenderer extends Renderer {

	private static Logger LOG = LoggerFactory.getLogger(PinDumpRenderer.class);

	void readImage(String filename, DMD dmd) {
		InputStream stream = null;
		long bufSize = new File(filename).length();
		try {
			if (filename.endsWith(".dump.gz")) {
				stream = new GZIPInputStream(new FileInputStream(filename),
						(int) bufSize);
			} else if (filename.endsWith(".dump")) {
				stream = new BufferedInputStream(new FileInputStream(filename));
			} else {
				throw new RuntimeException(
						"bad file type / file extension. *.dump or *.dump.gz expected");
			}

			long lastTimestamp = 0;
			long firstTimestamp = 0;
			long lastCRC = -1;
			DeviceMode deviceMode = DeviceMode.forOrdinal(stream.read());
			byte[] tcBuffer = new byte[4];
			long tc = 0;
			int numberOfFrames = 0;
			switch (deviceMode) {
			case Gottlieb:
				numberOfFrames = 5;
				break;
			case Stern:
				numberOfFrames = 4;
				break;

			default:
				numberOfFrames = 3;
				break;
			}
			int buflen = PinDmdEditor.PLANE_SIZE * numberOfFrames;
			while (stream.available() > 0) {
				stream.read(tcBuffer);
				tc = (((int)tcBuffer[3]&0xFF) << 24) + (((int)tcBuffer[2]&0xFF) << 16) 
						+ (((int)tcBuffer[1]&0xFF) << 8) + ((int)tcBuffer[0]&0xFF);
				if( firstTimestamp == 0) { firstTimestamp = tc; lastTimestamp = tc; }
				byte[] data = new byte[buflen];
				stream.read(data);
				long crc = getCRC(data);
				if( crc == lastCRC ) continue;
				lastCRC = crc;
				Frame res = null;
				// TODO for Gottlieb and WPC do an additive aggregation
				if( deviceMode.equals(DeviceMode.WPC) ||
						deviceMode.equals(DeviceMode.Gottlieb) ) {
					res = transformPlanes(data, deviceMode);
				} else {
					res = new Frame();
					for(int i = 0; i < numberOfFrames; i++) {
						res.planes.add(new Plane((byte)i, Frame.transform(data, i*PinDmdEditor.PLANE_SIZE, dmd.getPlaneSizeInByte())));
					}
				}

				// res = buildSummarizedFrame(dmd.getWidth(),
				// dmd.getHeight(),data, offset+4);

				res.delay = (int) (tc- lastTimestamp);
				res.timecode = (int) (tc - firstTimestamp);
				if (res.delay > 1) {
					// System.out.println("frame"+frames.size()+", delay: "+res.delay
					// + " "+p);
					LOG.debug("Frame {}", res);
					frames.add(res);
				}
				lastTimestamp = tc;
				// frameNo++;
			}
			this.maxFrame = frames.size();

		} catch (IOException e) {
			LOG.error("error on reading from stream for {}", filename, e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					LOG.error("error on closing stream for {}", filename, e);
				}
			}
		}
	}

	private long getCRC(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return crc.getValue();
	}

	private Frame transformPlanes(byte[] data, DeviceMode deviceMode) {
		byte[] plane1 = new byte[PinDmdEditor.PLANE_SIZE];
		byte[] plane2 = new byte[PinDmdEditor.PLANE_SIZE];
		
		for( int i = 0; i <PinDmdEditor.PLANE_SIZE ; i++) {
			byte v0 = data[i];
			byte v1 = data[i+PinDmdEditor.PLANE_SIZE];
			byte v2 = data[i+PinDmdEditor.PLANE_SIZE*2];
			for( int j = 0; j < 8; j++) {				
				int sum = v0&1 + v1&1 + v2&1;
				if( (sum&1) != 0) {
					plane1[i] |= (1 << j);
				}
				if( (sum&2) != 0 ) {
					plane1[i] |= (1 << j);
				}
				v0 >>= 1; v1 >>= 1; v2 >>= 1;
			}
		}
		return new Frame(plane1 , plane2);
	}

	@Override
	public long getTimeCode(int actFrame) {
		return actFrame < frames.size() ? frames.get(actFrame).timecode : 0;
	}

	public static void main(String[] args) {
		PinDumpRenderer renderer = new PinDumpRenderer();
		String base = "/Users/stefanri/Documents/";
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		renderer.convert(base + "250216_224617_pin2dmd.dump.gz", dmd, 0);
		List<Frame> frames = renderer.getFrames();
		try {
			int i = 0;
			FileOutputStream stream = new FileOutputStream("/tmp/test.dat");
			System.out.println(frames.size());
			for (Frame frame : frames) {
				for (Plane plane : frame.planes) {
					byte[] line = new byte[16];
					System.arraycopy(plane.data, 20*16, line, 0, 16);
					if( i++<3) stream.write(line);
					if( i == 5 ) i = 0;
				}
			}
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
