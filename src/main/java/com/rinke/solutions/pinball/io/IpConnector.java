package com.rinke.solutions.pinball.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.usb4java.Context;
import org.usb4java.DeviceHandle;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;

@Slf4j
public class IpConnector extends Pin2DmdConnector {

	public static final int DEFAULT_PORT = 9191;
	private static final int IP_PACKET_SIZE = 256;

	public IpConnector(String address) {
		super(address);
	}
	
	@Data
	public static class IpHandle extends ConnectionHandle {
		Socket socket;
		OutputStream ostream;
		InputStream istream;

		public IpHandle(Socket s) {
			super();
			this.socket = s;
		}
	}

	@Override
	protected byte[] receive(ConnectionHandle h, int len) {
		IpHandle ip = (IpHandle) h;
		byte[] data = new byte[len];
		try {
			ip.getIstream().read(data);
		} catch (IOException e) {
			log.error("receive problems with {}",address);
			throw new RuntimeException("receive problem "+address,e);
		}
		return data;
	}

	@Override
	protected void send(byte[] res, ConnectionHandle h) {
		IpHandle ip = (IpHandle) h;
		try {
			int remain = res.length;
			int offset = 0;
			while(remain > 0 ) {
				int toSend = Math.min(IP_PACKET_SIZE, remain);
				ip.getOstream().write(res, offset, toSend);
				ip.getOstream().flush();
				remain -= toSend;
				offset += toSend;
			}
		} catch (IOException e) {
			log.error("sending problems with {}",address);
			throw new RuntimeException("sending problem "+address,e);
		}
		
	}

	@Override
	public ConnectionHandle connect(String address) {
		String[] p = address.split(":");
		Socket s = null;
		try {
			s = new Socket(p[0], p.length == 2 ? Integer.parseInt(p[1]):DEFAULT_PORT);
		} catch (NumberFormatException | IOException e) {
			log.error("connecting problems with {}",address);
			throw new RuntimeException("connect problem "+address,e);
		}
		return new IpHandle(s);
	}

	@Override
	public void release(ConnectionHandle h) {
		IpHandle ip = (IpHandle) h;
		try {
			ip.getSocket().close();
		} catch (IOException e) {
			log.error("closing problems with {}",address);
			throw new RuntimeException("closing problem "+address,e);
		}
	}


}
