package com.rinke.solutions.pinball.view.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.databinding.observable.list.WritableList;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

@Bean
public class ViewModel {

	private static final long serialVersionUID = 1L;
	
	public int numberOfHashButtons = 4;

	private PropertyChangeSupport change = new PropertyChangeSupport(this);

	private void firePropertyChange(String propName, Object oldValue, Object newValue) {
		change.firePropertyChange(propName, oldValue, newValue);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		change.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		change.removePropertyChangeListener(l);
	}
	
	public void init() {
		setSelectedEditMode(EditMode.FIXED);
		setSelectedPaletteType(PaletteType.NORMAL);
		for(int i = 0; i < numberOfHashButtons; i++ ) {
			hashes.add(new byte[4]);
		}
	}
	
	public void loadTestData() {
		recordings.add(new TypedLabel(EditMode.FIXED.name(), "Recording1"));
		scenes.add(new TypedLabel(EditMode.REPLACE.name(), "Scence 1"));
		scenes.add(new TypedLabel(EditMode.COLMASK.name(), "Scence 2"));
		scenes.add(new TypedLabel(EditMode.FOLLOW.name(), "Scence 3"));
		
		bookmarks.add( new Bookmark("foo", 200));
		
		availableEditModes.addAll(Arrays.asList(EditMode.values()));
		availablePaletteTypes.addAll(Arrays.asList(PaletteType.values()));
		
		palettes.add( new Palette(Palette.defaultColors(), 1, "default") );
		palettes.add( new Palette(Palette.defaultColors(), 2, "foo") );
	}

	public DMD dmd = new DMD(128,32);

	// maybe not the real palette model class, but an variant for view model
	public WritableList palettes = new WritableList();
	public Palette selectedPalette = Palette.getDefaultPalettes().get(0);
	public String editedPaletteName;
	
	public java.util.List<Palette> previewPalettes = Palette.previewPalettes();

	public int selectedHashIndex;
	public int numberOfPlanes;
	public int duration;
	public int eventHigh;
	public int eventLow;
	
	public List<byte[]> hashes = new ArrayList<byte[]>(numberOfHashButtons);
	public String hashLbl[] = new String[numberOfHashButtons];
	public boolean[] hashButtonSelected = new boolean[numberOfHashButtons];
	public boolean[] hashButtonEnabled = new boolean[numberOfHashButtons];
	public boolean hashButtonsEnabled;
	
	
	public String projectFilename;
	
	public TypedLabel selectedRecording;
	public TypedLabel selectedScene;
	public TypedLabel selectedKeyFrame;
	public TypedLabel selectedFrameSeq;
	public WritableList recordings = new WritableList();
	public WritableList scenes = new WritableList();
	public WritableList keyframes = new WritableList();
	
	// this is for playing anis
	public Animation playingAni;
	public boolean animationIsStopped;
	public int minFrame;
	public int actFrame;
	public int maxFrame;
	public int frameIncrement; // TODO not bound? 
	public int delay;
	public int timecode;
	public CutInfo cutInfo = new CutInfo();
	
	public Bookmark selectedBookmark;
	public TreeSet<Bookmark> bookmarks = new TreeSet<>();
	public String editedBookmarkName;
	
	public WritableList availableEditModes = new WritableList();
	public EditMode selectedEditMode;
	
	public List<PaletteType> availablePaletteTypes = new ArrayList<>();
	public PaletteType selectedPaletteType;

	public int maskNumber;
	public boolean maskVisible;
	public boolean maskLocked;
	
	// controls enabled / disable
	public boolean maskSpinnerEnabled;
	public boolean maskInvertEnabled;
	
	public boolean deleteKeyFrameEnabled;
	public boolean deleteRecordingEnabled;
	public boolean deleteSceneEnabled;
	
	public boolean addPaletteSwitchEnabled;
	public boolean addColSceneEnabled;
	public boolean addFetchEnabled;
	public boolean addEventEnabled;

	public boolean maskOnEnabled;
	public boolean undoEnabled;
	public boolean redoEnabled;
	
	public boolean markStartEnabled;
	public boolean markStopEnabled;
	public boolean cutEnabled;
	public boolean startStopEnabled;
	public boolean prevEnabled;
	public boolean nextEnabled;
	
	public boolean copyToNextEnabled;
	public boolean copyToPrevEnabled;
	
