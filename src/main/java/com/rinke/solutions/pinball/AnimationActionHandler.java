package com.rinke.solutions.pinball;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.databinding.Command;
import com.rinke.solutions.pinball.animation.AniReader;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.ProgressEventListener;
import com.rinke.solutions.pinball.model.Model;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.ui.IProgress;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.handler.AbstractCommandHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;
import org.apache.commons.io.FilenameUtils;

@Slf4j
@Bean
public class AnimationActionHandler extends AbstractCommandHandler {
	
	@Autowired MessageUtil messageUtil;
	@Autowired FileChooserUtil fileChooserUtil;
	
	@Setter private Shell shell;
	private AniReader reader;
		
	public AnimationActionHandler(ViewModel vm) {
		super(vm);
	}
	
	protected IProgress getProgress() {
		return shell!=null ? new Progress(shell) : new Progress(Display.getCurrent().getActiveShell());
	}

	/**
	 * called from menu directly via action dispatcher. actually version=1 is fixed.
	 * @param version
	 */
	public void onSaveAniWithFC(int version) {
		String defaultName = vm.selectedRecording!=null ? vm.selectedRecording.getDesc() : "animation";
		String filename = fileChooserUtil.choose(SWT.SAVE, defaultName, new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(vm.recordings.values(), filename, version, true, true);
		}
	}

