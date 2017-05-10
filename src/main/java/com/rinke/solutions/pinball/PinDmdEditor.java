package com.rinke.solutions.pinball;

import java.awt.SplashScreen;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.beans.SimpleBeanFactory;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.api.BinaryExporter;
import com.rinke.solutions.pinball.api.BinaryExporterFactory;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManager.Capability;
import com.rinke.solutions.pinball.api.LicenseManagerFactory;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.UsbCmd;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.swt.ActionAdapter;
import com.rinke.solutions.pinball.swt.CocoaGuiEnhancer;
import com.rinke.solutions.pinball.swt.SWTDispatcher;
import com.rinke.solutions.pinball.ui.About;
import com.rinke.solutions.pinball.ui.ConfigDialog;
import com.rinke.solutions.pinball.ui.DeviceConfig;
import com.rinke.solutions.pinball.ui.ExportGoDdmd;
import com.rinke.solutions.pinball.ui.GifExporter;
import com.rinke.solutions.pinball.ui.RegisterLicense;
import com.rinke.solutions.pinball.ui.UsbConfig;
import com.rinke.solutions.pinball.util.ApplicationProperties;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.util.ObservableProperty;
import com.rinke.solutions.pinball.view.model.CutInfo;
import com.rinke.solutions.pinball.view.swt.EscUnselect;
import com.rinke.solutions.pinball.view.swt.IconLabelProvider;
import com.rinke.solutions.pinball.view.swt.LabelProviderAdapter;
import com.rinke.solutions.pinball.view.swt.RecentMenuManager;
import com.rinke.solutions.pinball.widget.CircleTool;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.FloodFillTool;
import com.rinke.solutions.pinball.widget.LineTool;
import com.rinke.solutions.pinball.widget.PaletteTool;
import com.rinke.solutions.pinball.widget.RectTool;
import com.rinke.solutions.pinball.widget.SelectTool;
import com.rinke.solutions.pinball.widget.SetPixelTool;


@Slf4j
public class PinDmdEditor implements EventHandler {

	private static final int FRAME_RATE = 40;
	private static final String HELP_URL = "http://pin2dmd.com/editor/";
	
	public static int DMD_WIDTH = 128;
	public static int DMD_HEIGHT = 32;
	public static int PLANE_SIZE = 128/8*32;

	// "constants
	private final String frameTextPrefix = "Pin2dmd Editor ";
	final java.util.List<Palette> previewPalettes;

	@Option(name = "-ani", usage = "animation file to load", required = false)
	private String aniToLoad;

	@Option(name = "-cut", usage = "<src name>,<new name>,<start>,<end>", required = false)
	private String cutCmd;

	@Option(name = "-nodirty", usage = "dont check dirty flag on close", required = false)
	private boolean nodirty = false;

	@Option(name = "-save", usage = "if set, project is saved right away", required = false)
	private String saveFile;

	@Option(name = "-load", usage = "if set, project is loaded right away", required = false)
	private String loadFile;

	@Argument
	private java.util.List<String> arguments = new ArrayList<String>();

	// model
	DMD dmd = new DMD(128, 32); // for sake of window builder
	ObservableMap<String, Animation> recordings = new ObservableMap<String, Animation>(new LinkedHashMap<>());
	ObservableMap<String, CompiledAnimation> scenes = new ObservableMap<String, CompiledAnimation>(new LinkedHashMap<>());
	Map<String,Integer> recordingsPosMap = new HashMap<String, Integer>();
	Map<String,Integer> scenesPosMap = new HashMap<String, Integer>();
	ObservableProperty<Animation> selectedRecording = new ObservableProperty<Animation>(null);
	ObservableProperty<CompiledAnimation> selectedScene = new ObservableProperty<CompiledAnimation>(null);
	java.util.List<Animation> playingAnis = new ArrayList<Animation>();
	Project project = new Project();
	byte[] emptyMask;
	int numberOfHashes = 4;
	java.util.List<byte[]> hashes = new ArrayList<byte[]>();
	// stores the overall controled enabled state, actual buttons only gets enabled ever, when this is true
	boolean[] btnHashEnabled = new boolean[numberOfHashes];
	Map<String, DrawTool> drawTools = new HashMap<>();
	int selectedHashIndex;
	PalMapping selectedPalMapping;
	int saveTimeCode;
	ObservableList<Animation> frameSeqList = new ObservableList<>(new ArrayList<>());
	private Observer editAniObserver;
	private boolean livePreviewActive;
	private ConnectionHandle handle;
	private String pin2dmdAdress = null;
	public DmdSize dmdSize;
	private int actMaskNumber;
	private String pluginsPath;
	private List<String> loadedPlugins = new ArrayList<>();

	// app state
	private String projectFilename;
	private EditMode editMode;
	boolean useGlobalMask;
	protected int lastTimeCode;
	Palette activePalette;

	Display display;
	protected Shell shell;

	// dependencies / collaborators
	AnimationHandler animationHandler = null;
	CyclicRedraw cyclicRedraw = new CyclicRedraw();
	// colaboration classes
	DMDClock clock = new DMDClock(false);
	FileHelper fileHelper = new FileHelper();	
	PaletteTool paletteTool;

	@Autowired
	MaskDmdObserver maskDmdObserver;
	
	@Autowired
	private FileChooserUtil fileChooserUtil;
	
	@Autowired
	CutInfo cutInfo;
	@Autowired
	SmartDMDImporter smartDMDImporter;

	LicenseManager licManager;

	@Autowired
	PaletteHandler paletteHandler;
	@Autowired
	AnimationActionHandler aniAction;
	@Autowired
	private ClipboardHandler clipboardHandler;
	@Autowired
	private AutosaveHandler autoSaveHandler;
	@Autowired
	MessageUtil msgUtil;
	@Autowired
	private SWTDispatcher dispatcher;
	

	// ---------------- THE VIEW --------
	/** instance level SWT widgets */
	Button btnHash[] = new Button[numberOfHashes];
	// UI / swt widgets
	Label lblTcval;
	Label lblFrameNo;
	Text txtDuration;
	Scale scale;
	ComboViewer paletteComboViewer;
	TableViewer aniListViewer;
	TableViewer keyframeTableViewer;
	Button btnRemoveAni;
	Button btnDeleteKeyframe;
	Button btnAddKeyframe;
	Button btnFetchDuration;
	Button btnPrev;
	Button btnNext;
	ComboViewer paletteTypeComboViewer;
	DMDWidget dmdWidget;
	ResourceManager resManager;
	Button btnNewPalette;
	Button btnRenamePalette;
	ToolBar drawToolBar;
	ComboViewer frameSeqViewer;
	Button btnMarkStart;
	Button btnMarkEnd;
	Button btnCut;
	Button btnStartStop;
	Button btnAddFrameSeq;
	DMDWidget previewDmd;
	Label lblPlanesVal;
	Text txtDelayVal;
	private Button btnSortAni;
	private Button btnMask;
	private Menu menuPopRecentProjects;
	private Menu mntmRecentAnimations;
	private Menu mntmRecentPalettes;
	Spinner maskSpinner;
	private GoDmdGroup goDmdGroup;
	private MenuItem mntmUploadProject;
	private MenuItem mntmUploadPalettes;
	private Button btnCopyToNext;
	private Button btnUndo;
	private Button btnRedo;
	private Button btnCopyToPrev;
	private Button btnLivePreview;
	MenuItem mntmSaveProject;
	Button btnDeleteColMask;
	private ComboViewer editModeViewer;
	TableViewer sceneListViewer;
	private Button btnRemoveScene;
	private Spinner spinnerDeviceId;
	private Spinner spinnerEventId;
	private Button btnAddEvent;
	private Composite grpKeyframe;
	private Text textProperty;
	private ComboViewer bookmarkComboViewer;
	Button btnInvert;
	private MenuItem mntmUndo;
	private MenuItem mntmRedo;

	private Composite createKeyFrameGroup(Composite parent) {
		grpKeyframe = new Composite(parent, 0);
		grpKeyframe.setLayout(new GridLayout(5, false));
		GridData gd_grpKeyframe = new GridData(SWT.FILL, SWT.TOP, false, false, 3, 4);
		gd_grpKeyframe.heightHint = 257;
		gd_grpKeyframe.widthHint = 490;
		grpKeyframe.setLayoutData(gd_grpKeyframe);
//		grpKeyframe.setText("KeyFrames");
//		grpKeyframe.setVisible(!ApplicationProperties.getBoolean(ApplicationProperties.GODMD_ENABLED_PROP_KEY, false));
		
		Composite composite_hash = new Composite(grpKeyframe, SWT.NONE);
		//gd_composite_hash.widthHint = 105;
		GridData gd_composite_hash = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_composite_hash.widthHint = 148;
		composite_hash.setLayoutData(gd_composite_hash);
		createHashButtons(composite_hash, 10, 0);
		
		previewDmd = new DMDWidget(grpKeyframe, SWT.DOUBLE_BUFFERED, dmd, false);
		GridData gd_dmdPreWidget = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
		gd_dmdPreWidget.heightHint = 64;
		gd_dmdPreWidget.widthHint = 235;
		previewDmd.setLayoutData(gd_dmdPreWidget);
		previewDmd.setDrawingEnabled(false);
		previewDmd.setPalette(previewPalettes.get(0));
		previewDmd.setFilterByMask(true);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);

		int btnWidth = 155;

