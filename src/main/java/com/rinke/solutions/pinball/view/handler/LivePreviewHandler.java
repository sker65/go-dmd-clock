package com.rinke.solutions.pinball.view.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.DeviceMode;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.UsbCmd;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class LivePreviewHandler {
	
	public final ViewModel vm;
	public final Model model;
	public final CmdDispatcher dispatcher;
	
	@Autowired
	Pin2DmdConnector connector;
	
	@Value(key=Config.PIN2DMD_ADRESS)
	private String pin2dmdAdress;
	
	private ConnectionHandle handle;
	
	@Autowired
	private MessageUtil messageUtil;
	
	@Autowired
	ExportHandler exportHandler;

	public LivePreviewHandler(ViewModel vm, Model model, CmdDispatcher dispatcher) {
		super();
		this.vm = vm;
		this.model = model;
		this.dispatcher = dispatcher;
	}

	public void onUploadProject() {
		Map<String, ByteArrayOutputStream> captureOutput = new HashMap<>();
		exportHandler.exportProject("a.dat", f -> {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			captureOutput.put(f, stream);
			return stream;
		}, true);

		connector.transferFile("pin2dmd.pal", new ByteArrayInputStream(captureOutput.get("a.dat").toByteArray()));
		if (captureOutput.containsKey("a.fsq")) {
			connector.transferFile("pin2dmd.fsq", new ByteArrayInputStream(captureOutput.get("a.fsq").toByteArray()));
		}
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
		}
		connector.sendCmd(UsbCmd.RESET);
	}
	
	public void onUploadPalette(Palette palette) {
		if(vm.isLivePreview() && connector != null) {
			connector.upload(palette);
		}
	}
	
	public void onLivePreviewChanged(boolean ov, boolean nv) {
		if (nv) {
			try {
				connector.switchToMode(DeviceMode.PinMame_RGB.ordinal(), null);
				handle = connector.connect(pin2dmdAdress);
				for( Palette pal : model.palettes ) {
					connector.upload(pal,handle);
				}
				// upload actual palette
				connector.switchToPal(vm.selectedPalette.index, handle);
			} catch (RuntimeException ex) {
				messageUtil.warn("usb problem", "Message was: " + ex.getMessage());
				vm.setLivePreview(false);
			}
		} else {
			if (handle != null) {
				try {
					connector.release(handle);
				} catch (RuntimeException ex) {
					messageUtil.warn("usb problem", "Message was: " + ex.getMessage());
				}
				handle = null;
			}
		}
	}


}
