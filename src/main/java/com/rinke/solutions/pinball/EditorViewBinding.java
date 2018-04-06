package com.rinke.solutions.pinball;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Button;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.view.handler.AbstractCommandHandler;
import com.rinke.solutions.pinball.view.handler.ViewBindingHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

@Bean
public class EditorViewBinding extends AbstractCommandHandler implements ViewBindingHandler, PropertyChangeListener {

	private EditorView editorView;
	
	@Autowired private AnimationHandler animationHandler;

	private String frameTextPrefix = "Pin2dmd Editor ";
	private String internalName;
	
	public EditorViewBinding(ViewModel vm) {
		super(vm);
	}
	
	public void onCut(Palette pal) {
		editorView.clipboardHandler.onCut(pal);
	}
	
	public void onCopy(Palette pal) {
		editorView.clipboardHandler.onCopy(pal);
	}
	
	public void onPaste() {
		editorView.clipboardHandler.onPaste();
	}

	public void onPasteHoover() {
		editorView.clipboardHandler.onPasteHoover();
	}
	
	private void updateTitle(String t, boolean dirty) {
		editorView.shell.setText(frameTextPrefix + " - " + (dirty?"* ":"") + (t==null?"":t));
	}

	public void onDirtyChanged( boolean ov, boolean nv) {
		updateTitle(this.internalName, nv);
	}

	public void onProjectFilenameChanged(String old, String projectFilename) {
		if( projectFilename != null ) this.internalName=new File(projectFilename).getName();
		else this.internalName = "";
		updateTitle(this.internalName, vm.dirty);
	}
	
	private Runnable animation = new Runnable() {
		@Override
		public void run() {
			if( !animationHandler.isStopped() ) {
				if( vm.selectedFrame < vm.maxFrame ) 
					vm.setSelectedFrame(vm.selectedFrame+vm.frameIncrement);
				editorView.timerExec(animationHandler.getRefreshDelay(), animation);
			}
		}
	};

	public void onAnimationIsPlayingChanged(boolean o, boolean n) {
		if( n ) {
			if( vm.selectedFrame < vm.maxFrame ) {
				vm.setSelectedFrame(vm.selectedFrame+vm.frameIncrement);
				editorView.timerExec(animationHandler.getRefreshDelay(), animation);
			}
		}
	}
	
	public void onSelectedPaletteChanged(Palette o, Palette newPalette) {
		if( editorView.clipboardHandler != null ) editorView.clipboardHandler.setPalette(newPalette);
	}
	
	// bind recent managers for menues
	public void onRecentAnimationsChanged(String o, String filename) {
		if( filename != null ) editorView.recentAnimationsMenuManager.populateRecent(filename);
		vm.recentAnimations = null;
	}

	public void onRecentProjectsChanged(String o, String filename) {
		if( filename != null ) editorView.recentProjectsMenuManager.populateRecent(filename);
		vm.recentProjects = null;
	}
	
	public void onRecentPaletteChanged(String o, String filename) {
		if( filename != null ) editorView.recentPalettesMenuManager.populateRecent(filename);
		vm.recentPalette = null;
	}

	@Override
	public void viewModelChanged(String propName, Object ov, Object nv) {
		// TODO das muss in den View selber
		Stream<Button> btns = Arrays.stream(editorView.btnHash);
		if( propName.equals("selectedHashIndex")) {
			for (int i = 0; i < vm.hashButtonSelected.length; i++) {
				vm.hashButtonSelected[i] = (i == (Integer)nv);
			}
			btns.forEach(b->b.setSelection(vm.hashButtonSelected[(int) b.getData()]));
		} else if( propName.equals("hashButtonSelected") ) {
			btns.forEach(b->b.setSelection(vm.hashButtonSelected[(int) b.getData()]));
		} else if( propName.equals("hashLbl")) {
			btns.forEach(b->b.setText(vm.hashLbl[(int) b.getData()]));
		} else if( propName.equals("paletteDirty")) {
			editorView.paletteTool.setPalette(vm.selectedPalette);
			vm.paletteDirty = false;
		} else if( propName.equals("hashButtonEnabled") || propName.equals("hashButtonsEnabled")) { // beware of the 's'
			btns.forEach(b->{
				b.setEnabled(vm.hashButtonEnabled[(int) b.getData()] && vm.hashButtonsEnabled );
			});
		}
	}
	
	// trigger redraw
	public void onDmdDirtyChanged(boolean o, boolean n) {
		editorView.dmdRedraw();
		vm.dmdDirty = false;
	}
	
	public void onShouldCloseChanged(boolean o, boolean shouldClose) {
		if( shouldClose ) {
			editorView.shell.dispose();
		}
	}
	
	// could maybe done directly with bindings
	public void onDmdSizeChanged(DmdSize old, DmdSize newSize) {
		editorView.dmdWidget.setResolution(vm.dmd);
		editorView.previewDmd.setResolution(vm.dmd);
	}

	public void setEditorView(EditorView editorView) {
		 this.editorView = editorView;
		 editorView.dmdWidget.addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if( evt.getPropertyName().equals("selection")) {
			vm.setSelection((Rect) evt.getNewValue());
		}
	}
}
