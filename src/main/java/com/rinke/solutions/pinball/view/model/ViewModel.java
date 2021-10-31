package com.rinke.solutions.pinball.view.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.databinding.ViewBinding;
import com.rinke.solutions.pinball.CutInfo;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.util.ObservableSet;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

@Bean
@Getter
public class ViewModel extends AbstractModel {
	
	public void resetMask(DmdSize size, int numberOfMasks) {
		masks.clear();
		for( int i = 0; i < numberOfMasks; i++) {
			masks.add( new Mask(size.planeSize) );
		}
	}

	public int numberOfHashButtons = 4;
	@Value public int numberOfColors = 64;
	@Value public int noOfPlanesWhenCutting = 6;
	public boolean has4PlanesRecording = false;
	
	public boolean dirty;
	public DmdSize dmdSize;
	public DmdSize srcDmdSize;
	public DmdSize prjDmdSize;
	public String pin2dmdAdress;
	public String projectFilename;
	public CutInfo cutInfo = new CutInfo();
	public Map<String,Integer> scenesPosMap = new HashMap<String, Integer>();
	public Map<String,Set<Bookmark>> bookmarksMap = new HashMap<String, Set<Bookmark>>();
	public List<String> inputFiles = new ArrayList<>();
	public HashMap<String,String> recordingNameMap = new HashMap<>();
	
	public List<Palette> previewPalettes = Palette.previewPalettes();
	
	// view should listen and update Menu
	public String recentPalette;
	public String recentProjects;
	public String recentAnimations;

	public byte[] emptyMask;
	
	// animation, listener in der UI Klasse
	public int nextTimerExec;
	public boolean shouldClose;
//	private List<EditMode> immutable = Arrays.asList( Animation.EditMode.FIXED );
	
	public void init(DMD dmd, DmdSize ds, DmdSize prjSize, String address, int noOfMasks, Config config) {
		this.dmd = dmd;
		setDmdSize(ds);
		setPrjDmdSize(prjSize);
		setSrcDmdSize(prjSize);
		setSelectedPalette( paletteMap.get(0) );
		setPin2dmdAdress( address );
		setProjectFilename(null);
		setDirty(false);
		Palette.getDefaultPalettes(numberOfColors).stream().forEach(p->paletteMap.put(p.index, p));
		setMaxNumberOfMasks(noOfMasks);
		setSelectedPalette(paletteMap.values().iterator().next());
		resetMask(ds, noOfMasks);
		//availableEditModes.replaceAll(immutable);
		//setSelectedEditMode(EditMode.FIXED); // if initialized to FIXED fire propchange is suppressed
	}
	
	// drawing
	@ViewBinding public Rect selection;
	
	@ViewBinding public int selectedSpinnerDeviceId;
	@ViewBinding public int selectedSpinnerEventId;
	
	@ViewBinding public boolean deleteRecordingEnabled;
	@ViewBinding public boolean deleteSceneEnabled;
	@ViewBinding public boolean deleteKeyFrameEnabled;
	@ViewBinding public boolean fetchDurationEnabled;
	@ViewBinding public boolean durationEnabled;
	@ViewBinding public boolean setKeyFramePalEnabled;
	@ViewBinding public boolean setFixKeyFramesEnabled;
	@ViewBinding public boolean drawingEnabled;
	@ViewBinding public boolean copyToNextEnabled;
	@ViewBinding public boolean copyToPrevEnabled;
	@ViewBinding public boolean undoEnabled;
	@ViewBinding public boolean redoEnabled;
	@ViewBinding public boolean btnInvertEnabled;
	@ViewBinding public boolean deleteColMaskEnabled;
	@ViewBinding public boolean btnLinkEnabled;
	
	public int duration;
	
	@ViewBinding public int paletteToolPlanes;
	public int lastTimeCode;
	public int saveTimeCode;
	
	@ViewBinding public boolean detectionMaskEnabled;
	@ViewBinding public boolean layerMaskEnabled;
	@ViewBinding public boolean detectionMaskActive;
	@ViewBinding public boolean layerMaskActive;
	@ViewBinding public boolean showMask;
	
