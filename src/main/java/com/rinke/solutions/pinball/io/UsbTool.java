package com.rinke.solutions.pinball.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;

public class UsbTool {

	public static enum UsbCmd {
		RESET(0), SAVE_CONFIG(1), SWITCH_DEVICEMODE(2), SWITCH_PALETTE(3),  UPLOAD_PALETTE(4), UPLOAD_MAPPING(5),
		UPLOAD_SMARTDMD_SIG(6), RESET_SETTINGS(7), SET_DISPLAY_TIMING(8), WRITE_FILE(9), WRITE_FILE_EX(10), SEND_SETTINGS(16),
		UPLOAD_MASK(17), DELETE_LICENSE(0xFD), RECEIVE_LICENSE(0xFE), DISPLAY_UID(0xFF);
		
		UsbCmd(int cmd) {
			this.cmd = (byte)cmd;
		}
		byte cmd;
	}; 
	
	private static Logger LOG = LoggerFactory.getLogger(UsbTool.class);

    public void upload(Palette palette) { 
        byte[] bytes = fromPalette(palette);
        bulk(bytes);
    }
    
    public void sendCmd(UsbCmd cmd) {
    	byte[] res = buildBuffer(cmd);
    	bulk(res);
    }
    
    public void upload(List<PalMapping> palMapppings) { 
        for (PalMapping palMapping : palMapppings) {
            byte[] bytes = fromMapping(palMapping);
            bulk(bytes);
        }
    }
    
    public void installLicense(String keyFile) {
    	byte[] res = buildBuffer(UsbCmd.RECEIVE_LICENSE);
    	Pair<Context,DeviceHandle> usb = null;
    	try (FileInputStream stream = new FileInputStream(keyFile)){
			stream.read(res, 5, 68);
			usb = initUsb();
			send(res, usb);
			Thread.sleep(100);
			receive(usb);
			Thread.sleep(1000);
			res = buildBuffer(UsbCmd.RESET);
			send(res,usb);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("problems installing license", e);
		} finally {
			releaseUsb(usb);
		}
    }
    
    public void sendFrame( Frame frame, Pair<Context, DeviceHandle> usb ) {
    	//LOG.info("sending frame to device: {}", frame);
    	byte[] buffer = buildFrameBuffer();
    	int i = 0;
    	if( frame.planes.size() == 2 ) {
    		System.arraycopy(Frame.transform(frame.planes.get(1).plane), 0, buffer, 4+1*512, 512);
    		System.arraycopy(Frame.transform(frame.planes.get(1).plane), 0, buffer, 4+2*512, 512);
    		byte[] planeOr = new byte[512];
    		byte[] planeAnd = new byte[512];
    		byte[] plane0 = frame.planes.get(0).plane;
    		byte[] plane1 = frame.planes.get(1).plane;
    		
    		for (int j = 0; j < plane0.length; j++) {
				planeOr[j] =  (byte) (plane0[j] | plane1[j]);
				planeAnd[j] =  (byte) (plane0[j] & plane1[j]);
			}
    		System.arraycopy(Frame.transform(planeOr), 0, buffer, 4+0*512, 512);
    		System.arraycopy(Frame.transform(planeAnd), 0, buffer, 4+3*512, 512);
    	} else {
        	for( Plane p : frame.planes) {
        		System.arraycopy(Frame.transform(p.plane), 0, buffer, 4+i*512, 512);
        		if( i++ > 3 ) break;
        	}
    	}
    	send(buffer, usb);
    }
    
    public void switchToPal( int standardPalNumber ) {
    	byte[] res = buildBuffer(UsbCmd.SWITCH_PALETTE);
    	res[5] = (byte) standardPalNumber;
    	bulk(res);
    }

    public void switchToMode( int deviceMode ) {
    	byte[] res = buildBuffer(UsbCmd.SWITCH_DEVICEMODE);
    	res[5] = (byte) deviceMode;
    	bulk(res);
    }
    
    public Pair<Context,DeviceHandle> initUsb() {
        Context ctx = initCtx();
        Device device = findDevice(ctx, (short) 0x314, (short)0xE457);
        if( device == null ) throw new LibUsbException("pin2dmd device not found",-1);
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to open USB device", result);
        result = LibUsb.claimInterface(handle, 0);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to claim USB interface", result);
        return Pair.of(ctx, handle);	
    }
    
