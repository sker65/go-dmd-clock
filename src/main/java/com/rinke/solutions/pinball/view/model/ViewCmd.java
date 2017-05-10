package com.rinke.solutions.pinball.view.model;

public class ViewCmd {
	
	// palette handler
	public static final String DELETE_PALETTE = "deletePalette";
	public static final String RENAME_PALETTE = "renamePalette";
	public static final String NEW_PALETTE = "newPalette";
	public static final String APPLY_PALETTE = "applyPalette";
	public static final String UPLOAD_PALETTE = "uploadPalette";
	public static final String SAVE_PALETTE = "savePalette";
	public static final String LOAD_PALETTE = "loadPalette";

	// bookmark handler
	public static final String DELETE_BOOKMARK = "deleteBookmark";
	public static final String NEW_BOOKMARK = "newBookmark";
	public static final String SELECTED_BOOKMARK = "selectedBookmark";
	
	// drawing handler
	public static final String CUT_SCENE = "cutScene";
	public static final String DELAY_TXT_CHANGED = "delayTxtChanged";
	public static final String REDO = "redo";
	public static final String UNDO = "undo";
	public static final String DELETE_MASK = "deleteMask";
	public static final String COPY_AND_MOVE_TO_NEXT_FRAME = "copyAndMoveToNextFrame";
	public static final String COPY_AND_MOVE_TO_PREV_FRAME = "copyAndMoveToPrevFrame";
	public static final String INVERT_MASK = "invertMask";
	public static final String MASK_ACTIVE = "maskActive";
		
	// scene handler
	public static final String SORT_SCENES = "sortScenes";
	public static final String DELETE_SCENE = "deleteScene";

	// playing ani handler
	public static final String FRAME_CHANGED = "frameChanged";
	public static final String NEXT_FRAME = "nextFrame";
	public static final String PREV_FRAME = "prevFrame";
	public static final String START_STOP = "startStop";

	// recordings handler
	public static final String SORT_RECORDING = "sortRecording";
	public static final String DELETE_RECORDING = "deleteRecording";
	
	// menu handler
	public static final String ABOUT = "about";
	public static final String DEVICE_CONFIGURATION = "deviceConfiguration";
	public static final String CONFIGURATION = "configuration";
	public static final String QUIT = "quit";
	
	// ani handler
	public static final String LOAD_ANI = "loadAni";
	public static final String SAVE_SINGLE_ANI_WITH_FC = "saveSingleAniWithFC";
	public static final String SAVE_ANI_WITH_FC = "saveAniWithFC";
	public static final String LOAD_ANI_WITH_FC = "loadAniWithFC";
	
	// clipboard handler
	public static final String REMOVE_SELECTION = "removeSelection";
	public static final String SELECT_ALL = "selectAll";
	public static final String PASTE_HOOVER = "pasteHoover";
	public static final String PASTE = "paste";
	public static final String COPY = "copy";
	public static final String CUT = "cut";
	
	// export handler
	public static final String EXPORT_VPIN_PROJECT = "exportVpinProject";
	public static final String EXPORT_REALPIN_PROJECT = "exportRealpinProject";
	public static final String EXPORT_GO_DMD = "exportGoDMD";
	public static final String EXPORT_GIF = "exportGif";
	
	// project handler
	public static final String IMPORT_PROJECT = "importProject";
	public static final String SAVE_PROJECT = "saveProject";
	public static final String LOAD_PROJECT = "loadProject";
	public static final String NEW_PROJECT = "newProject";
	public static final String UPLOAD_PROJECT = "uploadProject";
	
	// keyframe handler
	public static final String FETCH_DURATION = "fetchDuration";
	public static final String ADD_FRAME_SEQ = "addFrameSeq";
	public static final String ADD_KEY_FRAME = "addKeyFrame";
	public static final String HASH_SELECTED = "hashSelected";
	public static final String SORT_KEY_FRAMES = "sortKeyFrames";
	public static final String DELETE_KEY_FRAME = "deleteKeyFrame";

}