	@ViewBinding public int selectedMaskNumber;
	@ViewBinding public boolean maskSpinnerEnabled;
	@ViewBinding public int maxNumberOfMasks;
	
	@ViewBinding public int selectedToolSize = 1;
	@ViewBinding public boolean toolSizeSpinnerEnabled;
	
	@ViewBinding public boolean smartDrawEnabled;
	@ViewBinding public boolean smartDrawActive;

	public ObservableList<Mask> masks = new ObservableList<>(new ArrayList<>());
	
	public boolean dmdDirty;
	public boolean paletteDirty;
	public DMD dmd = new DMD(128,32);
	public DMD previewDMD = null;

	@ViewBinding public int numberOfPlanes = 2;
	
	public List<byte[]> hashes = new ArrayList<byte[]>(numberOfHashButtons);
	public String hashLbl[] = new String[numberOfHashButtons];
	public boolean[] hashButtonSelected = new boolean[numberOfHashButtons];
	public boolean[] hashButtonEnabled = new boolean[numberOfHashButtons];
	public boolean hashButtonsEnabled;
	public int selectedHashIndex;

	@ViewBinding public boolean markStartEnabled;
	@ViewBinding public boolean markEndEnabled;
	@ViewBinding public boolean cutSceneEnabled;
	
	@ViewBinding public Bookmark selectedBookmark;
	@ViewBinding public ObservableSet<Bookmark> bookmarks = new ObservableSet<>(new TreeSet<>());
	@ViewBinding public String editedBookmarkName;
	@ViewBinding public String editedPaletteName;
	@ViewBinding public boolean bookmarkComboEnabled;
	@ViewBinding public boolean btnDelBookmarkEnabled;
	@ViewBinding public boolean btnNewBookmarkEnabled;
	@ViewBinding public boolean btnDelFrameEnabled;
	@ViewBinding public boolean btnAdd2SceneEnabled;
	
	@ViewBinding public boolean btnSetHashEnabled;
	@ViewBinding public boolean btnAddKeyframeEnabled;
	@ViewBinding public boolean btnAddFrameSeqEnabled;
	@ViewBinding public boolean btnAddEventEnabled;
	@ViewBinding public boolean btnPreviewNextEnabled;
	@ViewBinding public boolean btnPreviewPrevEnabled;
	@ViewBinding public boolean btnCheckKeyframeEnabled;
	@ViewBinding public String btnAddFrameSeqLabel = "Color Scene";
	@ViewBinding public boolean btnSetScenePalEnabled;
	
	@ViewBinding public boolean livePreviewActive;
	@ViewBinding public boolean mntmUploadPalettesEnabled;
	@ViewBinding public boolean mntmUploadProjectEnabled = true;
	
	@ViewBinding public boolean cutEnabled = true;
	@ViewBinding public boolean copyEnabled = true;
	
	@ViewBinding public ObservableList<EditMode> availableEditModes = new ObservableList<>(new ArrayList<>());
	public EditMode selectedEditMode;
	@ViewBinding public EditMode suggestedEditMode;
	@ViewBinding public PaletteType selectedPaletteType;
	
	// animation stuff
	public ObservableList<Animation> playingAnis = new ObservableList<Animation>(new ArrayList<>());
	public boolean animationIsPlaying;
	public int playSpeed = 1;
	
	@ViewBinding public int minFrame;
	@ViewBinding public int selectedFrame;
	@ViewBinding public int selectedLinkFrame;
	public String selectedLinkRecordingName;
	
	public int linkedFrameOffset = 0;
	@ViewBinding public int maxFrame;
	public int frameIncrement;
	
	@ViewBinding public Palette previewDmdPalette = previewPalettes.get(0);
	
	@ViewBinding public int delay;
	@ViewBinding public int timecode;
	@ViewBinding public String linkVal;
	@ViewBinding public String hashVal;
	@ViewBinding public boolean keyFrame;
	
	@ViewBinding public boolean startStopEnabled = true;
	@ViewBinding public String startStopLabel = "Start";
	
	@ViewBinding public boolean btnPrevEnabled;
	@ViewBinding public boolean btnNextEnabled;
	
