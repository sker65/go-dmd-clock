package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationCompiler;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.FileChooserUtil;

@Slf4j
public class AnimationActionHandler {
	
	PinDmdEditor editor;
	FileChooserUtil fileChooserUtil;
	
	public AnimationActionHandler(PinDmdEditor pinDmdEditor, Shell shell) {
		editor = pinDmdEditor;
		fileChooserUtil = new FileChooserUtil(shell);
	}

	public void saveAniWithFC(int version) {
		String filename = fileChooserUtil.choose(SWT.SAVE, editor.selectedAnimation.get().getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(editor.animations.values(), filename, version);
		}
	}

	public int storeAnimations(Collection<Animation> anis, String filename, int version) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a -> a.isMutable()).collect(Collectors.toList());
		AnimationCompiler animationCompiler = new AnimationCompiler();
		animationCompiler.writeToCompiledFile(anisToSave, filename, version, editor.project.palettes);
		return anisToSave.size();
	}

	protected void loadAniWithFC(boolean append) {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.properties;*.ani;*.txt.gz;*.pcap;*.pcap.gz;*.*" }, new String[] { "Animationen",
				"properties, txt.gz, ani, mov" });

		if (filename != null) {
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

	public void loadAni(String filename, boolean append, boolean populateProject) {
		AnimationCompiler compiler = new AnimationCompiler();
		java.util.List<Animation> loadedList = new ArrayList<>();
		if (filename.endsWith(".ani")) {
			loadedList.addAll(compiler.readFromCompiledFile(filename));
		} else if (filename.endsWith(".txt.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.MAME));
		} else if (filename.endsWith(".properties")) {
			loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
		} else if (extensionIs(filename, ".pcap", ".pcap.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PCAP));
		} else if (extensionIs(filename, ".dump", ".dump.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PINDUMP));
		} else if (extensionIs(filename, ".mp4", ".3gp", ".avi")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.VIDEO));
		}
		log.info("loaded {} animations from {}", loadedList.size(), filename);

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

			if (ani.getAniColors() != null) {
				// if loaded colors with animations propagate as palette
				boolean colorsMatch = false;
				for (Palette p : editor.project.palettes) {
					if (p.sameColors(ani.getAniColors())) {
						colorsMatch = true;
						break;
					}
				}
				if (!colorsMatch) {
					Palette aniPalette = new Palette(ani.getAniColors(), editor.project.palettes.size(), ani.getDesc());
					editor.project.palettes.add(aniPalette);
				}
			}
		}
		editor.recentAnimationsMenuManager.populateRecent(filename);
		editor.project.dirty = true;
	}

	public void saveSingleAniWithFC(int version) {
		String filename = fileChooserUtil.choose(SWT.SAVE, editor.selectedAnimation.get().getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(Lists.newArrayList(editor.selectedAnimation.get()), filename, version);
		}
	}

}
