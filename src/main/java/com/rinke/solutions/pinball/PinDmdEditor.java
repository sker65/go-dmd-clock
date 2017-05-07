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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.SimpleBeanFactory;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
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
import com.rinke.solutions.pinball.ui.Config;
import com.rinke.solutions.pinball.util.ApplicationProperties;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.util.ObservableProperty;
import com.rinke.solutions.pinball.util.RecentMenuManager;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.PaletteTool;
import com.rinke.solutions.pinball.widget.SelectTool;


//@Slf4j
public class PinDmdEditor implements EventHandler {

	static final int FRAME_RATE = 40;
	static final String HELP_URL = "http://pin2dmd.com/editor/";
	
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
	PaletteTool paletteTool;
	int selectedHashIndex;
	PalMapping selectedPalMapping;
	int saveTimeCode;
	ObservableList<Animation> frameSeqList = new ObservableList<>(new ArrayList<>());
	private Observer editAniObserver;
	boolean livePreviewActive;
	ConnectionHandle handle;
	String pin2dmdAdress = null;
	public DmdSize dmdSize;
	private int actMaskNumber;
	String pluginsPath;
	List<String> loadedPlugins = new ArrayList<>();

	// app state
	private String projectFilename;
	private EditMode editMode;
	boolean useGlobalMask;
	protected int lastTimeCode;

	Display display;
	protected Shell shell;

	// dependencies / collaborators
	AnimationHandler animationHandler = null;
	CyclicRedraw cyclicRedraw = new CyclicRedraw();
	// colaboration classes
	DMDClock clock = new DMDClock(false);
	FileHelper fileHelper = new FileHelper();	

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
	@Autowired ClipboardHandler clipboardHandler;
	@Autowired
	private AutosaveHandler autoSaveHandler;
	@Autowired
	MessageUtil msgUtil;
	@Autowired
	private SWTDispatcher dispatcher;
	
	View view;

	@Autowired
	ViewModel viewModel;
	
	Pin2DmdConnector connector;

	enum TabMode {
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
		previewPalettes = Palette.previewPalettes();
	}
	
	void updateHashes(Observable o) {
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
			if (view.dmdWidget != null && !view.dmdWidget.isDisposed())
				view.dmdWidget.redraw();
			if (view.previewDmd != null && !view.previewDmd.isDisposed())
				view.previewDmd.redraw();
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

		ObserverManager.bind(maskDmdObserver, e -> view.btnUndo.setEnabled(e), () -> maskDmdObserver.canUndo());
		ObserverManager.bind(maskDmdObserver, e -> view.btnRedo.setEnabled(e), () -> maskDmdObserver.canRedo());
		
		maskDmdObserver.addObserver((dmd,o)->updateHashes(dmd));
		
		ObserverManager.bind(maskDmdObserver, e -> view.mntmRedo.setEnabled(e), () -> maskDmdObserver.canRedo());
		ObserverManager.bind(maskDmdObserver, e -> view.mntmUndo.setEnabled(e), () -> maskDmdObserver.canUndo());
		// do some bindings
		editAniObserver = ObserverManager.bind(animationHandler, e -> this.enableDrawing(e), () -> animationIsEditable());
		
		ObserverManager.bind(animationHandler, e -> view.dmdWidget.setDrawingEnabled(e), () -> animationHandler.isStopped());

		ObserverManager.bind(animationHandler, e -> view.btnPrev.setEnabled(e), () -> animationHandler.isStopped() && animationHandler.hasAnimations());
		ObserverManager.bind(animationHandler, e -> view.btnNext.setEnabled(e), () -> animationHandler.isStopped() && animationHandler.hasAnimations());

		ObserverManager.bind(cutInfo, e -> view.btnCut.setEnabled(e), () -> (cutInfo.getStart() > 0 && cutInfo.getEnd() > 0));

		ObserverManager.bind(cutInfo, e -> view.btnMarkEnd.setEnabled(e), () -> (cutInfo.getStart() > 0));

		//ObserverManager.bind(animations, e -> btnStartStop.setEnabled(e), () -> !this.animations.isEmpty() && animationHandler.isStopped());
		ObserverManager.bind(recordings, e -> view.btnPrev.setEnabled(e), () -> !this.recordings.isEmpty());
		ObserverManager.bind(recordings, e -> view.btnNext.setEnabled(e), () -> !this.recordings.isEmpty());
		ObserverManager.bind(recordings, e -> view.btnMarkStart.setEnabled(e), () -> !this.recordings.isEmpty());

		ObserverManager.bind(recordings, e -> view.aniListViewer.refresh(), () -> true);
		
		ObserverManager.bind(scenes, e -> view.sceneListViewer.refresh(), () -> true);
		ObserverManager.bind(scenes, e -> buildFrameSeqList(), () -> true);

		// ObserverManager.bind(animations, e->btnAddFrameSeq.setEnabled(e),
		// ()->!frameSeqList.isEmpty());
	}

