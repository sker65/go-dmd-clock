package com.rinke.solutions.pinball.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.usb4java.Context;
import org.usb4java.DeviceHandle;

import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;

@Slf4j
public abstract class Pin2DmdConnector {

	public static enum UsbCmd {
		RESET(0), SAVE_CONFIG(1), SWITCH_DEVICEMODE(2), SWITCH_PALETTE(3),  UPLOAD_PALETTE(4), UPLOAD_MAPPING(5),
		UPLOAD_SMARTDMD_SIG(6), RESET_SETTINGS(7), SET_DISPLAY_TIMING(8), WRITE_FILE(9), WRITE_FILE_EX(10), SEND_SETTINGS(16),
		UPLOAD_MASK(17), DELETE_LICENSE(0xFD), RECEIVE_LICENSE(0xFE), DISPLAY_UID(0xFF);
		
		UsbCmd(int cmd) {
			this.cmd = (byte)cmd;
		}
		byte cmd;
	};
	
	public static class ConnectionHandle {
		
	}
	
	protected String address;
	protected DmdSize dmdSize = DmdSize.Size128x32;
	
    public Pin2DmdConnector(String address) {
		super();
		this.address = address;
	}

	protected byte[] buildBuffer(UsbCmd usbCmd) {
        byte[] res = new byte[2052];
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe7; // used for small buffer 2052
        res[3] = (byte)0xFF; // do config
        res[4] = usbCmd.cmd;
        return res;
    }

    protected byte[] buildFrameBuffer(int size) {
        byte[] res = new byte[size];
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe7;
        res[3] = (byte)0x00;
        return res;
    }

    protected byte[] fromMapping(PalMapping palMapping) {
    	byte[] res = buildBuffer(UsbCmd.UPLOAD_MAPPING);
        int j = 5;
        for(int i = 0; i < palMapping.digest.length; i++)
            res[j++] = palMapping.digest[i];
        res[j++] = (byte) palMapping.palIndex;
        res[j++] = (byte) (palMapping.durationInFrames / 256);
        res[j++] = (byte) (palMapping.durationInFrames & 0xFF);
        return res;
    }

    protected byte[] fromPalette(Palette palette) {
    	byte[] res = buildBuffer(UsbCmd.UPLOAD_PALETTE);
        //palette.writeTo(os);
        res[5] = (byte) palette.index;
        res[6] = (byte) palette.type.ordinal();// 6: type / default
        // 7 palette data
        int j = 7;
        for( int i =0; i < palette.colors.length;i++) {
            res[j++] = (byte) palette.colors[i].red;
            res[j++] = (byte) palette.colors[i].green;
            res[j++] = (byte) palette.colors[i].blue;
        }
        
        return res;
    }

    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#sendCmd(com.rinke.solutions.pinball.io.UsbTool.UsbCmd)
	 */
    public void sendCmd(UsbCmd cmd) {
    	byte[] res = buildBuffer(cmd);
    	bulk(res);
    }
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#upload(java.util.List)
	 */
    public void upload(List<PalMapping> palMapppings) { 
        for (PalMapping palMapping : palMapppings) {
            byte[] bytes = fromMapping(palMapping);
            bulk(bytes);
        }
    }
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#installLicense(java.lang.String)
	 */
    public void installLicense(String keyFile) {
    	byte[] res = buildBuffer(UsbCmd.RECEIVE_LICENSE);
    	ConnectionHandle usb = null;
    	try (FileInputStream stream = new FileInputStream(keyFile)){
			stream.read(res, 5, 68);
			usb = connect(null);
			send(res, usb);
			Thread.sleep(100);
			receive(usb,64);
			Thread.sleep(1000);
			res = buildBuffer(UsbCmd.RESET);
			send(res,usb);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("problems installing license", e);
		} finally {
			release(usb);
		}
    }
    
	public void upload(Palette palette) { 
		log.info("uploading palette {}", palette);
		upload(palette, null);
    }
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#transferFile(java.lang.String, java.io.InputStream)
	 */
	public void transferFile(String filename, InputStream is) {
		log.info("tranfering file {}", filename);
    	byte[] data = buildBuffer(UsbCmd.WRITE_FILE_EX);
    	data[5] = (byte) 0;
    	String sdname = filename;
    	buildBytes(data, sdname);
    	ConnectionHandle usb = connect(this.address);    
        try {
        	send(data, usb);
        	doHandShake(usb);
        	byte[] buffer = new byte[PinDmdEditor.PLANE_SIZE];
        	int read;
        	while( (read = is.read(buffer)) > 0 ){
        		data = buildBuffer(UsbCmd.WRITE_FILE_EX);
        		data[5] = (byte) 1;
        		data[6] = (byte) (read >> 8);
        		data[7] = (byte) (read & 0xFF);
        		System.arraycopy(buffer, 0, data, 8, read);
        		send(data, usb);
        		doHandShake(usb);
        	}
        	data = buildBuffer(UsbCmd.WRITE_FILE_EX);
    		data[5] = (byte) 0xFF;
    		send(data, usb);
    		doHandShake(usb);
        } catch (IOException e) {
        	throw new RuntimeException(e);
		} finally {
        	release(usb);
        }
    	
    }