    public void transferFile(String filename, InputStream is) {
    	byte[] data = buildBuffer(UsbCmd.WRITE_FILE_EX);
    	data[5] = (byte) 0;
    	String sdname = "0:/"+filename;
    	buildBytes(data, sdname);
    	Pair<Context, DeviceHandle> usb = initUsb();    
        try {
        	send(data, usb);
        	doHandShake(usb);
        	byte[] buffer = new byte[512];
        	int read;
        	while( (read = is.read(buffer)) > 0 ){
        		data = buildBuffer(UsbCmd.WRITE_FILE_EX);
        		data[5] = (byte) 1;
        		data[6] = (byte) (read >> 8);
        		data[7] = (byte) (read & 0xFF);
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
        	releaseUsb(usb);
        }
    	
    }

	private void doHandShake(Pair<Context, DeviceHandle> usb) {
		byte[] res = receive(usb);
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
	
	public void releaseUsb(Pair<Context, DeviceHandle> usb) {
		if( usb != null ) {
	    	LibUsb.releaseInterface(usb.getRight(), 0);
	        LibUsb.close(usb.getRight());
	        LibUsb.exit(usb.getLeft());	
		}
	}
        
    public void bulk(byte[] data) {
        Pair<Context, DeviceHandle> usb = initUsb();      
        try {
        	send(data, usb);
        } finally {
        	releaseUsb(usb);
        }
    }
    
    private byte[] receive(Pair<Context, DeviceHandle> usb) {
    	byte[] data = new byte[64];
		IntBuffer transfered = IntBuffer.allocate(1);
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
		buffer.put(data);
		// Use device handle here
		int res = LibUsb.bulkTransfer(usb.getRight(), (byte) 0x81, buffer, transfered, 4000);
		if (res != LibUsb.SUCCESS) throw new LibUsbException("Control transfer failed", res);
		int read = transfered.get();
		if( read != data.length ) {
		    LOG.error("unexpected length returned on bulk: {}", read);
		}
    	return data;
    }

	private void send(byte[] data, Pair<Context, DeviceHandle> usb) {
		IntBuffer transfered = IntBuffer.allocate(1);
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
		buffer.put(data);
		// Use device handle here
		int res = LibUsb.bulkTransfer(usb.getRight(), (byte) 0x01, buffer, transfered, 4000);
		if (res != LibUsb.SUCCESS) throw new LibUsbException("Control transfer failed", res);
		if( transfered.get() != data.length ) {
		    LOG.error("unexpected length returned on bulk: {}", transfered.get());
		}
	}
    
    private byte[] buildBuffer(UsbCmd usbCmd) {
        byte[] res = new byte[2052];
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe7; // used for small buffer 2052
        res[3] = (byte)0xFF; // do config
        res[4] = usbCmd.cmd;
        return res;
    }

    private byte[] buildFrameBuffer() {
        byte[] res = new byte[2052];
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe7;
        res[3] = (byte)0x00;
        return res;
    }

    
    private byte[] fromMapping(PalMapping palMapping) {
    	byte[] res = buildBuffer(UsbCmd.UPLOAD_MAPPING);
        int j = 5;
        for(int i = 0; i < palMapping.digest.length; i++)
            res[j++] = palMapping.digest[i];
        res[j++] = (byte) palMapping.palIndex;
        res[j++] = (byte) (palMapping.durationInFrames / 256);
        res[j++] = (byte) (palMapping.durationInFrames & 0xFF);
        return res;
    }

    private byte[] fromPalette(Palette palette) {
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

    private Context initCtx() {
        LOG.debug("init libusb");
        Context context = new Context();
        int result = LibUsb.init(context);
        LOG.debug("got context: {}",context);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to initialize libusb.", result);
        return context;
    }

    public Device findDevice(Context ctx, short vendorId, short productId) {
        // Read the USB device list
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(ctx, list);
        if (result < 0)
            throw new LibUsbException("Unable to get device list", result);

        try {
            // Iterate over all devices and scan for the right one
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS)
                    throw new LibUsbException("Unable to read device descriptor", result);
                LOG.debug("scanning for device: "
                    +String.format("%04X", descriptor.idVendor())
                    +", "+String.format("%04X", descriptor.idProduct()));
                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
                    LOG.debug("found {}", device);
                	return device;
                }
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, false);
        }
        LOG.error("usb device not found");
        // Device not found
        return null;
    }

}
