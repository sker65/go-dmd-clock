package com.rinke.solutions.pinball.view.swt;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.ObservableSetContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.SimpleBeanFactory;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.GoDmdGroup;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.PinDmdEditor.TabMode;
// muss hier raus (nur demo daten laden )
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
// maybe split in two types (for decoupling)
import com.rinke.solutions.pinball.animation.EditMode;
// 
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.ui.RegisterLicense;
import com.rinke.solutions.pinball.ui.UsbConfig;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.handler.BookmarkHandler;
import com.rinke.solutions.pinball.view.handler.HashButtonHandler;
import com.rinke.solutions.pinball.view.handler.KeyFrameHandler;
import com.rinke.solutions.pinball.view.handler.PlayingAniHandler;
import com.rinke.solutions.pinball.view.handler.RecordingsHandler;
import com.rinke.solutions.pinball.view.handler.ViewHandler;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.widget.CircleTool;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.FloodFillTool;
import com.rinke.solutions.pinball.widget.LineTool;
import com.rinke.solutions.pinball.widget.PaletteTool;
import com.rinke.solutions.pinball.widget.RectTool;
import com.rinke.solutions.pinball.widget.SelectTool;
import com.rinke.solutions.pinball.widget.SetPixelTool;
import com.thoughtworks.xstream.XStream;

import static com.rinke.solutions.pinball.view.model.ViewCmd.*;

import org.eclipse.core.databinding.beans.PojoProperties;

@Slf4j
public class MainView {
	
	private DataBindingContext m_bindingContext;
	
	private static final String HELP_URL = "http://pin2dmd.com/editor/";
	
	@SuppressWarnings("unchecked")
	private <T> T getFirstSelected(SelectionChangedEvent e) {
		IStructuredSelection selection = (IStructuredSelection) e.getSelection();
		return selection.isEmpty() ? null : (T)selection.getFirstElement();
	}

	@Autowired
	private CmdDispatcher dispatcher;

	private <T> void dispatchCmd(String name, T param) {
		log.debug("dispatchCmd '{}', param={}", name, param);
		dispatcher.dispatch(new Command<T>(param, name));
	}

	private void dispatchCmd(String name) {
		log.debug("dispatchCmd '{}'", name);
		dispatcher.dispatch(new Command<Object>(null, name));
	}
	
