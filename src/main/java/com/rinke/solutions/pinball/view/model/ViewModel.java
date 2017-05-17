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
import com.rinke.solutions.databinding.BindingTarget;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.view.model.ViewModel.PasteData;

@Bean
public class ViewModel {
	
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
		availableEditModes.replace(Arrays.asList(EditMode.values()));
		availablePaletteTypes.addAll(Arrays.asList(PaletteType.values()));
		setSelectedEditMode(EditMode.FIXED);
		setSelectedPaletteType(PaletteType.NORMAL);
		for(int i = 0; i < numberOfHashButtons; i++ ) {
			hashes.add(new byte[4]);
		}
	}
	
	public void loadTestData() {
//		recordings.add(new TypedLabel(EditMode.FIXED.name(), "Recording1"));
//		scenes.add(new TypedLabel(EditMode.REPLACE.name(), "Scence 1"));
//		scenes.add(new TypedLabel(EditMode.COLMASK.name(), "Scence 2"));
//		scenes.add(new TypedLabel(EditMode.FOLLOW.name(), "Scence 3"));
		
		bookmarks.add( new Bookmark("foo", 200));
		
		
		palettes.add( new Palette(Palette.defaultColors(), 1, "default") );
		palettes.add( new Palette(Palette.defaultColors(), 2, "foo") );
	}

	public static class PasteData {
		public int dx,dy;
		public int width;
		public int height;
		public Frame frame;
		public boolean maskOnly;
		public PasteData(int dx, int dy, int width, int height, Frame frame, boolean maskOnly) {
			super();
			this.dx = dx;
			this.dy = dy;
			this.width = width;
			this.height = height;
			this.frame = frame;
			this.maskOnly = maskOnly;
		}
		public PasteData() {
			super();
		}
		public PasteData(PasteData src) {
			this.dx = src.dx;
			this.dy = src.dy;
			this.width = src.width;
			this.height = src.height;
			this.frame = new Frame(src.frame);
			this.maskOnly = src.maskOnly;
		}
	}
	
	public PasteData pasteData;
	
	public int numberOfHashButtons = 4;
	public DMD dmd = new DMD(128,32);
	public String drawTool;
	// syntetic property just to trigger redraw
	public long frameRedraw;

	// maybe not the real palette model class, but an variant for view model
	@BindingTarget public ObservableList<Palette> palettes = new ObservableList<>(new ArrayList<>());
	public Palette selectedPalette = Palette.getDefaultPalettes().get(0);
	public String editedPaletteName;
	
	public java.util.List<Palette> previewPalettes = Palette.previewPalettes();

	public int selectedHashIndex;
	@BindingTarget public int numberOfPlanes;
	@BindingTarget public int duration;
	@BindingTarget public int selectedEventHigh;
	@BindingTarget public int selectedEventLow;
	
	public List<byte[]> hashes = new ArrayList<byte[]>(numberOfHashButtons);
	public String hashLbl[] = new String[numberOfHashButtons];
	public boolean[] hashButtonSelected = new boolean[numberOfHashButtons];
	public boolean[] hashButtonEnabled = new boolean[numberOfHashButtons];
	public boolean hashButtonsEnabled;
	
	public String projectFilename;
	
	public Animation selectedRecording;
	public CompiledAnimation selectedScene;
	public TypedLabel selectedKeyFrame;
	public String selectedFrameSeq;
	@BindingTarget public ObservableList<Animation> recordings = new ObservableList<>(new ArrayList<>());
	@BindingTarget public ObservableList<CompiledAnimation> scenes = new ObservableList<>(new ArrayList<>());
	@BindingTarget public ObservableList<TypedLabel> keyframes = new ObservableList<>(new ArrayList<>());
	
	// this is for playing anis
	public Animation playingAni;
	public boolean animationIsPlaying;
	@BindingTarget public int minFrame;
	@BindingTarget public int selectedFrame;
	@BindingTarget public int maxFrame;
	@BindingTarget public int delay;
	@BindingTarget public int timecode;
	public int skip;
	public CutInfo cutInfo;
	
	public Bookmark selectedBookmark;
	public TreeSet<Bookmark> bookmarks = new TreeSet<>();
	public String editedBookmarkName;
	
	@BindingTarget public ObservableList<EditMode> availableEditModes = new ObservableList<EditMode>(new ArrayList<>());
	public EditMode selectedEditMode;
	
	public List<PaletteType> availablePaletteTypes = new ArrayList<>();
	public PaletteType selectedPaletteType;

	@BindingTarget public int selectedMaskNumber;
	public boolean maskVisible;
	public boolean maskLocked;
	
	// controls enabled / disable
	@BindingTarget public boolean maskNumberEnabled;
	@BindingTarget public boolean maskInvertEnabled;
	
	@BindingTarget public boolean deleteKeyFrameEnabled;
	@BindingTarget public boolean deleteRecordingEnabled;
	@BindingTarget public boolean deleteSceneEnabled;
	
	@BindingTarget public boolean addPaletteSwitchEnabled;
	@BindingTarget public boolean addColSceneEnabled;
	@BindingTarget public boolean fetchDurationEnabled;
	@BindingTarget public boolean addEventEnabled;

	@BindingTarget public boolean maskOnEnabled;
	@BindingTarget public boolean undoEnabled;
	@BindingTarget public boolean redoEnabled;
	
	@BindingTarget public boolean markStartEnabled;
	@BindingTarget public boolean markEndEnabled;
	@BindingTarget public boolean cutEnabled;
	@BindingTarget public boolean startStopEnabled;
	@BindingTarget public boolean prevEnabled;
	@BindingTarget public boolean nextEnabled;
	
	@BindingTarget public boolean copyToNextEnabled;
	@BindingTarget public boolean copyToPrevEnabled;
	@BindingTarget public boolean deleteColMaskEnabled;
	
	@BindingTarget public String startStopLabel = "Start";
	
	public boolean drawingEnabled;
	public int drawMask;
	
	public Palette previewPalette;

	public Rect dmdSelection;
	
	public boolean livePreview;
	
	public String shellTitle;
	
	// --- generated getter / setter code --- do not edit
	
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
	
	public TypedLabel getSelectedKeyFrame() {
		return selectedKeyFrame;
	}
	public void setSelectedKeyFrame(TypedLabel selectedKeyFrame) {
		firePropertyChange("selectedKeyFrame", this.selectedKeyFrame, this.selectedKeyFrame = selectedKeyFrame);
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
	public ObservableList<EditMode> getAvailableEditModes() {
		return availableEditModes;
	}
	public void setAvailableEditModes(ObservableList<EditMode> availableEditModes) {
		firePropertyChange("availableEditModes", this.availableEditModes, this.availableEditModes = availableEditModes);
	}
	public EditMode getSelectedEditMode() {
		return selectedEditMode;
	}
	public void setSelectedEditMode(EditMode selectedEditMode) {
		firePropertyChange("selectedEditMode", this.selectedEditMode, this.selectedEditMode = selectedEditMode);
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

	public boolean isFetchDurationEnabled() {
		return fetchDurationEnabled;
	}

	public void setFetchDurationEnabledEnabled(boolean fetchDurationEnabled) {
		firePropertyChange("fetchDurationEnabled", this.fetchDurationEnabled, this.fetchDurationEnabled = fetchDurationEnabled);
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

	public Palette getSelectedPalette() {
		return selectedPalette;
	}

	public void setSelectedPalette(Palette selectedPalette) {
		firePropertyChange("selectedPalette", this.selectedPalette, this.selectedPalette = selectedPalette);
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

	public String getSelectedFrameSeq() {
		return selectedFrameSeq;
	}

	public void setSelectedFrameSeq(String selectedFrameSeq) {
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

	public String getShellTitle() {
		return shellTitle;
	}

	public void setShellTitle(String shellTitle) {
		firePropertyChange("shellTitle", this.shellTitle, this.shellTitle = shellTitle);
	}

	public String getDrawTool() {
		return drawTool;
	}

	public void setDrawTool(String drawTool) {
		firePropertyChange("drawTool", this.drawTool, this.drawTool = drawTool);
	}

	public PasteData getPasteData() {
		return pasteData;
	}

	public void setPasteData(PasteData pasteData) {
		firePropertyChange("pasteData", this.pasteData, this.pasteData = pasteData);
	}

	public boolean isAnimationIsPlaying() {
		return animationIsPlaying;
	}

	public void setAnimationIsPlaying(boolean animationIsPlaying) {
		firePropertyChange("animationIsPlaying", this.animationIsPlaying, this.animationIsPlaying = animationIsPlaying);
	}

	public boolean isDeleteColMaskEnabled() {
		return deleteColMaskEnabled;
	}

	public void setDeleteColMaskEnabled(boolean deleteColMaskEnabled) {
		firePropertyChange("deleteColMaskEnabled", this.deleteColMaskEnabled, this.deleteColMaskEnabled = deleteColMaskEnabled);
	}

	public long getFrameRedraw() {
		return frameRedraw;
	}

	public void setFrameRedraw(long frameRedraw) {
		firePropertyChange("frameRedraw", this.frameRedraw, this.frameRedraw = frameRedraw);
	}

	public int getSkip() {
		return skip;
	}

	public void setSkip(int skip) {
		firePropertyChange("skip", this.skip, this.skip = skip);
	}

	public void setHashButtonEnabled(boolean[] hashButtonEnabled) {
		firePropertyChange("hashButtonEnabled", this.hashButtonEnabled, this.hashButtonEnabled = hashButtonEnabled);
	}

	public boolean isMarkEndEnabled() {
		return markEndEnabled;
	}

	public void setMarkEndEnabled(boolean markEndEnabled) {
		firePropertyChange("markEndEnabled", this.markEndEnabled, this.markEndEnabled = markEndEnabled);
	}

	public String getStartStopLabel() {
		return startStopLabel;
	}

	public void setStartStopLabel(String startStopLabel) {
		firePropertyChange("startStopLabel", this.startStopLabel, this.startStopLabel = startStopLabel);
	}

	public int getSelectedMaskNumber() {
		return selectedMaskNumber;
	}

	public void setSelectedMaskNumber(int selectedMaskNumber) {
		firePropertyChange("selectedMaskNumber", this.selectedMaskNumber, this.selectedMaskNumber = selectedMaskNumber);
	}

	public boolean isMaskNumberEnabled() {
		return maskNumberEnabled;
	}

	public void setMaskNumberEnabled(boolean maskNumberEnabled) {
		firePropertyChange("maskNumberEnabled", this.maskNumberEnabled, this.maskNumberEnabled = maskNumberEnabled);
	}

	public int getSelectedEventHigh() {
		return selectedEventHigh;
	}

	public void setSelectedEventHigh(int selectedEventHigh) {
		firePropertyChange("selectedEventHigh", this.selectedEventHigh, this.selectedEventHigh = selectedEventHigh);
	}

	public int getSelectedEventLow() {
		return selectedEventLow;
	}

	public void setSelectedEventLow(int selectedEventLow) {
		firePropertyChange("selectedEventLow", this.selectedEventLow, this.selectedEventLow = selectedEventLow);
	}

	public int getSelectedFrame() {
		return selectedFrame;
	}

	public void setSelectedFrame(int selectedFrame) {
		firePropertyChange("selectedFrame", this.selectedFrame, this.selectedFrame = selectedFrame);
	}

	public ObservableList<TypedLabel> getKeyframes() {
		return keyframes;
	}

	public void setKeyframes(ObservableList<TypedLabel> keyframes) {
		firePropertyChange("keyframes", this.keyframes, this.keyframes = keyframes);
	}

	public ObservableList<CompiledAnimation> getScenes() {
		return scenes;
	}

	public void setScenes(ObservableList<CompiledAnimation> scenes) {
		firePropertyChange("scenes", this.scenes, this.scenes = scenes);
	}

	public CompiledAnimation getSelectedScene() {
		return selectedScene;
	}

	public void setSelectedScene(CompiledAnimation selectedScene) {
		firePropertyChange("selectedScene", this.selectedScene, this.selectedScene = selectedScene);
	}

	public Animation getSelectedRecording() {
		return selectedRecording;
	}

	public void setSelectedRecording(Animation selectedRecording) {
		firePropertyChange("selectedRecording", this.selectedRecording, this.selectedRecording = selectedRecording);
	}

	public ObservableList<Animation> getRecordings() {
		return recordings;
	}

	public void setRecordings(ObservableList<Animation> recordings) {
		firePropertyChange("recordings", this.recordings, this.recordings = recordings);
	}

	public ObservableList<Palette> getPalettes() {
		return palettes;
	}

	public void setPalettes(ObservableList<Palette> palettes) {
		firePropertyChange("palettes", this.palettes, this.palettes = palettes);
	}

}
