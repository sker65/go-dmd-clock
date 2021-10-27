package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class HashCmdHandler extends AbstractCommandHandler implements ViewBindingHandler {

	public HashCmdHandler(ViewModel vm) {
		super(vm);
	}

	public void onHashSelected(int idx, boolean on) {
		if( on ) {
			if (vm.selectedKeyFrame != null) {
				vm.selectedKeyFrame.hashIndex = idx;
			}
			// switch palettes in preview
			int palIdx = vm.numberOfPlanes==4 || vm.previewDMD != null ? idx : (idx&1)*4;
			Palette palette = vm.previewPalettes.get(palIdx);
			log.info("switch to preview palette: {}", palette);
			vm.setPreviewDmdPalette(palette);
			vm.setSelectedHashIndex(idx);
		} else {
			vm.setSelectedHashIndex(-1);
		}
	}

	public void updateKeyFrameButtons( Animation selectedRecording, Animation selectedFrameSeq, int selectedHashIndex) {
		vm.setBtnAddKeyframeEnabled(selectedRecording != null && selectedHashIndex != -1);
		vm.setBtnAddFrameSeqEnabled(selectedRecording != null && selectedFrameSeq != null && selectedHashIndex != -1);
		vm.setBtnAddEventEnabled(selectedRecording != null && selectedHashIndex != -1);
		vm.setBtnSetHashEnabled((selectedRecording != null && selectedHashIndex != -1) || (vm.selectedScene != null && selectedHashIndex != -1  && (vm.selectedEditMode.haveLocalMask || vm.selectedEditMode.haveSceneDetectionMasks || vm.selectedEditMode.pullFrameDataFromAssociatedRecording )));
		vm.setBtnPreviewNextEnabled(vm.selectedScene != null && (vm.selectedEditMode.haveLocalMask || vm.selectedEditMode.haveSceneDetectionMasks || vm.selectedEditMode.pullFrameDataFromAssociatedRecording ));
		vm.setBtnPreviewPrevEnabled(vm.selectedScene != null && (vm.selectedEditMode.haveLocalMask || vm.selectedEditMode.haveSceneDetectionMasks || vm.selectedEditMode.pullFrameDataFromAssociatedRecording ));

	}

	public void onSelectedHashIndexChanged(int old, int n) {
		updateKeyFrameButtons(vm.selectedRecording, vm.selectedFrameSeq, n);
	}
	
	public static String getPrintableHashes(byte[] p) {
		StringBuffer hexString = new StringBuffer();
		for (int j = 0; j < p.length; j++)
			hexString.append(String.format("%02X", p[j]));
		return hexString.toString();
	}
	
	private void saveHashes(java.util.List<byte[]> hashes) {
		if (hashes != null) {
			vm.hashes.clear();
			for (byte[] h : hashes) {
				vm.hashes.add(Arrays.copyOf(h, h.length));
			}
		}
	}
	
	public void onDmdDirtyChanged(boolean o, boolean n) {
		if( n ) updateHashes(vm.dmd.getFrame());
	}
	
	public void onSelectedMaskChanged(Mask o, Mask n) {
		updateHashes(vm.dmd.getFrame());
	}

	private void selectHash(CompiledAnimation ani) {
		byte[] crc32 = ani.frames.get(ani.actFrame).crc32;
		boolean foundHash = false;
		for( int i =0; i < vm.hashes.size(); i++ ) {
			if( Arrays.equals(vm.hashes.get(i), crc32) ) {
				vm.setSelectedHashIndex(i);
				foundHash = true;
				Palette palette = vm.previewPalettes.get(i);
				vm.setPreviewDmdPalette(palette);
				break;
			}
		}
		if( !foundHash ) vm.setSelectedHashIndex(-1);
	}

	public void updateHashes(Frame frame) {
		if( frame == null ) return;
		Frame f = new Frame(frame);
	
		// if preview DMD uses its own dmd instance (e.g. for raw recording) use
		// plane from that instance instead
		if( vm.previewDMD != null ) {
			f.planes.clear();
			List<Plane> planes = vm.previewDMD.getFrame().planes;
			for(int i=0; i<planes.size(); i++) {
				f.planes.add(planes.get(i));
			}
		} 
		if( vm.selectedScene!=null && ( vm.selectedEditMode.enableDetectionMask )) {
			selectHash(vm.selectedScene);
		}
		List<byte[]> hashes = f.getHashes();
		refreshHashButtons(hashes, vm.detectionMaskActive && f.hasMask(), vm.selectedMaskNumber);
		saveHashes(hashes);
	}

	void refreshHashButtons(List<byte[]> hashes, boolean hasMask, int maskNumber) {
		//if( v.btnHash[0] == null ) return; // avoid NPE if not initialized
		int i = 0;
		String[] lbls = Arrays.copyOf(vm.hashLbl, vm.numberOfHashButtons);
		boolean[] enabled = Arrays.copyOf(vm.hashButtonEnabled, vm.numberOfHashButtons);
		for (byte[] p : hashes) {
			String hash = getPrintableHashes(p);
			// disable for empty frame: crc32 for empty frame is B2AA7578
			if (hash.startsWith(getEmptyHash())) {/* "BF619EAC0CDF3F68D496EA9344137E8B" */
				lbls[i]="";
				enabled[i]=false;
				if( vm.selectedHashIndex == i ) vm.setSelectedHashIndex(-1);
			} else {
				//if( hasMask && i == vm.selectedHashIndex) {
				if(vm.selectedKeyFrame != null && vm.detectionMaskActive && hash.equals(HashCmdHandler.getPrintableHashes(vm.selectedKeyFrame.crc32))) {
					lbls[i]=String.format("M%d %s", maskNumber, hash);
				} else {
					lbls[i]=hash;
				}
				enabled[i]=true;//btnHashEnabled[i]; // wird nie gesetzt
			}
			i++;
			if (i >= vm.numberOfHashButtons)
				break;
		}
		while (i < vm.numberOfHashButtons) {
			lbls[i]="";
			enabled[i]=false;
			i++;
		}
		vm.setHashLbl(lbls);
		vm.setHashButtonEnabled(enabled);
	}

	String getEmptyHash() {
		return vm.dmdSize.equals(DmdSize.Size128x32) ? "B2AA7578" : "6C1CE17E";
	}

}
