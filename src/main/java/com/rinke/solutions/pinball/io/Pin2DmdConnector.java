package com.rinke.solutions.pinball.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.util.MessageUtil;

import static java.lang.Math.*;

@Slf4j
public abstract class Pin2DmdConnector {
	
	@Autowired MessageUtil messageUtil;

	private static final int USB_BUFFER_SIZE = 512;

	public static enum UsbCmd {
		RESET(0), SAVE_CONFIG(1), SWITCH_DEVICEMODE(2), SWITCH_PALETTE(3),  UPLOAD_PALETTE(4), UPLOAD_MAPPING(5),
		UPLOAD_SMARTDMD_SIG(6), RESET_SETTINGS(7), SET_DISPLAY_TIMING(8), WRITE_FILE(9), WRITE_FILE_EX(10), SEND_SETTINGS(0x10), RECEIVE_SETTINGS(0x20),
		UPLOAD_MASK(17), SEND_LICENSE(0xFC), DELETE_LICENSE(0xFD), RECEIVE_LICENSE(0xFE), DISPLAY_UID(0xFF);
		
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

	protected byte[] buildPalBuffer(Palette palette) {
        byte[] res = new byte[6 + palette.colors.length*3];
        res[0] = (byte)0x01;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe7; 
        res[3] = (byte)0xFE; 
        res[4] = (byte)0xED;
        res[5] = (byte)palette.colors.length;
        int j = 6;
        for( int i =0; i < palette.colors.length;i++) {
            res[j++] = (byte) palette.colors[i].red;
            res[j++] = (byte) palette.colors[i].green;
            res[j++] = (byte) palette.colors[i].blue;
        }

        return res;
    }

    byte[] buildFrameBuffer(int size, int headerByte, int sizeByte) {
        byte[] res = new byte[size+4]; // add header size
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)headerByte;
        res[3] = (byte)sizeByte;
        return res;
    }

    byte[] fromMapping(PalMapping palMapping) {
    	byte[] res = buildBuffer(UsbCmd.UPLOAD_MAPPING);
        int j = 5;
        for(int i = 0; i < palMapping.digest.length; i++)
            res[j++] = palMapping.digest[i];
        res[j++] = (byte) palMapping.palIndex;
        res[j++] = (byte) (palMapping.durationInFrames / 256);
        res[j++] = (byte) (palMapping.durationInFrames & 0xFF);
        return res;
    }