	// Listen
	@ViewBinding public Animation selectedRecording;
	@ViewBinding public ObservableMap<String, Animation> recordings = new ObservableMap<>(new LinkedHashMap<>());
	@ViewBinding public CompiledAnimation selectedScene;
	@ViewBinding public ObservableMap<String, CompiledAnimation> scenes = new ObservableMap<String, CompiledAnimation>(new LinkedHashMap<>());
	@ViewBinding public PalMapping selectedKeyFrame;
	@ViewBinding public ObservableMap<String, PalMapping> keyframes = new ObservableMap<>(new LinkedHashMap<>());

	@ViewBinding public Animation selectedFrameSeq;
	// try to bound it to scenes as well
	//@ViewBinding public ObservableList<Animation> frameSeqList = new ObservableList<>(new ArrayList<>());
	
	@ViewBinding public Palette selectedPalette = Palette.getDefaultPalettes(numberOfColors).get(0);
	@ViewBinding public ObservableMap<Integer,Palette> paletteMap = new ObservableMap<>(new LinkedHashMap<>());
	@ViewBinding public int selectedColor;
	
	public int loadedAniVersion; 	// remember ani version wenn loading a project, to support messaging for incompatible changes
	
	public void setDmdSize(DmdSize dmdSize) {
		firePropertyChange("dmdSize", this.dmdSize, this.dmdSize = dmdSize);
	}

	public void setSrcDmdSize(DmdSize dmdSize) {
		firePropertyChange("srcDmdSize", this.srcDmdSize, this.srcDmdSize = dmdSize);
	}

	public void setDirty(boolean dirty) {
		firePropertyChange("dirty", this.dirty, this.dirty = dirty);
	}

	public void setPin2dmdAdress(String pin2dmdAdress) {
		firePropertyChange("pin2dmdAdress", this.pin2dmdAdress, this.pin2dmdAdress = pin2dmdAdress);
	}

	public void setProjectFilename(String projectFilename) {
		firePropertyChange("projectFilename", this.projectFilename, this.projectFilename = projectFilename);
	}

	public void setCutInfo(CutInfo cutInfo) {
		firePropertyChange("cutInfo", this.cutInfo, this.cutInfo = cutInfo);
	}

	public void setDeleteRecordingEnabled(boolean e) {
		firePropertyChange("deleteRecordingEnabled", this.deleteRecordingEnabled, this.deleteRecordingEnabled = e);
	}

	public void setDeleteSceneEnabled(boolean deleteSceneEnabled) {
		firePropertyChange("deleteSceneEnabled", this.deleteSceneEnabled, this.deleteSceneEnabled = deleteSceneEnabled);
	}

	public void setDeleteKeyFrameEnabled(boolean deleteKeyFrameEnabled) {
		firePropertyChange("deleteKeyFrameEnabled", this.deleteKeyFrameEnabled, this.deleteKeyFrameEnabled = deleteKeyFrameEnabled);
	}

	public void setFetchDurationEnabled(boolean fetchDurationEnabled) {
		firePropertyChange("fetchDurationEnabled", this.fetchDurationEnabled, this.fetchDurationEnabled = fetchDurationEnabled);
	}

	public void setDurationEnabled(boolean durationEnabled) {
		firePropertyChange("durationEnabled", this.durationEnabled, this.durationEnabled = durationEnabled);
	}

	public void setSetKeyFramePalEnabled(boolean setKeyFramePalEnabled) {
		firePropertyChange("setKeyFramePalEnabled", this.setKeyFramePalEnabled, this.setKeyFramePalEnabled = setKeyFramePalEnabled);
	}

	public void setSetFixKeyFramesEnabled(boolean setFixKeyFramesEnabled) {
		firePropertyChange("setFixKeyFramesEnabled", this.setFixKeyFramesEnabled, this.setFixKeyFramesEnabled = setFixKeyFramesEnabled);
	}
	
	public void setDrawingEnabled(boolean drawingEnabled) {
		firePropertyChange("drawingEnabled", this.drawingEnabled, this.drawingEnabled = drawingEnabled);
	}

	public void setCopyToNextEnabled(boolean copyToNextEnabled) {
		firePropertyChange("copyToNextEnabled", this.copyToNextEnabled, this.copyToNextEnabled = copyToNextEnabled);
	}

