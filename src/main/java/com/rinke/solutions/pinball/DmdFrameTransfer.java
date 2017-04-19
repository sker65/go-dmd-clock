package com.rinke.solutions.pinball;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import com.rinke.solutions.pinball.model.Frame;

@Slf4j
public class DmdFrameTransfer extends ByteArrayTransfer {
	
	private static final String DMD_FRAME_TYPE = "DMD_FRAME_TYPE";
	private static final int DMD_FRAME_TYPE_ID = registerType(DMD_FRAME_TYPE);
	private static DmdFrameTransfer _instance = new DmdFrameTransfer();
	
	private DmdFrameTransfer() {}
	
	public static DmdFrameTransfer getInstance () {
		return _instance;
 	}
	 
	@Override
	protected int[] getTypeIds() {
		return new int[] {DMD_FRAME_TYPE_ID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] {DMD_FRAME_TYPE};
	}

	@Override
	protected void javaToNative(Object object, TransferData transferData) {
		if (object == null || !(object instanceof Frame)) return;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream writeOut = new DataOutputStream(out);
		Frame frame = (Frame) object;
		try {
			log.debug("writing frame {} to clipboard", frame);
			frame.writeTo(writeOut, false, 2);
		} catch (IOException e) {
			log.error("error writing frame to clipboard",e);
		}
		super.javaToNative(out.toByteArray(), transferData);
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		if (isSupportedType(transferData)) {
			byte[] buffer = (byte[])super.nativeToJava(transferData);
			if (buffer == null) return null;
			ByteArrayInputStream in = new ByteArrayInputStream(buffer);
			DataInputStream readIn = new DataInputStream(in);
			try {
				Frame frame = Frame.readFrom(readIn, false, 2);
				log.debug("reading frame {} from clipboard", frame);
				return frame;
			} catch (IOException e) {
				log.error("error reading frame from clipboard",e);
				return null;
			}
		}
		return null;
	}

}