    byte[] fromPalette(Palette palette) {
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
    
    public void sendBrightness(int value) {
    	byte[] res = buildBuffer(UsbCmd.SET_DISPLAY_TIMING);
    	res[17] = (byte) value;
    	bulk(res);
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
	    byte[] bytes = fromPalette(palette);
    	bulk(bytes);
    }
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#transferFile(java.lang.String, java.io.InputStream)
	 */
	public void transferFile(String filename, InputStream is) {
		log.info("transfering file {}", filename);
    	byte[] data = buildBuffer(UsbCmd.WRITE_FILE_EX);
    	data[5] = (byte) 0;
    	String sdname = filename;
    	buildBytes(data, sdname);
    	ConnectionHandle usb = connect(this.address);
    	if (usb != null) {
	        try {
	        	send(data, usb);
	        	doHandShake(usb);
	        	byte[] buffer = new byte[USB_BUFFER_SIZE];
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
    }

	public byte[] loadConfig () {
		log.info("receiving config from device");
    	ConnectionHandle usb = connect(this.address);
    	byte[] ret;
    	if (usb == null) {
    		return null;
    	}
        try {
        	byte[] data = buildBuffer(UsbCmd.SEND_SETTINGS);
        	send(data, usb);
        	ret = receive(usb,63);
		} finally {
        	release(usb);
        }
    	return ret;
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
    public void sendFrame( Frame frame ) {
    	//LOG.info("sending frame to device: {}", frame);
    	int i = 0;
    	int headerSize = 4;
    	int planeSize = dmdSize.planeSize;
    	int bufferSize = 0;
    	int planeCount = 0;
    	
    	
    	for( Plane p : frame.planes) planeCount++;
    	
    	if (planeCount < 24) {
   			bufferSize = min(6,frame.planes.size()) * planeSize;
	    	// XL dmd is handled different: use E8 framing with size byte
	    	if( dmdSize.equals(DmdSize.Size256x64) ) {
	        	byte[] buffer = buildFrameBuffer( bufferSize, 0xE8, bufferSize/512 );
	    		for( Plane p : frame.planes) {
	        		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
	        		i++;
	        		if( i > 5 ) break; // max 6 planes
	        	}
	           	bulk(buffer);
	        }
	        else if( dmdSize.equals(DmdSize.Size192x64) ) {
	        	byte[] buffer = buildFrameBuffer( bufferSize, 0xE8, bufferSize/512 );
	    		for( Plane p : frame.planes) {
	        		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
	        		i++;
	        		if( i > 5 ) break; // max 4 planes
	        	}
	           	bulk(buffer);
	    	} else {
	        	if( frame.planes.size() == 2 ) {
	        		byte[] buffer = buildFrameBuffer(bufferSize, 0xE8, bufferSize/512);
		    		for( Plane p : frame.planes) {
		        		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
		        		i++;
		        		if( i > 1 ) break; // max 2 planes
		        	}
		           	bulk(buffer);
	        	} else if( frame.planes.size() == 4 ) {
	        		byte[] buffer = buildFrameBuffer(bufferSize, 0xE7, 0);
	            	for( Plane p : frame.planes) {
	            		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
	            		i++;
	            		if( i > 3 ) break;
	            	}
		           	bulk(buffer);
	        	} else {
		        	byte[] buffer = buildFrameBuffer( bufferSize, 0xE8, bufferSize/512 );
	            	for( Plane p : frame.planes) {
	            		System.arraycopy(Frame.transform(p.data), 0, buffer, headerSize+i*planeSize, planeSize);
	            		i++;
	            		if( i > 5 ) break;
	            	}
    	           	bulk(buffer);
	        	}
	    	}
    	} else { // 0-7 red, 8-15 green, 16-23 blue
    		bufferSize = min(15,frame.planes.size()) * planeSize;
        	byte[] buffer = buildFrameBuffer(bufferSize, 0xE8, bufferSize/512 );
        	for( int j=3; j < (planeCount/3); j++ ) {
    			byte[] planeR = frame.planes.get(j).data;
    			byte[] planeG = frame.planes.get(j+8).data;
    			byte[] planeB = frame.planes.get(j+16).data;
        		System.arraycopy(Frame.transform(planeR), 0, buffer, headerSize+((j-3)*planeSize), planeSize);
        		System.arraycopy(Frame.transform(planeG), 0, buffer, headerSize+((j-3)*planeSize)+(5*planeSize), planeSize);
        		System.arraycopy(Frame.transform(planeB), 0, buffer, headerSize+((j-3)*planeSize)+(10*planeSize), planeSize);
        	}
           	bulk(buffer);
    	}
    }
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#switchToPal(int)
	 */
    public void switchToPal(int standardPalNumber) {
    	byte[] res = buildBuffer(UsbCmd.SWITCH_PALETTE);
    	res[5] = (byte) standardPalNumber;
	    bulk(res);
    }
    
    public void setPal(Palette palette) {
    	byte[] res = buildPalBuffer(palette);
	    bulk(res);
    }

    /* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.Pin2DmdConnector#switchToMode(int)
	 */
    public void switchToMode( int deviceMode ) {
    	byte[] res = buildBuffer(UsbCmd.SWITCH_DEVICEMODE);
    	res[5] = (byte) deviceMode;
    	bulk(res);
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
	    if (usb != null) {
		    try {
		    	send(data, usb);
		    } catch(Exception e){
		    }	
		    finally {
		    		release(usb);
		    }
	    }
	}

	public void setDmdSize(DmdSize dmdSize) {
		this.dmdSize = dmdSize;
	}

}