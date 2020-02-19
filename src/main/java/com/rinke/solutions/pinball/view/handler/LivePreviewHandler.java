package com.rinke.solutions.pinball.view.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DeviceMode;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.UsbCmd;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class LivePreviewHandler extends AbstractCommandHandler implements ViewBindingHandler {

	public LivePreviewHandler(ViewModel vm) {
		super(vm);
	}

	ConnectionHandle handle;
	Pin2DmdConnector connector;
	
	@Autowired MessageUtil messageUtil;
	@Autowired ProjectHandler projectHandler;
	
	public void init() {
		connector = ConnectorFactory.create(vm.pin2dmdAdress);
		connector.setDmdSize(vm.dmdSize);	
	}
	
	public void onDmdSizeChanged( DmdSize o, DmdSize newSize) {
		if( connector != null ) connector.setDmdSize(newSize);
	}
	
	public void onUploadProject() {
		Map<String, ByteArrayOutputStream> captureOutput = new HashMap<>();
		projectHandler.onExportProject("a.dat", f -> {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			captureOutput.put(f, stream);
			return stream;
		}, true);

		connector.transferFile("pin2dmd.pal", new ByteArrayInputStream(captureOutput.get("a.dat").toByteArray()));
		if (captureOutput.containsKey("a.fsq")) {
			connector.transferFile("pin2dmd.fsq", new ByteArrayInputStream(captureOutput.get("a.fsq").toByteArray()));
		}
		sleep(1500);
		connector.sendCmd(UsbCmd.RESET);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * cmd gets called from DMDWidget change listener
	 * @param frame
	 */
	public void onFrameChanged(Frame frame) {
		if (vm.livePreviewActive && handle != null) {
			connector.sendFrame(frame, handle);
		}
	}
	
	public void onPin2dmdAdressChanged(String oldAddr, String newAddr) {
		log.info("onPin2dmdAdressChanged {} -> {}", oldAddr, newAddr);
		if (handle != null) {
			connector.release(handle);
			connector = null;
		}
		if( newAddr != null ) {
			connector = ConnectorFactory.create(newAddr);
		}
	}
	
	public void onUploadPalette(Palette pal) {
		log.info("uploading palette: {}", pal.index);
		if( pal != null ) {
			if( handle != null) {
				connector.upload(pal,handle);
			} else {
				ConnectionHandle h = connector.connect(vm.pin2dmdAdress);
				connector.upload(pal,h);
				connector.release(handle);
			}
		}
	}

	public void onLivePreviewActiveChanged(boolean old, boolean livePreviewIsOn) {
		if (livePreviewIsOn) {
			try {
				connector.switchToMode(DeviceMode.PinMame.ordinal(), null);
				handle = connector.connect(vm.pin2dmdAdress);
				if (handle != null) {
					for( Palette pal : vm.paletteMap.values() ) {
						log.debug("uploading palette: {}", pal);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {}
						connector.upload(pal,handle);
					}
					// upload actual palette
					connector.switchToPal(vm.selectedPalette.index, handle);
					setEnableUsbTooling(!livePreviewIsOn);
				} else {
					messageUtil.warn("PIN2DMD device not found","Please check USB connection");
				}
			} catch (RuntimeException ex) {
				messageUtil.warn("usb problem", "Message was: " + ex.getMessage());
				// TODO veto v.btnLivePreview.setSelection(false);
			}
		} else {
			if (handle != null) {
				try {
					connector.release(handle);
					setEnableUsbTooling(!livePreviewIsOn);
				} catch (RuntimeException ex) {
					messageUtil.warn("usb problem", "Message was: " + ex.getMessage());
				}
				handle = null;
			}
		}

	}

	private void setEnableUsbTooling(boolean enabled) {
		vm.setMntmUploadPalettesEnabled(enabled);
		vm.setMntmUploadProjectEnabled(enabled);
	}
	
	public void onSelectedPaletteChanged(Palette o, Palette newPalette) {
		if (vm.livePreviewActive && handle != null) {
	//		connector.upload(vm.selectedPalette,handle);
			connector.switchToPal(newPalette.index, handle);
		}
	}

	public void sendFrame(Frame frame) {
		if( connector != null && handle != null) {
			connector.sendFrame(frame, handle);
		}
	}

}