	public void storeAnimations(Collection<Animation> anis, String filename, int version, boolean saveAll, boolean withProgress) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a -> saveAll || a.isDirty()).collect(Collectors.toList());
		if( anisToSave.isEmpty() ) return;// Pair.of(0, Collections.emptyMap());
		IProgress progress = null;
		if (withProgress)
			progress =  getProgress();
		AniWriter aniWriter = new AniWriter(anisToSave, filename, version, vm.paletteMap, progress);
		if( progress != null ) {
			progress.open(aniWriter);
			if( aniWriter.hasError() ) {
				messageUtil.warn(SWT.ICON_ERROR | SWT.OK ,
						"Error writing animation project",
						"the following error occured while writing: " + aniWriter.getRuntimeException().getMessage() + "\n");
			}

		} else {
			aniWriter.run();
			if( aniWriter.hasError() ) {
				throw aniWriter.getRuntimeException();
			}
		}
		anisToSave.forEach(a->a.setDirty(false));
	}
	
	@Command
	public void onLoadAnimation(String filename, boolean append, boolean populate) {
		loadAni(filename, append, true, null);
	}

	public void onLoadAniWithFC(boolean append, boolean wantScene) {
		List<String> filenames = fileChooserUtil.chooseMulti(SWT.OPEN|SWT.MULTI, null, new String[] { "*.properties;*.ani;*.txt.gz;*.pcap;*.pcap.gz;*.*" }, new String[] { "Animationen",
				"properties, txt.gz, ani, mov" });

		for(String filename : filenames) {
			loadAni(filename, append, true, null, wantScene);
		}
	}

	boolean extensionIs(String name, String... args) {
		for (String ext : args) {
			if (name.endsWith(ext))
				return true;
		}
		return false;
	}
	
	public java.util.List<Animation> loadAni(String filename, boolean append, boolean populateProject, ProgressEventListener listener ) {
		return loadAni(filename,append,populateProject,listener,false);
	}
	
	public java.util.List<Animation> loadAni(String filename, boolean append, boolean populateProject, ProgressEventListener listener, boolean wantScene) {
		java.util.List<Animation> loadedList = new ArrayList<>();
		try {
		if (filename.endsWith(".ani")) {
			this.reader = new AniReader();
			loadedList.addAll(reader.read(filename));
		} else if (filename.endsWith(".txt.gz")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.MAME));
		} else if (filename.endsWith(".properties")) {
			loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
		} else if (extensionIs(filename, ".pcap", ".pcap.gz")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.PCAP));
		} else if (extensionIs(filename, ".rgb", ".rgb.gz")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.RGB));
		} else if (extensionIs(filename, ".raw", ".raw.gz")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.RAW));
		} else if (extensionIs(filename, ".dump", ".dump.gz")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.PINDUMP));
		} else if (extensionIs(filename, ".gif")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.GIF));
		} else if (extensionIs(filename, ".mp4", ".3gp", ".avi")) {
			loadedList.add(AnimationFactory.buildAnimationFromFile(filename, AnimationType.VIDEO));
		}
		log.info("loaded {} animations from {}", loadedList.size(), filename);
		} catch( IOException e) {
			log.error("error load anis from {}", filename, e);
		}
		
		if (populateProject && !wantScene) {
			if (!append)
				vm.inputFiles.clear();
			String baseFilename = FilenameUtils.getBaseName(filename)
	                + "." + FilenameUtils.getExtension(filename);
			//if (!vm.inputFiles.contains(baseFilename))
				vm.inputFiles.add(baseFilename);
		}

		// animationHandler.setAnimations(sourceAnis);
		if (!append) {
			vm.recordings.clear();
			vm.scenes.clear();
			vm.playingAnis.clear();
		}
		DMD	dmd = new DMD(vm.srcDmdSize);
		for (Animation lani : loadedList) {
			Animation ani = lani;
			if( wantScene ) { // try to convert
				//ani.getAniColors().length;
				dmd = new DMD(vm.dmdSize);
				lani.init(dmd);
				if( lani.end == 0) lani.end = lani.getRenderer().getFrames().size()-1;
				int noPlanes = lani.getRenderer().getNumberOfPlanes();
				ani = lani.cutScene(ani.start, ani.end, noPlanes);
				ani.setAniColors(lani.getAniColors());
				ani.setDesc(lani.getDesc());
				populatePalette(ani, vm.paletteMap);
			}
			if( ani instanceof CompiledAnimation ) {
				CompiledAnimation cani = (CompiledAnimation)ani;
				vm.inputFiles.remove(filename);
				ani.setProjectAnimation(true);
				if( EditMode.FIXED.equals(ani.getEditMode())) {
					ani.setEditMode(EditMode.REPLACE);
					ani.setMutable(true);
				}
				int planeSize = cani.frames.get(0).getPlane(0).length;
				if( planeSize != 512 && ani.width == 128 ) {
					// adjust with / height for version 1
					if (planeSize == 2048)
						ani.width = 256; ani.height = 64;
					if( planeSize == 1536 ) {
						ani.width = 192; ani.height = 64;
					} else if( planeSize == 256 ) {
						ani.width = 128; ani.height = 16;
					}
					log.info("ani size was adjusted: {}", ani);
				}
//				if( ani.width != vm.dmdSize.width || ani.height != vm.dmdSize.height) {
//					int r = messageUtil.warn(SWT.OK | SWT.CANCEL, "Size mismatch", "size of animation does not match to project dmd size");
//					if( r == SWT.CANCEL ) break;
//				} else {
					if (vm.numberOfColors == 64 && cani.frames.get(0).planes.size() < 6) {
						for( Frame inFrame : cani.frames ) {
							while(inFrame.planes.size() < 6)
								inFrame.planes.add(new Plane((byte)inFrame.planes.size(),new byte[planeSize]));
						}
					}
					populateAni(cani, vm.scenes);
//				}
			} else {
				if (!vm.has4PlanesRecording || vm.prjDmdSize.equals(vm.srcDmdSize)) {
					lani.init(dmd);
					if( lani.end == 0) lani.end = lani.getRenderer().getFrames().size()-1;
					int noPlanes = lani.getRenderer().getNumberOfPlanes();
					int planeSize = 0;
//					if (lani.actFrame != 0)
					if (lani.getType()!=AnimationType.RAW)
						planeSize = lani.getRenderer().getFrames().get(0).getPlane(0).length;
					else
						planeSize = lani.width*lani.height/8;
					if (planeSize == 2048) {
						lani.width = 256;
						lani.height = 64;
					}
					if (planeSize == 512) {
						lani.width = 128;
						lani.height = 32;
						vm.setSrcDmdSize(DmdSize.fromWidthHeight(128, 32));
					}	
					if (noPlanes == 4) 
						vm.has4PlanesRecording = true;
				}
				populateAni(ani, vm.recordings);
			}	
			
			ani.setProgressEventListener(listener);
			// should be done in background thread
			ani.init(dmd);
			
			populatePalette(ani, vm.paletteMap);
		}
		vm.setRecentAnimations(filename);
		vm.setDirty(true);
		return loadedList;
	}

	<T extends Animation> void populateAni( T ani, Map<String, T> anis) {
		if (anis.containsKey(ani.getDesc())) {
			int i = 0;
			String desc = ani.getDesc();
			while (i < 1000) {
				String newDesc = desc + "-" + i;
				if (!anis.containsKey(newDesc)) {
					ani.setDesc(newDesc);
					break;
				}
				i++;
			}
		}
		anis.put(ani.getDesc(), ani);
	}

	void populatePalette(Animation ani, Map<Integer,Palette> palettes) {
		if (ani.getAniColors() != null) {
			// sanitize number of color per palette when importing
			RGB[] aniColors = getSaveSizeRGBArray(ani.getAniColors());
			
			if (aniColors.length < vm.numberOfColors) {
				RGB rgb[] = new RGB[vm.numberOfColors];
				int k = 0; 
				while (k < vm.numberOfColors) {
					for( int j = 0; j<aniColors.length; j++) {
						rgb[k++] = aniColors[j];
						if (k == vm.numberOfColors)
							break;
					}
				}
				aniColors = rgb;
				ani.setAniColors(rgb);
			}
			// if loaded colors with animations propagate as palette
			boolean colorsMatch = false;
			int aniPalIndex = ani.getPalIndex();
			for (Palette p : palettes.values()) {
				if (p.sameColors(aniColors)) {
					colorsMatch = true;
					if (aniPalIndex != p.index)
						messageUtil.warn("Warning ! Duplicate Palette","Palette with colors in scene \"" + ani.getDesc() + "\" already exists. Setting scene palette to first occurance with same colors");
					ani.setPalIndex(p.index);
					break;
				}
			}
			if (!colorsMatch) {
				Palette aniPalette = new Palette(aniColors, palettes.size(), ani.getDesc());
				palettes.put(aniPalette.index,aniPalette);
				ani.setPalIndex(aniPalette.index);
			}
		}
	}

	/**
	 * create a "save" size for palette (actually this means always 16 colors, because firmware cant handle other) 
	 * @param aniColors imput array
	 * @return rgb array with at least 16 colors
	 */
	RGB[] getSaveSizeRGBArray(RGB[] aniColors) {
		if( aniColors.length == 16 ) return aniColors;
		RGB[] res = Arrays.copyOf(aniColors, aniColors.length < 16 ? 16 : aniColors.length);
		for(int i = 0; i < res.length; i++) {
			if( res[i] == null ) res[i] = new RGB(0, 0, 0); 		// fill with black
		}
		return res;
	}

	/**
	 * called from menu directly via action dispatcher. actually version=1 is fixed.
	 * @param version
	 */
	public void onSaveSingleAniWithFC(int version) {
		if( vm.selectedScene!=null ) {
			String filename = fileChooserUtil.choose(SWT.SAVE, vm.selectedScene.getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
			if (filename != null) {
				log.info("store animation to {}", filename);
				storeAnimations(Lists.newArrayList(vm.selectedScene), filename, version, true, true);
			}
		}
	}

	public int getVersionOfLastLoad() {
		return reader.version;
	}

}