	public void setCopyToPrevEnabled(boolean copyToPrevEnabled) {
		firePropertyChange("copyToPrevEnabled", this.copyToPrevEnabled, this.copyToPrevEnabled = copyToPrevEnabled);
	}

	public void setAnimationIsPlaying(boolean animationIsPlaying) {
		firePropertyChange("animationIsPlaying", this.animationIsPlaying, this.animationIsPlaying = animationIsPlaying);
	}

	public void setMinFrame(int minFrame) {
		firePropertyChange("minFrame", this.minFrame, this.minFrame = minFrame);
	}

	public void setSelectedFrame(int selectedFrame) {
		firePropertyChange("selectedFrame", this.selectedFrame, this.selectedFrame = selectedFrame);
	}

	public void setSelectedLinkFrame(int selectedLinkFrame) {
		firePropertyChange("selectedLinkFrame", this.selectedLinkFrame, this.selectedLinkFrame = selectedLinkFrame);
	}
	
	public void setMaxFrame(int maxFrame) {
		firePropertyChange("maxFrame", this.maxFrame, this.maxFrame = maxFrame);
	}

	public void setDelay(int delay) {
		firePropertyChange("delay", this.delay, this.delay = delay);
	}

	public void setTimecode(int timecode) {
		firePropertyChange("timecode", this.timecode, this.timecode = timecode);
	}

	public void setStartStopEnabled(boolean startStopEnabled) {
		firePropertyChange("startStopEnabled", this.startStopEnabled, this.startStopEnabled = startStopEnabled);
	}

	public void setStartStopLabel(String startStopLabel) {
		firePropertyChange("startStopLabel", this.startStopLabel, this.startStopLabel = startStopLabel);
	}

	public void setFrameIncrement(int frameIncrement) {
		firePropertyChange("frameIncrement", this.frameIncrement, this.frameIncrement = frameIncrement);
	}

	public void setUndoEnabled(boolean undoEnabled) {
		firePropertyChange("undoEnabled", this.undoEnabled, this.undoEnabled = undoEnabled);
	}

	public void setRedoEnabled(boolean redoEnabled) {
		firePropertyChange("redoEnabled", this.redoEnabled, this.redoEnabled = redoEnabled);
	}

	public void setDmdDirty(boolean dmdDirty) {
		firePropertyChange("dmdDirty", this.dmdDirty, this.dmdDirty = dmdDirty);
	}

	public void setSelectedRecording(Animation selectedRecording) {
		firePropertyChange("selectedRecording", this.selectedRecording, this.selectedRecording = selectedRecording);
	}

	public void setSelectedScene(CompiledAnimation selectedScene) {
		firePropertyChange("selectedScene", this.selectedScene, this.selectedScene = selectedScene);
	}

	public void setSelectedKeyFrame(PalMapping selectedKeyFrame) {
		firePropertyChange("selectedKeyFrame", this.selectedKeyFrame, this.selectedKeyFrame = selectedKeyFrame);
	}

	public void setSelectedFrameSeq(CompiledAnimation selectedFrameSeq) {
		firePropertyChange("selectedFrameSeq", this.selectedFrameSeq, this.selectedFrameSeq = selectedFrameSeq);
	}

	public void setSelectedPalette(Palette selectedPalette) {
		firePropertyChange("selectedPalette", this.selectedPalette, this.selectedPalette = selectedPalette);
	}

	public void setSceneCutEnabled(boolean cutEnabled) {
		firePropertyChange("cutSceneEnabled", this.cutSceneEnabled, this.cutSceneEnabled = cutEnabled);
	}

	public void setMarkEndEnabled(boolean markEndEnabled) {
		firePropertyChange("markEndEnabled", this.markEndEnabled, this.markEndEnabled = markEndEnabled);
	}

	public void setMarkStartEnabled(boolean markStartEnabled) {
		firePropertyChange("markStartEnabled", this.markStartEnabled, this.markStartEnabled = markStartEnabled);
	}

	public void setBookmarks(ObservableSet<Bookmark> bookmarks) {
		firePropertyChange("bookmarks", this.bookmarks, this.bookmarks = bookmarks);
	}

