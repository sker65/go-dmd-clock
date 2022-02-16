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
		}, true, null);

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
		if (vm.livePreviewActive) {
			connector.sendFrame(frame);
		}
	}
	
	public void onPin2dmdAdressChanged(String oldAddr, String newAddr) {
		log.info("onPin2dmdAdressChanged {} -> {}", oldAddr, newAddr);
		if( newAddr != null ) {
			connector = ConnectorFactory.create(newAddr);
		}
	}
	
	public void onUploadPalette(Palette pal) {
		log.info("uploading palette: {}", pal.index);
		if( pal != null ) {
			connector.upload(pal);
		}
	}

	public void onLivePreviewActiveChanged(boolean old, boolean livePreviewIsOn) {
		if (livePreviewIsOn) {
			try {
				connector.switchToMode(DeviceMode.PinMame.ordinal());
				/*for( Palette pal : vm.paletteMap.values() ) {
					log.debug("uploading palette: {}", pal);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
					connector.upload(pal);
				}
				// upload actual palette
				connector.switchToPal(vm.selectedPalette.index);*/
				connector.setPal(vm.paletteMap.get(vm.selectedPalette.index));
				setEnableUsbTooling(!livePreviewIsOn);
			} catch (RuntimeException ex) {
				messageUtil.warn("usb problem", "Message was: " + ex.getMessage());
				// TODO veto v.btnLivePreview.setSelection(false);
			}
		} else {
			setEnableUsbTooling(!livePreviewIsOn);
		}

	}

	private void setEnableUsbTooling(boolean enabled) {
		vm.setMntmUploadPalettesEnabled(enabled);
		vm.setMntmUploadProjectEnabled(enabled);
	}
	
	public void onSelectedPaletteChanged(Palette o, Palette newPalette) {
		if (vm.livePreviewActive && connector != null) {
	//		connector.upload(vm.selectedPalette,handle);
	//		connector.switchToPal(newPalette.index);
			connector.setPal(vm.paletteMap.get(newPalette.index));
			connector.sendFrame(vm.dmd.getFrame());
		}
	}

	public void sendFrame(Frame frame) {
		if( connector != null )
			connector.sendFrame(frame);
	}

}