	public String startStopLabel = "Start";
	
	public boolean drawingEnabled;
	public int drawMask;
	
	public Palette previewPalette;

	public Rect dmdSelection;
	
	public boolean livePreview;
	
	public java.util.List<Palette> getPreviewPalettes() {
		return previewPalettes;
	}
	public void setPreviewPalettes(java.util.List<Palette> previewPalettes) {
		firePropertyChange("previewPalettes", this.previewPalettes, this.previewPalettes = previewPalettes);
	}
	public int getSelectedHashIndex() {
		return selectedHashIndex;
	}
	public void setSelectedHashIndex(int selectedHashIndex) {
		firePropertyChange("selectedHashIndex", this.selectedHashIndex, this.selectedHashIndex = selectedHashIndex);
	}
	public String getProjectFilename() {
		return projectFilename;
	}
	public void setProjectFilename(String projectFilename) {
		firePropertyChange("projectFilename", this.projectFilename, this.projectFilename = projectFilename);
	}
	public TypedLabel getSelectedRecording() {
		return selectedRecording;
	}
	public void setSelectedRecording(TypedLabel selectedRecording) {
		firePropertyChange("selectedRecording", this.selectedRecording, this.selectedRecording = selectedRecording);
	}
	public TypedLabel getSelectedScene() {
		return selectedScene;
	}
	public void setSelectedScene(TypedLabel selectedScene) {
		firePropertyChange("selectedScene", this.selectedScene, this.selectedScene = selectedScene);
	}
	public TypedLabel getSelectedKeyFrame() {
		return selectedKeyFrame;
	}
	public void setSelectedKeyFrame(TypedLabel selectedKeyFrame) {
		firePropertyChange("selectedKeyFrame", this.selectedKeyFrame, this.selectedKeyFrame = selectedKeyFrame);
	}
	public WritableList getRecordings() {
		return recordings;
	}
	public void setRecordings(WritableList recordings) {
		firePropertyChange("recordings", this.recordings, this.recordings = recordings);
	}
	public WritableList getScenes() {
		return scenes;
	}
	public void setScenes(WritableList scenes) {
		firePropertyChange("scenes", this.scenes, this.scenes = scenes);
	}
	public boolean isAnimationIsStopped() {
		return animationIsStopped;
	}
	public void setAnimationIsStopped(boolean animationIsStopped) {
		firePropertyChange("animationIsStopped", this.animationIsStopped, this.animationIsStopped = animationIsStopped);
	}
	public int getActFrame() {
		return actFrame;
	}
	public void setActFrame(int actFrame) {
		firePropertyChange("actFrame", this.actFrame, this.actFrame = actFrame);
	}
	public CutInfo getCutInfo() {
		return cutInfo;
	}
	public void setCutInfo(CutInfo cutInfo) {
		firePropertyChange("cutInfo", this.cutInfo, this.cutInfo = cutInfo);
	}
	public Bookmark getSelectedBookmark() {
		return selectedBookmark;
	}
	public void setSelectedBookmark(Bookmark selectedBookmark) {
		firePropertyChange("selectedBookmark", this.selectedBookmark, this.selectedBookmark = selectedBookmark);
	}
	public Set<Bookmark> getBookmarks() {
		return bookmarks;
	}
	public void setBookmarks(TreeSet<Bookmark> bookmarks) {
		firePropertyChange("bookmarks", this.bookmarks, this.bookmarks = bookmarks);
	}
	public WritableList getAvailableEditModes() {
		return availableEditModes;
	}
	public void setAvailableEditModes(WritableList availableEditModes) {
		firePropertyChange("availableEditModes", this.availableEditModes, this.availableEditModes = availableEditModes);
	}
	public EditMode getSelectedEditMode() {
		return selectedEditMode;
	}
	public void setSelectedEditMode(EditMode selectedEditMode) {
		firePropertyChange("selectedEditMode", this.selectedEditMode, this.selectedEditMode = selectedEditMode);
	}
	public int getMaskNumber() {
		return maskNumber;
	}
	public void setMaskNumber(int maskNumber) {
		firePropertyChange("maskNumber", this.maskNumber, this.maskNumber = maskNumber);
	}

	public boolean isMaskSpinnerEnabled() {
		return maskSpinnerEnabled;
	}

