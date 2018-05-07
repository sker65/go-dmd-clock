package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
public class AbstractListCmdHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired
	protected AnimationHandler animationHandler;

	public AbstractListCmdHandler(ViewModel vm) {
		super(vm);
	}

	<T extends Animation> void updateAnimationMapKey(String oldKey, String newKey, ObservableMap<String, T> anis) {
		if (!oldKey.equals(newKey)) {
			T ani = anis.remove(oldKey);
			if( ani != null ) anis.put(newKey, ani);
		}
	}

	public <T extends Animation> void onRemove(Animation a, ObservableMap<String, T> map) {
		if (a != null) {
			String key = a.getDesc();
			if( a.isProjectAnimation() ) vm.setDirty(true);
			map.remove(key);
			vm.playingAnis.clear();
			animationHandler.setAnimations(vm.playingAnis);
			animationHandler.setClockActive(true);
			vm.setDirty(true);
		}
	}

	protected <T extends Animation> void onSortAnimations(ObservableMap<String, T> map) {
		ArrayList<Entry<String, T>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new Comparator<Entry<String, T>>() {
	
			@Override
			public int compare(Entry<String, T> o1, Entry<String, T> o2) {
				return o1.getValue().getDesc().compareTo(o2.getValue().getDesc());
			}
		});
		map.clear();
		for (Entry<String, T> entry : list) {
			map.put(entry.getKey(), (T)entry.getValue());
		}
	}
	
	public void setEnableHashButtons(boolean enabled) {
		boolean[] sel = Arrays.copyOf(vm.hashButtonSelected, vm.hashButtonSelected.length);
		if( !enabled )  {
			for (int i = 0; i < vm.hashButtonSelected.length; i++) {
				sel[i] = false;
			}
			vm.setHashButtonSelected(sel);
		}
		vm.setHashButtonsEnabled(enabled);
	}
	
	public void setPlayingAni(Animation ani, int pos) {
		log.debug("set playing ani {}, {}", pos, ani);
		vm.playingAnis.clear();
		vm.playingAnis.add(ani);
		animationHandler.setAnimations(vm.playingAnis);
		animationHandler.setPos(pos);
		vm.setSelectedFrame(pos);
	}

	public void setAnimationHandler(AnimationHandler animationHandler) {
		 this.animationHandler = animationHandler;
	}

}
