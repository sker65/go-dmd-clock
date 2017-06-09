package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class HashButtonHandler extends ViewHandler {

	public HashButtonHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}
	
	private void setLabelAndState(String lbl, int i) {
		vm.hashLbl[i]= ( lbl != null ? lbl : "");
		vm.hashButtonEnabled[i] = !StringUtils.isEmpty(lbl);
	}
	
	public void onUpdateHashes(List<byte[]> hashes) {
		int i = 0;
		for (byte[] p : hashes) {
			String hash = getPrintableHashes(p);
			// disable for empty frame: crc32 for empty frame is B2AA7578
			if (hash.startsWith("B2AA7578" /* "BF619EAC0CDF3F68D496EA9344137E8B" */)) {
				setLabelAndState("", i);
			} else {
				setLabelAndState(hash, i);
			}
			i++;
			if (i >= vm.hashLbl.length)
				break;
		}
		while (i < vm.numberOfHashButtons) {
			setLabelAndState("", i);
			i++;
		}
		// copy is essential to propagate change
		vm.setHashLbl(Arrays.copyOf(vm.hashLbl, vm.numberOfHashButtons));
		vm.setHashButtonEnabled(Arrays.copyOf(vm.hashButtonEnabled, vm.numberOfHashButtons));
	}
	
	public String getPrintableHashes(byte[] p) {
		StringBuffer hexString = new StringBuffer();
		for (int j = 0; j < p.length; j++)
			hexString.append(String.format("%02X", p[j]));
		return hexString.toString();
	}

	public void onHashSelected(int noOfHash ) {
		log.debug("onHashSelected {}", noOfHash);
		if( vm.selectedKeyFrame != null ) {
			
		}
		boolean[] btn = new boolean[vm.numberOfHashButtons];
		for (int j = 0; j < vm.numberOfHashButtons; j++) {
			btn[j] = j==noOfHash;
		}
		vm.setHashButtonSelected(btn);
		vm.setSelectedHashIndex(noOfHash);
		//CHECK vm.previewPalette = vm.getPreviewPalettes().get(vm.numberOfPlanes==4?vm.selectedHashIndex:vm.selectedHashIndex*4);
	}

}
