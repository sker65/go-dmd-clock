package com.rinke.solutions.pinball.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

/**
 * usb tool models the usb protocol to pin2dmd controller for various
 * transfers: config, license key, frames for live preview, palettes,
 * uploading files.
 * @author stefanri
 */
@Slf4j
public class UsbConnector extends Pin2DmdConnector {

	public UsbConnector(String address) {
		super(address);
		// TODO Auto-generated constructor stub
	}

	@Data
	public static class UsbHandle extends ConnectionHandle {
		private Context context;
		private DeviceHandle deviceHandle;
		public UsbHandle(Context context, DeviceHandle deviceHandle) {
			super();
			this.context = context;
			this.deviceHandle = deviceHandle;
		}
	}
	
    @Override
    public ConnectionHandle connect(String address) {
        Context ctx = initCtx();
        Device device = findDevice(ctx, (short) 0x314, (short)0xE457);
        if( device != null ) log.info("libusb device found for pin2dmd");
        else throw new LibUsbException("pin2dmd device not found",-1);
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to open USB device", result);
        result = LibUsb.claimInterface(handle, 0);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to claim USB interface", result);
        return new UsbHandle(ctx,handle);	
    }
    
    @Override
	public void release(ConnectionHandle h) {
		if( h != null ) {
			UsbHandle usb = (UsbHandle) h;
	    	LibUsb.releaseInterface(usb.getDeviceHandle(), 0);
	        LibUsb.close(usb.getDeviceHandle());
	        LibUsb.exit(usb.getContext());	
		}
	}
        
	@Override
    protected byte[] receive(ConnectionHandle h, int len) {
    	byte[] data = new byte[len];
		IntBuffer transfered = IntBuffer.allocate(1);
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
		buffer.put(data);
		// Use device handle here
		UsbHandle usb = (UsbHandle) h;
		int res = LibUsb.bulkTransfer(usb.getDeviceHandle(), (byte) 0x81, buffer, transfered, 4000);
		if (res != LibUsb.SUCCESS) throw new LibUsbException("Control transfer failed", res);
		int read = transfered.get();
		if( read != data.length ) {
		    log.error("unexpected length returned on bulk: {}", read);
		}
		buffer.get(data,0,len);
    	return data;
    }

    @Override
	protected void send(byte[] data, ConnectionHandle handle) {
		IntBuffer transfered = IntBuffer.allocate(1);
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
		buffer.put(data);
		UsbHandle usb = (UsbHandle) handle;
		// Use device handle here
		int res = LibUsb.bulkTransfer(usb.getDeviceHandle(), (byte) 0x01, buffer, transfered, 4000);
		if (res != LibUsb.SUCCESS) throw new LibUsbException("Control transfer failed", res);
		if( transfered.get() != data.length ) {
		    log.error("unexpected length returned on bulk: {}", transfered.get());
		}
	}
	
    private Context initCtx() {
        log.debug("init libusb");
        Context context = new Context();
        int result = LibUsb.init(context);
        log.debug("got context: {}",context);
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
                log.debug("scanning for device: "
                    +String.format("%04X", descriptor.idVendor())
                    +", "+String.format("%04X", descriptor.idProduct()));
                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
                    log.debug("found {}", device);
                	return device;
                }
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, false);
        }
        log.error("usb device not found");
        // Device not found
        return null;
    }

}
