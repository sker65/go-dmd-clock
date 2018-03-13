package com.rinke.solutions.pinball;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Model;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
@Bean
public class AnimationActionHandler {
	
	@Autowired ViewModel vm;
	
	@Autowired MessageUtil messageUtil;
	@Autowired FileChooserUtil fileChooserUtil;
	@Setter private Shell shell;
		
	protected Progress getProgress() {
		return shell!=null ? new Progress(shell) : null;
	}

	public void onSaveAniWithFC(int version) {
		String defaultName = vm.selectedRecording!=null ? vm.selectedRecording.getDesc() : "animation";
		String filename = fileChooserUtil.choose(SWT.SAVE, defaultName, new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(vm.recordings.values(), filename, version, true);
		}
	}

	public void storeAnimations(Collection<Animation> anis, String filename, int version, boolean saveAll) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a -> saveAll || a.isDirty()).collect(Collectors.toList());
		if( anisToSave.isEmpty() ) return;// Pair.of(0, Collections.emptyMap());
		Progress progress = getProgress();
		AniWriter aniWriter = new AniWriter(anisToSave, filename, version, vm.paletteMap, progress);
		if( progress != null ) 
			progress.open(aniWriter);
		else
			aniWriter.run();
		anisToSave.forEach(a->a.setDirty(false));
	}

	protected void onLoadAniWithFC(boolean append) {
		List<String> filenames = fileChooserUtil.chooseMulti(SWT.OPEN|SWT.MULTI, null, new String[] { "*.properties;*.ani;*.txt.gz;*.pcap;*.pcap.gz;*.*" }, new String[] { "Animationen",
				"properties, txt.gz, ani, mov" });

		for(String filename : filenames) {
			loadAni(filename, append, true);
		}
	}

	boolean extensionIs(String name, String... args) {
		for (String ext : args) {
			if (name.endsWith(ext))
				return true;
		}
		return false;
	}

	public java.util.List<Animation> loadAni(String filename, boolean append, boolean populateProject) {
		java.util.List<Animation> loadedList = new ArrayList<>();
		try {
		if (filename.endsWith(".ani")) {
			loadedList.addAll(CompiledAnimation.read(filename));
		} else if (filename.endsWith(".txt.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.MAME));
		} else if (filename.endsWith(".properties")) {
			loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename,shell));
		} else if (extensionIs(filename, ".pcap", ".pcap.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PCAP));
		} else if (extensionIs(filename, ".dump", ".dump.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PINDUMP, shell));
		} else if (extensionIs(filename, ".gif")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.GIF));
		} else if (extensionIs(filename, ".mp4", ".3gp", ".avi")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.VIDEO, shell));
		}
		log.info("loaded {} animations from {}", loadedList.size(), filename);
		} catch( IOException e) {
			log.error("error load anis from {}", filename, e);
		}
		if (populateProject) {
			if (!append)
				vm.inputFiles.clear();
			if (!vm.inputFiles.contains(filename))
				vm.inputFiles.add(filename);
		}

		// animationHandler.setAnimations(sourceAnis);
		if (!append) {
			vm.recordings.clear();
			vm.scenes.clear();
			vm.playingAnis.clear();
		}
		DMD dmd = new DMD(vm.dmdSize.width,vm.dmdSize.height);
		for (Animation ani : loadedList) {
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
					if( planeSize == 1536 ) {
						ani.width = 192; ani.height = 64;
					} else if( planeSize == 256 ) {
						ani.width = 128; ani.height = 16;
					}
					log.info("ani size was adjusted: {}", ani);
				}
				if( ani.width != vm.dmdSize.width || ani.height != vm.dmdSize.height) {
					messageUtil.warn("Size mismatch", "size of animation does not match to project dmd size");
				} else {
					populateAni(cani, vm.scenes);
				}
			} else {
				populateAni(ani, vm.recordings);
			}	
			
			ani.init(dmd);
			populatePalette(ani, vm.paletteMap);
		}
		vm.setRecentAnimations(filename);
		vm.setDirty(true);
		return loadedList;
	}
	
	private <T extends Animation> void populateAni( T ani, Map<String, T> anis) {
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

	private void populatePalette(Animation ani, Map<Integer,Palette> palettes) {
		if (ani.getAniColors() != null) {
			// if loaded colors with animations propagate as palette
			boolean colorsMatch = false;
			for (Palette p : palettes.values()) {
				if (p.sameColors(ani.getAniColors())) {
					colorsMatch = true;
					ani.setPalIndex(p.index);
					break;
				}
			}
			if (!colorsMatch) {
				Palette aniPalette = new Palette(ani.getAniColors(), palettes.size(), ani.getDesc());
				palettes.put(aniPalette.index,aniPalette);
				ani.setPalIndex(aniPalette.index);
			}
		}
	}

	public void onSaveSingleAniWithFC(int version) {
		if( vm.selectedScene!=null ) {
			String filename = fileChooserUtil.choose(SWT.SAVE, vm.selectedScene.getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
			if (filename != null) {
				log.info("store animation to {}", filename);
				storeAnimations(Lists.newArrayList(vm.selectedScene), filename, version, true);
			}
		}
	}

}