	private void doHandShake(ConnectionHandle usb) {
		byte[] res = receive(usb,1);
		if( res[0] != 0) throw new RuntimeException("handshake error");
	}

	private void buildBytes(byte[] res, String sdname) {
		try {
			byte[] namebytes = sdname.getBytes("ASCII");
			int i = 6;
			for (byte b : namebytes) {
				res[i++] = b;
			}
			res[i] = 0;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	

    
    protected abstract byte[] receive(ConnectionHandle usb, int len);

	protected abstract void send(byte[] res, ConnectionHandle usb);

	/* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#sendFrame(com.rinke.solutions.pinball.model.Frame, org.apache.commons.lang3.tuple.Pair)
	 */
    public void sendFrame( Frame frame, ConnectionHandle usb ) {
    	//LOG.info("sending frame to device: {}", frame);
    	int headerSize = 4;
    	byte[] buffer = buildFrameBuffer(4*dmdSize.planeSize + headerSize); // max 4 planes
    	int i = 0;
    	int planeSize = dmdSize.planeSize;
    	if( dmdSize.equals(DmdSize.Size192x64) ) {
    		// XL dmd is handled different
    		for( Plane p : frame.planes) {
        		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
        		if( i++ > 3 ) break; // max 4 planes
        	}
    		buffer[3] = (byte)(i*planeSize/512);
    	} else {
        	if( frame.planes.size() == 2 ) {
        		byte[] planeAnd = new byte[planeSize];
        		byte[] plane0 = frame.planes.get(0).data;
        		byte[] plane1 = frame.planes.get(1).data;
        		
        		for (int j = 0; j < plane0.length; j++) {
    				planeAnd[j] =  (byte) (plane0[j] & plane1[j]);
    			}
        		System.arraycopy(Frame.transform(plane0), 0, buffer, headerSize+0*planeSize, planeSize);
        		System.arraycopy(Frame.transform(plane1), 0, buffer, headerSize+2*planeSize, planeSize);
        		System.arraycopy(Frame.transform(planeAnd), 0, buffer, headerSize+1*planeSize, planeSize);
        		System.arraycopy(Frame.transform(planeAnd), 0, buffer, headerSize+3*planeSize, planeSize);
        		buffer[3] = (byte)0x04;
        	} else {
            	for( Plane p : frame.planes) {
            		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
            		if( i++ > 3 ) break;
            	}
            	buffer[3] = (byte)(i*planeSize/512);
        	}
    	}
    	send(buffer, usb);
    }
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#switchToPal(int)
	 */
    public void switchToPal( int standardPalNumber, ConnectionHandle handle ) {
    	byte[] res = buildBuffer(UsbCmd.SWITCH_PALETTE);
    	res[5] = (byte) standardPalNumber;
	    if( handle == null ) {
	    	bulk(res);
	    } else {
	    	send(res, handle);
	    }
    }

    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#switchToMode(int)
	 */
    public void switchToMode( int deviceMode, ConnectionHandle handle ) {
    	byte[] res = buildBuffer(UsbCmd.SWITCH_DEVICEMODE);
    	res[5] = (byte) deviceMode;
	    if( handle == null ) {
	    	bulk(res);
	    } else {
	    	send(res, handle);
	    }
    }

	public abstract ConnectionHandle connect(String address);
	
	public abstract void release(ConnectionHandle usb);

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void bulk(byte[] data) {
	    ConnectionHandle usb = connect(this.address);      
	    try {
	    	send(data, usb);
	    } finally {
	    	release(usb);
	    }
	}

	public void upload(Palette palette, ConnectionHandle handle) { 
	    byte[] bytes = fromPalette(palette);
	    if( handle == null ) {
	    	bulk(bytes);
	    } else {
	    	send(bytes, handle);
	    }
		
	}

	public void setDmdSize(DmdSize dmdSize) {
		this.dmdSize = dmdSize;
	}

}