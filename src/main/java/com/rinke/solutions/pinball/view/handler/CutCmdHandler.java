package com.rinke.solutions.pinball.view.handler;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationQuantizer;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.ui.NamePrompt;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.view.View;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class CutCmdHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Value(defaultValue="4") 
	int noOfPlanesWhenCutting;
	
	@Value boolean addPalWhenCut;
	@Value boolean createBookmarkAfterCut;
	@Value boolean autoKeyframeWhenCut;
	
	@Autowired PaletteHandler paletteHandler;
	@Autowired BookmarkHandler bookmarkHandler;
	@Autowired KeyframeHandler keyframeHandler;
	@Autowired MessageUtil messageUtil;
	@Autowired View namePrompt;
	
	public CutCmdHandler(ViewModel vm) {
		super(vm);
	}
	
	public void onSelectedRecordingChanged(Animation o, Animation n) {
		vm.setMarkStartEnabled(n!=null||vm.selectedScene!=null);
	}

	public void onSelectedSceneChanged(Animation o, Animation n) {
		vm.setMarkStartEnabled(n!=null||vm.selectedRecording!=null);
	}
	
	public void onSelectedFrameChanged(int o, int n) {
		vm.setSceneCutEnabled(vm.cutInfo.canCut());
		vm.setMarkEndEnabled(vm.cutInfo.canMarkEnd(n));
	}

	Animation getSourceAnimation() {
		if( vm.selectedRecording != null ) return vm.selectedRecording;
		else if ( vm.selectedScene != null ) return vm.selectedScene;
		return null;
	}

	public void onMarkStart() {
		if( getSourceAnimation() != null ) {
			vm.cutInfo.setStart(getSourceAnimation().actFrame);
			vm.setSceneCutEnabled(vm.cutInfo.canCut());
			vm.setMarkEndEnabled(vm.cutInfo.canMarkEnd(getSourceAnimation().actFrame));
		}
	}

	public void onMarkEnd() {
		if( getSourceAnimation() != null ) {
			vm.cutInfo.setEnd(getSourceAnimation().actFrame);
			vm.setSceneCutEnabled(vm.cutInfo.canCut());
		}
	}

	public void onQuantizeScene() {
		CompiledAnimation src = vm.selectedScene;
		if( src != null ) {
			AnimationQuantizer quantizer = new AnimationQuantizer();
			String name = getUniqueName(src.getDesc()+"_q",vm.scenes.keySet());
			CompiledAnimation newScene = quantizer.quantize(name, src, vm.selectedPalette);
			newScene.setDesc(name);
			newScene.setPalIndex(vm.selectedPalette.index);
			newScene.setProjectAnimation(true);
			newScene.setEditMode(EditMode.REPLACE);
					
			vm.scenes.put(name, newScene);
			vm.scenes.refresh();

		}		
	}

	public void onConvertSceneToRGB() {
		CompiledAnimation src = vm.selectedScene;
		if( src != null ) {
			AnimationQuantizer quantizer = new AnimationQuantizer();
			String name = getUniqueName(src.getDesc()+"_rgb",vm.scenes.keySet());
			CompiledAnimation newScene = quantizer.convertSceneToRGB(name, src, vm.selectedPalette);
			newScene.setDesc(name);
			newScene.setProjectAnimation(true);
			newScene.setEditMode(EditMode.REPLACE);
					
			vm.scenes.put(name, newScene);
			vm.scenes.refresh();

		}		
	}
	
	public void onConvertAllScenesToRGB() {
		for(CompiledAnimation src : vm.scenes.values()) {
			if( src.getNumberOfPlanes() < 15 ) {
				log.info("converting {} {} to rgb", src.getDesc(), src.getNumberOfPlanes());
				AnimationQuantizer quantizer = new AnimationQuantizer();
				String name = src.getDesc();
				Palette palette = vm.paletteMap.get(src.getPalIndex());
				CompiledAnimation newScene = quantizer.convertSceneToRGB(name, src, palette);
				newScene.setDesc(name);
				newScene.setProjectAnimation(true);
				newScene.setEditMode(EditMode.REPLACE);
				vm.scenes.put(name, newScene);
			}
		}
		vm.scenes.refresh();
	}

	String getUniqueName(String name, Set<String> set) {
		String res = name;
		int i=0;
		while( set.contains(res) ) {
			res = name + "_" + i;
			i++;
		}
		return res;
	}

	public void onCutScene() {
		// respect number of planes while cutting / copying
		Animation src = getSourceAnimation();
		if( src != null ) {
			cutScene(src, vm.cutInfo.getStart(), vm.cutInfo.getEnd(), buildUniqueName(vm.scenes));
			log.info("cutting out scene from {}", vm.cutInfo);
			vm.cutInfo.reset();
			vm.setMarkStartEnabled(true);
		}
		
	}
	
	public void onSplitScene() {
		// respect number of planes while cutting / copying
		Animation src = getSourceAnimation();
		if( src != null ) {
			splitScene(src, 0);
			log.info("splitting scene {}", src.getDesc());
		}
		
	}
	
	/**
	 * creates a unique key name for scenes
	 * @param anis the map containing the keys
	 * @return the new unique name
	 */
	public <T extends Animation> String buildUniqueName(ObservableMap<String, T> anis) {
		return buildUniqueNameWithPrefix(anis,"Scene", anis.size());
	}
	
	public <T extends Animation> String buildUniqueNameWithPrefix(ObservableMap<String, T> anis, String prefix, int startIdx) {
		int no = startIdx;
		String name = prefix + " " + no;
		while( anis.containsKey(name)) {
			no++;
			name = prefix+ " " + no;
		}
		return name;
	}
	
	public Animation cutScene(Animation animation, int start, int end, String name) {
		CompiledAnimation cutScene = animation.cutScene(start, end, noOfPlanesWhenCutting);
		do {
			NamePrompt namePrompt = (NamePrompt) this.namePrompt;
			namePrompt.setItemName("Scene");
			namePrompt.setPrompt(name);
			namePrompt.open();
			if( namePrompt.isOkay() ) name = namePrompt.getPrompt();
			else return null;
			
			if( vm.scenes.containsKey(name) ) {
				messageUtil.error("Scene Name exists", "A scene '"+name+"' already exists");
			}
		} while(vm.scenes.containsKey(name));

		if( addPalWhenCut )
			paletteHandler.copyPalettePlaneUpgrade(name);
		
		cutScene.setDesc(name);
		cutScene.setPalIndex(vm.selectedPalette.index);
		cutScene.setProjectAnimation(true);
		cutScene.setEditMode(EditMode.COLMASK);
		cutScene.setRecordingLink(new RecordingLink(animation.getDesc(), start));
				
		vm.scenes.put(name, cutScene);
		vm.scenes.refresh();
		
		if( createBookmarkAfterCut )
			bookmarkHandler.addBookmark(animation, name, start);
		
		vm.setSelectedFrameSeq(cutScene);

		if( autoKeyframeWhenCut ) {
			if( vm.selectedRecording!=null ) keyframeHandler.onAddKeyframe(SwitchMode.REPLACE);
		}

		vm.setSelectedScene(cutScene);

		return cutScene;
	}
	
	public Animation splitScene(Animation animation, int splitSize) {
		CompiledAnimation splitScene = null;
		String namePrefix = animation.getDesc();
		do {
			NamePrompt namePrompt = (NamePrompt) this.namePrompt;
			namePrompt.setItemName("Scene");
			namePrompt.setPrompt(namePrefix);
			namePrompt.open();
			if( namePrompt.isOkay() ) namePrefix = namePrompt.getPrompt();
			else return null;
			
			if( vm.scenes.containsKey(namePrefix+" 1") ) {
				messageUtil.error("Scene Name exists", "A scene '"+namePrefix+" 1"+"' already exists");
			}
		} while(vm.scenes.containsKey(namePrefix+" 1"));
		int start = 0;
		int end = 0;
		do {
			end = start + splitSize - 1;
			if (end > animation.end)
				end = animation.end;
			String name = buildUniqueNameWithPrefix(vm.scenes,namePrefix, 1);
			splitScene = animation.cutScene(start, end, noOfPlanesWhenCutting);
			
			splitScene.setDesc(name);
			splitScene.setPalIndex(vm.selectedPalette.index);
			splitScene.setProjectAnimation(true);
			splitScene.setEditMode(EditMode.COLMASK);
					
			vm.scenes.put(name, splitScene);
			start += splitSize;
		} while (end < animation.end);

		vm.scenes.refresh();

		vm.setSelectedFrameSeq(splitScene);

		vm.setSelectedScene(splitScene);
			
		return splitScene;
	}


}