		btnAddKeyframe = new Button(grpKeyframe, SWT.NONE);
		btnAddKeyframe.setToolTipText("Adds a key frame that switches palette");
		GridData gd_btnAddKeyframe = new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1);
		gd_btnAddKeyframe.widthHint = btnWidth;
		btnAddKeyframe.setLayoutData(gd_btnAddKeyframe);
		btnAddKeyframe.setText("Add Palette Switch");
		btnAddKeyframe.setEnabled(false);
		btnAddKeyframe.addListener(SWT.Selection, e -> onAddKeyFrameClicked(SwitchMode.PALETTE));
		
		Label label = new Label(grpKeyframe, SWT.NONE);
		label.setText(" ");
		new Label(grpKeyframe, SWT.NONE);
		
		Label lblScene = new Label(grpKeyframe, SWT.NONE);
		lblScene.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblScene.setText("Scene:");
		
		frameSeqViewer = new ComboViewer(grpKeyframe, SWT.NONE);
		Combo frameSeqCombo = frameSeqViewer.getCombo();
		frameSeqCombo.setToolTipText("Choose frame sequence to use with key frame");
		GridData gd_frameSeqCombo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_frameSeqCombo.widthHint = 100;
		frameSeqCombo.setLayoutData(gd_frameSeqCombo);
		frameSeqViewer.setLabelProvider(new LabelProviderAdapter<Animation>(o -> o.getDesc()));
		frameSeqViewer.setContentProvider(ArrayContentProvider.getInstance());
		frameSeqViewer.setInput(frameSeqList);
		frameSeqViewer.addSelectionChangedListener(event -> onFrameSeqChanged(getFirstSelected(event)));
		
		btnAddFrameSeq = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnAddFrameSeq = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddFrameSeq.widthHint = btnWidth;
		btnAddFrameSeq.setLayoutData(gd_btnAddFrameSeq);
		btnAddFrameSeq.setToolTipText("Adds a keyframe that triggers playback of a scene");
		btnAddFrameSeq.setText("Add ColorScene Switch");
		// TODO add switch mode depend on ani scene
		btnAddFrameSeq.addListener(SWT.Selection, e -> onAddFrameSeqClicked(SwitchMode.REPLACE));
		btnAddFrameSeq.setEnabled(false);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		
		Label lblDuration = new Label(grpKeyframe, SWT.NONE);
		lblDuration.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDuration.setText("Duration:");
		
		txtDuration = new Text(grpKeyframe, SWT.BORDER);
		txtDuration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		txtDuration.setText("0");
		txtDuration.addListener(SWT.Verify, e -> e.doit = Pattern.matches("^[0-9]+$", e.text));
		txtDuration.addListener(SWT.Modify, e -> {
			if (selectedPalMapping != null) {
				selectedPalMapping.durationInMillis = Integer.parseInt(txtDuration.getText());
				selectedPalMapping.durationInFrames = (int) selectedPalMapping.durationInMillis / 40;
			}
		});
		
		btnFetchDuration = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnFetchDuration = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnFetchDuration.widthHint = btnWidth;
		btnFetchDuration.setLayoutData(gd_btnFetchDuration);
		btnFetchDuration.setToolTipText("Fetches duration for palette switches by calculating the difference between actual timestamp and keyframe timestamp");
		btnFetchDuration.setText("Fetch Duration");
		btnFetchDuration.setEnabled(false);
		btnFetchDuration.addListener(SWT.Selection, e -> {
			if (selectedPalMapping != null) {
				selectedPalMapping.durationInMillis = lastTimeCode - saveTimeCode;
				selectedPalMapping.durationInFrames = (int) selectedPalMapping.durationInMillis / FRAME_RATE;
				txtDuration.setText(selectedPalMapping.durationInMillis + "");
			}
		});
		new Label(grpKeyframe, SWT.NONE);
		
		Label lblNewLabel = new Label(grpKeyframe, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.widthHint = 121;
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		
		Label lblEvent = new Label(grpKeyframe, SWT.NONE);
		lblEvent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblEvent.setText("Event:");
		
		Composite composite_5 = new Composite(grpKeyframe, SWT.NONE);
		GridLayout gl_composite_5 = new GridLayout(2, false);
		gl_composite_5.marginWidth = 0;
		gl_composite_5.marginHeight = 0;
		composite_5.setLayout(gl_composite_5);
		GridData gd_composite_5 = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_composite_5.heightHint = 24;
		gd_composite_5.widthHint = 134;
		composite_5.setLayoutData(gd_composite_5);
		
		spinnerDeviceId = new Spinner(composite_5, SWT.BORDER);
		spinnerDeviceId.setMaximum(255);
		spinnerDeviceId.setMinimum(0);
		spinnerDeviceId.addModifyListener(e->onEventSpinnerChanged(spinnerDeviceId, 8));
		
		spinnerEventId = new Spinner(composite_5, SWT.BORDER);
		spinnerEventId.setMaximum(255);
		spinnerEventId.setMinimum(0);
		spinnerEventId.addModifyListener(e->onEventSpinnerChanged(spinnerEventId, 0));
		
		btnAddEvent = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnAddEvent = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddEvent.widthHint = btnWidth;
		btnAddEvent.setLayoutData(gd_btnAddEvent);
		btnAddEvent.setText("Add Event");
		new Label(grpKeyframe, SWT.NONE);
		btnAddEvent.addListener(SWT.Selection, e->onAddKeyFrameClicked(SwitchMode.EVENT));
		
		return grpKeyframe;
	}

	/**
	 * creates the top level menu
	 */
	private void createMenu(Shell shell) {
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");

		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);

		MenuItem mntmNewProject = new MenuItem(menu_1, SWT.NONE);
		mntmNewProject.setText("New Project\tCtrl-N");
		mntmNewProject.setAccelerator(SWT.MOD1 + 'N');
		mntmNewProject.addListener(SWT.Selection, e -> {
			if (dirtyCheck()) {
				onNewProject();
			}
		});

		MenuItem mntmLoadProject = new MenuItem(menu_1, SWT.NONE);
		mntmLoadProject.setText("Load Project\tCtrl-O");
		mntmLoadProject.setAccelerator(SWT.MOD1 + 'O');
		mntmLoadProject.addListener(SWT.Selection, e -> onLoadProjectSelected());

		mntmSaveProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveProject.setText("Save Project\tCrtl-S");
		mntmSaveProject.setAccelerator(SWT.MOD1 + 'S');
		mntmSaveProject.addListener(SWT.Selection, e -> onSaveProjectSelected(false));

		MenuItem mntmSaveAsProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveAsProject.setText("Save Project as\tShift-Crtl-S");
		mntmSaveAsProject.setAccelerator(SWT.MOD1|SWT.MOD2 + 'S');
		mntmSaveAsProject.addListener(SWT.Selection, e -> onSaveProjectSelected(true));

		MenuItem mntmRecentProjects = new MenuItem(menu_1, SWT.CASCADE);
		mntmRecentProjects.setText("Recent Projects");

		menuPopRecentProjects = new Menu(mntmRecentProjects);
		mntmRecentProjects.setMenu(menuPopRecentProjects);

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmImportProject = new MenuItem(menu_1, SWT.NONE);
		mntmImportProject.setText("Import Project");
		mntmImportProject.addListener(SWT.Selection, e -> onImportProjectSelected());

		MenuItem mntmExportRealPinProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportRealPinProject.setText("Export Project (real pin)");
		mntmExportRealPinProject.addListener(SWT.Selection, e -> onExportRealPinProject());

		MenuItem mntmExportVpinProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportVpinProject.setText("Export Project (virt pin)");
		mntmExportVpinProject.addListener(SWT.Selection, e -> onExportVirtualPinProject());

		mntmUploadProject = new MenuItem(menu_1, SWT.NONE);
		mntmUploadProject.setText("Upload Project");
		mntmUploadProject.addListener(SWT.Selection, e -> onUploadProjectSelected());

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setText("Exit\tCtrl-Q");
		mntmExit.addListener(SWT.Selection, e -> {
			if (dirtyCheck()) {
				shell.close();
				shell.dispose();
			}
		});

		MenuItem mntmedit = new MenuItem(menu, SWT.CASCADE);
		mntmedit.setText("&Edit");

		Menu menu_5 = new Menu(mntmedit);
		mntmedit.setMenu(menu_5);

		MenuItem mntmCut = new MenuItem(menu_5, SWT.NONE);
		mntmCut.setText("Cut \tCtrl-X");
		mntmCut.setAccelerator(SWT.MOD1 + 'X');
		mntmCut.addListener(SWT.Selection, e -> clipboardHandler.onCut(activePalette));

		MenuItem mntmCopy = new MenuItem(menu_5, SWT.NONE);
		mntmCopy.setText("Copy \tCtrl-C");
		mntmCopy.setAccelerator(SWT.MOD1 + 'C');
		mntmCopy.addListener(SWT.Selection, e -> clipboardHandler.onCopy(activePalette));

		MenuItem mntmPaste = new MenuItem(menu_5, SWT.NONE);
		mntmPaste.setText("Paste\tCtrl-V");
		mntmPaste.setAccelerator(SWT.MOD1 + 'V');
		mntmPaste.addListener(SWT.Selection, e -> { 
			clipboardHandler.onPaste(); 
			dmdRedraw();
		});

		MenuItem mntmPasteWithHover = new MenuItem(menu_5, SWT.NONE);
		mntmPasteWithHover.setText("Paste Over\tShift-Ctrl-V");
		mntmPasteWithHover.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'V');
		mntmPasteWithHover.addListener(SWT.Selection, e -> clipboardHandler.onPasteHoover());
		
		MenuItem mntmSelectAll = new MenuItem(menu_5, SWT.NONE);
		mntmSelectAll.setText("Select All\tCtrl-A");
		mntmSelectAll.setAccelerator(SWT.MOD1 + 'A');
		mntmSelectAll.addListener(SWT.Selection, e -> onSelectAll());

		MenuItem mntmDeSelect = new MenuItem(menu_5, SWT.NONE);
		mntmDeSelect.setText("Remove Selection\tShift-Ctrl-A");
		mntmDeSelect.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'A');
		mntmDeSelect.addListener(SWT.Selection, e -> onRemoveSelection() );

		new MenuItem(menu_5, SWT.SEPARATOR);

		mntmUndo = new MenuItem(menu_5, SWT.NONE);
		mntmUndo.setText("Undo\tCtrl-Z");
		mntmUndo.setAccelerator(SWT.MOD1 + 'Z');
		mntmUndo.addListener(SWT.Selection, e -> onUndoClicked());

		mntmRedo = new MenuItem(menu_5, SWT.NONE);
		mntmRedo.setText("Redo\tShift-Ctrl-Z");
		mntmRedo.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'Z');
		mntmRedo.addListener(SWT.Selection, e -> onRedoClicked());

		MenuItem mntmAnimations = new MenuItem(menu, SWT.CASCADE);
		mntmAnimations.setText("&Animations");

		Menu menu_2 = new Menu(mntmAnimations);
		mntmAnimations.setMenu(menu_2);

		MenuItem mntmLoadAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmLoadAnimation.setText("Load Animation(s)");
		mntmLoadAnimation.addListener(SWT.Selection, e -> aniAction.onLoadAniWithFC(true));
		
		MenuItem mntmLoadRecordings = new MenuItem(menu_2, SWT.NONE);
		mntmLoadRecordings.setText("Load Recording(s)");
		mntmLoadRecordings.addListener(SWT.Selection, e -> aniAction.onLoadAniWithFC(true));
		
		MenuItem mntmSaveAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveAnimation.setText("Save Animation(s) ...");
		mntmSaveAnimation.addListener(SWT.Selection, e -> aniAction.onSaveAniWithFC(1));
		
		MenuItem mntmSaveSingleAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveSingleAnimation.setText("Save single Animation");
		mntmSaveSingleAnimation.addListener(SWT.Selection, e -> aniAction.onSaveSingleAniWithFC(1));

		MenuItem mntmRecentAnimationsItem = new MenuItem(menu_2, SWT.CASCADE);
		mntmRecentAnimationsItem.setText("Recent Animations");

		mntmRecentAnimations = new Menu(mntmRecentAnimationsItem);
		mntmRecentAnimationsItem.setMenu(mntmRecentAnimations);

		new MenuItem(menu_2, SWT.SEPARATOR);

		MenuItem mntmExportAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmExportAnimation.setText("Export Animation as GIF");
		
		mntmExportAnimation.addListener(SWT.Selection, e -> {
			Animation ani = playingAnis.get(0);
			Palette pal = project.palettes.get(ani.getPalIndex());
			GifExporter exporter = new GifExporter(shell, pal, ani);
			exporter.open();
		});

		MenuItem mntmExportForGodmd = new MenuItem(menu_2, SWT.NONE);
		mntmExportForGodmd.setText("Export for goDMD ...");
		mntmExportForGodmd.addListener(SWT.Selection, e->{
			ExportGoDdmd exportGoDdmd = new ExportGoDdmd(shell, 0);
			Pair<String,Integer> res = exportGoDdmd.open();
			if( res != null ) {
				exportForGoDMD( res.getLeft(), res.getRight() );
			}
		});

		MenuItem mntmpalettes = new MenuItem(menu, SWT.CASCADE);
		mntmpalettes.setText("&Palettes / Mode");
		Menu menu_3 = new Menu(mntmpalettes);
		mntmpalettes.setMenu(menu_3);

		MenuItem mntmLoadPalette = new MenuItem(menu_3, SWT.NONE);
		mntmLoadPalette.setText("Load Palette");
		mntmLoadPalette.addListener(SWT.Selection, e -> paletteHandler.loadPalette());

		MenuItem mntmSavePalette = new MenuItem(menu_3, SWT.NONE);
		mntmSavePalette.setText("Save Palette");
		mntmSavePalette.addListener(SWT.Selection, e -> paletteHandler.savePalette());

		MenuItem mntmRecentPalettesItem = new MenuItem(menu_3, SWT.CASCADE);
		mntmRecentPalettesItem.setText("Recent Palettes");

		mntmRecentPalettes = new Menu(mntmRecentPalettesItem);
		mntmRecentPalettesItem.setMenu(mntmRecentPalettes);

		new MenuItem(menu_3, SWT.SEPARATOR);

		mntmUploadPalettes = new MenuItem(menu_3, SWT.NONE);
		mntmUploadPalettes.setText("Upload Palettes");
		mntmUploadPalettes.addListener(SWT.Selection, e -> connector.upload(activePalette));

		new MenuItem(menu_3, SWT.SEPARATOR);

		MenuItem mntmConfig = new MenuItem(menu_3, SWT.NONE);
		mntmConfig.setText("Configuration");
		mntmConfig.addListener(SWT.Selection, e -> {
			ConfigDialog config = new ConfigDialog(shell);
			config.open(pin2dmdAdress);
			if( config.okPressed ) {
				refreshPin2DmdHost(config.getPin2DmdHost());
				if( !dmdSize.equals(config.getDmdSize())) {
					refreshDmdSize(config.getDmdSize());
				}
			}
		});

		MenuItem mntmDevice = new MenuItem(menu_3, SWT.NONE);
		mntmDevice.setText("Create Device File / WiFi");
		mntmDevice.addListener(SWT.Selection, e -> {
			DeviceConfig deviceConfig = new DeviceConfig(shell);
			deviceConfig.open();
		});

		MenuItem mntmUsbconfig = new MenuItem(menu_3, SWT.NONE);
		mntmUsbconfig.setText("Configure Device via USB");
		mntmUsbconfig.addListener(SWT.Selection, e -> new UsbConfig(shell).open());

		MenuItem mntmhelp = new MenuItem(menu, SWT.CASCADE);
		mntmhelp.setText("&Help");

		Menu menu_4 = new Menu(mntmhelp);
		mntmhelp.setMenu(menu_4);

		MenuItem mntmGetHelp = new MenuItem(menu_4, SWT.NONE);
		mntmGetHelp.setText("Get help");
		mntmGetHelp.addListener(SWT.Selection, e -> Program.launch(HELP_URL));

		MenuItem mntmRegister = new MenuItem(menu_4, SWT.NONE);
		mntmRegister.setText("Register");
		mntmRegister.addListener(SWT.Selection, e -> new RegisterLicense(shell).open());

		new MenuItem(menu_4, SWT.SEPARATOR);

		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");
		mntmAbout.addListener(SWT.Selection, e -> { 
			About a = new About(shell);
			a.setPluginsPath(pluginsPath);
			a.setPlugins(loadedPlugins);
			a.open();
		});
	}


	/**
	 * Create contents of the window.
	 */
	void createContents(Shell shell, DMD dmd) {
		shell.setSize(1380, 660);
		shell.setText("Pin2dmd - Editor");
		shell.setLayout(new GridLayout(4, false));

		createMenu(shell);
		
		setProjectFilename(null);
		
		Config config = new Config();

		recentProjectsMenuManager = new RecentMenuManager("recentProject", 4, menuPopRecentProjects, e -> loadProject((String) e.widget.getData()), config);
		recentProjectsMenuManager.loadRecent();

		recentPalettesMenuManager = new RecentMenuManager("recentPalettes", 4, mntmRecentPalettes, e -> paletteHandler.loadPalette((String) e.widget.getData()), config);
		recentPalettesMenuManager.loadRecent();

		recentAnimationsMenuManager = new RecentMenuManager("recentAnimations", 4, mntmRecentAnimations, e -> aniAction.loadAni(((String) e.widget.getData()),
				true, true), config);
		recentAnimationsMenuManager.loadRecent();

		resManager = new LocalResourceManager(JFaceResources.getResources(), shell);

		Label lblAnimations = new Label(shell, SWT.NONE);
		lblAnimations.setText("Recordings");
		
		Label lblScences = new Label(shell, SWT.NONE);
		lblScences.setText("Scences");

		Label lblKeyframes = new Label(shell, SWT.NONE);
		lblKeyframes.setText("KeyFrames");

		Label lblPreview = new Label(shell, SWT.NONE);
		lblPreview.setText("Preview");
		
		int listWidth = 150;
		int listHeight = 231;
		int colWidth = 220;

		aniListViewer = new TableViewer(shell, SWT.BORDER | SWT.V_SCROLL);
		Table aniList = aniListViewer.getTable();
		GridData gd_aniList = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_aniList.heightHint = listHeight;
		gd_aniList.widthHint = listWidth;
		aniList.setLayoutData(gd_aniList);
		aniList.setLinesVisible(true);
		aniList.addKeyListener(new EscUnselect(aniListViewer));
		aniListViewer.setContentProvider(ArrayContentProvider.getInstance());
		aniListViewer.setLabelProvider(new LabelProviderAdapter<Animation>(ani -> ani.getDesc()));
		aniListViewer.setInput(recordings.values());
		aniListViewer.addSelectionChangedListener(event -> onRecordingSelectionChanged(getFirstSelected(event)));
		
		// created edit support for ani / recordings
		TableViewerColumn viewerCol1 = new TableViewerColumn(aniListViewer, SWT.LEFT);
		viewerCol1.setEditingSupport(new GenericTextCellEditor<Animation>(aniListViewer, ani -> ani.getDesc(), (ani, v) -> {
			updateAnimationMapKey(ani.getDesc(), v, recordings);
			ani.setDesc(v);
		}));
		viewerCol1.getColumn().setWidth(colWidth);
		viewerCol1.setLabelProvider(new IconLabelProvider<Animation>(shell, o -> o.getIconAndText() ));
		
		sceneListViewer = new TableViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		Table sceneList = sceneListViewer.getTable();
		GridData gd_list = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_list.heightHint = listHeight;
		gd_list.widthHint = listWidth;
		sceneList.setLayoutData(gd_list);
		sceneList.setLinesVisible(true);
		sceneList.addKeyListener(new EscUnselect(sceneListViewer));
		sceneListViewer.setContentProvider(ArrayContentProvider.getInstance());
		sceneListViewer.setLabelProvider(new LabelProviderAdapter<Animation>(o -> o.getDesc()));
		sceneListViewer.setInput(scenes.values());
		sceneListViewer.addSelectionChangedListener(event -> onSceneSelectionChanged(getFirstSelected(event)));

		TableViewerColumn viewerCol2 = new TableViewerColumn(sceneListViewer, SWT.LEFT);
		viewerCol2.setEditingSupport(new GenericTextCellEditor<Animation>(sceneListViewer, ani -> ani.getDesc(), (ani, v) -> {
			updateAnimationMapKey(ani.getDesc(), v, scenes);
			ani.setDesc(v);
			frameSeqViewer.refresh();
		}));
		viewerCol2.getColumn().setWidth(colWidth);
		viewerCol2.setLabelProvider(new IconLabelProvider<Animation>(shell, ani -> ani.getIconAndText() ));

		keyframeTableViewer = new TableViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		Table keyframeList = keyframeTableViewer.getTable();
		GridData gd_keyframeList = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_keyframeList.heightHint = listHeight;
		gd_keyframeList.widthHint = listWidth;
		keyframeList.setLinesVisible(true);
		keyframeList.setLayoutData(gd_keyframeList);
		keyframeList.addKeyListener(new EscUnselect(keyframeTableViewer));

		//keyframeTableViewer.setLabelProvider(new KeyframeLabelProvider(shell));
		keyframeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		keyframeTableViewer.setInput(project.palMappings);
		keyframeTableViewer.addSelectionChangedListener(event -> onKeyframeChanged(event));

		TableViewerColumn viewerColumn = new TableViewerColumn(keyframeTableViewer, SWT.LEFT);
		viewerColumn.setEditingSupport(new GenericTextCellEditor<PalMapping>(keyframeTableViewer, e -> e.name, (e, v) -> { e.name = v; }));

		viewerColumn.getColumn().setWidth(colWidth);
		viewerColumn.setLabelProvider(new IconLabelProvider<PalMapping>(shell, o -> Pair.of(o.switchMode.name().toLowerCase(), o.name ) ));

		dmdWidget = new DMDWidget(shell, SWT.DOUBLE_BUFFERED, dmd, true);
		// dmdWidget.setBounds(0, 0, 700, 240);
		GridData gd_dmdWidget = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_dmdWidget.heightHint = listHeight;
		gd_dmdWidget.widthHint = 816;
		dmdWidget.setLayoutData(gd_dmdWidget);
		dmdWidget.setPalette(activePalette);
		dmdWidget.addListeners(l -> onFrameChanged(l));

		Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new GridLayout(2, false));
		GridData gd_composite_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite_1.widthHint = listWidth;
		composite_1.setLayoutData(gd_composite_1);

		btnRemoveAni = new Button(composite_1, SWT.NONE);
		btnRemoveAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnRemoveAni.setText("Remove");
		btnRemoveAni.setEnabled(false);
		btnRemoveAni.addListener(SWT.Selection, e -> {
			project.bookmarksMap.remove(selectedRecording.get().getDesc());
			onRemove(selectedRecording, recordings);
		} );

		btnSortAni = new Button(composite_1, SWT.NONE);
		btnSortAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortAni.setText("Sort");
		btnSortAni.addListener(SWT.Selection, e -> onSortAnimations(recordings));
		
		Composite composite_4 = new Composite(shell, SWT.NONE);
		composite_4.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		composite_4.setLayout(new GridLayout(2, false));
		
		btnRemoveScene = new Button(composite_4, SWT.NONE);
		btnRemoveScene.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnRemoveScene.setEnabled(false);
		btnRemoveScene.setText("Remove");
		btnRemoveScene.addListener(SWT.Selection, e -> onRemove(selectedScene, scenes) );
		
		Button btnSortScene = new Button(composite_4, SWT.NONE);
		btnSortScene.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortScene.setText("Sort");
		btnSortScene.addListener(SWT.Selection, e -> onSortAnimations(scenes));

		Composite composite_2 = new Composite(shell, SWT.NONE);
		composite_2.setLayout(new GridLayout(3, false));
		GridData gd_composite_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite_2.widthHint = listWidth;
		composite_2.setLayoutData(gd_composite_2);

		btnDeleteKeyframe = new Button(composite_2, SWT.NONE);
		GridData gd_btnDeleteKeyframe = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_btnDeleteKeyframe.widthHint = 88;
		btnDeleteKeyframe.setLayoutData(gd_btnDeleteKeyframe);
		btnDeleteKeyframe.setText("Remove");
		btnDeleteKeyframe.setEnabled(false);
		btnDeleteKeyframe.addListener(SWT.Selection, e -> {
			if (selectedPalMapping != null) {
				project.palMappings.remove(selectedPalMapping);
				keyframeTableViewer.refresh();
				checkReleaseMask();
			}
		});

		Button btnSortKeyFrames = new Button(composite_2, SWT.NONE);
		btnSortKeyFrames.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortKeyFrames.setText("Sort");
		btnSortKeyFrames.addListener(SWT.Selection, e -> onSortKeyFrames());
		new Label(composite_2, SWT.NONE);

		scale = new Scale(shell, SWT.NONE);
		GridData gd_scale = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_scale.widthHint = 826;
		scale.setLayoutData(gd_scale);
		scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));
		
		CTabFolder tabFolder = new CTabFolder(shell, SWT.FLAT);
		GridData gd_tabFolder = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 4);
		gd_tabFolder.heightHint = 255;
		gd_tabFolder.widthHint = 504;
		tabFolder.setLayoutData(gd_tabFolder);
		
		CTabItem tbtmKeyframe = new CTabItem(tabFolder, SWT.NONE);
		tbtmKeyframe.setText(TabMode.KEYFRAME.label);
		tbtmKeyframe.setControl(createKeyFrameGroup(tabFolder));
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		new Label(grpKeyframe, SWT.NONE);
		
		CTabItem tbtmGodmd = new CTabItem(tabFolder, SWT.NONE);
		tbtmGodmd.setText(TabMode.GODMD.label);
		
		goDmdGroup =  new GoDmdGroup(tabFolder);
		tbtmGodmd.setControl( goDmdGroup.getGrpGoDMDCrtls() );

		CTabItem tbtmPropertyText = new CTabItem(tabFolder, SWT.NONE);
		tbtmPropertyText.setText(TabMode.PROP.label);
		
		textProperty = new Text(tabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		tbtmPropertyText.setControl(textProperty);
		
		tabFolder.setSelection(tbtmKeyframe);
		tabFolder.addListener(SWT.Selection, e->{
			log.debug("tab changed: {}", tabFolder.getSelection().getText());
			//this.tabMode = TabMode.fromLabel(tabFolder.getSelection().getText());
		});

		Group grpDetails = new Group(shell, SWT.NONE);
		grpDetails.setLayout(new GridLayout(10, false));
		GridData gd_grpDetails = new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1);
		gd_grpDetails.heightHint = 27;
		gd_grpDetails.widthHint = 815;
		grpDetails.setLayoutData(gd_grpDetails);
		grpDetails.setText("Details");

		Label lblFrame = new Label(grpDetails, SWT.NONE);
		lblFrame.setText("Frame:");

		lblFrameNo = new Label(grpDetails, SWT.NONE);
		GridData gd_lblFrameNo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblFrameNo.widthHint = 66;
		gd_lblFrameNo.minimumWidth = 60;
		lblFrameNo.setLayoutData(gd_lblFrameNo);
		lblFrameNo.setText("---");

		Label lblTimecode = new Label(grpDetails, SWT.NONE);
		lblTimecode.setText("Timecode:");

		lblTcval = new Label(grpDetails, SWT.NONE);
		GridData gd_lblTcval = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblTcval.widthHint = 62;
		gd_lblTcval.minimumWidth = 80;
		lblTcval.setLayoutData(gd_lblTcval);
		lblTcval.setText("---");

		Label lblDelay = new Label(grpDetails, SWT.NONE);
		lblDelay.setText("Delay:");

		txtDelayVal = new Text(grpDetails, SWT.NONE);
		GridData gd_lblDelayVal = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblDelayVal.widthHint = 53;
		txtDelayVal.setLayoutData(gd_lblDelayVal);
		txtDelayVal.setText("");
		txtDelayVal.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if( event.keyCode == SWT.CR ) {
					String val = txtDelayVal.getText();
					int delay = StringUtils.isEmpty(val)?0:Integer.parseInt(val);
					if( selectedScene.isPresent() ) {
						CompiledAnimation ani = selectedScene.get();
						if( actFrameOfSelectedAni<ani.frames.size() ) {
							log.debug("Setting delay of frame {} to {}", actFrameOfSelectedAni, delay);
							ani.frames.get(actFrameOfSelectedAni).delay = delay;
						}
						project.dirty = true;
					}
				}
			}
		} );
		
		txtDelayVal.addListener(SWT.Verify, e -> e.doit = Pattern.matches("^[0-9]*$", e.text));

		Label lblPlanes = new Label(grpDetails, SWT.NONE);
		lblPlanes.setText("Planes:");

		lblPlanesVal = new Label(grpDetails, SWT.NONE);
		lblPlanesVal.setText("---");
		new Label(grpDetails, SWT.NONE);

		btnLivePreview = new Button(grpDetails, SWT.CHECK);
		btnLivePreview.setToolTipText("controls live preview to real display device");
		btnLivePreview.setText("Live Preview");
		btnLivePreview.addListener(SWT.Selection, e -> onLivePreviewSwitched(btnLivePreview.getSelection()));

		Composite composite = new Composite(shell, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 779;
		composite.setLayoutData(gd_composite);
		composite.setLayout(new GridLayout(11, false));

		btnStartStop = new Button(composite, SWT.NONE);
		btnStartStop.setText("Start");
		btnStartStop.addListener(SWT.Selection, e -> onStartStopClicked(animationHandler.isStopped()));

		btnPrev = new Button(composite, SWT.NONE);
		btnPrev.setText("<");
		btnPrev.addListener(SWT.Selection, e -> onPrevFrameClicked());

		btnNext = new Button(composite, SWT.NONE);
		btnNext.setText(">");
		btnNext.addListener(SWT.Selection, e -> onNextFrameClicked());

		btnMarkStart = new Button(composite, SWT.NONE);
		btnMarkStart.setToolTipText("Marks start of scene for cutting");
		btnMarkEnd = new Button(composite, SWT.NONE);
		btnCut = new Button(composite, SWT.NONE);
		btnCut.setToolTipText("Cuts out a new scene for editing and use a replacement or color mask");

		btnMarkStart.setText("Mark Start");
		btnMarkStart.addListener(SWT.Selection, e -> {
			cutInfo.setStart(selectedRecording.get().actFrame);
		});

		btnMarkEnd.setText("Mark End");
		btnMarkEnd.addListener(SWT.Selection, e -> {
			cutInfo.setEnd(selectedRecording.get().actFrame);
		});

		btnCut.setText("Cut");
		btnCut.addListener(SWT.Selection, e -> {
			// respect number of planes while cutting / copying
				cutScene(selectedRecording.get(), cutInfo.getStart(), cutInfo.getEnd(), buildUniqueName(scenes));
				log.info("cutting out scene from {}", cutInfo);
				cutInfo.reset();
			});

		Button btnIncPitch = new Button(composite, SWT.NONE);
		btnIncPitch.setText("+");
		btnIncPitch.addListener(SWT.Selection, e -> dmdWidget.incPitch());

		Button btnDecPitch = new Button(composite, SWT.NONE);
		btnDecPitch.setText("-");
		btnDecPitch.addListener(SWT.Selection, e -> dmdWidget.decPitch());
		
		bookmarkComboViewer = new ComboViewer(composite, SWT.NONE);
		Combo combo_3 = bookmarkComboViewer.getCombo();
		GridData gd_combo_3 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_3.widthHint = 106;
		combo_3.setLayoutData(gd_combo_3);
		bookmarkComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		bookmarkComboViewer.setLabelProvider(new LabelProviderAdapter<Bookmark>(o -> o.name+" - "+o.pos));
		bookmarkComboViewer.addSelectionChangedListener(e -> {
			Bookmark bm = getFirstSelected(e);
			if( bm != null && selectedRecording.isPresent() ) {
				animationHandler.setPos(bm.pos);
			}
		});
			
		Button btnNewBookMark = new Button(composite, SWT.NONE);
		btnNewBookMark.setText("New");
		btnNewBookMark.addListener(SWT.Selection, e->{
			if( selectedRecording.isPresent() ) {
				Animation r = selectedRecording.get();
				Set<Bookmark> set = project.bookmarksMap.get(r.getDesc());
				if( set == null ) {
					set = new TreeSet<Bookmark>();
					project.bookmarksMap.put(r.getDesc(),set);
					
				}
				String bookmarkName = bookmarkComboViewer.getCombo().getText();
				set.add(new Bookmark(bookmarkName, r.actFrame));
				bookmarkComboViewer.setInput(set);
				bookmarkComboViewer.refresh();
			}
		});
		
		Button btnDelBookmark = new Button(composite, SWT.NONE);
		btnDelBookmark.setText("Del.");
		btnDelBookmark.addListener(SWT.Selection, e->{
			if( selectedRecording.isPresent() ) {
				Animation r = selectedRecording.get();
				Set<Bookmark> set = project.bookmarksMap.get(r.getDesc());
				if( set != null ) {
					set.remove(getSelectionFromViewer(bookmarkComboViewer));
					bookmarkComboViewer.refresh();
				}
			}
		});

		Group grpPalettes = new Group(shell, SWT.NONE);
		GridLayout gl_grpPalettes = new GridLayout(6, false);
		gl_grpPalettes.verticalSpacing = 2;
		gl_grpPalettes.horizontalSpacing = 2;
		grpPalettes.setLayout(gl_grpPalettes);
		GridData gd_grpPalettes = new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1);
		gd_grpPalettes.widthHint = 814;
		gd_grpPalettes.heightHint = 71;
		grpPalettes.setLayoutData(gd_grpPalettes);
		grpPalettes.setText("Palettes");

		paletteComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
		Combo combo = paletteComboViewer.getCombo();
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_combo.widthHint = 166;
		combo.setLayoutData(gd_combo);
		paletteComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		paletteComboViewer.setLabelProvider(new LabelProviderAdapter<Palette>(o -> o.index + " - " + o.name));
		paletteComboViewer.setInput(project.palettes);
		paletteComboViewer.addSelectionChangedListener(event -> onPaletteChanged(getFirstSelected(event)));

		paletteTypeComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
		Combo combo_1 = paletteTypeComboViewer.getCombo();
		combo_1.setToolTipText("Type of palette. Default palette is choosen at start and after timed switch is expired");
		GridData gd_combo_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_1.widthHint = 96;
		combo_1.setLayoutData(gd_combo_1);
		paletteTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		paletteTypeComboViewer.setInput(PaletteType.values());
		setViewerSelection(paletteTypeComboViewer, activePalette.type);
		paletteTypeComboViewer.addSelectionChangedListener(e -> onPaletteTypeChanged(e));
						
		Button btnApplyPalette = new Button(grpPalettes, SWT.NONE);
		btnApplyPalette.setText("Apply");
		btnApplyPalette.addListener(SWT.Selection, e -> onApplyPalette(activePalette));
		
		btnNewPalette = new Button(grpPalettes, SWT.NONE);
		btnNewPalette.setToolTipText("Creates a new palette by copying the actual colors");
		btnNewPalette.setText("New");
		btnNewPalette.addListener(SWT.Selection, e -> paletteHandler.newPalette());

		btnRenamePalette = new Button(grpPalettes, SWT.NONE);
		btnRenamePalette.setToolTipText("Confirms the new palette name");
		btnRenamePalette.setText("Rename");
		btnRenamePalette.addListener(SWT.Selection, e -> {
			String newName = paletteComboViewer.getCombo().getText();
			if (newName.contains(" - ")) {
				activePalette.name = newName.split(" - ")[1];
				setPaletteViewerByIndex(activePalette.index);
				paletteComboViewer.refresh();
			} else {
				msgUtil.warn("Illegal palette name", "Palette names must consist of palette index and name.\nName format therefore must be '<idx> - <name>'");
				paletteComboViewer.getCombo().setText(activePalette.index + " - " + activePalette.name);
			}

		});
		
		Button btnDeletePalette = new Button(grpPalettes, SWT.NONE);
		btnDeletePalette.setText("Delete");
		btnDeletePalette.addListener(SWT.Selection, e->paletteHandler.onDeletePalette());

		Composite grpPal = new Composite(grpPalettes, SWT.NONE);
		grpPal.setLayout(new GridLayout(1, false));
		GridData gd_grpPal = new GridData(SWT.LEFT, SWT.TOP, false, false, 3, 1);
		gd_grpPal.widthHint = 333;
		gd_grpPal.heightHint = 22;
		grpPal.setLayoutData(gd_grpPal);
		// GridData gd_grpPal = new GridData(SWT.LEFT, SWT.CENTER, false, false,
		// 1, 1);
		// gd_grpPal.widthHint = 223;
		// gd_grpPal.heightHint = 61;
		// grpPal.setLayoutData(gd_grpPal);
		//
		paletteTool = new PaletteTool(shell, grpPal, SWT.FLAT | SWT.RIGHT, activePalette);

		paletteTool.addListener(dmdWidget);
								
		Label lblCtrlclickToEdit = new Label(grpPalettes, SWT.NONE);
		GridData gd_lblCtrlclickToEdit = new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1);
		gd_lblCtrlclickToEdit.widthHint = 131;
		lblCtrlclickToEdit.setLayoutData(gd_lblCtrlclickToEdit);
		lblCtrlclickToEdit.setText("Ctrl-Click to edit color");
		new Label(grpPalettes, SWT.NONE);
		new Label(grpPalettes, SWT.NONE);
		new Label(grpPalettes, SWT.NONE);
		new Label(grpPalettes, SWT.NONE);
		new Label(grpPalettes, SWT.NONE);
		new Label(grpPalettes, SWT.NONE);
		new Label(grpPalettes, SWT.NONE);
		

		Group grpDrawing = new Group(shell, SWT.NONE);
		grpDrawing.setLayout(new GridLayout(6, false));
		GridData gd_grpDrawing = new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1);
		gd_grpDrawing.heightHint = 83;
		gd_grpDrawing.widthHint = 812;
		grpDrawing.setLayoutData(gd_grpDrawing);
		grpDrawing.setText("Drawing");

		drawToolBar = new ToolBar(grpDrawing, SWT.FLAT | SWT.RIGHT);
		GridData gd_drawToolBar = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_drawToolBar.widthHint = 184;
		drawToolBar.setLayoutData(gd_drawToolBar);

		ToolItem tltmPen = new ToolItem(drawToolBar, SWT.RADIO);
		tltmPen.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/pencil.png")));
		tltmPen.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("pencil")));

		ToolItem tltmFill = new ToolItem(drawToolBar, SWT.RADIO);
		tltmFill.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/color-fill.png")));
		tltmFill.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("fill")));

		ToolItem tltmRect = new ToolItem(drawToolBar, SWT.RADIO);
		tltmRect.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/rect.png")));
		tltmRect.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("rect")));

		ToolItem tltmLine = new ToolItem(drawToolBar, SWT.RADIO);
		tltmLine.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/line.png")));
		tltmLine.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("line")));

		ToolItem tltmCircle = new ToolItem(drawToolBar, SWT.RADIO);
		tltmCircle.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/oval.png")));
		tltmCircle.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("circle")));

		ToolItem tltmFilledCircle = new ToolItem(drawToolBar, SWT.RADIO);
		tltmFilledCircle.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/oval2.png")));
		tltmFilledCircle.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("filledCircle")));

		//		ToolItem tltmColorize = new ToolItem(drawToolBar, SWT.RADIO);
