package com.rinke.solutions.pinball.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;

public class UsbTool {

	public static enum UsbCmd {
		RESET(0), SAVE_CONFIG(1), SWITCH_DEVICEMODE(2), SWITCH_PALETTE(3),  UPLOAD_PALETTE(4), UPLOAD_MAPPING(5),
		UPLOAD_SMARTDMD_SIG(6), RESET_SETTINGS(7), SET_DISPLAY_TIMING(8), WRITE_FILE(9), SEND_SETTINGS(16),
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
    
    public void upload(List<PalMapping> palMapppings) { 
        for (PalMapping palMapping : palMapppings) {
            byte[] bytes = fromMapping(palMapping);
            bulk(bytes);
        }
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
    
    public void bulk(byte[] data) {
        Context ctx = initUsb();
        Device device = findDevice(ctx, (short) 0x314, (short)0xE457);
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to open USB device", result);
        result = LibUsb.claimInterface(handle, 0);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to claim USB interface", result);
        
        try {
        	IntBuffer transfered = IntBuffer.allocate(1);
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
            buffer.put(data);
            // Use device handle here
            int res = LibUsb.bulkTransfer(handle, (byte) 0x01, buffer, transfered, 4000);
            if (res != LibUsb.SUCCESS) throw new LibUsbException("Control transfer failed", res);
            if( transfered.get() != data.length ) {
                LOG.error("unexpected length returned on bulk: {}", transfered.get());
            }

        } finally {
        	LibUsb.releaseInterface(handle, 0);
            LibUsb.close(handle);
            LibUsb.exit(ctx);
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

    private Context initUsb() {
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
