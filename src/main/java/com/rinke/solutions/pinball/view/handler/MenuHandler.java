package com.rinke.solutions.pinball.view.handler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.PinDumpWriter;
import com.rinke.solutions.pinball.api.BinaryExporter;
import com.rinke.solutions.pinball.api.BinaryExporterFactory;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.ui.ConfigDialog;
import com.rinke.solutions.pinball.ui.ExportGoDmd;
import com.rinke.solutions.pinball.ui.GifExporter;
import com.rinke.solutions.pinball.ui.IProgress;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.View;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class MenuHandler extends AbstractCommandHandler implements ViewBindingHandler {
	
	@Autowired
	private View about;
	@Autowired
	private View deviceConfig;
	@Autowired
	private GifExporter gifExporter;
	@Autowired
	private ExportGoDmd exportGoDdmd;
	
	@Autowired
	private ConfigDialog configDialog;
	@Autowired Config config;
	
	@Autowired
	private MessageUtil messageUtil;
	
	@Autowired
	private AnimationHandler animationHandler;
	
	@Autowired FileChooserUtil fileChooserUtil;

	@Value
	private boolean nodirty; // if set ignore dirty check

	public MenuHandler(ViewModel vm) {
		super(vm);
	}

	public void onExportGif() {
		if(vm.selectedScene == null)
			return;
		Animation ani = vm.playingAnis.get(0);
		if( ani != null ) {
			gifExporter.setAni(ani);
			gifExporter.setPalette(vm.selectedPalette);
			gifExporter.open();
		}
	}
	
	public void onExportRaw() {
		if(vm.selectedScene == null)
			return;
		CompiledAnimation cani = vm.selectedScene;
		if( cani != null ) {
			int planes = messageUtil.warn(0,
					"RAW export",
					"Please select",
					"number of planes to export",
					new String[]{"", "2", "4"}, 2) * 2;
			String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.raw" }, new String[] { "RAW file"});
			int p = filename.lastIndexOf(".");
			if (p != -1)
				filename = filename.substring(0, p) + ".raw";
			else
				filename = filename + ".raw";
			IProgress progress = null;
			PinDumpWriter pinDumpWriter = new PinDumpWriter(cani, filename, planes, progress);
			pinDumpWriter.run();
		}
	}

	public void onExportGoDmd() {
		Pair<String,Integer> res = exportGoDdmd.open();
		if( res != null ) {
			exportForGoDMD( res.getLeft(), res.getRight() );
		}
	}

	void exportForGoDMD(String path, int version) {
		// version could be 1 or 4
		// 1. decide on what to store
		
	}

	/**
	 * called when dmd size has changed
	 * @param newSize
	 */
	public void onDmdSizeChanged(DmdSize old, DmdSize newSize) {
		// reallocate some objects
		byte[] emptyMask = new byte[newSize.planeSize];
		Arrays.fill(emptyMask, (byte) 0xFF);
		vm.setEmptyMask(emptyMask);
		// dmd, dmdWidget, previewWidget
		vm.dmd.setSize(newSize.width, newSize.height, true);
		vm.setDmdDirty(true);
	}

	public void onNewProject() {
		// init palette
		vm.paletteMap.clear();
		Palette.getDefaultPalettes(vm.numberOfColors).stream().forEach(p->vm.paletteMap.put(p.index, p));
		vm.setSelectedPalette(vm.paletteMap.get(0));

		// init masks
		vm.setDetectionMaskActive(false);
		vm.setLayerMaskActive(false);
		vm.masks.clear();
		for(int i = 0; i < vm.maxNumberOfMasks; i++) {
			vm.masks.add( new Mask(vm.dmdSize.planeSize) );
		}
		
		// clear lists
		vm.recordings.clear();
		vm.has4PlanesRecording = false;
		vm.scenes.clear();
		vm.inputFiles.clear();
		vm.keyframes.clear();
		
		vm.recordingNameMap.clear();
		
		vm.playingAnis.clear();
		vm.setSelectedRecording(null);
		vm.setSelectedScene(null);
		animationHandler.setAnimations(vm.playingAnis);
		vm.setProjectFilename(null);
	}

	public void onAbout() {
		log.info("onAbout");
		about.open();
	}
	
	public void onDeviceConfiguration() {
		log.info("onDeviceConfiguration");
		deviceConfig.open();
	}
	
	public void onConfiguration() {
		log.info("onConfiguration");
		configDialog.open();
		// TODO model class to decouple
		if( configDialog.okPressed ) {
			vm.setPin2dmdAdress(configDialog.getPin2DmdHost());
			// check changed size
			if( !vm.dmdSize.equals(configDialog.getDmdSize()) ) {
				vm.dmd = new DMD(configDialog.getDmdSize());
				if( vm.previewDMD != null ) vm.previewDMD = new DMD(configDialog.getDmdSize());
			}
			vm.setDmdSize(configDialog.getDmdSize());
			vm.setSrcDmdSize(configDialog.getDmdSize());
		}
	}
	
	public void onQuit() {
		log.info("onQuit");
		// UI should listen and close Shell
		vm.setShouldClose(true);
	}

	public void setAnimationHandler(AnimationHandler animationHandler) {
		 this.animationHandler = animationHandler;
	}
		
}