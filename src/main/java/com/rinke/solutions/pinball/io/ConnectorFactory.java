package com.rinke.solutions.pinball.io;

public class ConnectorFactory {

	public static Pin2DmdConnector create(String address) {
		if( address == null || address.startsWith("usb://")) {
			return new UsbConnector(address);
		} else {
			return new IpConnector(address);
		}
		
	}
	
}