	private void dispatchCmd(String name, Object...params ) {
		log.debug("dispatchCmd '{}', params={}", name, params);
		dispatcher.dispatch(new Command<Object[]>(params, name));
	}
	
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Realm.runWithDefault(SWTObservables.getRealm(display), new Runnable() {
			public void run() {
				try {
					MainView mainView = new MainView();
					mainView.open();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	void init() {
		beanFactory = new SimpleBeanFactory();
		beanFactory.setValueProvider(new Config());
		beanFactory.scanPackages("com.rinke.solutions.pinball");
		
		// support bean factory methods by scanning @Bean annotation at method level
		beanFactory.setSingleton(display);
		beanFactory.setSingleton(shell);
		beanFactory.setSingleton("plugins", Arrays.asList(""));
		beanFactory.setSingleton("pluginsPath", "foo");
		beanFactory.setSingleton("beanFactory", beanFactory);
		
		beanFactory.inject(this);
	}
	
	@Autowired ViewModel vm;
	@Autowired Model model;
	@Autowired RecordingsHandler recordingsHandler;
	@Autowired Config config;
	
	/**
	 * Open the window.
	 */
	public void open() {
		display = Display.getDefault();
		shell = new Shell();
		
		Project p = new Project();
		p.palMappings.add( new PalMapping(1, "KeyFrame 1", SwitchMode.PALETTE));
		p.palMappings.add( new PalMapping(2, "KeyFrame 2", SwitchMode.REPLACE));
		p.palMappings.add( new PalMapping(2, "KeyFrame 3", SwitchMode.FOLLOW));
		p.palMappings.add( new PalMapping(2, "KeyFrame 4", SwitchMode.EVENT));
		
		init();
		
		vm.loadTestData();
		
		model.recordings.addObserver((o,a)->recordingsHandler.populate());

		// load some recording
		String filename = "./src/test/resources/drwho-dump.txt.gz";
		Animation animation = Animation.buildAnimationFromFile(filename, AnimationType.MAME);
		model.recordings.put(animation.getDesc(), animation);
		
		createContents(shell,vm.dmd);
		beanFactory.setSingleton("recentPalettesMenuManager",recentPalettesMenuManager);

		List<ViewHandler> handlers = beanFactory.getBeansOfType(ViewHandler.class);
		handlers.forEach(h->dispatcher.registerHandler(h));
		
		vm.addPropertyChangeListener( e->viewModelChanged(e) );

		vm.init();
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}


	protected Shell shell;
	RecentMenuManager recentProjectsMenuManager;
	RecentMenuManager recentPalettesMenuManager;
	RecentMenuManager recentAnimationsMenuManager;
	PaletteTool paletteTool;
		
	private int numberOfHashes = 4;

	/** instance level SWT widgets */
	Button btnHash[] = new Button[numberOfHashes];
	// UI / swt widgets
	Label lblTcval;
	Label lblFrameNo;
	Text txtDuration;
	Scale scale;
	ComboViewer paletteComboViewer;
	TableViewer recordingsListViewer;
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
	Map<String, DrawTool> drawTools = new HashMap<>();
	private Combo combo_3;

	private SimpleBeanFactory beanFactory;
	
	public void createHashButtons(Composite parent, int x, int y) {
		for (int i = 0; i < numberOfHashes; i++) {
			btnHash[i] = new Button(parent, SWT.CHECK | SWT.RADIO);
			if (i == 0)
				btnHash[i].setSelection(true);
			btnHash[i].setData(Integer.valueOf(i));
			btnHash[i].setText("Hash" + i);
			// btnHash[i].setFont(new Font(shell.getDisplay(), "sans", 10, 0));
			btnHash[i].setBounds(x, y + i * 16, 331, 18);
			btnHash[i].addListener(SWT.Selection, e -> dispatchCmd(HASH_SELECTED, (Integer) e.widget.getData()));
		}
	}

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
		
		previewDmd = new DMDWidget(grpKeyframe, SWT.DOUBLE_BUFFERED, vm.dmd, false);
		GridData gd_dmdPreWidget = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
		gd_dmdPreWidget.heightHint = 64;
		gd_dmdPreWidget.widthHint = 235;
		previewDmd.setLayoutData(gd_dmdPreWidget);
		previewDmd.setDrawingEnabled(false);
		previewDmd.setPalette(vm.previewPalettes.get(0));
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
		btnAddKeyframe.addListener(SWT.Selection, e -> dispatchCmd(ADD_KEY_FRAME, SwitchMode.PALETTE));
		
		Label label = new Label(grpKeyframe, SWT.NONE);
		label.setText(" ");
		new Label(grpKeyframe, SWT.NONE);
		
		Label lblScene = new Label(grpKeyframe, SWT.NONE);
		lblScene.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblScene.setText("Scene:");
		
		frameSeqViewer = new ComboViewer(grpKeyframe, SWT.NONE);
		frameSeqCombo = frameSeqViewer.getCombo();
		frameSeqCombo.setToolTipText("Choose frame sequence to use with key frame");
		GridData gd_frameSeqCombo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_frameSeqCombo.widthHint = 100;
		frameSeqCombo.setLayoutData(gd_frameSeqCombo);
		
		btnAddFrameSeq = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnAddFrameSeq = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddFrameSeq.widthHint = btnWidth;
		btnAddFrameSeq.setLayoutData(gd_btnAddFrameSeq);
		btnAddFrameSeq.setToolTipText("Adds a keyframe that triggers playback of a scene");
		btnAddFrameSeq.setText("Add ColorScene Switch");
		// TODO add switch mode depend on ani scene
		btnAddFrameSeq.addListener(SWT.Selection, e -> dispatchCmd(ADD_FRAME_SEQ,SwitchMode.REPLACE));
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
// VIEW			if (selectedPalMapping != null) {
//				selectedPalMapping.durationInMillis = Integer.parseInt(txtDuration.getText());
//				selectedPalMapping.durationInFrames = (int) selectedPalMapping.durationInMillis / 40;
//			}
		});
		
		btnFetchDuration = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnFetchDuration = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnFetchDuration.widthHint = btnWidth;
		btnFetchDuration.setLayoutData(gd_btnFetchDuration);
		btnFetchDuration.setToolTipText("Fetches duration for palette switches by calculating the difference between actual timestamp and keyframe timestamp");
		btnFetchDuration.setText("Fetch Duration");
		btnFetchDuration.setEnabled(false);
		btnFetchDuration.addListener(SWT.Selection, e -> dispatchCmd(FETCH_DURATION));
//	VIEW		if (selectedPalMapping != null) {
//				selectedPalMapping.durationInMillis = lastTimeCode - saveTimeCode;
//				selectedPalMapping.durationInFrames = (int) selectedPalMapping.durationInMillis / FRAME_RATE;
//				txtDuration.setText(selectedPalMapping.durationInMillis + "");
//			}
//		});
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
//VIEW		spinnerDeviceId.addModifyListener(e->onEventSpinnerChanged(spinnerDeviceId, 8));
		
		spinnerEventId = new Spinner(composite_5, SWT.BORDER);
		spinnerEventId.setMaximum(255);
		spinnerEventId.setMinimum(0);
//VIEW		spinnerEventId.addModifyListener(e->onEventSpinnerChanged(spinnerEventId, 0));
		
		btnAddEvent = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnAddEvent = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddEvent.widthHint = btnWidth;
		btnAddEvent.setLayoutData(gd_btnAddEvent);
		btnAddEvent.setText("Add Event");
		new Label(grpKeyframe, SWT.NONE);
		btnAddEvent.addListener(SWT.Selection, e->dispatchCmd(ADD_KEY_FRAME,SwitchMode.EVENT));
		
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
		mntmNewProject.addListener(SWT.Selection, e ->  dispatchCmd(NEW_PROJECT));
//		{
//			if (dirtyCheck()) {
//				onNewProject();
//			}
//		});

		MenuItem mntmLoadProject = new MenuItem(menu_1, SWT.NONE);
		mntmLoadProject.setText("Load Project\tCtrl-O");
		mntmLoadProject.setAccelerator(SWT.MOD1 + 'O');
		mntmLoadProject.addListener(SWT.Selection, e -> dispatchCmd(LOAD_PROJECT));

		mntmSaveProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveProject.setText("Save Project\tCrtl-S");
		mntmSaveProject.setAccelerator(SWT.MOD1 + 'S');
		mntmSaveProject.addListener(SWT.Selection, e -> dispatchCmd(SAVE_PROJECT,false));

		MenuItem mntmSaveAsProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveAsProject.setText("Save Project as\tShift-Crtl-S");
		mntmSaveAsProject.setAccelerator(SWT.MOD1|SWT.MOD2 + 'S');
		mntmSaveAsProject.addListener(SWT.Selection, e -> dispatchCmd(SAVE_PROJECT,true));

		MenuItem mntmRecentProjects = new MenuItem(menu_1, SWT.CASCADE);
		mntmRecentProjects.setText("Recent Projects");

		menuPopRecentProjects = new Menu(mntmRecentProjects);
		mntmRecentProjects.setMenu(menuPopRecentProjects);

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmImportProject = new MenuItem(menu_1, SWT.NONE);
		mntmImportProject.setText("Import Project");
		mntmImportProject.addListener(SWT.Selection, e -> dispatchCmd(IMPORT_PROJECT));

		MenuItem mntmExportRealPinProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportRealPinProject.setText("Export Project (real pin)");
		mntmExportRealPinProject.addListener(SWT.Selection, e -> dispatchCmd(EXPORT_REALPIN_PROJECT));

		MenuItem mntmExportVpinProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportVpinProject.setText("Export Project (virt pin)");
		mntmExportVpinProject.addListener(SWT.Selection, e -> dispatchCmd(EXPORT_VPIN_PROJECT));

		mntmUploadProject = new MenuItem(menu_1, SWT.NONE);
		mntmUploadProject.setText("Upload Project");
		mntmUploadProject.addListener(SWT.Selection, e -> dispatchCmd(UPLOAD_PROJECT));

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setText("Exit\tCtrl-Q");
		mntmExit.addListener(SWT.Selection, e -> dispatchCmd(QUIT) );
//	VIEW	{
//			if (dirtyCheck()) {
//				shell.close();
//				shell.dispose();
//			}
//		});

		MenuItem mntmedit = new MenuItem(menu, SWT.CASCADE);
		mntmedit.setText("&Edit");

		Menu menu_5 = new Menu(mntmedit);
		mntmedit.setMenu(menu_5);

		MenuItem mntmCut = new MenuItem(menu_5, SWT.NONE);
		mntmCut.setText("Cut \tCtrl-X");
		mntmCut.setAccelerator(SWT.MOD1 + 'X');
		mntmCut.addListener(SWT.Selection, e -> dispatchCmd(CUT, vm.selectedPalette));

		MenuItem mntmCopy = new MenuItem(menu_5, SWT.NONE);
		mntmCopy.setText("Copy \tCtrl-C");
		mntmCopy.setAccelerator(SWT.MOD1 + 'C');
		mntmCopy.addListener(SWT.Selection, e -> dispatchCmd(COPY, vm.selectedPalette));

		MenuItem mntmPaste = new MenuItem(menu_5, SWT.NONE);
		mntmPaste.setText("Paste\tCtrl-V");
		mntmPaste.setAccelerator(SWT.MOD1 + 'V');
		mntmPaste.addListener(SWT.Selection, e -> dispatchCmd(PASTE, vm.selectedPalette));
//	VIEW		clipboardHandler.onPaste(); 
//			dmdRedraw();
//		});

		MenuItem mntmPasteWithHover = new MenuItem(menu_5, SWT.NONE);
		mntmPasteWithHover.setText("Paste Over\tShift-Ctrl-V");
		mntmPasteWithHover.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'V');
		mntmPasteWithHover.addListener(SWT.Selection, e -> dispatchCmd(PASTE_HOOVER, vm.selectedPalette));
		
		MenuItem mntmSelectAll = new MenuItem(menu_5, SWT.NONE);
		mntmSelectAll.setText("Select All\tCtrl-A");
		mntmSelectAll.setAccelerator(SWT.MOD1 + 'A');
		mntmSelectAll.addListener(SWT.Selection, e -> dispatchCmd(SELECT_ALL, vm.selectedPalette));

		MenuItem mntmDeSelect = new MenuItem(menu_5, SWT.NONE);
		mntmDeSelect.setText("Remove Selection\tShift-Ctrl-A");
		mntmDeSelect.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'A');
		mntmDeSelect.addListener(SWT.Selection, e -> dispatchCmd(REMOVE_SELECTION, vm.selectedPalette) );