	private void enableDrawing(boolean e) {
		view.drawToolBar.setEnabled(e);
		if( e ) {
			if( selectedRecording.isPresent()) setViewerSelection(view.editModeViewer, selectedRecording.get().getEditMode());
		} else {
			setViewerSelection(view.editModeViewer, EditMode.FIXED);
		}
		view.btnCopyToNext.setEnabled(e);
		view.btnCopyToPrev.setEnabled(e);
		view.btnDeleteColMask.setEnabled(e);
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
		view.frameSeqViewer.setInput(frameSeqList);
		view.frameSeqViewer.refresh();
		if( !frameSeqList.isEmpty() ) setViewerSelection(view.frameSeqViewer, frameSeqList.get(0));
	}

	SimpleBeanFactory beanFactory;
	
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

		dmdSize = DmdSize.fromOrdinal(ApplicationProperties.getInteger(ApplicationProperties.PIN2DMD_DMDSIZE_PROP_KEY,0));
		pin2dmdAdress = ApplicationProperties.get(ApplicationProperties.PIN2DMD_ADRESS_PROP_KEY);

		emptyMask = new byte[dmdSize.planeSize];
		Arrays.fill(emptyMask, (byte) 0xFF);
		
		dmd = new DMD(dmdSize.width, dmdSize.height);
		
		licManager = LicenseManagerFactory.getInstance();
		connector = ConnectorFactory.create(pin2dmdAdress);

			view = new View();
			view.setEditor(this);
		
			this.shell = view.shell;

			checkForPlugins();

			view.init(args, dmd);

			this.init();
		
			view.setViewModel(viewModel);
				
		animationHandler = new AnimationHandler(playingAnis, clock, dmd);
		animationHandler.setScale(view.scale);
		animationHandler.setEventHandler(this);

		boolean goDMDenabled = ApplicationProperties.getBoolean(ApplicationProperties.GODMD_ENABLED_PROP_KEY);
		animationHandler.setEnableClock(goDMDenabled);
		
		onNewProject();
		view.setProject(project);
		view.createBindings();

		paletteHandler.setPalettes(this.project.palettes);
		paletteHandler.setActivePalette(project.palettes.get(0));
		paletteTool.setPalette(paletteHandler.getActivePalette());
		view.dmdWidget.setPalette(paletteHandler.getActivePalette());
		
		createBindings();
		
		view.open();

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
		
		this.display = Display.getCurrent();
		
		beanFactory = new SimpleBeanFactory();
		beanFactory.scanPackages("com.rinke.solutions.pinball");
		beanFactory.setSingleton("editor",this);
		beanFactory.setSingleton("dmd",dmd);
		beanFactory.setSingleton(shell);
		beanFactory.setSingleton(display);
		beanFactory.setSingleton(view.dmdWidget);
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
		cutScene.setPalIndex(paletteHandler.getActivePalette().index);
		cutScene.setProjectAnimation(true);
		cutScene.setEditMode(EditMode.REPLACE);
				
		scenes.put(name, cutScene);
		
		setViewerSelection(view.frameSeqViewer, cutScene);

		if( ApplicationProperties.getBoolean(ApplicationProperties.AUTOKEYFRAME)) {
			onAddFrameSeqClicked(SwitchMode.REPLACE);
		}

		setViewerSelection(view.sceneListViewer, cutScene);

