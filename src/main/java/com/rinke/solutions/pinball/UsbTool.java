package com.rinke.solutions.pinball;

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

    
    public void bulk(byte[] data) {
        Context ctx = initUsb();
        Device device = findDevice((short) 0x314, (short)0xE457);
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to open USB device", result);
        try {
            IntBuffer intBuffer = IntBuffer.allocate(1);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            if( intBuffer.array()[0] != data.length ) {
                LOG.error("unexpected length returned on bulk: {}", intBuffer.array()[0]);
            }
            // Use device handle here
            LibUsb.bulkTransfer(handle, (byte) 0, buffer, intBuffer, 4000);
        } finally {
            LibUsb.close(handle);
            LibUsb.exit(ctx);
        }
    }
    
    private byte[] fromMapping(PalMapping palMapping) {
        byte[] res = new byte[24];
        res[0] = (byte)0x81;
        res[1] = (byte)0xc3;
        res[2] = (byte)0xe8;
        res[3] = (byte)0xFF; // do config
        res[4] = (byte)0x05; // upload mapping
        int j = 5;
        for(int i = 0; i < palMapping.digest.length; i++)
            res[j++] = palMapping.digest[i];
        res[j++] = (byte) palMapping.palIndex;
        res[j++] = (byte) (palMapping.durationInFrames / 256);
        res[j++] = (byte) (palMapping.durationInFrames & 0xFF);
        return res;
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
