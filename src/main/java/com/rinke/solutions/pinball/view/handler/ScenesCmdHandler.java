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
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.FrameLink;
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
		vm.setLinkedFrameOffset(0);
		Animation current = o;
		// detect changes
		if( current == null && nextScene == null ) return;
		if( nextScene != null && current != null && nextScene.getDesc().equals(current.getDesc())) return;
		
		if( current != null ) {
			vm.scenesPosMap.put(current.getDesc(), current.actFrame);
			if ((vm.dmdSize.planeSize == current.width*current.height/8) && vm.detectionMaskActive == false)
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
			vm.setBtnLinkEnabled(true);
			vm.setBtnDelFrameEnabled(nextScene.frames.size()>1);
			if( nextScene.getRecordingLink() != null) {
				RecordingLink rl = nextScene.getRecordingLink();
				vm.setLinkVal(rl.associatedRecordingName+":"+rl.startFrame);
				vm.selectedLinkRecordingName = rl.associatedRecordingName;
			} else {
				vm.setLinkVal("-");
			}
			vm.setKeyFrame(nextScene.frames.get(0).keyFrame);
			
			// just to ensure a reasonable default
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

			vm.dmd.setSize(nextScene.width, nextScene.height);
			vm.dmd.setNumberOfPlanes(numberOfPlanes);
			vm.setDmdSize(DmdSize.fromWidthHeight(nextScene.width, nextScene.height));

			setPlayingAni(nextScene, vm.scenesPosMap.getOrDefault(nextScene.getDesc(), 0));
			maskHandler.updateDrawingEnabled();
			if( m.haveLocalMask || m.haveSceneDetectionMasks || vm.selectedEditMode.pullFrameDataFromAssociatedRecording) {
				maskHandler.updateMaskChange(false, true);
				vm.setHashVal(HashCmdHandler.getPrintableHashes(nextScene.getActualFrame().crc32));
			} else {
				vm.setHashVal("");
			}
			
			vm.setPaletteToolPlanes(vm.layerMaskActive||vm.detectionMaskActive?1:numberOfPlanes);
			
			if (vm.selectedEditMode.pullFrameDataFromAssociatedRecording && vm.selectedScene.getRecordingLink() == null) {
				messageUtil.warn("Warning", "Linked Recording missing !!\nCalculated hashes may be invalid.");
			}
						
		} else {
			vm.setDrawingEnabled(false);
			vm.setBtnLinkEnabled(false);
			vm.setHashVal("");
		}
		// v.goDmdGroup.updateAniModel(nextScene);
		vm.setDeleteSceneEnabled(nextScene!=null);
		vm.setBtnSetScenePalEnabled(nextScene!=null);
		animationHandler.forceRerender();
	}
	
	public void onDeleteUnusedScenes() {
		
		Iterator<CompiledAnimation> it = vm.scenes.values().iterator();
		while(it.hasNext()) {
			Animation a = it.next();
			ArrayList<String> res = new ArrayList<>();
			if( a!=null) {
				for( PalMapping pm : vm.keyframes.values()) {
					if( a.getDesc().equals(pm.frameSeqName) ) {
						res.add( pm.name );
					}
				}
				if( res.isEmpty() ) {
					String filename = null;
					int i = -1;
					filename = a.getName();
					i = vm.inputFiles.indexOf(filename);					
					it.remove();
					boolean nameExists = false;
					for (Animation r: vm.recordings.values()) {
						String name = r.getName();
						if (name.equals(filename))
							nameExists = true;
					}
					if ( nameExists != true && (i != -1)) {
						vm.inputFiles.remove(i);
					}
				}
			}
		}
		vm.scenes.refresh();
		vm.setDirty(true);
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
			String filename = null;
			int i = -1;
			if (vm.selectedScene != null) {
				filename = vm.selectedScene.getName();
				i = vm.inputFiles.indexOf(filename);
			}
			onRemove(a, vm.scenes);
			boolean nameExists = false;
			for (Animation r: vm.recordings.values()) {
				String name = r.getName();
				if (name.equals(filename))
					nameExists = true;
			}
			if ( nameExists != true && (i != -1)) vm.inputFiles.remove(i);
			vm.setDirty(true);
		} else {
			messageUtil.warn("Scene cannot be deleted", "It is used by "+res);
		}
	}

	public void onSortScenes() {
		onSortAnimations(vm.scenes);
		vm.setDirty(true);
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
		vm.setDirty(true);
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
		vm.setDirty(true);
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
		vm.setDirty(true);
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
		RecordingLink link = null;
		editLink.setRecordingLink(link);
		if (vm.selectedScene.getRecordingLink() != null)
			for(Animation a: vm.recordings.values()) {
				if( vm.selectedScene.getRecordingLink().associatedRecordingName.equals(a.getDesc())) {
					editLink.setRecordingLink(vm.selectedScene.getRecordingLink());
					break;
				}
			}
		editLink.open();
		if( editLink.okClicked() && vm.selectedScene != null) {
			if (vm.selectedScene.getActFrame() == 0 || vm.selectedScene.getRecordingLink() == null)
				vm.selectedScene.setRecordingLink(editLink.getRecordingLink());
			if (vm.selectedScene.getActualFrame().frameLink != null) {
				vm.selectedScene.getActualFrame().frameLink.recordingName = editLink.getRecordingLink().associatedRecordingName;
				vm.selectedScene.getActualFrame().frameLink.frame = editLink.getRecordingLink().startFrame; 
			} else {
				vm.selectedScene.getActualFrame().frameLink = new FrameLink(editLink.getRecordingLink().associatedRecordingName, editLink.getRecordingLink().startFrame);
			}
		}
        animationHandler.forceRerender();
 		vm.setDirty(true);
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