		return cutScene;
	}

	void onNewProject() {
		project.clear();
		project.setDimension(dmdSize.width, dmdSize.height);
		paletteHandler.setActivePalette(project.palettes.get(0));
		setViewerSelection(view.paletteComboViewer,paletteHandler.getActivePalette());
		view.paletteComboViewer.refresh();
		view.keyframeTableViewer.refresh();
		recordings.clear();
		scenes.clear();
		playingAnis.clear();
		selectedRecording.set(null);
		selectedScene.set(null);
		animationHandler.setAnimations(playingAnis);
		setProjectFilename(null);
	}

	void onLoadProjectSelected() {
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
			msgUtil.warn( SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT,"Override warning", "the following frame seq have NOT been \nimported due to name collisions: " + collisions + "\n");
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
		setViewerSelection(view.paletteComboViewer, optPal.orElse(paletteHandler.getActivePalette()));
		log.info("setting pal.index to {}",palIndex);
	}
	
	protected void setupUIonProjectLoad() {
		setPaletteViewerByIndex(0);
		view.keyframeTableViewer.setInput(project.palMappings);
		for (Animation ani : recordings.values()) {
			setViewerSelection(view.aniListViewer, ani);
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
			view.recentProjectsMenuManager.populateRecent(filename);
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
	
	void onExportRealPinProject() {
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
	
	void onExportVirtualPinProject() {
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
		Stream.of(view.btnHash).forEach(e->e.setEnabled(enabled));
		if( !enabled )  Stream.of(view.btnHash).forEach(e->e.setSelection(false));
		Arrays.fill(btnHashEnabled, enabled);
	}

	public void createHashButtons(Composite parent, int x, int y) {
		for (int i = 0; i < numberOfHashes; i++) {
			view.btnHash[i] = new Button(parent, SWT.CHECK);
			if (i == 0)
				view.btnHash[i].setSelection(true);
			view.btnHash[i].setData(Integer.valueOf(i));
			view.btnHash[i].setText("Hash" + i);
			// btnHash[i].setFont(new Font(shell.getDisplay(), "sans", 10, 0));
			view.btnHash[i].setBounds(x, y + i * 16, 331, 18);
			view.btnHash[i].addListener(SWT.Selection, e -> {
				selectedHashIndex = (Integer) e.widget.getData();
				if (selectedPalMapping != null) {
					selectedPalMapping.hashIndex = selectedHashIndex;
				}
				for (int j = 0; j < numberOfHashes; j++) {
					if (j != selectedHashIndex)
						view.btnHash[j].setSelection(false);
				}
				int planes = Integer.parseInt(view.lblPlanesVal.getText() );
				// switch palettes in preview
				Palette palette = previewPalettes.get(planes==4?selectedHashIndex:selectedHashIndex*4);
				log.info("switch to preview palette: {}", palette);
				view.previewDmd.setPalette(palette);
			});
		}
	}

	public void onAddKeyFrameClicked(SwitchMode switchMode) {
		PalMapping palMapping = new PalMapping(paletteHandler.getActivePalette().index, "KeyFrame " + (project.palMappings.size() + 1));
		if (selectedHashIndex != -1) {
			palMapping.setDigest(hashes.get(selectedHashIndex));
		}
		palMapping.animationName = selectedRecording.get().getDesc();
		palMapping.frameIndex = selectedRecording.get().actFrame;
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = (view.spinnerDeviceId.getSelection()<<8) + view.spinnerEventId.getSelection();
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
			view.keyframeTableViewer.refresh();
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

	void onEventSpinnerChanged(Spinner spinner, int i) {
		if( selectedPalMapping != null ) {
			if( i == 8 ) {
				selectedPalMapping.durationInMillis = (selectedPalMapping.durationInMillis & 0xFF) + (spinner.getSelection()<<8); 
			} else {
				selectedPalMapping.durationInMillis = (selectedPalMapping.durationInMillis & 0xFF00) + (spinner.getSelection()<<0); 
			}
		}
	}

	void onEditModeChanged(EditMode mode) {
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
					setViewerSelection(view.editModeViewer,editMode);
				}
			}
			if(editMode.equals(EditMode.FOLLOW)) {
				animation.ensureMask();
			} else {
				view.btnMask.setSelection(false); // switch off mask if selected
				onMaskChecked(false);			
			}
			view.btnMask.setEnabled(editMode.equals(EditMode.FOLLOW));
			setEnableHashButtons(editMode.equals(EditMode.FOLLOW));
			animation.setEditMode(editMode);
		}
		setDrawMaskByEditMode(editMode);
		view.sceneListViewer.refresh();
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
		dmd.fill(view.dmdWidget.isShowMask()?(byte)0xFF:0);
		dmdRedraw();
	}

	void onStartStopClicked(boolean stopped) {
		if( stopped )
		{
			selectedScene.ifPresent(a->a.commitDMDchanges(dmd, hashes.get(selectedHashIndex)));
			animationHandler.start();
			display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
			view.btnStartStop.setText("Stop");
			view.dmdWidget.resetSelection();
		} else {
			animationHandler.stop();
			view.btnStartStop.setText("Start");
		}
	}

	void onPrevFrameClicked() {
		selectedScene.ifPresent(a->a.commitDMDchanges(dmd, hashes.get(selectedHashIndex)));
		animationHandler.prev();
		if( view.dmdWidget.isShowMask() ) {
			onMaskChecked(true);
		}
		if( editMode.equals(EditMode.FOLLOW) && selectedScene.isPresent()) {
			selectHash(selectedScene.get());
		}
		view.dmdWidget.resetSelection();
	}
	
	private void selectHash(CompiledAnimation ani) {
		byte[] crc32 = ani.frames.get(ani.actFrame).crc32;
		for( int i =0; i < hashes.size(); i++ ) {
			if( Arrays.equals(hashes.get(i), crc32) ) {
				selectedHashIndex = i;
			}
		}
		for (int j = 0; j < numberOfHashes; j++) {
			view.btnHash[j].setSelection(j == selectedHashIndex);
		}
	}

	void onNextFrameClicked() {
		selectedScene.ifPresent(a->a.commitDMDchanges(dmd,hashes.get(selectedHashIndex)));
		animationHandler.next();
		if( view.dmdWidget.isShowMask() ) {
			onMaskChecked(true);
		}
		if( editMode.equals(EditMode.FOLLOW) && selectedScene.isPresent()) {
			selectHash(selectedScene.get());
		}
		view.dmdWidget.resetSelection();
	}
	
	void onCopyAndMoveToNextFrameClicked() {
		onNextFrameClicked();
		CompiledAnimation ani = selectedScene.get();
		if( !ani.hasEnded() ) {
			ani.frames.get(ani.actFrame-1).copyToWithMask(dmd.getFrame(), dmd.getDrawMask());
			dmdRedraw();
		}
	}
	
	void onCopyAndMoveToPrevFrameClicked() {
		onPrevFrameClicked();
		CompiledAnimation ani = selectedScene.get();
		if( ani.getActFrame() >= ani.getStart() ) {
			ani.frames.get(ani.actFrame+1).copyToWithMask(dmd.getFrame(), dmd.getDrawMask());
			dmdRedraw();
		}
	}

	void onSortKeyFrames() {
		Collections.sort(project.palMappings, new Comparator<PalMapping>() {
			@Override
			public int compare(PalMapping o1, PalMapping o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		view.keyframeTableViewer.refresh();
	}

	private void setDrawMaskByEditMode(EditMode mode) {
		if( view.dmdWidget.isShowMask() ) {
			// only draw on mask
			// TODO mask drawing and plane drawing with mask should be controlled seperately
			dmd.setDrawMask( 0b00000001);
		} else {
			boolean drawWithMask = EditMode.COLMASK.equals(mode) || EditMode.FOLLOW.equals(mode);
			view.btnDeleteColMask.setEnabled(drawWithMask);
			dmd.setDrawMask(drawWithMask ? 0b11111000 : 0xFFFF);
		}
	}

	/**
	 * checks all pal mappings and releases masks if not used anymore
	 */
	void checkReleaseMask() {
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

	void onSceneSelectionChanged(CompiledAnimation a) {
		log.info("onSceneSelectionChanged: {}", a);
		Animation current = selectedScene.get();
		// detect changes
		if( current == null && a == null ) return;
		if(a!= null && current != null && a.getDesc().equals(current.getDesc())) return;
		if( current != null ) scenesPosMap.put(current.getDesc(), current.actFrame);
		if( a != null ) {
			// deselect recording
			view.dmdWidget.resetSelection();
			view.aniListViewer.setSelection(StructuredSelection.EMPTY);
			view.goDmdGroup.updateAnimation(a);
			view.btnMask.setEnabled(a.getEditMode().equals(EditMode.FOLLOW));
			view.maskSpinner.setEnabled(false);
			if( a.getEditMode() == null || a.getEditMode().equals(EditMode.FIXED) ) {
				// old animation may be saved with wrong edit mode
				a.setEditMode(EditMode.REPLACE);
			}
			view.editModeViewer.setInput(mutable);
			view.editModeViewer.refresh();
			
			setEnableHashButtons(a.getEditMode().equals(EditMode.FOLLOW));
			
			selectedScene.set(a);

			int numberOfPlanes = a.getRenderer().getNumberOfPlanes();
			if( numberOfPlanes == 5) {
				numberOfPlanes = 4;
			}
			if (numberOfPlanes == 3) {
				numberOfPlanes = 2;
				view.goDmdGroup.transitionCombo.select(1);
			} else {
				view.goDmdGroup.transitionCombo.select(0);
			}

			setPaletteViewerByIndex(a.getPalIndex());

			setViewerSelection(view.editModeViewer, a.getEditMode());
			setDrawMaskByEditMode(a.getEditMode());// doesnt fire event?????
			dmd.setNumberOfSubframes(numberOfPlanes);
			paletteTool.setNumberOfPlanes(useGlobalMask?1:numberOfPlanes);

			setPlayingAni(a, scenesPosMap.getOrDefault(a.getDesc(), 0));
			
		} else {
			selectedScene.set(null);
			view.sceneListViewer.setSelection(StructuredSelection.EMPTY);
		}
		view.goDmdGroup.updateAniModel(a);
		view.btnRemoveScene.setEnabled(a!=null);
	}
	
	void onRecordingSelectionChanged(Animation a) {
		log.info("onRecordingSelectionChanged: {}", a);
		Animation current = selectedRecording.get();
		if( current == null && a == null ) return;
		if(a!= null && current != null && a.getDesc().equals(current.getDesc())) return;
		if( current != null ) recordingsPosMap.put(current.getDesc(), current.actFrame);
		if( a != null) {		
			view.dmdWidget.resetSelection();
			view.sceneListViewer.setSelection(StructuredSelection.EMPTY);
			view.btnMask.setEnabled(true);
			view.maskSpinner.setEnabled(true);
			view.editModeViewer.setInput(immutable);
			view.editModeViewer.refresh();
			setEnableHashButtons(true);

			selectedRecording.set(a);
			setPlayingAni(a, recordingsPosMap.getOrDefault(a.getDesc(), 0));

			int numberOfPlanes = a.getRenderer().getNumberOfPlanes();
			if( numberOfPlanes == 5) {
				numberOfPlanes = 4;
			}
			if (numberOfPlanes == 3) {
				numberOfPlanes = 2;
				view.goDmdGroup.transitionCombo.select(1);
			} else {
				view.goDmdGroup.transitionCombo.select(0);
			}

			setViewerSelection(view.editModeViewer, a.getEditMode());
			//onColorMaskChecked(a.getEditMode()==EditMode.COLMASK);// doesnt fire event?????
			dmd.setNumberOfSubframes(numberOfPlanes);
			paletteTool.setNumberOfPlanes(useGlobalMask?1:numberOfPlanes);
			Set<Bookmark> set = project.bookmarksMap.get(a.getDesc());
			if( set != null ) view.bookmarkComboViewer.setInput(set);
			else view.bookmarkComboViewer.setInput(Collections.EMPTY_SET);
		} else {
			selectedRecording.set(null);
			view.aniListViewer.setSelection(StructuredSelection.EMPTY);
			view.bookmarkComboViewer.setInput(Collections.EMPTY_SET);
		}
		view.goDmdGroup.updateAniModel(a);
		view.btnRemoveAni.setEnabled(a != null);
		view.btnAddKeyframe.setEnabled(a != null);
		view.btnAddFrameSeq.setEnabled(a!=null && view.frameSeqViewer.getSelection() != null);
		view.btnAddEvent.setEnabled(a != null);
	}
	
	void onApplyPalette(Palette selectedPalette) {
		if (selectedPalMapping != null) {
			selectedPalMapping.palIndex = paletteHandler.getActivePalette().index;
			log.info("change index in Keyframe {} to {}", selectedPalMapping.name, paletteHandler.getActivePalette().index);
		}
		// change palette in ANI file
		if (selectedScene.isPresent()) {
			selectedScene.get().setPalIndex(paletteHandler.getActivePalette().index);
		}
		
	}

	void onPaletteChanged(Palette newPalette) {
		if( newPalette != null) {
			log.info("new palette is {}", newPalette);
			paletteHandler.setActivePalette(newPalette);
			view.dmdWidget.setPalette(newPalette);
			paletteTool.setPalette(newPalette);
			clipboardHandler.setPalette(newPalette);
			setViewerSelection(view.paletteTypeComboViewer,newPalette.type);
			if (livePreviewActive)
				connector.switchToPal(newPalette.index, handle);
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

	void onFrameChanged(Frame frame) {
		if (livePreviewActive) {
			connector.sendFrame(frame, handle);
		}
	}

	void onLivePreviewSwitched(boolean livePreviewIsOn) {
		if (livePreviewIsOn) {
			try {
				connector.switchToMode(DeviceMode.PinMame_RGB.ordinal(), null);
				handle = connector.connect(pin2dmdAdress);
				livePreviewActive = livePreviewIsOn;
				for( Palette pal : project.palettes ) {
					connector.upload(pal,handle);
				}
				// upload actual palette
				connector.switchToPal(paletteHandler.getActivePalette().index, handle);
				setEnableUsbTooling(!livePreviewIsOn);
			} catch (RuntimeException ex) {
				msgUtil.warn("usb problem", "Message was: " + ex.getMessage());
				view.btnLivePreview.setSelection(false);
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
		view.mntmUploadPalettes.setEnabled(enabled);
		view.mntmUploadProject.setEnabled(enabled);
	}
	
	<T> T getSelectionFromViewer( AbstractListViewer viewer) {
		return (T) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
	}

	void onAddFrameSeqClicked(SwitchMode switchMode) {
		// retrieve switch mode from selected scene edit mode!!
		if (!view.frameSeqViewer.getSelection().isEmpty()) {
			if (selectedHashIndex != -1) {
				Animation ani = getSelectionFromViewer(view.frameSeqViewer);
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
				palMapping.palIndex = paletteHandler.getActivePalette().index;
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
					view.keyframeTableViewer.refresh();
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
			maskToUse = project.masks.get(view.maskSpinner.getSelection());
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
			view.dmdWidget.setMask(getCurrentMask());
			useGlobalMask = !EditMode.FOLLOW.equals(editMode);
		} else {
			paletteTool.setNumberOfPlanes(dmd.getNumberOfPlanes());
			view.dmdWidget.setShowMask(false);
			if( useGlobalMask ) { // commit edited global mask
				Mask mask = project.masks.get(view.maskSpinner.getSelection());
				mask.commit(dmd.getFrame().mask);
			}
			dmd.removeMask();
			useGlobalMask = false;
		}
		view.btnInvert.setEnabled(useMask);
		updateHashes(dmd.getFrame());
		view.previewDmd.redraw();
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
			view.dmdWidget.setMask(maskToUse);
			editAniObserver.update(animationHandler, null);
		}
	}

	<T extends Animation> void onSortAnimations(ObservableMap<String, T> map) {
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

	void dmdRedraw() {
		view.dmdWidget.redraw();
		view.previewDmd.redraw();
	}
	
	void onFrameSeqChanged(Animation ani) {
		view.btnAddFrameSeq.setEnabled(ani != null && selectedRecording.isPresent());
		//btnAddColormaskKeyFrame.setEnabled(selection.size() > 0);
	}

	void onKeyframeChanged(PalMapping palMapping) {
		if( palMapping != null) {
			if (palMapping.equals(selectedPalMapping)) {
				view.keyframeTableViewer.setSelection(StructuredSelection.EMPTY);
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

			view.txtDuration.setText(selectedPalMapping.durationInMillis + "");
			setPaletteViewerByIndex(selectedPalMapping.palIndex);
			if( palMapping.switchMode.equals(SwitchMode.EVENT)) {
				view.spinnerDeviceId.setSelection(palMapping.durationInMillis >> 8);
				view.spinnerEventId.setSelection(palMapping.durationInMillis & 0xFF);
			}
			
			for (int j = 0; j < numberOfHashes; j++) {
				view.btnHash[j].setSelection(j == selectedHashIndex);
			}
			
			setViewerSelection(view.sceneListViewer, null);
			setViewerSelection(view.aniListViewer, recordings.get(selectedPalMapping.animationName));
			
			if (selectedPalMapping.frameSeqName != null)
				setViewerSelection(view.frameSeqViewer, scenes.get(selectedPalMapping.frameSeqName));

			animationHandler.setPos(selectedPalMapping.frameIndex);

			if (selectedPalMapping.withMask) {
				String txt = view.btnHash[selectedHashIndex].getText();
				view.btnHash[selectedHashIndex].setText("M" + selectedPalMapping.maskNumber + " " + txt);
			}
			saveTimeCode = (int) selectedRecording.get().getTimeCode(selectedPalMapping.frameIndex);
		} else {
			selectedPalMapping = null;
		}
		view.btnDeleteKeyframe.setEnabled(palMapping != null);
		view.btnFetchDuration.setEnabled(palMapping != null);
	}

	void onPaletteTypeChanged(PaletteType palType) {
		paletteHandler.getActivePalette().type = palType;
		if (PaletteType.DEFAULT.equals(palType)) {
			for (int i = 0; i < project.palettes.size(); i++) {
				if (i != paletteHandler.getActivePalette().index) { // set previous default to
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
		if( view.dmdWidget.isDrawingEnabled() ) {
			SelectTool selectTool = (SelectTool) drawTools.get("select");
			selectTool.setSelection(0, 0, 0, 0);
		}
	}

	void onSelectAll() {
		if( view.dmdWidget.isDrawingEnabled() ) {
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
	void refreshDmdSize(DmdSize newSize) {
		dmdSize = newSize;
		// reallocate some objects
		emptyMask = new byte[dmdSize.planeSize];
		Arrays.fill(emptyMask, (byte) 0xFF);
		// dmd, dmdWidget, previewWidget
		dmd.setSize(dmdSize.width, dmdSize.height);
		view.dmdWidget.setResolution(dmd);
		view.previewDmd.setResolution(dmd);
		dmdRedraw();
		onNewProject();
		// bindings
		log.info("dmd size changed to {}", newSize.label);
		ApplicationProperties.put(ApplicationProperties.PIN2DMD_DMDSIZE_PROP_KEY, dmdSize.ordinal());
	}

	void onRedoClicked() {
		maskDmdObserver.redo();
		dmdRedraw();
	}

	void onUndoClicked() {
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
		if( view.dmdWidget.isShowMask() && currentMask != null) {
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
			view.lblFrameNo.setText("" + evt.ani.actFrame);
			actFrameOfSelectedAni = evt.ani.actFrame;
			view.lblTcval.setText("" + evt.frame.timecode);
			view.txtDelayVal.setText("" + evt.frame.delay);
			view.lblPlanesVal.setText("" + evt.frame.planes.size());

			updateHashes(evt.frame);
			
			lastTimeCode = evt.frame.timecode;
			if (livePreviewActive && evt.frame != null) {
				connector.sendFrame(evt.frame, handle);
			}
			break;
		case CLOCK:
			view.lblFrameNo.setText("");
			view.lblTcval.setText("");
			// sourceList.deselectAll();
			for (int j = 0; j < 4; j++)
				view.btnHash[j++].setText(""); // clear hashes
			break;
		case CLEAR:
			for (int j = 0; j < 4; j++)
				view.btnHash[j++].setText(""); // clear hashes
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
		if( view.btnHash[0] == null ) return; // avoid NPE if not initialized
		int i = 0;
		for (byte[] p : hashes) {
			String hash = getPrintableHashes(p);
			// disable for empty frame: crc32 for empty frame is B2AA7578
			if (hash.startsWith("B2AA7578" /* "BF619EAC0CDF3F68D496EA9344137E8B" */)) {
				view.btnHash[i].setText("");
				view.btnHash[i].setEnabled(false);
			} else {
				view.btnHash[i].setText(hash);
				view.btnHash[i].setEnabled(btnHashEnabled[i]);
			}
			i++;
			if (i >= view.btnHash.length)
				break;
		}
		while (i < 4) {
			view.btnHash[i].setText("");
			view.btnHash[i].setEnabled(false);
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