		new MenuItem(menu_5, SWT.SEPARATOR);

		mntmUndo = new MenuItem(menu_5, SWT.NONE);
		mntmUndo.setText("Undo\tCtrl-Z");
		mntmUndo.setAccelerator(SWT.MOD1 + 'Z');
		mntmUndo.addListener(SWT.Selection, e -> dispatchCmd(UNDO));

		mntmRedo = new MenuItem(menu_5, SWT.NONE);
		mntmRedo.setText("Redo\tShift-Ctrl-Z");
		mntmRedo.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'Z');
		mntmRedo.addListener(SWT.Selection, e -> dispatchCmd(REDO));

		MenuItem mntmAnimations = new MenuItem(menu, SWT.CASCADE);
		mntmAnimations.setText("&Animations");

		Menu menu_2 = new Menu(mntmAnimations);
		mntmAnimations.setMenu(menu_2);

		MenuItem mntmLoadAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmLoadAnimation.setText("Load Animation(s)");
		mntmLoadAnimation.addListener(SWT.Selection, e -> dispatchCmd(LOAD_ANI_WITH_FC, true));
		
		MenuItem mntmLoadRecordings = new MenuItem(menu_2, SWT.NONE);
		mntmLoadRecordings.setText("Load Recording(s)");
		mntmLoadRecordings.addListener(SWT.Selection, e -> dispatchCmd(LOAD_ANI_WITH_FC,true));
		
		MenuItem mntmSaveAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveAnimation.setText("Save Animation(s) ...");
		mntmSaveAnimation.addListener(SWT.Selection, e -> dispatchCmd(SAVE_ANI_WITH_FC,1));
		
		MenuItem mntmSaveSingleAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveSingleAnimation.setText("Save single Animation");
		mntmSaveSingleAnimation.addListener(SWT.Selection, e -> dispatchCmd(SAVE_SINGLE_ANI_WITH_FC,1));

		MenuItem mntmRecentAnimationsItem = new MenuItem(menu_2, SWT.CASCADE);
		mntmRecentAnimationsItem.setText("Recent Animations");

		mntmRecentAnimations = new Menu(mntmRecentAnimationsItem);
		mntmRecentAnimationsItem.setMenu(mntmRecentAnimations);

		new MenuItem(menu_2, SWT.SEPARATOR);

		MenuItem mntmExportAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmExportAnimation.setText("Export Animation as GIF");
		
		mntmExportAnimation.addListener(SWT.Selection, e -> dispatchCmd(EXPORT_GIF));
//VIEW		{
//			Animation ani = playingAnis.get(0);
//			Palette pal = project.palettes.get(ani.getPalIndex());
//			GifExporter exporter = new GifExporter(shell, pal, ani);
//			exporter.open();
//		});

		MenuItem mntmExportForGodmd = new MenuItem(menu_2, SWT.NONE);
		mntmExportForGodmd.setText("Export for goDMD ...");
		mntmExportForGodmd.addListener(SWT.Selection, e-> dispatchCmd(EXPORT_GO_DMD));
// VIEW		{
//			ExportGoDdmd exportGoDdmd = new ExportGoDdmd(shell, 0);
//			Pair<String,Integer> res = exportGoDdmd.open();
//			if( res != null ) {
//				exportForGoDMD( res.getLeft(), res.getRight() );
//			}
//		});

		MenuItem mntmpalettes = new MenuItem(menu, SWT.CASCADE);
		mntmpalettes.setText("&Palettes / Mode");
		Menu menu_3 = new Menu(mntmpalettes);
		mntmpalettes.setMenu(menu_3);

		MenuItem mntmLoadPalette = new MenuItem(menu_3, SWT.NONE);
		mntmLoadPalette.setText("Load Palette");
		mntmLoadPalette.addListener(SWT.Selection, e -> dispatchCmd(LOAD_PALETTE));

		MenuItem mntmSavePalette = new MenuItem(menu_3, SWT.NONE);
		mntmSavePalette.setText("Save Palette");
		mntmSavePalette.addListener(SWT.Selection, e -> dispatchCmd(SAVE_PALETTE));

		MenuItem mntmRecentPalettesItem = new MenuItem(menu_3, SWT.CASCADE);
		mntmRecentPalettesItem.setText("Recent Palettes");

		mntmRecentPalettes = new Menu(mntmRecentPalettesItem);
		mntmRecentPalettesItem.setMenu(mntmRecentPalettes);

		new MenuItem(menu_3, SWT.SEPARATOR);

		mntmUploadPalettes = new MenuItem(menu_3, SWT.NONE);
		mntmUploadPalettes.setText("Upload Palettes");
		mntmUploadPalettes.addListener(SWT.Selection, e -> dispatchCmd(UPLOAD_PALETTE,vm.selectedPalette));

		new MenuItem(menu_3, SWT.SEPARATOR);

		MenuItem mntmConfig = new MenuItem(menu_3, SWT.NONE);
		mntmConfig.setText("Configuration");
		mntmConfig.addListener(SWT.Selection, e -> dispatchCmd(CONFIGURATION));
// VIEW		{
//			Config config = new Config(shell);
//			config.open(pin2dmdAdress);
//			if( config.okPressed ) {
//				refreshPin2DmdHost(config.getPin2DmdHost());
//				if( !dmdSize.equals(config.getDmdSize())) {
//					refreshDmdSize(config.getDmdSize());
//				}
//			}
//		});

		MenuItem mntmDevice = new MenuItem(menu_3, SWT.NONE);
		mntmDevice.setText("Create Device File / WiFi");
		mntmDevice.addListener(SWT.Selection, e ->  dispatchCmd(DEVICE_CONFIGURATION));