	public void setMaskSpinnerEnabled(boolean maskSpinnerEnabled) {
		firePropertyChange("maskSpinnerEnabled", this.maskSpinnerEnabled, this.maskSpinnerEnabled = maskSpinnerEnabled);
	}

	public boolean isMaskInvertEnabled() {
		return maskInvertEnabled;
	}

	public void setMaskInvertEnabled(boolean maskInvertEnabled) {
		firePropertyChange("maskInvertEnabled", this.maskInvertEnabled, this.maskInvertEnabled = maskInvertEnabled);
	}

	public boolean isMaskOnEnabled() {
		return maskOnEnabled;
	}

	public void setMaskOnEnabled(boolean maskOnEnabled) {
		firePropertyChange("maskOnEnabled", this.maskOnEnabled, this.maskOnEnabled = maskOnEnabled);
	}

	public boolean isUndoEnabled() {
		return undoEnabled;
	}

	public void setUndoEnabled(boolean undoEnabled) {
		firePropertyChange("undoEnabled", this.undoEnabled, this.undoEnabled = undoEnabled);
	}

	public boolean isRedoEnabled() {
		return redoEnabled;
	}

	public void setRedoEnabled(boolean redoEnabled) {
		firePropertyChange("redoEnabled", this.redoEnabled, this.redoEnabled = redoEnabled);
	}

	public boolean isMarkStartEnabled() {
		return markStartEnabled;
	}

	public void setMarkStartEnabled(boolean markStartEnabled) {
		firePropertyChange("markStartEnabled", this.markStartEnabled, this.markStartEnabled = markStartEnabled);
	}

	public boolean isMarkStopEnabled() {
		return markStopEnabled;
	}

	public void setMarkStopEnabled(boolean markStopEnabled) {
		firePropertyChange("markStopEnabled", this.markStopEnabled, this.markStopEnabled = markStopEnabled);
	}

	public boolean[] getHashButtonSelected() {
		return hashButtonSelected;
	}

	public void setHashButtonSelected(boolean[] hashButtonSelected) {
		firePropertyChange("hashButtonSelected", this.hashButtonSelected, this.hashButtonSelected = hashButtonSelected);
	}

	public int getNumberOfPlanes() {
		return numberOfPlanes;
	}

	public void setNumberOfPlanes(int numberOfPlanes) {
		firePropertyChange("numberOfPlanes", this.numberOfPlanes, this.numberOfPlanes = numberOfPlanes);
	}

	public Palette getPreviewPalette() {
		return previewPalette;
	}

	public void setPreviewPalette(Palette previewPalette) {
		firePropertyChange("previewPalette", this.previewPalette, this.previewPalette = previewPalette);
	}

	public boolean isDeleteKeyFrameEnabled() {
		return deleteKeyFrameEnabled;
	}

	public void setDeleteKeyFrameEnabled(boolean deleteKeyFrameEnabled) {
		firePropertyChange("deleteKeyFrameEnabled", this.deleteKeyFrameEnabled, this.deleteKeyFrameEnabled = deleteKeyFrameEnabled);
	}

	public boolean isDeleteRecordingEnabled() {
		return deleteRecordingEnabled;
	}

	public void setDeleteRecordingEnabled(boolean deleteRecordingEnabled) {
		firePropertyChange("deleteRecordingEnabled", this.deleteRecordingEnabled, this.deleteRecordingEnabled = deleteRecordingEnabled);
	}

	public boolean isDeleteSceneEnabled() {
		return deleteSceneEnabled;
	}

	public void setDeleteSceneEnabled(boolean deleteSceneEnabled) {
		firePropertyChange("deleteSceneEnabled", this.deleteSceneEnabled, this.deleteSceneEnabled = deleteSceneEnabled);
	}

	public boolean isAddPaletteSwitchEnabled() {
		return addPaletteSwitchEnabled;
	}

	public void setAddPaletteSwitchEnabled(boolean addPaletteSwitchEnabled) {
		firePropertyChange("addPaletteSwitchEnabled", this.addPaletteSwitchEnabled, this.addPaletteSwitchEnabled = addPaletteSwitchEnabled);
	}

	public boolean isAddColSceneEnabled() {
		return addColSceneEnabled;
	}

