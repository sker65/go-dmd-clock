package com.rinke.solutions.pinball.view.handler;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AniActionHandler;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.view.swt.RecentMenuManager;

@Slf4j
@Bean
public class AnimationHandler extends ViewHandler {

	@Autowired
	Shell shell;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	FileChooserUtil fileChooserUtil;
	@Autowired
	FileHelper fileHelper;
	@Autowired
	RecentMenuManager recentAnimationsMenuManager;
	@Autowired
	AniActionHandler aniActionHandler;
	
	public AnimationHandler(ViewModel vm, Model model, CmdDispatcher dispatcher) {
		super(vm, model, dispatcher);
	}
	
	public void onPlayingAniChanged(Animation ov, Animation nv) {
		vm.setSaveAniEnabled(nv!=null);
		vm.setExportGifEnabled(nv!=null);
	}
	
	public void onSaveSingleAniWithFC(int version) {
		if( vm.selectedScene==null && vm.selectedRecording==null ) {
			messageUtil.warn("no scene or recording selected", "you must select a scene or recording first");
		} else {
			Animation ani = vm.selectedRecording != null ? vm.selectedRecording : vm.selectedScene;
			String filename = fileChooserUtil.choose(SWT.SAVE, ani.getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
			if (filename != null) {
				log.info("store animation to {}", filename);
				storeAnimations(Lists.newArrayList(ani), filename, version, true);
			}
		} 
	}
	
	public void onLoadAni( String filename, boolean append, boolean populateModel ) {
		aniActionHandler.loadAni(filename, append, populateModel);
	}
	
	public void onSaveAniWithFC(int version) {
		String defaultName = vm.selectedRecording != null ? vm.selectedRecording.getDesc() : "animation";
		String filename = fileChooserUtil.choose(SWT.SAVE, defaultName, new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(model.recordings.values(), filename, version, true);
		}
	}
	
	protected Progress getProgress() {
		return new Progress(shell);
	}

	public void storeAnimations(Collection<Animation> anis, String filename, int version, boolean saveAll) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a -> saveAll || a.isDirty()).collect(Collectors.toList());
		if( anisToSave.isEmpty() ) return;
		Progress progress = getProgress();
		AniWriter aniWriter = new AniWriter(anisToSave, filename, version, vm.palettes, progress);
		if( progress != null ) 
			progress.open(aniWriter);
		else
			aniWriter.run();
		anisToSave.forEach(a->a.setDirty(false));
	}

}