// VIEW		{
//			DeviceConfig deviceConfig = new DeviceConfig(shell);
//			deviceConfig.open();
//		});

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
		mntmAbout.addListener(SWT.Selection, e -> dispatchCmd(ABOUT) );
	}

	public void setProjectFilename(String projectFilename) {
		vm.setProjectFilename(projectFilename);
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

		recentProjectsMenuManager = new RecentMenuManager("recentProject", 4, menuPopRecentProjects, 
				e -> dispatchCmd(LOAD_PROJECT, (String) e.widget.getData()), config);
		recentProjectsMenuManager.loadRecent();

		recentPalettesMenuManager = new RecentMenuManager("recentPalettes", 4, mntmRecentPalettes, 
				e -> dispatchCmd(LOAD_PALETTE, (String) e.widget.getData()), config);
		recentPalettesMenuManager.loadRecent();

		recentAnimationsMenuManager = new RecentMenuManager("recentAnimations", 4, mntmRecentAnimations, 
				e -> dispatchCmd(LOAD_ANI, (String) e.widget.getData(), true, true ), config );
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

		recordingsListViewer = new TableViewer(shell, SWT.BORDER | SWT.V_SCROLL);
		Table aniList = recordingsListViewer.getTable();
		GridData gd_aniList = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_aniList.heightHint = listHeight;
		gd_aniList.widthHint = listWidth;
		aniList.setLayoutData(gd_aniList);
		aniList.setLinesVisible(true);
		aniList.addKeyListener(new EscUnselect(recordingsListViewer));
		
//VIEW		aniListViewer.setContentProvider(ArrayContentProvider.getInstance());
//		aniListViewer.setLabelProvider(new LabelProviderAdapter<Animation>(ani -> ani.getDesc()));
//		aniListViewer.setInput(recordings.values());
//		aniListViewer.addSelectionChangedListener(event -> onRecordingSelectionChanged(getFirstSelected(event)));
		
		// created edit support for ani / recordings
//VIEW		TableViewerColumn viewerCol1 = new TableViewerColumn(aniListViewer, SWT.LEFT);
//		viewerCol1.setEditingSupport(new GenericTextCellEditor<Animation>(aniListViewer, ani -> ani.getDesc(), (ani, v) -> {
//			updateAnimationMapKey(ani.getDesc(), v, recordings);
//			ani.setDesc(v);
//		}));
//		viewerCol1.getColumn().setWidth(colWidth);
//		viewerCol1.setLabelProvider(new IconLabelProvider<Animation>(shell, o -> o.getIconAndText() ));
		
		sceneListViewer = new TableViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		Table sceneList = sceneListViewer.getTable();
		GridData gd_list = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_list.heightHint = listHeight;
		gd_list.widthHint = listWidth;
		sceneList.setLayoutData(gd_list);
		sceneList.setLinesVisible(true);
		sceneList.addKeyListener(new EscUnselect(sceneListViewer));
//VIEW		sceneListViewer.setContentProvider(ArrayContentProvider.getInstance());
//		sceneListViewer.setLabelProvider(new LabelProviderAdapter<Animation>(o -> o.getDesc()));
//		sceneListViewer.setInput(scenes.values());
//		sceneListViewer.addSelectionChangedListener(event -> onSceneSelectionChanged(getFirstSelected(event)));

//VIEW		TableViewerColumn viewerCol2 = new TableViewerColumn(sceneListViewer, SWT.LEFT);
//		viewerCol2.setEditingSupport(new GenericTextCellEditor<Animation>(sceneListViewer, ani -> ani.getDesc(), (ani, v) -> {
//			updateAnimationMapKey(ani.getDesc(), v, scenes);
//			ani.setDesc(v);
//			frameSeqViewer.refresh();
//		}));
//		viewerCol2.getColumn().setWidth(colWidth);
//		viewerCol2.setLabelProvider(new IconLabelProvider<Animation>(shell, ani -> ani.getIconAndText() ));

		keyframeTableViewer = new TableViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		Table keyframeList = keyframeTableViewer.getTable();
		GridData gd_keyframeList = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_keyframeList.heightHint = listHeight;
		gd_keyframeList.widthHint = listWidth;
		keyframeList.setLinesVisible(true);
		keyframeList.setLayoutData(gd_keyframeList);
		keyframeList.addKeyListener(new EscUnselect(keyframeTableViewer));

//VIEW		keyframeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
//		keyframeTableViewer.setInput(project.palMappings);
//		keyframeTableViewer.addSelectionChangedListener(event -> onKeyframeChanged(event));
//
//		TableViewerColumn viewerColumn = new TableViewerColumn(keyframeTableViewer, SWT.LEFT);
//		viewerColumn.setEditingSupport(new GenericTextCellEditor<PalMapping>(keyframeTableViewer, e -> e.name, (e, v) -> { e.name = v; }));
//
//		viewerColumn.getColumn().setWidth(colWidth);
//		viewerColumn.setLabelProvider(new IconLabelProvider<PalMapping>(shell, o -> Pair.of(o.switchMode.name().toLowerCase(), o.name ) ));

		dmdWidget = new DMDWidget(shell, SWT.DOUBLE_BUFFERED, dmd, true);
		// dmdWidget.setBounds(0, 0, 700, 240);
		GridData gd_dmdWidget = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_dmdWidget.heightHint = listHeight;
		gd_dmdWidget.widthHint = 816;
		dmdWidget.setLayoutData(gd_dmdWidget);
		dmdWidget.setPalette(vm.selectedPalette);
		dmdWidget.addListeners(l -> dispatchCmd(FRAME_CHANGED));

		Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new GridLayout(2, false));
		GridData gd_composite_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite_1.widthHint = listWidth;
		composite_1.setLayoutData(gd_composite_1);

		btnRemoveAni = new Button(composite_1, SWT.NONE);
		btnRemoveAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnRemoveAni.setText("Remove");
		btnRemoveAni.setEnabled(false);
		btnRemoveAni.addListener(SWT.Selection, e -> dispatchCmd(DELETE_RECORDING, vm.selectedRecording));
//VIEW		{
//			project.bookmarksMap.remove(selectedRecording.get().getDesc());
//			onRemove(selectedRecording, recordings);
//		} );

		btnSortAni = new Button(composite_1, SWT.NONE);
		btnSortAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortAni.setText("Sort");
		btnSortAni.addListener(SWT.Selection, e -> dispatchCmd(SORT_RECORDING));
		
		Composite composite_4 = new Composite(shell, SWT.NONE);
		composite_4.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		composite_4.setLayout(new GridLayout(2, false));
		
		btnRemoveScene = new Button(composite_4, SWT.NONE);
		btnRemoveScene.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnRemoveScene.setEnabled(false);
		btnRemoveScene.setText("Remove");
		btnRemoveScene.addListener(SWT.Selection, e -> dispatchCmd(DELETE_SCENE, vm.selectedScene) );
		
		Button btnSortScene = new Button(composite_4, SWT.NONE);
		btnSortScene.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortScene.setText("Sort");
		btnSortScene.addListener(SWT.Selection, e -> dispatchCmd(SORT_SCENES));

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
		btnDeleteKeyframe.addListener(SWT.Selection, e -> dispatchCmd(DELETE_KEY_FRAME, vm.selectedKeyFrame) );
//VIEW	{
//			if (selectedPalMapping != null) {
//				project.palMappings.remove(selectedPalMapping);
//				keyframeTableViewer.refresh();
//				checkReleaseMask();
//			}
//		});

		Button btnSortKeyFrames = new Button(composite_2, SWT.NONE);
		btnSortKeyFrames.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortKeyFrames.setText("Sort");
		btnSortKeyFrames.addListener(SWT.Selection, e ->dispatchCmd(SORT_KEY_FRAMES));
		new Label(composite_2, SWT.NONE);

		scale = new Scale(shell, SWT.NONE);
		GridData gd_scale = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_scale.widthHint = 826;
		scale.setLayoutData(gd_scale);
//VIEW		scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));
		
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
//VIEW		tabFolder.addListener(SWT.Selection, e->{
//			log.debug("tab changed: {}", tabFolder.getSelection().getText());
//			//this.tabMode = TabMode.fromLabel(tabFolder.getSelection().getText());
//		});

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
					dispatchCmd(DELAY_TXT_CHANGED, val);