//		tltmColorize.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/colorize.png")));
//		tltmColorize.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("colorize")));
		
		ToolItem tltmMark = new ToolItem(drawToolBar, SWT.RADIO);
		tltmMark.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/select.png")));
		tltmMark.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(drawTools.get("select")));

		drawTools.put("pencil", new SetPixelTool(paletteTool.getSelectedColor()));
		drawTools.put("fill", new FloodFillTool(paletteTool.getSelectedColor()));
		drawTools.put("rect", new RectTool(paletteTool.getSelectedColor()));
		drawTools.put("line", new LineTool(paletteTool.getSelectedColor()));
		drawTools.put("circle", new CircleTool(paletteTool.getSelectedColor(), false));
		drawTools.put("filledCircle", new CircleTool(paletteTool.getSelectedColor(), true));
//		drawTools.put("colorize", new ColorizeTool(paletteTool.getSelectedColor()));
		drawTools.put("select", new SelectTool(paletteTool.getSelectedColor(), dmdWidget));
		// notify draw tool on color changes
		drawTools.values().forEach(d -> paletteTool.addIndexListener(d));
		// let draw tools notify when draw action is finished
		drawTools.values().forEach(d->d.addObserver((dm,o)->updateHashes(dm)));
		
		paletteTool.addListener(palette -> {
			if (livePreviewActive) {
				connector.upload(activePalette, handle);
			}
		});
		
		editModeViewer = new ComboViewer(grpDrawing, SWT.READ_ONLY);
		Combo combo_2 = editModeViewer.getCombo();
		GridData gd_combo_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_2.widthHint = 141;
		combo_2.setLayoutData(gd_combo_2);
		editModeViewer.setContentProvider(ArrayContentProvider.getInstance());
		editModeViewer.setLabelProvider(new LabelProviderAdapter<EditMode>(o -> o.label));
		
		// TODO the list depends on animation type
		// for immutable only fixed ist selectable
		// else replace / mask / follow
		/*1 - Replacement
		2 - AddCol
		3 - AddCol mit Follow Hash

		könnte man wenn Mode = 3 und Mask = checked die Maske vom Frame editieren
		(Auswahl 1-10 wäre da ausgegraut)

		In Modus 1+2 würde ich die Mask-Checkbox, Maskennummer-Dropdown und die Hash-Checkboxen alle auch ausgrauen,
		da das alles editierten Content kein Sinn macht. 
		-> Die wären dann alle nur bei Dumps aktiv.*/

		editModeViewer.setInput(EditMode.values());
		if( selectedScene.isPresent() ) {
			setViewerSelection(editModeViewer, selectedScene.get().getEditMode());
		} else {
			setViewerSelection(editModeViewer, EditMode.FIXED);
		}
		editModeViewer.addSelectionChangedListener(e -> onEditModeChanged(e));
		//btnColorMask.add

		Label lblMaskNo = new Label(grpDrawing, SWT.NONE);
		lblMaskNo.setText("Mask No:");

		maskSpinner = new Spinner(grpDrawing, SWT.BORDER);
		maskSpinner.setToolTipText("select the mask to use");
		maskSpinner.setMinimum(0);
		maskSpinner.setMaximum(9);
		maskSpinner.setEnabled(false);
		maskSpinner.addListener(SWT.Selection, e -> onMaskNumberChanged(maskSpinner.getSelection()));
		
		btnMask = new Button(grpDrawing, SWT.CHECK);
		btnMask.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		GridData gd_btnMask = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnMask.widthHint = 62;
		btnMask.setLayoutData(gd_btnMask);
		btnMask.setText("Mask");
		btnMask.setEnabled(false);
		btnMask.addListener(SWT.Selection, e -> onMaskChecked(btnMask.getSelection()));
		
		btnInvert = new Button(grpDrawing, SWT.NONE);
		btnInvert.setText("Invert");
		btnInvert.addListener(SWT.Selection, e->onInvert());
		btnInvert.setEnabled(false);
		
		btnCopyToPrev = new Button(grpDrawing, SWT.NONE);
		btnCopyToPrev.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnCopyToPrev.setText("CopyToPrev");
		btnCopyToPrev.addListener(SWT.Selection, e->onCopyAndMoveToPrevFrameClicked());
		
		btnCopyToNext = new Button(grpDrawing, SWT.NONE);
		btnCopyToNext.setToolTipText("copy the actual scene / color mask to next frame and move forward");
		btnCopyToNext.setText("CopyToNext");
		btnCopyToNext.addListener(SWT.Selection, e->onCopyAndMoveToNextFrameClicked());
		
		btnUndo = new Button(grpDrawing, SWT.NONE);
		btnUndo.setText("&Undo");
		btnUndo.addListener(SWT.Selection, e -> onUndoClicked());
		
		btnRedo = new Button(grpDrawing, SWT.NONE);
		btnRedo.setText("&Redo");
		btnRedo.addListener(SWT.Selection, e -> onRedoClicked());
		
		btnDeleteColMask = new Button(grpDrawing, SWT.NONE);
		btnDeleteColMask.setText("Delete");
		btnDeleteColMask.setEnabled(false);
		new Label(grpDrawing, SWT.NONE);
		btnDeleteColMask.addListener(SWT.Selection, e -> onDeleteColMaskClicked());
	}
	


	Pin2DmdConnector connector;

	RecentMenuManager recentProjectsMenuManager;
	RecentMenuManager recentPalettesMenuManager;
	RecentMenuManager recentAnimationsMenuManager;

	public enum TabMode {
		KEYFRAME("KeyFrame"), GODMD("goDMD"), PROP("Properties");
		
		public final String label;

		private TabMode(String label) {
			this.label = label;
		}

		public static TabMode fromLabel(String text) {
			for( TabMode i : values()) {
				if( i.label.equals(text)) return i;
			}
			return null;
		}
	}

	public PinDmdEditor() {
		// avoid NPE we run in test context
		if( log == null ) {
			log = LoggerFactory.getLogger(PinDmdEditor.class);
		}
		dmdSize = DmdSize.fromOrdinal(ApplicationProperties.getInteger(ApplicationProperties.PIN2DMD_DMDSIZE_PROP_KEY,0));
		pin2dmdAdress = ApplicationProperties.get(ApplicationProperties.PIN2DMD_ADRESS_PROP_KEY);

		emptyMask = new byte[dmdSize.planeSize];
		Arrays.fill(emptyMask, (byte) 0xFF);
		
		dmd = new DMD(dmdSize.width, dmdSize.height);
		
		activePalette = project.palettes.get(0);
		previewPalettes = Palette.previewPalettes();
		licManager = LicenseManagerFactory.getInstance();
		checkForPlugins();
		connector = ConnectorFactory.create(pin2dmdAdress);
	}
	
	private void updateHashes(Observable o) {
		this.notifyAni(new AniEvent(Type.FRAMECHANGE, null, dmd.getFrame()));
	}

	static File logFile;
	static Logger log;
	
	/**
	 * create a tmp file that will be deleted on jvm exit and redirect log output to that file
	 * this enables the 'show log lines' feature in the global crash dialog.
	 */
	public static void configureLogging()
	{
		try {
			logFile = File.createTempFile("pin2dmd-editor", ".log");
			logFile.deleteOnExit();
		} catch (IOException e) {
			System.err.println("problems with creating logfile ...");
			e.printStackTrace();
		}
		if( System.getProperty(SimpleLogger.LOG_FILE_KEY) == null ) {
			System.setProperty(SimpleLogger.LOG_FILE_KEY, logFile.getAbsolutePath());
			System.out.println("logging to "+logFile.getAbsolutePath());
		}			
	}

	private void checkForPlugins() {
		Path currentRelativePath = Paths.get("");
		pluginsPath = currentRelativePath.toAbsolutePath().toString()+File.separator+"plugins";
		String[] fileList = new File(pluginsPath).list((dir, name) -> name.endsWith(".jar"));
		if( fileList!=null) Arrays.stream(fileList).forEach(file -> addSoftwareLibrary(new File(pluginsPath+File.separatorChar+file)));
		try {
			Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
			log.info("successfully loaded video plugin classes");
			loadedPlugins.add("Video");
		} catch (ClassNotFoundException e) {
		}
	}
	
	private  void addSoftwareLibrary(File file) {
		try {
		    Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
		    method.setAccessible(true);
		    method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{file.toURI().toURL()});
		    log.info("adding {} to classpath", file.toURI().toURL());
		} catch( Exception e) {
			log.warn("adding {} to classpath failed", file.getPath());
		}
	}
	
	public void refreshPin2DmdHost(String address) {
		if (address != null && !address.equals(pin2dmdAdress)) {
			if (handle != null) {
				connector.release(handle);
			}
			this.pin2dmdAdress = address;
			ApplicationProperties.put(ApplicationProperties.PIN2DMD_ADRESS_PROP_KEY, pin2dmdAdress);
			connector = ConnectorFactory.create(address);
		}
	}

	/**
	 * handles redraw of animations
	 * 
	 * @author steve
	 */
	private class CyclicRedraw implements Runnable {

		@Override
		public void run() {
			// if( !previewCanvas.isDisposed()) previewCanvas.redraw();
			if (dmdWidget != null && !dmdWidget.isDisposed())
				dmdWidget.redraw();
			if (previewDmd != null && !previewDmd.isDisposed())
				previewDmd.redraw();
			if (animationHandler != null && !animationHandler.isStopped()) {
				animationHandler.run();
				display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
			}
		}
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		configureLogging();
		log = LoggerFactory.getLogger(PinDmdEditor.class);
		Display display = Display.getDefault();
		Realm.runWithDefault(SWTObservables.getRealm(display), new Runnable() {
			public void run() {
				try {
					PinDmdEditor window = new PinDmdEditor();
					window.open(args);
				} catch (Exception e) {
					log.error("unexpected exception", e);
				}
			}
		});
		log.info("exiting");
	}

	private void saveHashes(java.util.List<byte[]> hashes) {
		if (hashes != null) {
			this.hashes.clear();
			for (byte[] h : hashes) {
				this.hashes.add(Arrays.copyOf(h, h.length));
			}
		}
	}

	public void createBindings() {

		ObserverManager.bind(maskDmdObserver, e -> btnUndo.setEnabled(e), () -> maskDmdObserver.canUndo());
		ObserverManager.bind(maskDmdObserver, e -> btnRedo.setEnabled(e), () -> maskDmdObserver.canRedo());
		
		maskDmdObserver.addObserver((dmd,o)->updateHashes(dmd));
		
		ObserverManager.bind(maskDmdObserver, e -> mntmRedo.setEnabled(e), () -> maskDmdObserver.canRedo());
		ObserverManager.bind(maskDmdObserver, e -> mntmUndo.setEnabled(e), () -> maskDmdObserver.canUndo());
		// do some bindings
		editAniObserver = ObserverManager.bind(animationHandler, e -> this.enableDrawing(e), () -> animationIsEditable());
		
		ObserverManager.bind(animationHandler, e -> dmdWidget.setDrawingEnabled(e), () -> animationHandler.isStopped());

		ObserverManager.bind(animationHandler, e -> btnPrev.setEnabled(e), () -> animationHandler.isStopped() && animationHandler.hasAnimations());
		ObserverManager.bind(animationHandler, e -> btnNext.setEnabled(e), () -> animationHandler.isStopped() && animationHandler.hasAnimations());

		ObserverManager.bind(cutInfo, e -> btnCut.setEnabled(e), () -> (cutInfo.getStart() > 0 && cutInfo.getEnd() > 0));

		ObserverManager.bind(cutInfo, e -> btnMarkEnd.setEnabled(e), () -> (cutInfo.getStart() > 0));

		//ObserverManager.bind(animations, e -> btnStartStop.setEnabled(e), () -> !this.animations.isEmpty() && animationHandler.isStopped());
		ObserverManager.bind(recordings, e -> btnPrev.setEnabled(e), () -> !this.recordings.isEmpty());
		ObserverManager.bind(recordings, e -> btnNext.setEnabled(e), () -> !this.recordings.isEmpty());
		ObserverManager.bind(recordings, e -> btnMarkStart.setEnabled(e), () -> !this.recordings.isEmpty());

		ObserverManager.bind(recordings, e -> aniListViewer.refresh(), () -> true);
		
		ObserverManager.bind(scenes, e -> sceneListViewer.refresh(), () -> true);
		ObserverManager.bind(scenes, e -> buildFrameSeqList(), () -> true);

		// ObserverManager.bind(animations, e->btnAddFrameSeq.setEnabled(e),
		// ()->!frameSeqList.isEmpty());
	}

	private void enableDrawing(boolean e) {
		drawToolBar.setEnabled(e);
		if( e ) {
			if( selectedRecording.isPresent()) setViewerSelection(editModeViewer, selectedRecording.get().getEditMode());
		} else {
			setViewerSelection(editModeViewer, EditMode.FIXED);
		}
		btnCopyToNext.setEnabled(e);
		btnCopyToPrev.setEnabled(e);
		btnDeleteColMask.setEnabled(e);
	}

	private boolean animationIsEditable() {
		return (this.useGlobalMask && !project.masks.get(actMaskNumber).locked) || (animationHandler.isStopped() && isEditable(animationHandler.getAnimations()));
	}

	private boolean isEditable(java.util.List<Animation> a) {
		if (a != null) {
			return a.size() == 1 && a.get(0).isMutable();
		}
		return false;
	}

	protected void buildFrameSeqList() {
		//Animation old = (Animation) frameSeqViewer.getSelection();
		frameSeqList.clear();
		frameSeqList.addAll(scenes.values().stream().filter(a -> a.isMutable()).collect(Collectors.toList()));
		log.info("frame seq list: {}", frameSeqList);
		frameSeqViewer.setInput(frameSeqList);
		frameSeqViewer.refresh();
		if( !frameSeqList.isEmpty() ) setViewerSelection(frameSeqViewer, frameSeqList.get(0));
	}

	BeanFactory beanFactory;
	
	/**
	 * Open the window.
	 * 
	 * @param args
	 */
	public void open(String[] args) {

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			System.exit(1);
		}
		
		display = Display.getDefault();
		shell = new Shell();

		if (SWT.getPlatform().equals("cocoa")) {
			CocoaGuiEnhancer enhancer = new CocoaGuiEnhancer("Pin2dmd Editor");
			enhancer.hookApplicationMenu(display, e -> e.doit = dirtyCheck(),
					// skipped setting of plugin list / plugin path
					new ActionAdapter(() -> new About(shell).open()),
					new ActionAdapter(() -> new ConfigDialog(shell).open(null)) );
		}
		
		createContents(shell, dmd);

		init();

		animationHandler = new AnimationHandler(playingAnis, clock, dmd);
		animationHandler.setScale(scale);
		animationHandler.setEventHandler(this);

		boolean goDMDenabled = ApplicationProperties.getBoolean(ApplicationProperties.GODMD_ENABLED_PROP_KEY);
		animationHandler.setEnableClock(goDMDenabled);
		
		onNewProject();

		paletteComboViewer.getCombo().select(0);
		paletteTool.setPalette(activePalette);

		createBindings();

		SplashScreen splashScreen = SplashScreen.getSplashScreen();
		if (splashScreen != null) {
			splashScreen.close();
		}
		
		shell.open();
		shell.layout();
		shell.addListener(SWT.Close, e -> {
			e.doit = dirtyCheck();
		});

		GlobalExceptionHandler.getInstance().setDisplay(display);
		GlobalExceptionHandler.getInstance().setShell(shell);

		autoSaveHandler.checkAutoSaveAtStartup();

		dispatcher.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
		dispatcher.timerExec(1000*300, autoSaveHandler);

		processCmdLine();

		int retry = 0;
		while (true) {
			try {
				log.info("entering event loop");
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch()) {
						display.sleep();
					}
				}
				autoSaveHandler.deleteAutosaveFiles();
				System.exit(0);
			} catch (Exception e) {
				GlobalExceptionHandler.getInstance().showError(e);
				log.error("unexpected error: {}", e);
				if (retry++ > 10) {
					autoSaveHandler.deleteAutosaveFiles();
					System.exit(1);
				}
			}
		}
	}

	void init() {
		beanFactory = new SimpleBeanFactory();
		beanFactory.scanPackages("com.rinke.solutions.pinball");
		beanFactory.setSingleton(display);
		beanFactory.setSingleton(shell);
		beanFactory.setSingleton("editor",this);
		beanFactory.setSingleton("dmd",dmd);
		beanFactory.setSingleton(dmdWidget);
		
		beanFactory.inject(this);
	}

	private void processCmdLine() {
		// cmd line processing
		if (loadFile != null) {
			loadProject(loadFile);
		}
		if (aniToLoad != null) {
			aniAction.loadAni(aniToLoad, false, true);
		}
		if (cutCmd != null && !recordings.isEmpty()) {
			String[] cuts = cutCmd.split(",");
			if (cuts.length >= 3) {
				cutScene(recordings.get(cuts[0]), Integer.parseInt(cuts[2]), Integer.parseInt(cuts[3]), cuts[1]);
			}
		}
		if (saveFile != null) {
			saveProject(saveFile);
		}
	}

	Animation cutScene(Animation animation, int start, int end, String name) {
		CompiledAnimation cutScene = animation.cutScene(start, end, ApplicationProperties.getInteger(ApplicationProperties.NOOFPLANES,4));
		// TODO improve to make it selectable how many planes
		
		paletteHandler.copyPalettePlaneUpgrade();
		
		cutScene.setDesc(name);
		cutScene.setPalIndex(activePalette.index);
		cutScene.setProjectAnimation(true);
		cutScene.setEditMode(EditMode.REPLACE);
				
		scenes.put(name, cutScene);
		
		setViewerSelection(frameSeqViewer, cutScene);

		if( ApplicationProperties.getBoolean(ApplicationProperties.AUTOKEYFRAME)) {
			onAddFrameSeqClicked(SwitchMode.REPLACE);
		}

		setViewerSelection(sceneListViewer, cutScene);

		return cutScene;
	}

	void onNewProject() {
		project.clear();
		project.setDimension(dmdSize.width, dmdSize.height);
		activePalette = project.palettes.get(0);
		setViewerSelection(paletteComboViewer,activePalette);
		paletteComboViewer.refresh();
		keyframeTableViewer.refresh();
		recordings.clear();
		scenes.clear();
		playingAnis.clear();
		selectedRecording.set(null);
		selectedScene.set(null);
		animationHandler.setAnimations(playingAnis);
		setProjectFilename(null);
	}

	private void onLoadProjectSelected() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml;*.json;" }, new String[] { "Project XML", "Project JSON" });
		if (filename != null) {
			loadProject(filename);
		}
	}

	/**
	 * imports a secondary project to implement a merge functionality
	 */
	void onImportProjectSelected() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml;*.json;" }, new String[] { "Project XML", "Project JSON" });
		if (filename != null)
			importProject(filename);
	}

	void importProject(String filename) {
		log.info("importing project from {}", filename);
		Project projectToImport = (Project) fileHelper.loadObject(filename);
		// merge into existing Project
		HashSet<String> collisions = new HashSet<>();
		for (String key : projectToImport.frameSeqMap.keySet()) {
			if (project.frameSeqMap.containsKey(key)) {
				collisions.add(key);
			} else {
				project.frameSeqMap.put(key, projectToImport.frameSeqMap.get(key));
			}
		}
		if (!collisions.isEmpty()) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT);

			messageBox.setText("Override warning");
			messageBox.setMessage("the following frame seq have NOT been \nimported due to name collisions: " + collisions + "\n");
			messageBox.open();
		}

		for (String inputFile : projectToImport.inputFiles) {
			aniAction.loadAni(buildRelFilename(filename, inputFile), true, true);
		}
		for (PalMapping palMapping : projectToImport.palMappings) {
			project.palMappings.add(palMapping);
		}
	}
	
	protected void setPaletteViewerByIndex(int palIndex) {
		Optional<Palette> optPal = project.palettes.stream().filter(p -> p.index==palIndex).findFirst();
		setViewerSelection(paletteComboViewer, optPal.orElse(activePalette));
		log.info("setting pal.index to {}",palIndex);
	}
	
	protected void setupUIonProjectLoad() {
		paletteComboViewer.setInput(project.palettes);
		setPaletteViewerByIndex(0);
		keyframeTableViewer.setInput(project.palMappings);
		for (Animation ani : recordings.values()) {
			setViewerSelection(aniListViewer, ani);
			break;
		}
	}

	void loadProject(String filename) {
		log.info("load project from {}", filename);
		Project projectToLoad = (Project) fileHelper.loadObject(filename);

		if (projectToLoad != null) {
			shell.setText(frameTextPrefix + " - " + new File(filename).getName());
			if( projectToLoad.width == 0) {
				projectToLoad.width = 128;
				projectToLoad.height = 32; // default for older projects
			}
			DmdSize newSize = DmdSize.fromWidthHeight(projectToLoad.width, projectToLoad.height);
			refreshDmdSize(newSize);
			setProjectFilename(filename);
			project = projectToLoad;
			recordings.clear();
			scenes.clear();
			
			// if inputFiles contain project filename remove it
			String aniFilename = replaceExtensionTo("ani", filename);
			project.inputFiles.remove(aniFilename); // full name
			project.inputFiles.remove(new File(aniFilename).getName()); // simple name
			
			for (String file : project.inputFiles) {
				aniAction.loadAni(buildRelFilename(filename, file), true, false);
			}
			
			List<Animation> loadedWithProject = aniAction.loadAni(aniFilename, true, false);
			loadedWithProject.stream().forEach(a->a.setProjectAnimation(true));
			
			setupUIonProjectLoad();
			ensureDefault();
			recentProjectsMenuManager.populateRecent(filename);
		}

	}

	private void ensureDefault() {
		boolean foundDefault = false;
		for (Palette p : project.palettes) {
			if (PaletteType.DEFAULT.equals(p.type)) {
				foundDefault = true;
				break;
			}
		}
		if (!foundDefault) {
			project.palettes.get(0).type = PaletteType.DEFAULT;
		}
	}

	String buildRelFilename(String parent, String file) {
		if( file.contains(File.separator)) return file;
		return new File(parent).getParent() + File.separator + new File(file).getName();
	}
	
	private void onExportRealPinProject() {
		licManager.requireOneOf( Capability.REALPIN, Capability.GODMD);
		String filename = fileChooserUtil.choose(SWT.SAVE, project.name, new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			msgUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			exportProject(filename, f -> new FileOutputStream(f), true);
			if( !filename.endsWith("pin2dmd.pal")) {
				msgUtil.warn("Hint", "Remember to rename your export file to pin2dmd.pal if you want to use it" + " in a real pinballs sdcard of pin2dmd.");
			}
		}
	}
	
	private void onExportVirtualPinProject() {
		licManager.requireOneOf(Capability.VPIN, Capability.GODMD);
		String filename = fileChooserUtil.choose(SWT.SAVE, project.name, new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			msgUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			exportProject(filename, f -> new FileOutputStream(f), false);
		}
	}

	void onSaveProjectSelected(boolean saveAs) {
		if( saveAs || getProjectFilename()==null ) {
			String filename = fileChooserUtil.choose(SWT.SAVE, project.name, new String[] { "*.xml" }, new String[] { "Project XML" });
			if (filename != null)
				saveProject(filename);
		} else {
			saveProject(getProjectFilename());
		}
	}

	@FunctionalInterface
	public interface OutputStreamProvider {
		OutputStream buildStream(String name) throws IOException;
	}

	void onUploadProjectSelected() {
		Map<String, ByteArrayOutputStream> captureOutput = new HashMap<>();
		exportProject("a.dat", f -> {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			captureOutput.put(f, stream);
			return stream;
		}, true);

		connector.transferFile("pin2dmd.pal", new ByteArrayInputStream(captureOutput.get("a.dat").toByteArray()));
		if (captureOutput.containsKey("a.fsq")) {
			connector.transferFile("pin2dmd.fsq", new ByteArrayInputStream(captureOutput.get("a.fsq").toByteArray()));
		}
		sleep(1500);
		connector.sendCmd(UsbCmd.RESET);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	void exportProject(String filename, OutputStreamProvider streamProvider, boolean realPin) {
		log.info("export project {} file {}", realPin?"real":"vpin", filename);
		licManager.requireOneOf(Capability.VPIN, Capability.REALPIN, Capability.GODMD);

		// rebuild frame seq map	
		project.frameSeqMap.clear();
		for (PalMapping p : project.palMappings) {
			if (p.frameSeqName != null) {
				FrameSeq frameSeq = new FrameSeq(p.frameSeqName);
				if (p.switchMode.equals(SwitchMode.ADD) || p.switchMode.equals(SwitchMode.FOLLOW) ) {
					frameSeq.mask = 0b11111100;
				}
				project.frameSeqMap.put(p.frameSeqName, frameSeq);
			}
		}
		
		// VPIN
		if( !realPin ) {
			List<Animation> anis = new ArrayList<>();
			for (FrameSeq p : project.frameSeqMap.values()) {
				Animation ani = scenes.get(p.name);
				// copy without extending frames
				CompiledAnimation cani = ani.cutScene(ani.start, ani.end, 0);
				cani.actFrame = 0;
				cani.setDesc(ani.getDesc());
				DMD tmp = new DMD(dmdSize.width, dmdSize.height);
				for (int i = cani.start; i <= cani.end; i++) {
					cani.getCurrentMask();
					Frame f = cani.render(tmp, false);
					for( int j = 0; j < f.planes.size(); j++) {
						if (((1 << j) & p.mask) == 0) {
							Arrays.fill(f.planes.get(j).data, (byte)0);
						}
					}
				}
				anis.add(cani);
			}
			if( !anis.isEmpty() ) {
				String aniFilename = replaceExtensionTo("vni", filename);
				AniWriter aniWriter = new AniWriter(anis, aniFilename, 4, project.palettes, null);
				aniWriter.setHeader("VPIN");
				aniWriter.run();
				try {
					BinaryExporter exporter = BinaryExporterFactory.getInstance();
					DataOutputStream dos2 = new DataOutputStream(streamProvider.buildStream(filename));
					// for vpins version is 2
					project.version = 2;
					exporter.writeTo(dos2, aniWriter.getOffsetMap(), project);
					dos2.close();
				} catch (IOException e) {
					throw new RuntimeException("error writing " + filename, e);
				}
				/*XStream xStream = new XStream();
				try {
					xStream.toXML(anis, new FileWriter("/Users/stefanri/ani.aml"));
				} catch (IOException e) {
					e.printStackTrace();
				}*/
			}
			
		} else {
			// for all referenced frame mapping we must also copy the frame data as
			// there are two models
			for (FrameSeq p : project.frameSeqMap.values()) {
				CompiledAnimation ani = scenes.get(p.name);				
				ani.actFrame = 0;
				DMD tmp = new DMD(dmdSize.width, dmdSize.height);
				for (int i = 0; i <= ani.end; i++) {
					ani.getCurrentMask();
					Frame frame = new Frame( ani.render(tmp, false) ); // copy frames to not remove in org
					// remove planes not in mask
					int pl = 0;
					for (Iterator<Plane> iter = frame.planes.iterator(); iter.hasNext();) {
						iter.next();
						if (((1 << pl) & p.mask) == 0) {
							iter.remove();
						}
						pl++;
					}
					p.frames.add(frame);
				}
			}
			// create addtional files for frame sequences
			try {
				Map<String, Integer> map = new HashMap<String, Integer>();
				BinaryExporter exporter = BinaryExporterFactory.getInstance();
				if (!project.frameSeqMap.isEmpty()) {
					log.info("exporter instance {} wrinting FSQ", exporter);
					DataOutputStream dos = new DataOutputStream(streamProvider.buildStream(replaceExtensionTo("fsq", filename)));
					map = exporter.writeFrameSeqTo(dos, project, 
							ApplicationProperties.getBoolean(ApplicationProperties.OLDEXPORT)?1:2);
					dos.close();
//					XStream xStream = new XStream();
//					xStream.toXML(project.frameSeqMap.values(), new FileWriter("/Users/stefanri/fsq.xml"));
					
				}

				project.version = 1;
				DataOutputStream dos2 = new DataOutputStream(streamProvider.buildStream(filename));
				exporter.writeTo(dos2, map, project);
				dos2.close();
				// fileHelper.storeObject(project, filename);
			} catch (IOException e) {
				throw new RuntimeException("error writing " + filename, e);
			}
		}		
	}

	void saveProject(String filename) {
		log.info("write project to {}", filename);
		String aniFilename = replaceExtensionTo("ani", filename);
		String baseName = new File(aniFilename).getName();
		String baseNameWithoutExtension = baseName.substring(0, baseName.indexOf('.'));
		if (project.name == null) {
			project.name = baseNameWithoutExtension;
		} else if (!project.name.equals(baseNameWithoutExtension)) {
			// save as
			project.inputFiles.remove(project.name + ".ani");
		}
		project.setDimension(dmdSize.width, dmdSize.height);
		
		// we need to "tag" the projects animations that are always stored in the projects ani file
		// the project ani file is not included in the inputFile list but animations gets loaded
		// implicitly
		
		String path = new File(filename).getParent(); 
		// so first check directly included anis in project inputfiles
		for( String inFile : project.inputFiles) {
			Optional<Animation> optAni = recordings.values().stream().filter(a -> a.getName().equals(path+File.separator+inFile)).findFirst();
			optAni.ifPresent(a-> {
				if( a.isDirty()) {
					aniAction.storeAnimations(Arrays.asList(a), a.getName(), 4, false);
					a.setDirty(false);
				}
			});
		}
		
		storeOrDeleteProjectAnimations(aniFilename);

		Map<String,FrameSeq> frameSeqMapSave = project.frameSeqMap;
		project.frameSeqMap = null; // remove this for saving
		fileHelper.storeObject(project, filename);
		project.frameSeqMap = frameSeqMapSave;
		project.dirty = false;
	}

	private void storeOrDeleteProjectAnimations(String aniFilename) {
		// only need to save ani's that are 'project' animations
		List<Animation> prjAnis = scenes.values().stream().filter(a->a.isProjectAnimation()).collect(Collectors.toList());
		if( !prjAnis.isEmpty() ) {
			aniAction.storeAnimations(prjAnis, aniFilename, 4, true);
		} else {
			new File(aniFilename).delete(); // delete project ani file
		}
	}

	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}
		
	public void setEnableHashButtons(boolean enabled) {
		Stream.of(btnHash).forEach(e->e.setEnabled(enabled));
		if( !enabled )  Stream.of(btnHash).forEach(e->e.setSelection(false));
		Arrays.fill(btnHashEnabled, enabled);
	}

	public void createHashButtons(Composite parent, int x, int y) {
		for (int i = 0; i < numberOfHashes; i++) {
			btnHash[i] = new Button(parent, SWT.CHECK);
			if (i == 0)
				btnHash[i].setSelection(true);
			btnHash[i].setData(Integer.valueOf(i));
			btnHash[i].setText("Hash" + i);
			// btnHash[i].setFont(new Font(shell.getDisplay(), "sans", 10, 0));
			btnHash[i].setBounds(x, y + i * 16, 331, 18);
			btnHash[i].addListener(SWT.Selection, e -> {
				selectedHashIndex = (Integer) e.widget.getData();
				if (selectedPalMapping != null) {
					selectedPalMapping.hashIndex = selectedHashIndex;
				}
				for (int j = 0; j < numberOfHashes; j++) {
					if (j != selectedHashIndex)
						btnHash[j].setSelection(false);
				}
				int planes = Integer.parseInt(lblPlanesVal.getText() );
				// switch palettes in preview
				Palette palette = previewPalettes.get(planes==4?selectedHashIndex:selectedHashIndex*4);
				log.info("switch to preview palette: {}", palette);
				previewDmd.setPalette(palette);
			});
		}
	}

	public void onAddKeyFrameClicked(SwitchMode switchMode) {
		PalMapping palMapping = new PalMapping(activePalette.index, "KeyFrame " + (project.palMappings.size() + 1));
		if (selectedHashIndex != -1) {
			palMapping.setDigest(hashes.get(selectedHashIndex));
		}
		palMapping.animationName = selectedRecording.get().getDesc();
		palMapping.frameIndex = selectedRecording.get().actFrame;
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = (spinnerDeviceId.getSelection()<<8) + spinnerEventId.getSelection();
		}
		palMapping.switchMode = switchMode;
		if (useGlobalMask) {
			palMapping.withMask = useGlobalMask;
			palMapping.maskNumber = actMaskNumber;
			project.masks.get(actMaskNumber).locked = true;
			onMaskChecked(true);
		}

		if (!checkForDuplicateKeyFrames(palMapping)) {
			project.palMappings.add(palMapping);
			saveTimeCode = lastTimeCode;
			keyframeTableViewer.refresh();
		} else {
			msgUtil.warn("Hash is already used", "The selected hash is already used by another key frame");
		}
	}

	boolean checkForDuplicateKeyFrames(PalMapping palMapping) {
		for (PalMapping p : project.palMappings) {
			if (Arrays.equals(p.crc32, palMapping.crc32))
				return true;
		}
		return false;
	}
	
	public <T extends Animation> void onRemove(ObservableProperty<? extends T> selection, ObservableMap<String, T> map) {
		if (selection.isPresent()) {
			T a = selection.get();
			String key = a.getDesc();
			if( a.isProjectAnimation() ) project.dirty = true;
			map.remove(key);
			playingAnis.clear();
			animationHandler.setAnimations(playingAnis);
			animationHandler.setClockActive(true);
		}
	}
	
	void onInvert() {
		dmd.addUndoBuffer();
		byte[] data = dmd.getFrame().mask.data;
		for( int i = 0; i < data.length; i++) {
			data[i] = (byte) ~data[i];
		}
		dmd.setMask(data);
	}

	private void onEventSpinnerChanged(Spinner spinner, int i) {
		if( selectedPalMapping != null ) {
			if( i == 8 ) {
				selectedPalMapping.durationInMillis = (selectedPalMapping.durationInMillis & 0xFF) + (spinner.getSelection()<<8); 
			} else {
				selectedPalMapping.durationInMillis = (selectedPalMapping.durationInMillis & 0xFF00) + (spinner.getSelection()<<0); 
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getFirstSelected(SelectionChangedEvent e) {
		IStructuredSelection selection = (IStructuredSelection) e.getSelection();
		return selection.isEmpty() ? null : (T)selection.getFirstElement();
	}

	private void onEditModeChanged(SelectionChangedEvent e) {
		EditMode mode = getFirstSelected(e);
		if( editMode != null && editMode.equals(mode)) return;	// no recursive calls
		editMode = mode;
		if( selectedScene.isPresent() ) {
			CompiledAnimation animation = selectedScene.get();
			if( animation.isDirty() && !animation.getEditMode().equals(editMode)) {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
				messageBox.setText("Changing edit mode");
				messageBox.setMessage("you are about to change edit mode, while scene was already modified. Really change?");
				int res = messageBox.open();
				if( res == SWT.NO ) {
					editMode = animation.getEditMode();
					setViewerSelection(editModeViewer,editMode);
				}
			}
			if(editMode.equals(EditMode.FOLLOW)) {
				animation.ensureMask();
			} else {
				btnMask.setSelection(false); // switch off mask if selected
				onMaskChecked(false);			
			}
			btnMask.setEnabled(editMode.equals(EditMode.FOLLOW));
			setEnableHashButtons(editMode.equals(EditMode.FOLLOW));
			animation.setEditMode(editMode);
		}
		setDrawMaskByEditMode(editMode);
		sceneListViewer.refresh();
	}

	private void setViewerSelection(TableViewer viewer, Object sel) {
		if( sel != null ) viewer.setSelection(new StructuredSelection(sel));
		else viewer.setSelection(StructuredSelection.EMPTY);
	}

	void setViewerSelection(AbstractListViewer viewer, Object sel) {
		if( sel != null ) viewer.setSelection(new StructuredSelection(sel));
		else viewer.setSelection(StructuredSelection.EMPTY);
	}

	/**
	 * creates a unique key name for scenes
	 * @param anis the map containing the keys
	 * @return the new unique name
	 */
	<T extends Animation> String buildUniqueName(ObservableMap<String, T> anis) {
		int no = anis.size();
		String name = "Scene " + no;
		while( anis.containsKey(name)) {
			no++;
			name = "Scene " + no;
		}
		return name;
	}

	/**
	 * deletes the 2 additional color masking planes.
	 * depending on draw mask (presence of a mask) this is plane 2,3 or 3,4
	 */
	 void onDeleteColMaskClicked() {
		dmd.addUndoBuffer();
		dmd.fill(dmdWidget.isShowMask()?(byte)0xFF:0);
		dmdRedraw();
	}

	private void onStartStopClicked(boolean stopped) {
		if( stopped )
		{
			selectedScene.ifPresent(a->a.commitDMDchanges(dmd, hashes.get(selectedHashIndex)));
			animationHandler.start();
			display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
			btnStartStop.setText("Stop");
			dmdWidget.resetSelection();
		} else {
			animationHandler.stop();
			btnStartStop.setText("Start");
		}
	}

	private void onPrevFrameClicked() {
		selectedScene.ifPresent(a->a.commitDMDchanges(dmd, hashes.get(selectedHashIndex)));
		animationHandler.prev();
		if( dmdWidget.isShowMask() ) {
			onMaskChecked(true);
		}
		if( editMode.equals(EditMode.FOLLOW) && selectedScene.isPresent()) {
			selectHash(selectedScene.get());
		}
		dmdWidget.resetSelection();
	}
	
	private void selectHash(CompiledAnimation ani) {
		byte[] crc32 = ani.frames.get(ani.actFrame).crc32;
		for( int i =0; i < hashes.size(); i++ ) {
			if( Arrays.equals(hashes.get(i), crc32) ) {
				selectedHashIndex = i;
			}
		}
		for (int j = 0; j < numberOfHashes; j++) {
			btnHash[j].setSelection(j == selectedHashIndex);
		}
	}

	private void onNextFrameClicked() {
		selectedScene.ifPresent(a->a.commitDMDchanges(dmd,hashes.get(selectedHashIndex)));
		animationHandler.next();
		if( dmdWidget.isShowMask() ) {
			onMaskChecked(true);
		}
		if( editMode.equals(EditMode.FOLLOW) && selectedScene.isPresent()) {
			selectHash(selectedScene.get());
		}
		dmdWidget.resetSelection();
	}
	
	private void onCopyAndMoveToNextFrameClicked() {
		onNextFrameClicked();
		CompiledAnimation ani = selectedScene.get();
		if( !ani.hasEnded() ) {
			ani.frames.get(ani.actFrame-1).copyToWithMask(dmd.getFrame(), dmd.getDrawMask());
			dmdRedraw();
		}
	}
	
	private void onCopyAndMoveToPrevFrameClicked() {
		onPrevFrameClicked();
		CompiledAnimation ani = selectedScene.get();
		if( ani.getActFrame() >= ani.getStart() ) {
			ani.frames.get(ani.actFrame+1).copyToWithMask(dmd.getFrame(), dmd.getDrawMask());
			dmdRedraw();
		}
	}

	private void onSortKeyFrames() {
		Collections.sort(project.palMappings, new Comparator<PalMapping>() {
			@Override
			public int compare(PalMapping o1, PalMapping o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		keyframeTableViewer.refresh();
	}

	private void setDrawMaskByEditMode(EditMode mode) {
		if( dmdWidget.isShowMask() ) {
			// only draw on mask
			// TODO mask drawing and plane drawing with mask should be controlled seperately
			dmd.setDrawMask( 0b00000001);
		} else {
			boolean drawWithMask = EditMode.COLMASK.equals(mode) || EditMode.FOLLOW.equals(mode);
			btnDeleteColMask.setEnabled(drawWithMask);
			dmd.setDrawMask(drawWithMask ? 0b11111000 : 0xFFFF);
		}
	}

	/**
	 * checks all pal mappings and releases masks if not used anymore
	 */
	private void checkReleaseMask() {
		HashSet<Integer> useMasks = new HashSet<>();
		for (PalMapping p : project.palMappings) {
			if (p.withMask) {
				useMasks.add(p.maskNumber);
			}
		}
		for (int i = 0; i < project.masks.size(); i++) {
			project.masks.get(i).locked = useMasks.contains(i);
		}
		onMaskChecked(useGlobalMask);
	}
	
	private void setPlayingAni(Animation ani, int pos) {
		log.debug("set playing ani {}, {}", pos, ani);
		playingAnis.clear();
		playingAnis.add(ani);
		animationHandler.setAnimations(playingAnis);
		animationHandler.setPos(pos);
		dmdRedraw();
	}
	
	// TODO !!! make selected animation observable to bind change handler to it (maybe remove) Optional
	// make this the general change handler, and let the click handler only set selected animation

	private void onSceneSelectionChanged(CompiledAnimation a) {
		log.info("onSceneSelectionChanged: {}", a);
		Animation current = selectedScene.get();
		// detect changes
		if( current == null && a == null ) return;
		if(a!= null && current != null && a.getDesc().equals(current.getDesc())) return;
		if( current != null ) scenesPosMap.put(current.getDesc(), current.actFrame);
		if( a != null ) {
			// deselect recording
			dmdWidget.resetSelection();
			aniListViewer.setSelection(StructuredSelection.EMPTY);
			goDmdGroup.updateAnimation(a);
			btnMask.setEnabled(a.getEditMode().equals(EditMode.FOLLOW));
			maskSpinner.setEnabled(false);
			if( a.getEditMode() == null || a.getEditMode().equals(EditMode.FIXED) ) {
				// old animation may be saved with wrong edit mode
				a.setEditMode(EditMode.REPLACE);
			}
			editModeViewer.setInput(mutable);
			editModeViewer.refresh();
			
			setEnableHashButtons(a.getEditMode().equals(EditMode.FOLLOW));
			
			selectedScene.set(a);

			int numberOfPlanes = a.getRenderer().getNumberOfPlanes();
			if( numberOfPlanes == 5) {
				numberOfPlanes = 4;
			}
			if (numberOfPlanes == 3) {
				numberOfPlanes = 2;
				goDmdGroup.transitionCombo.select(1);
			} else {
				goDmdGroup.transitionCombo.select(0);
			}

			setPaletteViewerByIndex(a.getPalIndex());

			setViewerSelection(editModeViewer, a.getEditMode());
			setDrawMaskByEditMode(a.getEditMode());// doesnt fire event?????
			dmd.setNumberOfSubframes(numberOfPlanes);
			paletteTool.setNumberOfPlanes(useGlobalMask?1:numberOfPlanes);

			setPlayingAni(a, scenesPosMap.getOrDefault(a.getDesc(), 0));
			
		} else {
			selectedScene.set(null);
			sceneListViewer.setSelection(StructuredSelection.EMPTY);
		}
		goDmdGroup.updateAniModel(a);
		btnRemoveScene.setEnabled(a!=null);
	}
	
	private void onRecordingSelectionChanged(Animation a) {
		log.info("onRecordingSelectionChanged: {}", a);
		Animation current = selectedRecording.get();
		if( current == null && a == null ) return;
		if(a!= null && current != null && a.getDesc().equals(current.getDesc())) return;
		if( current != null ) recordingsPosMap.put(current.getDesc(), current.actFrame);
		if( a != null) {		
			dmdWidget.resetSelection();
			sceneListViewer.setSelection(StructuredSelection.EMPTY);
			btnMask.setEnabled(true);
			maskSpinner.setEnabled(true);
			editModeViewer.setInput(immutable);
			editModeViewer.refresh();
			setEnableHashButtons(true);

			selectedRecording.set(a);
			setPlayingAni(a, recordingsPosMap.getOrDefault(a.getDesc(), 0));

			int numberOfPlanes = a.getRenderer().getNumberOfPlanes();
			if( numberOfPlanes == 5) {
				numberOfPlanes = 4;
			}
			if (numberOfPlanes == 3) {
				numberOfPlanes = 2;
				goDmdGroup.transitionCombo.select(1);
			} else {
				goDmdGroup.transitionCombo.select(0);
			}

			setViewerSelection(editModeViewer, a.getEditMode());
			//onColorMaskChecked(a.getEditMode()==EditMode.COLMASK);// doesnt fire event?????
			dmd.setNumberOfSubframes(numberOfPlanes);
			paletteTool.setNumberOfPlanes(useGlobalMask?1:numberOfPlanes);
			Set<Bookmark> set = project.bookmarksMap.get(a.getDesc());
			if( set != null ) bookmarkComboViewer.setInput(set);
			else bookmarkComboViewer.setInput(Collections.EMPTY_SET);
		} else {
			selectedRecording.set(null);
			aniListViewer.setSelection(StructuredSelection.EMPTY);
			bookmarkComboViewer.setInput(Collections.EMPTY_SET);
		}
		goDmdGroup.updateAniModel(a);
		btnRemoveAni.setEnabled(a != null);
		btnAddKeyframe.setEnabled(a != null);
		btnAddFrameSeq.setEnabled(a!=null && frameSeqViewer.getSelection() != null);
		btnAddEvent.setEnabled(a != null);
	}
	
	void onApplyPalette(Palette selectedPalette) {
		if (selectedPalMapping != null) {
			selectedPalMapping.palIndex = activePalette.index;
			log.info("change index in Keyframe {} to {}", selectedPalMapping.name, activePalette.index);
		}
		// change palette in ANI file
		if (selectedScene.isPresent()) {
			selectedScene.get().setPalIndex(activePalette.index);
		}
		
	}

	private void onPaletteChanged(Palette newPalette) {
		if( newPalette != null) {
			activePalette = newPalette;
			dmdWidget.setPalette(activePalette);
			paletteTool.setPalette(activePalette);
			clipboardHandler.setPalette(activePalette);
			log.info("new palette is {}", activePalette);
			setViewerSelection(paletteTypeComboViewer, activePalette.type);
			if (livePreviewActive)
				connector.switchToPal(activePalette.index, handle);
		}
	}

	<T extends Animation> void updateAnimationMapKey(String oldKey, String newKey, ObservableMap<String, T> anis) {
		ArrayList<T> tmp = new ArrayList<>();
		if (!oldKey.equals(newKey)) {
			anis.values().forEach(ani -> tmp.add(ani));
			anis.clear();
			tmp.forEach(ani -> anis.put(ani.getDesc(), ani));
		}
	}

	private void onFrameChanged(Frame frame) {
		if (livePreviewActive) {
			connector.sendFrame(frame, handle);
		}
	}

	private void onLivePreviewSwitched(boolean livePreviewIsOn) {
		if (livePreviewIsOn) {
			try {
				connector.switchToMode(DeviceMode.PinMame_RGB.ordinal(), null);
				handle = connector.connect(pin2dmdAdress);
				livePreviewActive = livePreviewIsOn;
				for( Palette pal : project.palettes ) {
					connector.upload(pal,handle);
				}
				// upload actual palette
				connector.switchToPal(activePalette.index, handle);
				setEnableUsbTooling(!livePreviewIsOn);
			} catch (RuntimeException ex) {
				msgUtil.warn("usb problem", "Message was: " + ex.getMessage());
				btnLivePreview.setSelection(false);
			}
		} else {
			if (handle != null) {
				try {
					connector.release(handle);
					livePreviewActive = livePreviewIsOn;
					setEnableUsbTooling(!livePreviewIsOn);
				} catch (RuntimeException ex) {
					msgUtil.warn("usb problem", "Message was: " + ex.getMessage());
				}
				handle = null;
			}
		}

	}

	private void setEnableUsbTooling(boolean enabled) {
		mntmUploadPalettes.setEnabled(enabled);
		mntmUploadProject.setEnabled(enabled);
	}
	
	private <T> T getSelectionFromViewer( AbstractListViewer viewer) {
		return (T) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
	}

	private void onAddFrameSeqClicked(SwitchMode switchMode) {
		// retrieve switch mode from selected scene edit mode!!
		if (!frameSeqViewer.getSelection().isEmpty()) {
			if (selectedHashIndex != -1) {
				Animation ani = getSelectionFromViewer(frameSeqViewer);
				//  add index, add ref to framesSeq
				if( !switchMode.equals(SwitchMode.PALETTE)) {
					switch(ani.getEditMode()) {
					case REPLACE:
						switchMode = SwitchMode.REPLACE;
						break;
					case COLMASK:
						switchMode = SwitchMode.ADD;
						break;
					case FOLLOW:
						switchMode = SwitchMode.FOLLOW;
						break;
					default:
						switchMode = SwitchMode.EVENT;
					}
				}
				PalMapping palMapping = new PalMapping(0, "KeyFrame " + ani.getDesc());
				palMapping.setDigest(hashes.get(selectedHashIndex));
				palMapping.palIndex = activePalette.index;
				palMapping.frameSeqName = ani.getDesc();
				palMapping.animationName = selectedRecording.get().getDesc();
				palMapping.switchMode = switchMode;
				palMapping.frameIndex = selectedRecording.get().actFrame;
				if (useGlobalMask) {
					palMapping.withMask = useGlobalMask;
					palMapping.maskNumber = actMaskNumber;
					project.masks.get(actMaskNumber).locked = true;
					onMaskChecked(true);
				}
				if (!checkForDuplicateKeyFrames(palMapping)) {
					project.palMappings.add(palMapping);
					keyframeTableViewer.refresh();
				} else {
					msgUtil.warn("duplicate hash", "There is already another Keyframe that uses the same hash");
				}
			} else {
				msgUtil.warn("no hash selected", "in order to create a key frame mapping, you must select a hash");
			}
		} else {
			msgUtil.warn("no scene selected", "in order to create a key frame mapping, you must select a scene");
		}
	}

	/**
	 * get current mask, either from scene or from on of the global masks
	 * @return
	 */
	private Mask getCurrentMask() {
		Mask maskToUse = null; 
		if( EditMode.FOLLOW.equals(editMode)) {
			// create mask from actual scene
			if( selectedScene.isPresent()) maskToUse = selectedScene.get().getCurrentMask();
		} else {
			// use one of the project masks
			maskToUse = project.masks.get(maskSpinner.getSelection());
		}
		return maskToUse;
	}

	/**
	 * button callback when mask checkbox is clicked.
	 * @param useMask
	 */
	 void onMaskChecked(boolean useMask) {
		// either we use masks with follow hash mode on scenes
		// or we use global masks on recordings
		if (useMask) {
			paletteTool.setNumberOfPlanes(1);
			dmdWidget.setMask(getCurrentMask());
			useGlobalMask = !EditMode.FOLLOW.equals(editMode);
		} else {
			paletteTool.setNumberOfPlanes(dmd.getNumberOfPlanes());
			dmdWidget.setShowMask(false);
			if( useGlobalMask ) { // commit edited global mask
				Mask mask = project.masks.get(maskSpinner.getSelection());
				mask.commit(dmd.getFrame().mask);
			}
			dmd.removeMask();
			useGlobalMask = false;
		}
		btnInvert.setEnabled(useMask);
		updateHashes(dmd.getFrame());
		previewDmd.redraw();
		setDrawMaskByEditMode(editMode);
		editAniObserver.update(animationHandler, null);
	}

	void onMaskNumberChanged(int newMaskNumber) {
		boolean hasChanged = false;
		if(newMaskNumber != actMaskNumber ) {
			log.info("mask number changed {} -> {}", actMaskNumber, newMaskNumber);
			actMaskNumber = newMaskNumber;
			hasChanged = true;
		}
		if (useGlobalMask && hasChanged) {
			Mask maskToUse = project.masks.get(newMaskNumber);
			dmdWidget.setMask(maskToUse);
			editAniObserver.update(animationHandler, null);
		}
	}

	private <T extends Animation> void onSortAnimations(ObservableMap<String, T> map) {
		ArrayList<Entry<String, T>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new Comparator<Entry<String, T>>() {

			@Override
			public int compare(Entry<String, T> o1, Entry<String, T> o2) {
				return o1.getValue().getDesc().compareTo(o2.getValue().getDesc());
			}
		});
		map.clear();
		for (Entry<String, T> entry : list) {
			map.put(entry.getKey(), (T)entry.getValue());
		}
	}

	private void dmdRedraw() {
		dmdWidget.redraw();
		previewDmd.redraw();
	}
	
	void onFrameSeqChanged(Animation ani) {
		btnAddFrameSeq.setEnabled(ani != null && selectedRecording.isPresent());
		//btnAddColormaskKeyFrame.setEnabled(selection.size() > 0);
	}

	void onKeyframeChanged(SelectionChangedEvent event) {
		PalMapping palMapping = getFirstSelected(event);
		if( palMapping != null) {
			if (palMapping.equals(selectedPalMapping)) {
				keyframeTableViewer.setSelection(StructuredSelection.EMPTY);
				selectedPalMapping = null;
				return;
			}
			// set new mapping
			selectedPalMapping = palMapping;

			log.debug("selected new palMapping {}", selectedPalMapping);

			selectedHashIndex = selectedPalMapping.hashIndex;

			// current firmware always checks with and w/o mask
			// btnMask.setSelection(selectedPalMapping.withMask);
			// btnMask.notifyListeners(SWT.Selection, new Event());

			txtDuration.setText(selectedPalMapping.durationInMillis + "");
			setPaletteViewerByIndex(selectedPalMapping.palIndex);
			if( palMapping.switchMode.equals(SwitchMode.EVENT)) {
				spinnerDeviceId.setSelection(palMapping.durationInMillis >> 8);
				spinnerEventId.setSelection(palMapping.durationInMillis & 0xFF);
			}
			
			for (int j = 0; j < numberOfHashes; j++) {
				btnHash[j].setSelection(j == selectedHashIndex);
			}
			
			setViewerSelection(sceneListViewer, null);
			setViewerSelection(aniListViewer, recordings.get(selectedPalMapping.animationName));
			
			if (selectedPalMapping.frameSeqName != null)
				setViewerSelection(frameSeqViewer, scenes.get(selectedPalMapping.frameSeqName));

			animationHandler.setPos(selectedPalMapping.frameIndex);

			if (selectedPalMapping.withMask) {
				String txt = btnHash[selectedHashIndex].getText();
				btnHash[selectedHashIndex].setText("M" + selectedPalMapping.maskNumber + " " + txt);
			}
			saveTimeCode = (int) selectedRecording.get().getTimeCode(selectedPalMapping.frameIndex);
		} else {
			selectedPalMapping = null;
		}
		btnDeleteKeyframe.setEnabled(palMapping != null);
		btnFetchDuration.setEnabled(palMapping != null);
	}

	void onPaletteTypeChanged(SelectionChangedEvent e) {
		PaletteType palType = getFirstSelected(e);
		activePalette.type = palType;
		if (PaletteType.DEFAULT.equals(palType)) {
			for (int i = 0; i < project.palettes.size(); i++) {
				if (i != activePalette.index) { // set previous default to
												// normal
					if (project.palettes.get(i).type.equals(PaletteType.DEFAULT)) {
						project.palettes.get(i).type = PaletteType.NORMAL;
					}
				}
			}
		}
	}

	/**
	 * check if dirty.
	 * 
	 * @return true, if not dirty or if user decides to ignore dirtyness (or
	 *         global ignore flag is set via cmdline)
	 */
	boolean dirtyCheck() {
		if (project.dirty && !nodirty) {
			int res = msgUtil.warn(SWT.ICON_WARNING | SWT.OK | SWT.CANCEL, "Unsaved Changes", "There are unsaved changes in project. Proceed?");
			return (res == SWT.OK);
		} else {
			return true;
		}
	}

	void onRemoveSelection() {
		if( dmdWidget.isDrawingEnabled() ) {
			SelectTool selectTool = (SelectTool) drawTools.get("select");
			selectTool.setSelection(0, 0, 0, 0);
		}
	}

	void onSelectAll() {
		if( dmdWidget.isDrawingEnabled() ) {
			SelectTool selectTool = (SelectTool) drawTools.get("select");
			selectTool.setSelection(0, 0, dmd.getWidth(), dmd.getHeight());
		}
	}

	void exportForGoDMD(String path, int version) {
		
	}

	/**
	 * called when dmd size has changed
	 * @param newSize
	 */
	private void refreshDmdSize(DmdSize newSize) {
		dmdSize = newSize;
		// reallocate some objects
		emptyMask = new byte[dmdSize.planeSize];
		Arrays.fill(emptyMask, (byte) 0xFF);
		// dmd, dmdWidget, previewWidget
		dmd.setSize(dmdSize.width, dmdSize.height);
		dmdWidget.setResolution(dmd);
		previewDmd.setResolution(dmd);
		dmdRedraw();
		onNewProject();
		// bindings
		log.info("dmd size changed to {}", newSize.label);
		ApplicationProperties.put(ApplicationProperties.PIN2DMD_DMDSIZE_PROP_KEY, dmdSize.ordinal());
	}

	private void onRedoClicked() {
		maskDmdObserver.redo();
		dmdRedraw();
	}

	private void onUndoClicked() {
		maskDmdObserver.undo();
		dmdRedraw();
	}

	public String getPrintableHashes(byte[] p) {
		StringBuffer hexString = new StringBuffer();
		for (int j = 0; j < p.length; j++)
			hexString.append(String.format("%02X", p[j]));
		return hexString.toString();
	}
	
	int actFrameOfSelectedAni = 0;
	private EditMode immutable[] = { EditMode.FIXED };
	private EditMode mutable[] = { EditMode.REPLACE, EditMode.COLMASK, EditMode.FOLLOW };

	
	private void updateHashes(Frame frame) {
		if( frame == null ) return;
		Frame f = new Frame(frame);
		Mask currentMask = getCurrentMask();
		if( dmdWidget.isShowMask() && currentMask != null) {
			f.setMask(getCurrentMask().data);
		}

		List<byte[]> hashes = f.getHashes();
		refreshHashButtons(hashes);

		saveHashes(hashes);
	}

	@Override
	public void notifyAni(AniEvent evt) {
		switch (evt.evtType) {
		case ANI:
			lblFrameNo.setText("" + evt.ani.actFrame);
			actFrameOfSelectedAni = evt.ani.actFrame;
			lblTcval.setText("" + evt.frame.timecode);
			txtDelayVal.setText("" + evt.frame.delay);
			lblPlanesVal.setText("" + evt.frame.planes.size());

			updateHashes(evt.frame);
			
			lastTimeCode = evt.frame.timecode;
			if (livePreviewActive && evt.frame != null) {
				connector.sendFrame(evt.frame, handle);
			}
			break;
		case CLOCK:
			lblFrameNo.setText("");
			lblTcval.setText("");
			// sourceList.deselectAll();
			for (int j = 0; j < 4; j++)
				btnHash[j++].setText(""); // clear hashes
			break;
		case CLEAR:
			for (int j = 0; j < 4; j++)
				btnHash[j++].setText(""); // clear hashes
			if (livePreviewActive) {
				connector.sendFrame(new Frame(), handle);
			}
			break;
		case FRAMECHANGE:
			updateHashes(evt.frame);
			break;
		}
		dmdRedraw();
	}

	private void refreshHashButtons(List<byte[]> hashes) {
		if( btnHash[0] == null ) return; // avoid NPE if not initialized
		int i = 0;
		for (byte[] p : hashes) {
			String hash = getPrintableHashes(p);
			// disable for empty frame: crc32 for empty frame is B2AA7578
			if (hash.startsWith("B2AA7578" /* "BF619EAC0CDF3F68D496EA9344137E8B" */)) {
				btnHash[i].setText("");
				btnHash[i].setEnabled(false);
			} else {
				btnHash[i].setText(hash);
				btnHash[i].setEnabled(btnHashEnabled[i]);
			}
			i++;
			if (i >= btnHash.length)
				break;
		}
		while (i < 4) {
			btnHash[i].setText("");
			btnHash[i].setEnabled(false);
			i++;
		}
	}

	public String getProjectFilename() {
		return projectFilename;
	}

	public void setProjectFilename(String projectFilename) {
		//mntmSaveProject.setEnabled(projectFilename!=null);
		this.projectFilename = projectFilename;
	}
}