	public void setAddColSceneEnabled(boolean addColSceneEnabled) {
		firePropertyChange("addColSceneEnabled", this.addColSceneEnabled, this.addColSceneEnabled = addColSceneEnabled);
	}

	public boolean isAddFetchEnabled() {
		return addFetchEnabled;
	}

	public void setAddFetchEnabled(boolean addFetchEnabled) {
		firePropertyChange("addFetchEnabled", this.addFetchEnabled, this.addFetchEnabled = addFetchEnabled);
	}

	public boolean isAddEventEnabled() {
		return addEventEnabled;
	}

	public void setAddEventEnabled(boolean addEventEnabled) {
		firePropertyChange("addEventEnabled", this.addEventEnabled, this.addEventEnabled = addEventEnabled);
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		firePropertyChange("duration", this.duration, this.duration = duration);
	}

	public int getEventHigh() {
		return eventHigh;
	}

	public void setEventHigh(int eventHigh) {
		firePropertyChange("eventHigh", this.eventHigh, this.eventHigh = eventHigh);
	}

	public int getEventLow() {
		return eventLow;
	}

	public void setEventLow(int eventLow) {
		firePropertyChange("eventLow", this.eventLow, this.eventLow = eventLow);
	}

	public boolean isPrevEnabled() {
		return prevEnabled;
	}

	public void setPrevEnabled(boolean prevEnabled) {
		firePropertyChange("prevEnabled", this.prevEnabled, this.prevEnabled = prevEnabled);
	}

	public boolean isNextEnabled() {
		return nextEnabled;
	}

	public void setNextEnabled(boolean nextEnabled) {
		firePropertyChange("nextEnabled", this.nextEnabled, this.nextEnabled = nextEnabled);
	}

	public boolean isStartStopEnabled() {
		return startStopEnabled;
	}

	public void setStartStopEnabled(boolean startStopEnabled) {
		firePropertyChange("startStopEnabled", this.startStopEnabled, this.startStopEnabled = startStopEnabled);
	}

	public boolean isCutEnabled() {
		return cutEnabled;
	}

	public void setCutEnabled(boolean cutEnabled) {
		firePropertyChange("cutEnabled", this.cutEnabled, this.cutEnabled = cutEnabled);
	}

	public String getStartStopLabel() {
		return startStopLabel;
	}

	public void setStartStopLabel(String startStopLabel) {
		firePropertyChange("startStopLabel", this.startStopLabel, this.startStopLabel = startStopLabel);
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		firePropertyChange("delay", this.delay, this.delay = delay);
	}

	public int getTimecode() {
		return timecode;
	}

	public void setTimecode(int timecode) {
		firePropertyChange("timecode", this.timecode, this.timecode = timecode);
	}

	public boolean isCopyToNextEnabled() {
		return copyToNextEnabled;
	}

	public void setCopyToNextEnabled(boolean copyToNextEnabled) {
		firePropertyChange("copyToNextEnabled", this.copyToNextEnabled, this.copyToNextEnabled = copyToNextEnabled);
	}

	public boolean isCopyToPrevEnabled() {
		return copyToPrevEnabled;
	}

	public void setCopyToPrevEnabled(boolean copyToPrevEnabled) {
		firePropertyChange("copyToPrevEnabled", this.copyToPrevEnabled, this.copyToPrevEnabled = copyToPrevEnabled);
	}

	public boolean isDrawingEnabled() {
		return drawingEnabled;
	}

	public void setDrawingEnabled(boolean drawingEnabled) {
		firePropertyChange("drawingEnabled", this.drawingEnabled, this.drawingEnabled = drawingEnabled);
	}

	public int getDrawMask() {
		return drawMask;
	}

	public void setDrawMask(int drawMask) {
		firePropertyChange("drawMask", this.drawMask, this.drawMask = drawMask);
	}

	public boolean isMaskVisible() {
		return maskVisible;
	}

	public void setMaskVisible(boolean maskVisible) {
		firePropertyChange("maskVisible", this.maskVisible, this.maskVisible = maskVisible);
	}

	public PaletteType getSelectedPaletteType() {
		return selectedPaletteType;
	}

	public void setSelectedPaletteType(PaletteType selectedPaletteType) {
		firePropertyChange("selectedPaletteType", this.selectedPaletteType, this.selectedPaletteType = selectedPaletteType);
	}