	public void setRecordings(ObservableMap<String, Animation> recordings) {
		firePropertyChange("recordings", this.recordings, this.recordings = recordings);
	}

	public void setScenes(ObservableMap<String, CompiledAnimation> scenes) {
		firePropertyChange("scenes", this.scenes, this.scenes = scenes);
	}

	public void setEditedBookmarkName(String editedBookmarkName) {
		firePropertyChange("editedBookmarkName", this.editedBookmarkName, this.editedBookmarkName = editedBookmarkName);
	}

	public void setSelectedBookmark(Bookmark selectedBookmark) {
		firePropertyChange("selectedBookmark", this.selectedBookmark, this.selectedBookmark = selectedBookmark);
	}

	public void setBookmarkComboEnabled(boolean bookmarkComboEnabled) {
		firePropertyChange("bookmarkComboEnabled", this.bookmarkComboEnabled, this.bookmarkComboEnabled = bookmarkComboEnabled);
	}

	public void setBtnNewBookmarkEnabled(boolean btnNewBookmarkEnabled) {
		firePropertyChange("btnNewBookmarkEnabled", this.btnNewBookmarkEnabled, this.btnNewBookmarkEnabled = btnNewBookmarkEnabled);
	}

	public void setBtnDelBookmarkEnabled(boolean btnDelBookmarkEnabled) {
		firePropertyChange("btnDelBookmarkEnabled", this.btnDelBookmarkEnabled, this.btnDelBookmarkEnabled = btnDelBookmarkEnabled);
	}

	public void setBtnDelFrameEnabled(boolean btnDelFrameEnabled) {
		firePropertyChange("btnDelFrameEnabled", this.btnDelFrameEnabled, this.btnDelFrameEnabled = btnDelFrameEnabled);
	}

	public void setAdd2SceneEnabled(boolean btnAdd2SceneEnabled) {
		firePropertyChange("btnAdd2SceneEnabled", this.btnAdd2SceneEnabled, this.btnAdd2SceneEnabled = btnAdd2SceneEnabled);
	}
	public void setBtnAddKeyframeEnabled(boolean btnAddKeyframeEnabled) {
		firePropertyChange("btnAddKeyframeEnabled", this.btnAddKeyframeEnabled, this.btnAddKeyframeEnabled = btnAddKeyframeEnabled);
	}

	public void setBtnAddFrameSeqEnabled(boolean btnAddFrameSeqEnabled) {
		firePropertyChange("btnAddFrameSeqEnabled", this.btnAddFrameSeqEnabled, this.btnAddFrameSeqEnabled = btnAddFrameSeqEnabled);
	}

	public void setBtnPreviewNextEnabled(boolean btnPreviewNextEnabled) {
		firePropertyChange("btnPreviewNextEnabled", this.btnPreviewNextEnabled, this.btnPreviewNextEnabled = btnPreviewNextEnabled);
	}

	public void setBtnPreviewPrevEnabled(boolean btnPreviewPrevEnabled) {
		firePropertyChange("btnPreviewPrevEnabled", this.btnPreviewPrevEnabled, this.btnPreviewPrevEnabled = btnPreviewPrevEnabled);
	}

	public void setBtnCheckKeyframeEnabled(boolean btnCheckKeyframeEnabled) {
		firePropertyChange("btnCheckKeyframeEnabled", this.btnCheckKeyframeEnabled, this.btnCheckKeyframeEnabled = btnCheckKeyframeEnabled);
	}

	public void setBtnAddEventEnabled(boolean btnAddEventEnabled) {
		firePropertyChange("btnAddEventEnabled", this.btnAddEventEnabled, this.btnAddEventEnabled = btnAddEventEnabled);
	}

	public void setAvailableEditModes(ObservableList<EditMode> availableEditModes) {
		firePropertyChange("availableEditModes", this.availableEditModes, this.availableEditModes = availableEditModes);
	}

	public void setSelectedEditMode(EditMode selectedEditMode) {
		firePropertyChange("selectedEditMode", this.selectedEditMode, this.selectedEditMode = selectedEditMode);
	}

	public void setNumberOfPlanes(int numberOfPlanes) {
		firePropertyChange("numberOfPlanes", this.numberOfPlanes, this.numberOfPlanes = numberOfPlanes);
	}

