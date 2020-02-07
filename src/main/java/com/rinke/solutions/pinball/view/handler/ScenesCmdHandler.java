package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.ui.EditLinkView;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class ScenesCmdHandler extends AbstractListCmdHandler implements ViewBindingHandler {

	@Autowired MessageUtil messageUtil;
	
	private List<EditMode> mutable = Arrays.asList( EditMode.REPLACE, EditMode.REPLACE_FOLLOW, EditMode.COLMASK, 
			EditMode.COLMASK_FOLLOW, EditMode.LAYEREDCOL, EditMode.LAYEREDREPLACE );

	@Autowired DrawCmdHandler drawCmdHandler;
	@Autowired MaskHandler maskHandler;
	
	@Autowired EditLinkView editLink;

	public ScenesCmdHandler(ViewModel vm) {
		super(vm);
	}
	
	public void onSelectedSceneChanged(CompiledAnimation o, CompiledAnimation nextScene) {
		log.info("onSceneSelectionChanged: {}", nextScene);
		Animation current = o;
		// detect changes
		if( current == null && nextScene == null ) return;
		if( nextScene != null && current != null && nextScene.getDesc().equals(current.getDesc())) return;
		
		if( current != null ) {
			vm.scenesPosMap.put(current.getDesc(), current.actFrame);
			current.commitDMDchanges(vm.dmd);
			vm.setDirty(vm.dirty | current.isDirty());
		}
		if( nextScene != null ) {
			// deselect recording
			vm.cutInfo.reset();
			vm.setSelection(null);
			vm.setSelectedRecording(null);
			vm.setPreviewDMD(null);
			vm.setSelectedKeyFrame(null);
			
		//	v.goDmdGroup.updateAnimation(nextScene);
			
			EditMode m = nextScene.getEditMode();
			vm.setDetectionMaskEnabled(m.enableDetectionMask);
			vm.setMaskSpinnerEnabled(m.enableDetectionMaskSpinner);
			vm.setLayerMaskEnabled(m.enableLayerMask);
			vm.setDetectionMaskActive(false);
			vm.setSelectedMaskNumber(0);
			vm.setLayerMaskActive(false);
			vm.setBtnLinkEnabled(m.haveLocalMask);
			if( nextScene.getRecordingLink() != null) {
				RecordingLink rl = nextScene.getRecordingLink();
				vm.setLinkVal(rl.associatedRecordingName+":"+rl.startFrame);
			} else {
				vm.setLinkVal("-");
			}
			
			// warum mask auch gleich active setzen
			// was formerly vm.setDetectionMaskActive(nextScene.getEditMode().useLocalMask);
			vm.setMaskSpinnerEnabled(m.enableLayerMask);

			// just to enasure a reasonable default
			if( nextScene.getEditMode() == null || nextScene.getEditMode().equals(EditMode.FIXED) ) {
				// old animation may be saved with wrong edit mode
				nextScene.setEditMode(EditMode.REPLACE);
			}
			if( current == null ) vm.availableEditModes.replaceAll(mutable);
			vm.setSuggestedEditMode(nextScene.getEditMode());
			vm.setSelectedEditMode(nextScene.getEditMode());
			
			setEnableHashButtons(m.enableDetectionMask);
			
			// we are in the change handler anyways
			// vm.setSelectedScene(nextScene);

			int numberOfPlanes = nextScene.getRenderer().getNumberOfPlanes();
			if( numberOfPlanes == 5) {
				numberOfPlanes = 4;
			}
			if (numberOfPlanes == 3) {
				numberOfPlanes = 2;
				// v.goDmdGroup.transitionCombo.select(1);
			} else {
				// v.goDmdGroup.transitionCombo.select(0);
			}

			vm.setSelectedPaletteByIndex(nextScene.getPalIndex());
			
			drawCmdHandler.setDrawMaskByEditMode(m);

			vm.dmd.setNumberOfPlanes(numberOfPlanes);
			vm.setPaletteToolPlanes(vm.layerMaskActive||vm.detectionMaskActive?1:numberOfPlanes);

			setPlayingAni(nextScene, vm.scenesPosMap.getOrDefault(nextScene.getDesc(), 0));
			maskHandler.updateDrawingEnabled();
			if( m.haveLocalMask ) {
				vm.setHashVal(HashCmdHandler.getPrintableHashes(nextScene.getActualFrame().crc32));
			} else {
				vm.setHashVal("");
			}
						
		} else {
			vm.setDrawingEnabled(false);
			vm.setBtnLinkEnabled(false);
			vm.setHashVal("");
		}
		// v.goDmdGroup.updateAniModel(nextScene);
		vm.setDeleteSceneEnabled(nextScene!=null);
		vm.setBtnSetScenePalEnabled(nextScene!=null);
	}
	
	public void onDeleteScene() {
		Animation a = vm.selectedScene;
		ArrayList<String> res = new ArrayList<>();
		if( a!=null) {
			for( PalMapping pm : vm.keyframes.values()) {
				if( a.getDesc().equals(pm.frameSeqName) ) {
					res.add( pm.name );
				}
			}
		}
		if( res.isEmpty() ) {
			onRemove(a, vm.scenes);
		} else {
			messageUtil.warn("Scene cannot be deleted", "It is used by "+res);
		}
	}

	public void onSortScenes() {
		onSortAnimations(vm.scenes);
	}
	
	// called when scene gets renamed
	void updateBookmarkNames(String old, String newName) {
		for( Set<Bookmark> bookmarks : vm.bookmarksMap.values()) {
			Iterator<Bookmark> i = bookmarks.iterator();
			while(i.hasNext() ) {
				Bookmark bm = i.next();
				if( bm.name.equals(old) ) {
					i.remove();
					bookmarks.add( new Bookmark(newName, bm.pos));
					break;
				}
			}
		}	
	}

	/**
	 * if a scene gets renamed, this update function is called.
	 * if newKey is not equal to old, the refering pal mappings gets updated
	 * @param oldKey old name of the scene
	 * @param newKey new name of scene
	 */
	private void updatePalMappingsSceneNames(String oldKey, String newKey) {
		if( StringUtils.equals(oldKey, newKey) ) return;
		vm.keyframes.values().forEach(p->{
			if( p.frameSeqName != null && p.frameSeqName.equals(oldKey)) {
				p.frameSeqName = newKey;
			}
		});
	}

	public void onSetScenePalette() {
		if (vm.selectedScene!=null && vm.selectedPalette != null) {
			CompiledAnimation scene = vm.selectedScene;
			scene.setPalIndex(vm.selectedPalette.index);
			log.info("change pal index in scene {} to {}", scene.getDesc(), vm.selectedPalette.index);
			for(PalMapping p : vm.keyframes.values()) {
				if( p.switchMode!=null && p.switchMode.hasSceneReference) {
					if(p.frameSeqName.equals(scene.getDesc())) {
						log.info("adjusting pal index for keyframe {} to {}", p, vm.selectedPalette.index);
						p.palIndex = vm.selectedPalette.index;
					}
				}
			}
		}
	}

	public void onRenameScene(String oldName, String newName){
		updateAnimationMapKey(oldName, newName, vm.scenes);
		updateBookmarkNames( oldName, newName );
		updatePalMappingsSceneNames(oldName, newName);
		vm.setDirty(true);
	}
	
	public void onEditLink() {
		editLink.setRecordings(vm.recordings);
		editLink.setSceneName(vm.selectedScene.getDesc());
		editLink.setRecordingLink(vm.selectedScene.getRecordingLink());
		editLink.open();
		if( editLink.okClicked() && vm.selectedScene != null) {
			vm.selectedScene.setRecordingLink(editLink.getRecordingLink());
		}
	}
	
	public void onUnlockSceneMasks() {
		if( vm.selectedScene != null ) {
			int res = messageUtil.warn(0, "Warning",
					"Unlock Scene Masks", 
					"Unlocking scene masks will allowed changes to the masks attached to the scene.\n"
							+ " But hashes for all frames will be deleted and must be set again.",
					new String[]{"", "Cancel", "Proceed"},2);
			if( res != 2 ) return;
			vm.selectedScene.getMasks().stream().forEach(m->m.locked=false);
			vm.selectedScene.frames.stream().forEach(f->f.crc32 = new byte[4]);
			vm.setHashVal("");
			vm.setDmdDirty(true);
		}
	}
}
