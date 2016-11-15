package com.rinke.solutions.pinball;

import java.util.Observable;
import java.util.Observer;

/**
 * encapsulates delegating logic for mask and dmd regarding undo / redo and abservable
 * do it delegates (can)Undo and (can)Redo calls, depending on mask set
 * and forwards observer notifies.
 */
class MaskDmdObserver extends Observable implements Observer {
	private DMD dmd;
	private DMD mask;
	
	public void setDmd(DMD dmd) {
		 this.dmd = dmd;
		 dmd.addObserver(this);
		 setChanged(); notifyObservers();
	}

	public void setMask(DMD mask) {
		this.mask = mask;
		if( mask != null ) {
			mask.addObserver(this);
		}
		setChanged(); notifyObservers();
	}
	
	public boolean canUndo() {
		return mask != null ? mask.canUndo() : dmd.canUndo();
	}
	
	public boolean canRedo() {
		return mask != null ? mask.canRedo(): dmd.canRedo();
	}
	
	public void redo() {
		if( mask != null ) mask.redo(); else dmd.redo();	
	}
	
	public void undo() {
		if( mask != null ) mask.undo(); else dmd.undo();
	}
	
	@Override
	public void update(Observable o, Object arg) {
		setChanged(); notifyObservers();
	}
}