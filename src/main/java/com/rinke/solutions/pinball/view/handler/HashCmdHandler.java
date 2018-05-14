package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
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
			Palette palette = vm.previewPalettes.get(vm.numberOfPlanes==4?idx:(idx&1)*4);
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
	}

	public void onSelectedHashIndexChanged(int old, int n) {
		updateKeyFrameButtons(vm.selectedRecording, vm.selectedFrameSeq, n);
	}
	
	public String getPrintableHashes(byte[] p) {
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
	
	public void updateHashes(Frame frame) {
		if( frame == null ) return;
		Frame f = new Frame(frame);
		if( vm.selectedMask != null ) f.setMask(vm.selectedMask.data);
		else f.setMask(null);
		
		List<byte[]> hashes = f.getHashes();
		refreshHashButtons(hashes, vm.selectedMask, vm.selectedMaskNumber);
		saveHashes(hashes);
	}

	void refreshHashButtons(List<byte[]> hashes, Mask mask, int maskNumber) {
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
				if( mask!=null && i == vm.selectedHashIndex) {
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