	public void setHashLbl(String[] hashLbl) {
		firePropertyChange("hashLbl", this.hashLbl, this.hashLbl = hashLbl);
	}

	public void setHashes(List<byte[]> hashes) {
		firePropertyChange("hashes", this.hashes, this.hashes = hashes);
	}

	public void setHashButtonEnabled(boolean[] hashButtonEnabled) {
		firePropertyChange("hashButtonEnabled", this.hashButtonEnabled, this.hashButtonEnabled = hashButtonEnabled);
	}

	public void setHashButtonsEnabled(boolean hashButtonsEnabled) {
		firePropertyChange("hashButtonsEnabled", this.hashButtonsEnabled, this.hashButtonsEnabled = hashButtonsEnabled);
	}

	public void setHashButtonSelected(boolean[] hashButtonSelected) {
		firePropertyChange("hashButtonSelected", this.hashButtonSelected, this.hashButtonSelected = hashButtonSelected);
	}

	public void setSelectedHashIndex(int selectedHashIndex) {
		firePropertyChange("selectedHashIndex", this.selectedHashIndex, this.selectedHashIndex = selectedHashIndex);
	}

	public void setPreviewDmdPalette(Palette previewDmdPalette) {
		firePropertyChange("previewDmdPalette", this.previewDmdPalette, this.previewDmdPalette = previewDmdPalette);
	}

	public void setBtnInvertEnabled(boolean btnInvertEnabled) {
		firePropertyChange("btnInvertEnabled", this.btnInvertEnabled, this.btnInvertEnabled = btnInvertEnabled);
	}

	public void setPaletteToolPlanes(int paletteToolPlanes) {
		firePropertyChange("paletteToolPlanes", this.paletteToolPlanes, this.paletteToolPlanes = paletteToolPlanes);
	}

	public void setSelection(Rect selection) {
		firePropertyChange("selection", this.selection, this.selection = selection);
	}

	public void setDeleteColMaskEnabled(boolean deleteColMaskEnabled) {
		firePropertyChange("deleteColMaskEnabled", this.deleteColMaskEnabled, this.deleteColMaskEnabled = deleteColMaskEnabled);
	}

	public void setMaskSpinnerEnabled(boolean maskSpinnerEnabled) {
		firePropertyChange("maskSpinnerEnabled", this.maskSpinnerEnabled, this.maskSpinnerEnabled = maskSpinnerEnabled);
	}
	
	public void setSelectedMaskNumber(int selectedMaskNumber) {
		firePropertyChange("selectedMaskNumber", this.selectedMaskNumber, -1); //hack to commit mask before change
		firePropertyChange("selectedMaskNumber", this.selectedMaskNumber, this.selectedMaskNumber = selectedMaskNumber);
	}
	
	public void setMaxNumberOfMasks(int maxNumberOfMasks) {
		firePropertyChange("maxNumberOfMasks", this.maxNumberOfMasks, this.maxNumberOfMasks = maxNumberOfMasks);
	}

	public void setSelectedToolSize(int selectedToolSize) {
		firePropertyChange("selectedToolSize", this.selectedToolSize, this.selectedToolSize = selectedToolSize);
	}
	
	public void setToolSizeSpinnerEnabled(boolean toolSizeSpinnerEnabled) {
		firePropertyChange("toolSizeSpinnerEnabled", this.toolSizeSpinnerEnabled, this.toolSizeSpinnerEnabled = toolSizeSpinnerEnabled);
	}
	
	public void setSmartDrawEnabled(boolean smartDrawEnabled) {
		firePropertyChange("smartDrawEnabled", this.smartDrawEnabled, this.smartDrawEnabled = smartDrawEnabled);
	}
	
	public void setSmartDrawActive(boolean smartDrawActive) {
		firePropertyChange("smartDrawActive", this.smartDrawActive, this.smartDrawActive = smartDrawActive);
	}
	
	public void setBtnSetScenePalEnabled(boolean btnSetScenePalEnabled) {
		firePropertyChange("btnSetScenePalEnabled", this.btnSetScenePalEnabled, this.btnSetScenePalEnabled = btnSetScenePalEnabled);
	}