	public List<PaletteType> getAvailablePaletteTypes() {
		return availablePaletteTypes;
	}

	public void setAvailablePaletteTypes(List<PaletteType> availablePaletteTypes) {
		firePropertyChange("availablePaletteTypes", this.availablePaletteTypes, this.availablePaletteTypes = availablePaletteTypes);
	}

	public String getEditedBookmarkName() {
		return editedBookmarkName;
	}

	public void setEditedBookmarkName(String editedBookmarkName) {
		firePropertyChange("editedBookmarkName", this.editedBookmarkName, this.editedBookmarkName = editedBookmarkName);
	}

	public WritableList getPalettes() {
		return palettes;
	}

	public void setPalettes(WritableList palettes) {
		firePropertyChange("palettes", this.palettes, this.palettes = palettes);
	}

	public Palette getSelectedPalette() {
		return selectedPalette;
	}

	public void setSelectedPalette(Palette selectedPalette) {
		firePropertyChange("selectedPalette", this.selectedPalette, this.selectedPalette = selectedPalette);
	}

	public WritableList getKeyframes() {
		return keyframes;
	}

	public void setKeyframes(WritableList keyframes) {
		firePropertyChange("keyframes", this.keyframes, this.keyframes = keyframes);
	}

	public boolean isMaskLocked() {
		return maskLocked;
	}

	public void setMaskLocked(boolean maskLocked) {
		firePropertyChange("maskLocked", this.maskLocked, this.maskLocked = maskLocked);
	}

	public List<byte[]> getHashes() {
		return hashes;
	}

	public void setHashes(List<byte[]> hashes) {
		firePropertyChange("hashes", this.hashes, this.hashes = hashes);
	}

	public TypedLabel getSelectedFrameSeq() {
		return selectedFrameSeq;
	}

	public void setSelectedFrameSeq(TypedLabel selectedFrameSeq) {
		firePropertyChange("selectedFrameSeq", this.selectedFrameSeq, this.selectedFrameSeq = selectedFrameSeq);
	}

	public Rect getDmdSelection() {
		return dmdSelection;
	}

	public void setDmdSelection(Rect dmdSelection) {
		firePropertyChange("dmdSelection", this.dmdSelection, this.dmdSelection = dmdSelection);
	}

	public boolean isHashButtonsEnabled() {
		return hashButtonsEnabled;
	}

	public void setHashButtonsEnabled(boolean hashButtonsEnabled) {
		firePropertyChange("hashButtonsEnabled", this.hashButtonsEnabled, this.hashButtonsEnabled = hashButtonsEnabled);
	}

	public Animation getPlayingAni() {
		return playingAni;
	}

	public void setPlayingAni(Animation playingAni) {
		firePropertyChange("playingAni", this.playingAni, this.playingAni = playingAni);
	}

	public DMD getDmd() {
		return dmd;
	}

	public void setDmd(DMD dmd) {
		firePropertyChange("dmd", this.dmd, this.dmd = dmd);
	}

	public int getMinFrame() {
		return minFrame;
	}

	public void setMinFrame(int minFrame) {
		firePropertyChange("minFrame", this.minFrame, this.minFrame = minFrame);
	}

	public int getMaxFrame() {
		return maxFrame;
	}

	public void setMaxFrame(int maxFrame) {
		firePropertyChange("maxFrame", this.maxFrame, this.maxFrame = maxFrame);
	}

	public int getFrameIncrement() {
		return frameIncrement;
	}

	public void setFrameIncrement(int frameIncrement) {
		firePropertyChange("frameIncrement", this.frameIncrement, this.frameIncrement = frameIncrement);
	}

	public String[] getHashLbl() {
		return hashLbl;
	}

	public void setHashLbl(String[] hashLbl) {
		firePropertyChange("hashLbl", this.hashLbl, this.hashLbl = hashLbl);
	}

	public String getEditedPaletteName() {
		return editedPaletteName;
	}

	public void setEditedPaletteName(String editedPaletteName) {
		firePropertyChange("editedPaletteName", this.editedPaletteName, this.editedPaletteName = editedPaletteName);
	}

	public boolean isLivePreview() {
		return livePreview;
	}

	public void setLivePreview(boolean livePreview) {
		firePropertyChange("livePreview", this.livePreview, this.livePreview = livePreview);
	}

}
