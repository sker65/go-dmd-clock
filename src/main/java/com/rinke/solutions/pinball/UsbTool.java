package com.rinke.solutions.pinball;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import com.rinke.solutions.pinball.model.Palette;

public class UsbTool {
    
    private static Logger LOG = LoggerFactory.getLogger(UsbTool.class);

    public void upload(Palette palette) {
        Context ctx = initUsb();
        Device device = findDevice((short) 0x314, (short)0xE457);
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to open USB device", result);
        try {
            IntBuffer intBuffer = IntBuffer.allocate(1);
            byte[] bytes = fromPalette(palette);
            ByteBuffer buffer = ByteBuffer.wrap(bytes );
            // Use device handle here
            LibUsb.bulkTransfer(handle, (byte) 0, buffer, intBuffer, 4000);
        } finally {
            LibUsb.close(handle);
            LibUsb.exit(ctx);
        }
    }

    private byte[] fromPalette(Palette palette) {
        byte[] res = new byte[55];
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe8;
        res[3] = (byte)0xFF; // do config
        res[4] = (byte)0x04; // upload pal
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

    public Device findDevice(short vendorId, short productId) {
        // Read the USB device list
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(null, list);
        if (result < 0)
            throw new LibUsbException("Unable to get device list", result);

        try {
            // Iterate over all devices and scan for the right one
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS)
                    throw new LibUsbException("Unable to read device descriptor", result);
                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId)
                    return device;
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }

        // Device not found
        return null;
    }

}