	public void setLastTimeCode(int lastTimeCode) {
		firePropertyChange("lastTimeCode", this.lastTimeCode, this.lastTimeCode = lastTimeCode);
	}

	public void setSaveTimeCode(int saveTimeCode) {
		firePropertyChange("saveTimeCode", this.saveTimeCode, this.saveTimeCode = saveTimeCode);
	}

	public void setSelectedPaletteType(PaletteType selectedPaletteType) {
		firePropertyChange("selectedPaletteType", this.selectedPaletteType, this.selectedPaletteType = selectedPaletteType);
	}

	public void setPaletteMap(ObservableMap<Integer, Palette> paletteMap) {
		firePropertyChange("paletteMap", this.paletteMap, this.paletteMap = paletteMap);
	}

	public void setDuration(int duration) {
		firePropertyChange("duration", this.duration, this.duration = duration);
	}

	public void setSelectedPaletteByIndex(int palIndex) {
		Optional<Palette> optPal = paletteMap.values().stream().filter(p -> p.index==palIndex).findFirst();
		setSelectedPalette(optPal.orElse(selectedPalette));
	}

	public void setSelectedSpinnerDeviceId(int selectedSpinnerDeviceId) {
		firePropertyChange("selectedSpinnerDeviceId", this.selectedSpinnerDeviceId, this.selectedSpinnerDeviceId = selectedSpinnerDeviceId);
	}

	public void setSelectedSpinnerEventId(int selectedSpinnerEventId) {
		firePropertyChange("selectedSpinnerEventId", this.selectedSpinnerEventId, this.selectedSpinnerEventId = selectedSpinnerEventId);
	}

	public void setSelectedFrameSeq(Animation selectedFrameSeq) {
		firePropertyChange("selectedFrameSeq", this.selectedFrameSeq, this.selectedFrameSeq = selectedFrameSeq);
	}

	public void setLivePreviewActive(boolean livePreviewActive) {
		firePropertyChange("livePreviewActive", this.livePreviewActive, this.livePreviewActive = livePreviewActive);
	}

	public void setPlaySpeed(int playSpeed) {
		firePropertyChange("playSpeed", this.playSpeed, this.playSpeed = playSpeed);
	}
	
	public void setMntmUploadPalettesEnabled(boolean mntmUploadPalettesEnabled) {
		firePropertyChange("mntmUploadPalettesEnabled", this.mntmUploadPalettesEnabled, this.mntmUploadPalettesEnabled = mntmUploadPalettesEnabled);
	}

	public void setMntmUploadProjectEnabled(boolean mntmUploadProjectEnabled) {
		firePropertyChange("mntmUploadProjectEnabled", this.mntmUploadProjectEnabled, this.mntmUploadProjectEnabled = mntmUploadProjectEnabled);
	}

	public void setEditedPaletteName(String editedPaletteName) {
		firePropertyChange("editedPaletteName", this.editedPaletteName, this.editedPaletteName = editedPaletteName);
	}

	public void setRecentPalette(String recentPalette) {
		firePropertyChange("recentPalette", this.recentPalette, this.recentPalette = recentPalette);
	}

	public void setNextTimerExec(int nextTimerExec) {
		firePropertyChange("nextTimerExec", this.nextTimerExec, this.nextTimerExec = nextTimerExec);
	}

	public void setShouldClose(boolean shouldClose) {
		firePropertyChange("shouldClose", this.shouldClose, this.shouldClose = shouldClose);
	}

	public void setRecentProjects(String recentProjects) {
		firePropertyChange("recentProjects", this.recentProjects, this.recentProjects = recentProjects);
	}

	public void setEmptyMask(byte[] emptyMask) {
		firePropertyChange("emptyMask", this.emptyMask, this.emptyMask = emptyMask);
	}

	public void setRecentAnimations(String recentAnimations) {
		firePropertyChange("recentAnimations", this.recentAnimations, this.recentAnimations = recentAnimations);
	}

	public void setBookmarksMap(Map<String, Set<Bookmark>> bookmarksMap) {
		firePropertyChange("bookmarksMap", this.bookmarksMap, this.bookmarksMap = bookmarksMap);
	}

