package com.rinke.solutions.pinball;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.FileChooserUtil;

@Slf4j
public class AnimationActionHandler {
	
	PinDmdEditor editor;
	FileChooserUtil fileChooserUtil;
	private Shell shell;
	
	public AnimationActionHandler(PinDmdEditor pinDmdEditor, Shell shell) {
		editor = pinDmdEditor;
		this.shell = shell;
		fileChooserUtil = new FileChooserUtil(shell);
	}
	
	protected Progress getProgress() {
		return new Progress(shell);
	}

	public void onSaveAniWithFC(int version) {
		String defaultName = editor.selectedAnimation.isPresent() ? editor.selectedAnimation.get().getDesc() : "animation";
		String filename = fileChooserUtil.choose(SWT.SAVE, defaultName, new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(editor.animations.values(), filename, version, true);
		}
	}

	public Pair<Integer,Map<String,Integer>> storeAnimations(Collection<Animation> anis, String filename, int version, boolean saveAll) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a -> saveAll || a.isDirty()).collect(Collectors.toList());
		if( anisToSave.isEmpty() ) return Pair.of(0, Collections.emptyMap());
		Progress progress = getProgress();
		AniWriter aniWriter = new AniWriter(anisToSave, filename, version, editor.project.palettes, progress);
		if( progress != null ) 
			progress.open(aniWriter);
		else
			aniWriter.run();
		anisToSave.forEach(a->a.setDirty(false));
		return Pair.of(anisToSave.size(), aniWriter.getOffsetMap());
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
				editor.project.inputFiles.clear();
			if (!editor.project.inputFiles.contains(filename))
				editor.project.inputFiles.add(filename);
		}

		// animationHandler.setAnimations(sourceAnis);
		if (!append) {
			editor.animations.clear();
			editor.playingAnis.clear();
		}
		for (Animation ani : loadedList) {
			if (editor.animations.containsKey(ani.getDesc())) {
				int i = 0;
				String desc = ani.getDesc();
				while (i < 1000) {
					String newDesc = desc + "-" + i;
					if (!editor.animations.containsKey(newDesc)) {
						ani.setDesc(newDesc);
						break;
					}
					i++;
				}
			}
			editor.animations.put(ani.getDesc(), ani);

			populatePalette(ani, editor.project.palettes);
		}
		editor.recentAnimationsMenuManager.populateRecent(filename);
		editor.project.dirty = true;
		return loadedList;
	}

	private void populatePalette(Animation ani, List<Palette> palettes) {
		if (ani.getAniColors() != null) {
			// if loaded colors with animations propagate as palette
			boolean colorsMatch = false;
			for (Palette p : palettes) {
				if (p.sameColors(ani.getAniColors())) {
					colorsMatch = true;
					ani.setPalIndex(p.index);
					break;
				}
			}
			if (!colorsMatch) {
				Palette aniPalette = new Palette(ani.getAniColors(), palettes.size(), ani.getDesc());
				palettes.add(aniPalette);
			}
		}
	}

	public void onSaveSingleAniWithFC(int version) {
		String filename = fileChooserUtil.choose(SWT.SAVE, editor.selectedAnimation.get().getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(Lists.newArrayList(editor.selectedAnimation.get()), filename, version, true);
		}
	}

}