//					int delay = StringUtils.isEmpty(val)?0:Integer.parseInt(val);
//					if( selectedScene.isPresent() ) {
//						CompiledAnimation ani = selectedScene.get();
//						if( actFrameOfSelectedAni<ani.frames.size() ) {
//							log.debug("Setting delay of frame {} to {}", actFrameOfSelectedAni, delay);
//							ani.frames.get(actFrameOfSelectedAni).delay = delay;
//						}
//						project.dirty = true;
//					}
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
// VIEW PropertyChange not Command		
//		btnLivePreview.addListener(SWT.Selection, e -> dispatchCmd("livePreviewChanged", btnLivePreview.getSelection()));

		Composite composite = new Composite(shell, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 779;
		composite.setLayoutData(gd_composite);
		composite.setLayout(new GridLayout(11, false));

		btnStartStop = new Button(composite, SWT.NONE);
		btnStartStop.setText("Start");
		btnStartStop.addListener(SWT.Selection, e -> dispatchCmd(START_STOP, vm.animationIsStopped ));

		btnPrev = new Button(composite, SWT.NONE);
		btnPrev.setText("<");
		btnPrev.addListener(SWT.Selection, e -> dispatchCmd(PREV_FRAME));

		btnNext = new Button(composite, SWT.NONE);
		btnNext.setText(">");
		btnNext.addListener(SWT.Selection, e -> dispatchCmd(NEXT_FRAME));

		btnMarkStart = new Button(composite, SWT.NONE);
		btnMarkStart.setToolTipText("Marks start of scene for cutting");
		btnMarkEnd = new Button(composite, SWT.NONE);
		btnCut = new Button(composite, SWT.NONE);
		btnCut.setToolTipText("Cuts out a new scene for editing and use a replacement or color mask");

		btnMarkStart.setText("Mark Start");
		btnMarkStart.addListener(SWT.Selection, e -> vm.cutInfo.setStart(vm.actFrame));

		btnMarkEnd.setText("Mark End");
		btnMarkEnd.addListener(SWT.Selection,  e -> vm.cutInfo.setEnd(vm.actFrame) );

		btnCut.setText("Cut");
		btnCut.addListener(SWT.Selection, e -> dispatchCmd(CUT_SCENE, vm.cutInfo) );
//	VIEW		// respect number of planes while cutting / copying
//				cutScene(selectedRecording.get(), cutInfo.getStart(), cutInfo.getEnd(), buildUniqueName(scenes));
//				log.info("cutting out scene from {}", cutInfo);
//				cutInfo.reset();
//			});

		Button btnIncPitch = new Button(composite, SWT.NONE);
		btnIncPitch.setText("+");
		btnIncPitch.addListener(SWT.Selection, e -> dmdWidget.incPitch());

		Button btnDecPitch = new Button(composite, SWT.NONE);
		btnDecPitch.setText("-");
		btnDecPitch.addListener(SWT.Selection, e -> dmdWidget.decPitch());
		
		bookmarkComboViewer = new ComboViewer(composite, SWT.NONE);
		combo_3 = bookmarkComboViewer.getCombo();
		GridData gd_combo_3 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_3.widthHint = 106;
		combo_3.setLayoutData(gd_combo_3);
		bookmarkComboViewer.addSelectionChangedListener(e->dispatchCmd(SELECTED_BOOKMARK,getFirstSelected(e)));
		registerProp(SELECTED_BOOKMARK, bookmarkComboViewer);
			
		Button btnNewBookMark = new Button(composite, SWT.NONE);
		btnNewBookMark.setText("New");
		btnNewBookMark.addListener(SWT.Selection, e -> dispatchCmd(NEW_BOOKMARK, vm.actFrame));

		Button btnDelBookmark = new Button(composite, SWT.NONE);
		btnDelBookmark.setText("Del.");
		btnDelBookmark.addListener(SWT.Selection, e -> dispatchCmd(DELETE_BOOKMARK, vm.selectedBookmark));

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
		paletteViewerCombo = paletteComboViewer.getCombo();
		GridData gd_paletteViewerCombo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_paletteViewerCombo.widthHint = 166;
		paletteViewerCombo.setLayoutData(gd_paletteViewerCombo);

		ObservableListContentProvider listContentProvider = new ObservableListContentProvider();
		paletteComboViewer.setLabelProvider(new LabelProviderAdapter<Palette>(o -> o.index + " - " + o.name));
		paletteComboViewer.setContentProvider(listContentProvider);
		//
		IObservableList palettesVmObserveList = BeanProperties.list("palettes").observe(vm);
		paletteComboViewer.setInput(palettesVmObserveList);
		//
		paletteComboViewer.addSelectionChangedListener( e-> setProp(vm, "selectedPalette", getFirstSelected(e)));
		registerProp("selectedPalette", paletteComboViewer);		
		//

		paletteTypeComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
		Combo combo_1 = paletteTypeComboViewer.getCombo();
		combo_1.setToolTipText("Type of palette. Default palette is choosen at start and after timed switch is expired");
		GridData gd_combo_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_1.widthHint = 96;
		combo_1.setLayoutData(gd_combo_1);
		paletteTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		paletteTypeComboViewer.setInput(vm.availablePaletteTypes);
		paletteTypeComboViewer.setLabelProvider(new LabelProviderAdapter<PaletteType>(o -> o.label));
		paletteTypeComboViewer.addSelectionChangedListener( e-> setProp(vm, "selectedPaletteType", getFirstSelected(e)));
		registerProp("selectedPaletteType", paletteTypeComboViewer);
						
		Button btnApplyPalette = new Button(grpPalettes, SWT.NONE);
		btnApplyPalette.setText("Apply");
		btnApplyPalette.addListener(SWT.Selection, e -> dispatchCmd(APPLY_PALETTE,vm.selectedPalette));
		
		btnNewPalette = new Button(grpPalettes, SWT.NONE);
		btnNewPalette.setToolTipText("Creates a new palette by copying the actual colors");
		btnNewPalette.setText("New");
		btnNewPalette.addListener(SWT.Selection, e -> dispatchCmd(NEW_PALETTE));

		btnRenamePalette = new Button(grpPalettes, SWT.NONE);
		btnRenamePalette.setToolTipText("Confirms the new palette name");
		btnRenamePalette.setText("Rename");
		btnRenamePalette.addListener(SWT.Selection, e -> dispatchCmd(RENAME_PALETTE, vm.selectedPalette));
//VIEW		{
//			String newName = paletteComboViewer.getCombo().getText();
//			if (newName.contains(" - ")) {
//				activePalette.name = newName.split(" - ")[1];
//				setPaletteViewerByIndex(activePalette.index);
//				paletteComboViewer.refresh();
//			} else {
//				msgUtil.warn("Illegal palette name", "Palette names must consist of palette index and name.\nName format therefore must be '<idx> - <name>'");
//				paletteComboViewer.getCombo().setText(activePalette.index + " - " + activePalette.name);
//			}
//
//		});
		
		Button btnDeletePalette = new Button(grpPalettes, SWT.NONE);
		btnDeletePalette.setText("Delete");
		btnDeletePalette.addListener(SWT.Selection, e->dispatchCmd(DELETE_PALETTE, vm.selectedPalette));

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
		paletteTool = new PaletteTool(shell, grpPal, SWT.FLAT | SWT.RIGHT, vm.selectedPalette);

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

//VIEW		drawTools.values().forEach(d->d.addObserver((dm,o)->updateHashes(dm)));

//VIEW changeListener on active palette
//		paletteTool.addListener(palette -> {
//			if (livePreviewActive) {
//				connector.upload(activePalette, handle);
//			}
//		});
		
		editModeViewer = new ComboViewer(grpDrawing, SWT.READ_ONLY);
		Combo combo_2 = editModeViewer.getCombo();
		GridData gd_combo_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_2.widthHint = 141;
		combo_2.setLayoutData(gd_combo_2);
		editModeViewer.setContentProvider(ArrayContentProvider.getInstance());
		editModeViewer.setLabelProvider(new LabelProviderAdapter<EditMode>(o -> o.label));
		editModeViewer.setInput(vm.availableEditModes);
		editModeViewer.addSelectionChangedListener(e->setProp(vm, "selectedEditMode", getFirstSelected(e)));
		registerProp("selectedEditMode", editModeViewer);
		
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

// bound availableEditmodes
// selectedEditMode
		
//		editModeViewer.setInput(EditMode.values());
//		if( selectedScene.isPresent() ) {
//			setViewerSelection(editModeViewer, selectedScene.get().getEditMode());
//		} else {
//			setViewerSelection(editModeViewer, EditMode.FIXED);
//		}
//		editModeViewer.addSelectionChangedListener(e -> onEditModeChanged(e));

		Label lblMaskNo = new Label(grpDrawing, SWT.NONE);
		lblMaskNo.setText("Mask No:");

		maskSpinner = new Spinner(grpDrawing, SWT.BORDER);
		maskSpinner.setToolTipText("select the mask to use");
		maskSpinner.setMinimum(0);
		maskSpinner.setMaximum(9);
		maskSpinner.setEnabled(false);
// 
//VIEW		maskSpinner.addListener(SWT.Selection, e -> onMaskNumberChanged(maskSpinner.getSelection()));
		
		btnMask = new Button(grpDrawing, SWT.CHECK);
		GridData gd_btnMask = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnMask.widthHint = 62;
		btnMask.setLayoutData(gd_btnMask);
		btnMask.setText("Mask");
		btnMask.setEnabled(false);
		btnMask.addListener(SWT.Selection, e -> dispatchCmd(MASK_ACTIVE,btnMask.getSelection()));
		
		btnInvert = new Button(grpDrawing, SWT.NONE);
		btnInvert.setText("Invert");
		btnInvert.addListener(SWT.Selection, e->dispatchCmd(INVERT_MASK));
		btnInvert.setEnabled(false);
		
		btnCopyToPrev = new Button(grpDrawing, SWT.NONE);
		btnCopyToPrev.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnCopyToPrev.setText("CopyToPrev");
		btnCopyToPrev.addListener(SWT.Selection, e->dispatchCmd(COPY_AND_MOVE_TO_PREV_FRAME));
		
		btnCopyToNext = new Button(grpDrawing, SWT.NONE);
		btnCopyToNext.setToolTipText("copy the actual scene / color mask to next frame and move forward");
		btnCopyToNext.setText("CopyToNext");
		btnCopyToNext.addListener(SWT.Selection, e->dispatchCmd(COPY_AND_MOVE_TO_NEXT_FRAME));
		
		btnUndo = new Button(grpDrawing, SWT.NONE);
		btnUndo.setText("&Undo");
		btnUndo.addListener(SWT.Selection, e -> dispatchCmd(UNDO));
		
		btnRedo = new Button(grpDrawing, SWT.NONE);
		btnRedo.setText("&Redo");
		btnRedo.addListener(SWT.Selection, e -> dispatchCmd(REDO));
		
		btnDeleteColMask = new Button(grpDrawing, SWT.NONE);
		btnDeleteColMask.setText("Delete");
		btnDeleteColMask.setEnabled(false);
		new Label(grpDrawing, SWT.NONE);
		btnDeleteColMask.addListener(SWT.Selection, e -> dispatchCmd(DELETE_MASK));
		
		// include binding created with SWT-Designer
		m_bindingContext = initDataBindings();
		
		// add some bindings manually (SWT Designer doesn't like parsing these??? )
		ObservableListContentProvider listContentProvider1 = new ObservableListContentProvider();
		recordingsListViewer.setLabelProvider(new IconLabelProvider<TypedLabel>(shell, o -> o.getIconAndText()));
		recordingsListViewer.setContentProvider(listContentProvider1);
		//
		IObservableList recordingsVmObserveList = BeanProperties.list("recordings").observe(vm);
		recordingsListViewer.setInput(recordingsVmObserveList);
		//
		ObservableListContentProvider listContentProvider_1 = new ObservableListContentProvider();
		sceneListViewer.setLabelProvider(new IconLabelProvider<TypedLabel>(shell, o -> o.getIconAndText()));
		sceneListViewer.setContentProvider(listContentProvider_1);
		//
		IObservableList scenesVmObserveList = BeanProperties.list("scenes").observe(vm);
		sceneListViewer.setInput(scenesVmObserveList);
		//
		ObservableListContentProvider listContentProvider_2 = new ObservableListContentProvider();
		keyframeTableViewer.setLabelProvider(new IconLabelProvider<TypedLabel>(shell, o -> o.getIconAndText()));
		keyframeTableViewer.setContentProvider(listContentProvider_2);
		//
		//IObservableList keyframesVmObserveList = BeanProperties.list("keyframes").observe(vm);
		keyframeTableViewer.setInput(vm.keyframes);

		ObservableSetContentProvider setContentProvider = new ObservableSetContentProvider();
		bookmarkComboViewer.setLabelProvider(new LabelProviderAdapter<Bookmark>(o -> o.name+" - "+o.pos));
		bookmarkComboViewer.setContentProvider(setContentProvider);
		//
		IObservableSet bookmarksVmObserveSet = BeanProperties.set("bookmarks").observe(vm);
		bookmarkComboViewer.setInput(bookmarksVmObserveSet);
		//
		
	}

	private void setViewerSelection(TableViewer viewer, Object sel) {
		if( sel != null ) viewer.setSelection(new StructuredSelection(sel));
		else viewer.setSelection(StructuredSelection.EMPTY);
	}

	void setViewerSelection(AbstractListViewer viewer, Object sel) {
		if( sel != null ) viewer.setSelection(new StructuredSelection(sel));
		else viewer.setSelection(StructuredSelection.EMPTY);
	}
	
	private void registerProp( String propName, AbstractListViewer viewer) {
		viewerBindingMap.put(propName, viewer);
	}
	
	Map<String,AbstractListViewer> viewerBindingMap = new HashMap<>();
	private Combo frameSeqCombo;

	private Display display;
	private Combo paletteViewerCombo;

	private void setProp(Object bean, String propName, Object val) {
		try {
			BeanUtils.setProperty(bean, propName, val);
		} catch (IllegalAccessException | InvocationTargetException e) {
			log.error("could not set {} on {}", propName, bean);
		}
	}

	private void viewModelChanged(PropertyChangeEvent e) {
		log.debug("view model changed {} {}->{}", e.getPropertyName(), e.getOldValue(), e.getNewValue());
		String p = e.getPropertyName();
		Object nv = e.getNewValue();
		Object ov = e.getOldValue();
		
		Stream<Button> btns = Arrays.stream(btnHash);
		// scan special arrays
		if( p.equals("hashButtonSelected") ) {
			btns.forEach(b->b.setSelection(vm.hashButtonSelected[(int) b.getData()]));
		} else
			// these should be done elsewhere
			if( p.equals("selectedKeyFrame") ) {
			vm.setDeleteKeyFrameEnabled(nv!=null);
		} else if( p.equals("selectedRecording") ) {
			vm.setDeleteRecordingEnabled(nv!=null);
		} else if( p.equals("selectedScene") ) {
			vm.setDeleteSceneEnabled(nv!=null);
		} 
		
		else if( p.equals("hashLbl")) {
			btns.forEach(b->b.setText(vm.hashLbl[(int) b.getData()]));
		} else if( p.equals("hashButtonEnabled") || p.equals("hashButtonsEnabled")) { // beware of the 's'
			btns.forEach(b->b.setEnabled(vm.hashButtonEnabled[(int) b.getData()] && vm.hashButtonsEnabled ));
		} else if( p.equals("livePreview") ) { 
			mntmUploadPalettes.setEnabled(((Boolean) nv).booleanValue());
			mntmUploadProject.setEnabled(((Boolean) nv).booleanValue());
		} 
		
		if( viewerBindingMap.containsKey(p)) {
			setViewerSelection(viewerBindingMap.get(p), nv);
		}
		Class<?> clz = nv!=null?nv.getClass():(ov!=null?ov.getClass():null);
		Class<?> clz1 = null;
		if( clz != null ) {
			String methodName = "on"+StringUtils.capitalize(p)+"Changed";
			clz1 = toPrimitive(clz);
			for(ViewHandler h : dispatcher.getViewHandlers()) {
//				log.debug("looking for method {}({}) in {}", methodName, clz,h); 
				try {
					Method m = h.getClass().getDeclaredMethod(methodName, new Class<?>[]{clz,clz});
					m.invoke(h, new Object[]{ov,nv});
				} catch ( SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					log.error("error calling {}", methodName, e1);
					//throw new RuntimeException("error calling "+methodName, e1);
				} catch (NoSuchMethodException e1) {
					if( clz1 != null ) {
						try {
							Method m = h.getClass().getDeclaredMethod(methodName, new Class<?>[]{clz1,clz1});
							m.invoke(h, new Object[]{ov,nv});
						} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
							log.error("error calling {}", methodName, e2);
						} catch (NoSuchMethodException e2) {
						}
						
					}
				}
			}
		}
		// new vm is ready
		XStream xStream = new XStream();
		log.info( xStream.toXML(vm) );
	}
	
	private Class<?> toPrimitive(Class<?> clz) {
		if( Integer.class.equals(clz) ) {
			return Integer.TYPE;
		}
		return null;
	}

	public PaletteTool getPaletteTool() {
		return paletteTool;
	}

	public void setPaletteTool(PaletteTool paletteTool) {
		 this.paletteTool = paletteTool;
	}
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeSelectionMaskSpinnerObserveWidget = WidgetProperties.selection().observe(maskSpinner);
		IObservableValue maskNumberVmObserveValue = BeanProperties.value("maskNumber").observe(vm);
		bindingContext.bindValue(observeSelectionMaskSpinnerObserveWidget, maskNumberVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnInvertObserveWidget = WidgetProperties.enabled().observe(btnInvert);
		IObservableValue maskInvertEnabledVmObserveValue = BeanProperties.value("maskInvertEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnInvertObserveWidget, maskInvertEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnRemoveAniObserveWidget = WidgetProperties.enabled().observe(btnRemoveAni);
		IObservableValue recordingDeleteEnabledVmObserveValue = BeanProperties.value("deleteRecordingEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnRemoveAniObserveWidget, recordingDeleteEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnRemoveSceneObserveWidget = WidgetProperties.enabled().observe(btnRemoveScene);
		IObservableValue sceneDeleteEnabledVmObserveValue = BeanProperties.value("deleteSceneEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnRemoveSceneObserveWidget, sceneDeleteEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnDeleteKeyframeObserveWidget = WidgetProperties.enabled().observe(btnDeleteKeyframe);
		IObservableValue keyFrameDeleteEnabledVmObserveValue = BeanProperties.value("deleteKeyFrameEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnDeleteKeyframeObserveWidget, keyFrameDeleteEnabledVmObserveValue, null, null);
		//
		IObservableValue observeSelectionScaleObserveWidget = WidgetProperties.selection().observe(scale);
		IObservableValue actFrameVmObserveValue = BeanProperties.value("actFrame").observe(vm);
		bindingContext.bindValue(observeSelectionScaleObserveWidget, actFrameVmObserveValue, null, null);
		//
		ObservableListContentProvider listContentProvider_3 = new ObservableListContentProvider();
		IObservableMap observeMap_2 = BeansObservables.observeMap(listContentProvider_3.getKnownElements(), TypedLabel.class, "label");
		frameSeqViewer.setLabelProvider(new ObservableMapLabelProvider(observeMap_2));
		frameSeqViewer.setContentProvider(listContentProvider_3);
		//
		IObservableList scenesVmObserveList = BeanProperties.list("scenes").observe(vm);
		frameSeqViewer.setInput(scenesVmObserveList);
		//
		IObservableValue observeSingleSelectionSceneListViewer = ViewerProperties.singleSelection().observe(sceneListViewer);
		IObservableValue selectedSceneVmObserveValue = BeanProperties.value("selectedScene").observe(vm);
		bindingContext.bindValue(observeSingleSelectionSceneListViewer, selectedSceneVmObserveValue, null, null);
		//
		IObservableValue observeSingleSelectionAniListViewer = ViewerProperties.singleSelection().observe(recordingsListViewer);
		IObservableValue selectedRecordingVmObserveValue = BeanProperties.value("selectedRecording").observe(vm);
		bindingContext.bindValue(observeSingleSelectionAniListViewer, selectedRecordingVmObserveValue, null, null);
		//
		IObservableValue observeSingleSelectionKeyframeTableViewer = ViewerProperties.singleSelection().observe(keyframeTableViewer);
		IObservableValue selectedKeyFrameVmObserveValue = BeanProperties.value("selectedKeyFrame").observe(vm);
		bindingContext.bindValue(observeSingleSelectionKeyframeTableViewer, selectedKeyFrameVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnUndoObserveWidget = WidgetProperties.enabled().observe(btnUndo);
		IObservableValue undoEnabledVmObserveValue = BeanProperties.value("undoEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnUndoObserveWidget, undoEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnRedoObserveWidget = WidgetProperties.enabled().observe(btnRedo);
		IObservableValue redoEnabledVmObserveValue = BeanProperties.value("redoEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnRedoObserveWidget, redoEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnAddKeyframeObserveWidget = WidgetProperties.enabled().observe(btnAddKeyframe);
		IObservableValue addPaletteSwitchEnabledVmObserveValue = BeanProperties.value("addPaletteSwitchEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnAddKeyframeObserveWidget, addPaletteSwitchEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnAddFrameSeqObserveWidget = WidgetProperties.enabled().observe(btnAddFrameSeq);
		IObservableValue addColSceneEnabledVmObserveValue = BeanProperties.value("addColSceneEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnAddFrameSeqObserveWidget, addColSceneEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnFetchDurationObserveWidget = WidgetProperties.enabled().observe(btnFetchDuration);
		IObservableValue addFetchEnabledVmObserveValue = BeanProperties.value("addFetchEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnFetchDurationObserveWidget, addFetchEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnAddEventObserveWidget = WidgetProperties.enabled().observe(btnAddEvent);
		IObservableValue addEventEnabledVmObserveValue = BeanProperties.value("addEventEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnAddEventObserveWidget, addEventEnabledVmObserveValue, null, null);
		//
		IObservableValue observeTextTxtDurationObserveWidget = WidgetProperties.text(SWT.Modify).observe(txtDuration);
		IObservableValue durationVmObserveValue = BeanProperties.value("duration").observe(vm);
		bindingContext.bindValue(observeTextTxtDurationObserveWidget, durationVmObserveValue, null, null);
		//
		IObservableValue observeSelectionSpinnerDeviceIdObserveWidget = WidgetProperties.selection().observe(spinnerDeviceId);
		IObservableValue eventHighVmObserveValue = BeanProperties.value("eventHigh").observe(vm);
		bindingContext.bindValue(observeSelectionSpinnerDeviceIdObserveWidget, eventHighVmObserveValue, null, null);
		//
		IObservableValue observeSelectionSpinnerEventIdObserveWidget = WidgetProperties.selection().observe(spinnerEventId);
		IObservableValue eventLowVmObserveValue = BeanProperties.value("eventLow").observe(vm);
		bindingContext.bindValue(observeSelectionSpinnerEventIdObserveWidget, eventLowVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnStartStopObserveWidget = WidgetProperties.enabled().observe(btnStartStop);
		IObservableValue startStopEnabledVmObserveValue = BeanProperties.value("startStopEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnStartStopObserveWidget, startStopEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnPrevObserveWidget = WidgetProperties.enabled().observe(btnPrev);
		IObservableValue prevEnabledVmObserveValue = BeanProperties.value("prevEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnPrevObserveWidget, prevEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnNextObserveWidget = WidgetProperties.enabled().observe(btnNext);
		IObservableValue nextEnabledVmObserveValue = BeanProperties.value("nextEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnNextObserveWidget, nextEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnMarkStartObserveWidget = WidgetProperties.enabled().observe(btnMarkStart);
		IObservableValue markStartEnabledVmObserveValue = BeanProperties.value("markStartEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnMarkStartObserveWidget, markStartEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnMarkEndObserveWidget = WidgetProperties.enabled().observe(btnMarkEnd);
		IObservableValue markStopEnabledVmObserveValue = BeanProperties.value("markStopEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnMarkEndObserveWidget, markStopEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnCutObserveWidget = WidgetProperties.enabled().observe(btnCut);
		IObservableValue cutEnabledVmObserveValue = BeanProperties.value("cutEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnCutObserveWidget, cutEnabledVmObserveValue, null, null);
		//
		IObservableValue observeTextBtnStartStopObserveWidget = WidgetProperties.text().observe(btnStartStop);
		IObservableValue startStopLabelVmObserveValue = BeanProperties.value("startStopLabel").observe(vm);
		bindingContext.bindValue(observeTextBtnStartStopObserveWidget, startStopLabelVmObserveValue, null, null);
		//
		IObservableValue observeTextLblFrameNoObserveWidget = WidgetProperties.text().observe(lblFrameNo);
		bindingContext.bindValue(observeTextLblFrameNoObserveWidget, actFrameVmObserveValue, null, null);
		//
		IObservableValue observeTextLblTcvalObserveWidget = WidgetProperties.text().observe(lblTcval);
		IObservableValue timecodeVmObserveValue = BeanProperties.value("timecode").observe(vm);
		bindingContext.bindValue(observeTextLblTcvalObserveWidget, timecodeVmObserveValue, null, null);
		//
		IObservableValue observeTextLblPlanesValObserveWidget = WidgetProperties.text().observe(lblPlanesVal);
		IObservableValue numberOfPlanesVmObserveValue = BeanProperties.value("numberOfPlanes").observe(vm);
		bindingContext.bindValue(observeTextLblPlanesValObserveWidget, numberOfPlanesVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnCopyToPrevObserveWidget = WidgetProperties.enabled().observe(btnCopyToPrev);
		IObservableValue copyToPrevEnabledVmObserveValue = BeanProperties.value("copyToPrevEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnCopyToPrevObserveWidget, copyToPrevEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledBtnCopyToNextObserveWidget = WidgetProperties.enabled().observe(btnCopyToNext);
		IObservableValue copyToNextEnabledVmObserveValue = BeanProperties.value("copyToNextEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledBtnCopyToNextObserveWidget, copyToNextEnabledVmObserveValue, null, null);
		//
		IObservableValue observeEnabledDrawToolBarObserveWidget = WidgetProperties.enabled().observe(drawToolBar);
		IObservableValue drawingEnabledVmObserveValue = BeanProperties.value("drawingEnabled").observe(vm);
		bindingContext.bindValue(observeEnabledDrawToolBarObserveWidget, drawingEnabledVmObserveValue, null, null);
		//
		IObservableValue observeSelectionBtnMaskObserveWidget = WidgetProperties.selection().observe(btnMask);
		IObservableValue maskVisibleVmObserveValue = BeanProperties.value("maskVisible").observe(vm);
		bindingContext.bindValue(observeSelectionBtnMaskObserveWidget, maskVisibleVmObserveValue, null, null);
		//
		IObservableValue showMaskDmdWidgetObserveValue = BeanProperties.value("showMask").observe(dmdWidget);
		bindingContext.bindValue(showMaskDmdWidgetObserveValue, maskVisibleVmObserveValue, null, null);
		//
		IObservableValue observeTextCombo_3ObserveWidget = WidgetProperties.text().observe(combo_3);
		IObservableValue editedBookmarkNameVmObserveValue = BeanProperties.value("editedBookmarkName").observe(vm);
		bindingContext.bindValue(observeTextCombo_3ObserveWidget, editedBookmarkNameVmObserveValue, null, null);
		//
		IObservableValue maskLockedDmdWidgetObserveValue = BeanProperties.value("maskLocked").observe(dmdWidget);
		IObservableValue maskLockedVmObserveValue = BeanProperties.value("maskLocked").observe(vm);
		bindingContext.bindValue(maskLockedDmdWidgetObserveValue, maskLockedVmObserveValue, null, null);
		//
		IObservableValue observeSelectionFrameSeqComboObserveWidget = WidgetProperties.selection().observe(frameSeqCombo);
		IObservableValue selectedFrameSeqVmObserveValue = BeanProperties.value("selectedFrameSeq").observe(vm);
		bindingContext.bindValue(observeSelectionFrameSeqComboObserveWidget, selectedFrameSeqVmObserveValue, null, null);
		//
		IObservableValue observeMinScaleObserveWidget = WidgetProperties.minimum().observe(scale);
		IObservableValue minFrameVmObserveValue = BeanProperties.value("minFrame").observe(vm);
		bindingContext.bindValue(observeMinScaleObserveWidget, minFrameVmObserveValue, null, null);
		//
		IObservableValue observeMaxScaleObserveWidget = WidgetProperties.maximum().observe(scale);
		IObservableValue maxFrameVmObserveValue = BeanProperties.value("maxFrame").observe(vm);
		bindingContext.bindValue(observeMaxScaleObserveWidget, maxFrameVmObserveValue, null, null);
		//
		IObservableValue observeTextTxtDelayValObserveWidget = WidgetProperties.text(SWT.Modify).observe(txtDelayVal);
		IObservableValue delayVmObserveValue = BeanProperties.value("delay").observe(vm);
		bindingContext.bindValue(observeTextTxtDelayValObserveWidget, delayVmObserveValue, null, null);
		//
		IObservableValue observeTextPaletteViewerComboObserveWidget = WidgetProperties.text().observe(paletteViewerCombo);
		IObservableValue editedPaletteNameVmObserveValue = BeanProperties.value("editedPaletteName").observe(vm);
		bindingContext.bindValue(observeTextPaletteViewerComboObserveWidget, editedPaletteNameVmObserveValue, null, null);
		//
		IObservableValue palettePaletteToolObserveValue = PojoProperties.value("palette").observe(paletteTool);
		IObservableValue selectedPaletteVmObserveValue = BeanProperties.value("selectedPalette").observe(vm);
		bindingContext.bindValue(palettePaletteToolObserveValue, selectedPaletteVmObserveValue, null, null);
		//
		IObservableValue observeSelectionBtnLivePreviewObserveWidget = WidgetProperties.selection().observe(btnLivePreview);
		IObservableValue livePreviewVmObserveValue = BeanProperties.value("livePreview").observe(vm);
		bindingContext.bindValue(observeSelectionBtnLivePreviewObserveWidget, livePreviewVmObserveValue, null, null);
		//
		return bindingContext;
	}
}