	public void setBtnNextEnabled(boolean btnNextEnabled) {
		firePropertyChange("btnNextEnabled", this.btnNextEnabled, this.btnNextEnabled = btnNextEnabled);
	}

	public void setBtnPrevEnabled(boolean btnPrevEnabled) {
		firePropertyChange("btnPrevEnabled", this.btnPrevEnabled, this.btnPrevEnabled = btnPrevEnabled);
	}

	public void setDetectionMaskEnabled(boolean detectionMaskEnabled) {
		firePropertyChange("detectionMaskEnabled", this.detectionMaskEnabled, this.detectionMaskEnabled = detectionMaskEnabled);
	}

	public void setLayerMaskEnabled(boolean layerMaskEnabled) {
		firePropertyChange("layerMaskEnabled", this.layerMaskEnabled, this.layerMaskEnabled = layerMaskEnabled);
	}

	public void setDetectionMaskActive(boolean detectionMaskActive) {
		firePropertyChange("detectionMaskActive", this.detectionMaskActive, this.detectionMaskActive = detectionMaskActive);
	}

	public void setLayerMaskActive(boolean layerMaskActive) {
		firePropertyChange("layerMaskActive", this.layerMaskActive, this.layerMaskActive = layerMaskActive);
	}

	public void setCutEnabled(boolean cutEnabled) {
		firePropertyChange("cutEnabled", this.cutEnabled, this.cutEnabled = cutEnabled);
	}

	public void setCopyEnabled(boolean copyEnabled) {
		firePropertyChange("copyEnabled", this.copyEnabled, this.copyEnabled = copyEnabled);
	}

	public void setSuggestedEditMode(EditMode suggestedEditMode) {
		firePropertyChange("suggestedEditMode", this.suggestedEditMode, this.suggestedEditMode = suggestedEditMode);
	}

	public void setShowMask(boolean showMask) {
		firePropertyChange("showMask", this.showMask, this.showMask = showMask);
	}

	public void setBtnAddFrameSeqLabel(String btnAddFrameSeqLabel) {
		firePropertyChange("btnAddFrameSeqLabel", this.btnAddFrameSeqLabel, this.btnAddFrameSeqLabel = btnAddFrameSeqLabel);
	}

	public void setPaletteDirty(boolean paletteDirty) {
		firePropertyChange("paletteDirty", this.paletteDirty, this.paletteDirty = paletteDirty);
	}

	public void setSelectedColor(int selectedColor) {
		firePropertyChange("selectedColor", this.selectedColor, this.selectedColor = selectedColor);
	}

	public void setPreviewDMD(DMD previewDMD) {
		firePropertyChange("previewDMD", this.previewDMD, this.previewDMD = previewDMD);
	}

	public void setHashVal(String hashVal) {
		firePropertyChange("hashVal", this.hashVal, this.hashVal = hashVal);
	}
	
	public void setKeyFrame(boolean keyFrame) {
		firePropertyChange("keyFrame", this.keyFrame, this.keyFrame = keyFrame);
	}

	public void setLinkVal(String linkVal) {
		firePropertyChange("linkVal", this.linkVal, this.linkVal = linkVal);
	}

	public void setBtnSetHashEnabled(boolean btnSetHashEnabled) {
		firePropertyChange("btnSetHashEnabled", this.btnSetHashEnabled, this.btnSetHashEnabled = btnSetHashEnabled);
	}

	public void setBtnLinkEnabled(boolean btnLinkEnabled) {
		firePropertyChange("btnLinkEnabled", this.btnLinkEnabled, this.btnLinkEnabled = btnLinkEnabled);
	}

	public void setLoadedAniVersion(int loadedAniVersion) {
		firePropertyChange("loadedAniVersion", this.loadedAniVersion, this.loadedAniVersion = loadedAniVersion);
	}

	public void setLinkedFrameOffset(int linkedFrameOffset) {
		firePropertyChange("linkedFrameOffset", this.linkedFrameOffset, this.linkedFrameOffset = linkedFrameOffset);
	}

	public void setPrjDmdSize(DmdSize prjDmdSize) {
		firePropertyChange("prjDmdSize", this.prjDmdSize, this.prjDmdSize = prjDmdSize);
	}

}