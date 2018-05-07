package com.rinke.solutions.pinball.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.pinball.ClipboardFacade;

public class SWTClipboard implements ClipboardFacade {
	
	Clipboard clipboard;
	private Display display;

	public SWTClipboard() {
		super();
		this.display = Display.getCurrent();
		this.clipboard = new Clipboard(display);
	}

	@Override
	public Object getContents(String transfer) {
		return clipboard.getContents(getTransferFromString(transfer));
	}

	private Transfer getTransferFromString(String transfer) {
		if( "DmdFrameTransfer".equals(transfer)) return DmdFrameTransfer.getInstance();
		if( "ImageTransfer".equals(transfer)) return ImageTransfer.getInstance();
		return null;
	}

	@Override
	public void setContents(Object[] contents, String[] transfers) {
		clipboard.setContents(contents, getTransfersFromStrings(transfers));
	}

	private Transfer[] getTransfersFromStrings(String[] transfers) {
		List<Transfer> res = new ArrayList<>();
		for(String i : transfers) {
			res.add(getTransferFromString(i));
		}
		return res.toArray(new Transfer[res.size()]);
	}

	@Override
	public String[] getAvailableTypeNames() {
		return clipboard.getAvailableTypeNames();
	}

}
