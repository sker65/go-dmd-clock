package com.rinke.solutions.pinball;

import java.awt.SplashScreen;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.rinke.solutions.pinball.PinDmdEditor.TabMode;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.swt.ActionAdapter;
import com.rinke.solutions.pinball.swt.CocoaGuiEnhancer;
import com.rinke.solutions.pinball.ui.About;
import com.rinke.solutions.pinball.ui.Config;
import com.rinke.solutions.pinball.ui.DeviceConfig;
import com.rinke.solutions.pinball.ui.ExportGoDdmd;
import com.rinke.solutions.pinball.ui.GifExporter;
import com.rinke.solutions.pinball.ui.RegisterLicense;
import com.rinke.solutions.pinball.ui.UsbConfig;
import com.rinke.solutions.pinball.util.RecentMenuManager;
import com.rinke.solutions.pinball.widget.CircleTool;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.FloodFillTool;
import com.rinke.solutions.pinball.widget.LineTool;
import com.rinke.solutions.pinball.widget.PaletteTool;
import com.rinke.solutions.pinball.widget.RectTool;
import com.rinke.solutions.pinball.widget.SelectTool;
import com.rinke.solutions.pinball.widget.SetPixelTool;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.property.Properties;

public class View {
	private DataBindingContext m_bindingContext;

	private PinDmdEditor editor = new PinDmdEditor();

	// ---------------- THE VIEW --------
	/** instance level SWT widgets */
	Button btnHash[];
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
	Button btnMask;
	private Menu menuPopRecentProjects;
	private Menu mntmRecentAnimations;
	private Menu mntmRecentPalettes;
	Spinner maskSpinner;
	GoDmdGroup goDmdGroup;
	MenuItem mntmUploadProject;
	MenuItem mntmUploadPalettes;
	Button btnCopyToNext;
	Button btnUndo;
	Button btnRedo;
	Button btnCopyToPrev;
	Button btnLivePreview;
	MenuItem mntmSaveProject;
	Button btnDeleteColMask;
	ComboViewer editModeViewer;
	TableViewer sceneListViewer;
	Button btnRemoveScene;
	Spinner spinnerDeviceId;
	Spinner spinnerEventId;
	Button btnAddEvent;
	private Composite grpKeyframe;
	private Text textProperty;
	ComboViewer bookmarkComboViewer;
	Button btnInvert;
	MenuItem mntmUndo;
	MenuItem mntmRedo;
	private Display display;
	Shell shell;
	
	RecentMenuManager recentProjectsMenuManager;
	RecentMenuManager recentPalettesMenuManager;
	RecentMenuManager recentAnimationsMenuManager;

	private PaletteHandler palHandler;

	private DMD dmd;
	private ViewModel viewModel;
	private Project project;
	private Combo combo;

	/**
	 * @param pinDmdEditor
	 */
	public View() {
		super();
		this.shell = new Shell();
		this.display = Display.getDefault();
		this.btnHash = new Button[this.editor.numberOfHashes];
	}

	/**
	 * Open the window.
	 * @wbp.parser.entryPoint
	 */
	public void open1() {
		Display display = Display.getDefault();
		shell = new Shell();
		this.dmd = new DMD(128, 32);
		createContents();
		this.dmdWidget.setPalette(new Palette(Palette.defaultColors(), 1, "foo"));
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public void init(String[] args, DMD dmd) {
		this.dmd = dmd;
		if (SWT.getPlatform().equals("cocoa")) {
			CocoaGuiEnhancer enhancer = new CocoaGuiEnhancer("Pin2dmd Editor");
			enhancer.hookApplicationMenu(display, e -> e.doit = editor.dirtyCheck(),
					new ActionAdapter(() -> new About(shell).open(editor.pluginsPath, editor.loadedPlugins)),
					new ActionAdapter(() -> new Config(shell).open(null)));
		}

		createContents();
	}

	void setViewerSelection(AbstractListViewer viewer, Object sel) {
		if( sel != null ) viewer.setSelection(new StructuredSelection(sel));
		else viewer.setSelection(StructuredSelection.EMPTY);
	}

	public void createBindings() {
		palHandler = editor.paletteHandler;
		palHandler.addObserver((o,arg)->{
			setViewerSelection(paletteComboViewer, ((PaletteHandler)o).getActivePalette());
		});
		m_bindingContext = initDataBindings();
	}

	public void open() {
		
		SplashScreen splashScreen = SplashScreen.getSplashScreen();
		if (splashScreen != null) {
			splashScreen.close();
		}

		shell.open();
		shell.layout();
		shell.addListener(SWT.Close, e -> {
			e.doit = editor.dirtyCheck();
		});

		GlobalExceptionHandler.getInstance().setDisplay(display);
		GlobalExceptionHandler.getInstance().setShell(shell);
	}

	/**
	 * Create contents of the window.
	 * 
	 * @wbp.parser.entryPoint
	 */
	void createContents() {
		shell.setSize(1380, 660);
		shell.setText("Pin2dmd - Editor");
		shell.setLayout(new GridLayout(4, false));

		createMenu(shell);

		this.editor.setProjectFilename(null);

		this.recentProjectsMenuManager = new RecentMenuManager("recentProject", 4, menuPopRecentProjects, e -> this.editor.loadProject((String) e.widget
				.getData()));
		this.recentProjectsMenuManager.loadRecent();

		this.recentPalettesMenuManager = new RecentMenuManager("recentPalettes", 4, mntmRecentPalettes,
				e -> this.editor.paletteHandler.loadPalette((String) e.widget.getData()));
		this.recentPalettesMenuManager.loadRecent();

		this.recentAnimationsMenuManager = new RecentMenuManager("recentAnimations", 4, mntmRecentAnimations, e -> this.editor.aniAction.loadAni(
				((String) e.widget.getData()), true, true));
		this.recentAnimationsMenuManager.loadRecent();

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
		aniListViewer.setInput(this.editor.recordings.values());
		aniListViewer.addSelectionChangedListener(event -> this.editor.onRecordingSelectionChanged(getFirstSelected(event)));

		// created edit support for ani / recordings
		
		TableViewerColumn viewerCol1 = new TableViewerColumn(aniListViewer, SWT.LEFT);
		viewerCol1.setEditingSupport(new GenericTextCellEditor<Animation>(aniListViewer, ani -> ani.getDesc(), (ani, v) -> {
			this.editor.updateAnimationMapKey(ani.getDesc(), v, this.editor.recordings);
			ani.setDesc(v);
		}));
		viewerCol1.getColumn().setWidth(colWidth);
		viewerCol1.setLabelProvider(new IconLabelProvider<Animation>(shell, o -> o.getIconAndText()));

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
		sceneListViewer.setInput(this.editor.scenes.values());
		sceneListViewer.addSelectionChangedListener(event -> this.editor.onSceneSelectionChanged(getFirstSelected(event)));

		
		TableViewerColumn viewerCol2 = new TableViewerColumn(sceneListViewer, SWT.LEFT);
		viewerCol2.setEditingSupport(new GenericTextCellEditor<Animation>(sceneListViewer, ani -> ani.getDesc(), (ani, v) -> {
			this.editor.updateAnimationMapKey(ani.getDesc(), v, this.editor.scenes);
			ani.setDesc(v);
			frameSeqViewer.refresh();
		}));
		viewerCol2.getColumn().setWidth(colWidth);
		viewerCol2.setLabelProvider(new IconLabelProvider<Animation>(shell, ani -> ani.getIconAndText()));		 

		keyframeTableViewer = new TableViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		Table keyframeList = keyframeTableViewer.getTable();
		GridData gd_keyframeList = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_keyframeList.heightHint = listHeight;
		gd_keyframeList.widthHint = listWidth;
		keyframeList.setLinesVisible(true);
		keyframeList.setLayoutData(gd_keyframeList);
		keyframeList.addKeyListener(new EscUnselect(keyframeTableViewer));

		// keyframeTableViewer.setLabelProvider(new
		// KeyframeLabelProvider(shell));
		keyframeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		keyframeTableViewer.setInput(this.editor.project.palMappings);
		keyframeTableViewer.addSelectionChangedListener(event -> this.editor.onKeyframeChanged(getFirstSelected(event)));
		
		TableViewerColumn viewerColumn = new TableViewerColumn(keyframeTableViewer, SWT.LEFT);
		viewerColumn.setEditingSupport(new GenericTextCellEditor<PalMapping>(keyframeTableViewer, e -> e.name, (e, v) -> {
			e.name = v;
		}));

		viewerColumn.getColumn().setWidth(colWidth);
		viewerColumn.setLabelProvider(new IconLabelProvider<PalMapping>(shell, o -> Pair.of(o.switchMode.name().toLowerCase(), o.name)));		 

		dmdWidget = new DMDWidget(shell, SWT.DOUBLE_BUFFERED, dmd, true);
		// dmdWidget.setBounds(0, 0, 700, 240);
		GridData gd_dmdWidget = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_dmdWidget.heightHint = listHeight;
		gd_dmdWidget.widthHint = 816;
		dmdWidget.setLayoutData(gd_dmdWidget);
		dmdWidget.addListeners(l -> this.editor.onFrameChanged(l));

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
			this.editor.project.bookmarksMap.remove(this.editor.selectedRecording.get().getDesc());
			this.editor.onRemove(this.editor.selectedRecording, this.editor.recordings);
		});

		btnSortAni = new Button(composite_1, SWT.NONE);
		btnSortAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortAni.setText("Sort");
		btnSortAni.addListener(SWT.Selection, e -> this.editor.onSortAnimations(this.editor.recordings));

		Composite composite_4 = new Composite(shell, SWT.NONE);
		composite_4.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		composite_4.setLayout(new GridLayout(2, false));

		btnRemoveScene = new Button(composite_4, SWT.NONE);
		btnRemoveScene.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnRemoveScene.setEnabled(false);
		btnRemoveScene.setText("Remove");
		btnRemoveScene.addListener(SWT.Selection, e -> this.editor.onRemove(this.editor.selectedScene, this.editor.scenes));

		Button btnSortScene = new Button(composite_4, SWT.NONE);
		btnSortScene.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortScene.setText("Sort");
		btnSortScene.addListener(SWT.Selection, e -> this.editor.onSortAnimations(this.editor.scenes));

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
			if (this.editor.selectedPalMapping != null) {
				this.editor.project.palMappings.remove(this.editor.selectedPalMapping);
				keyframeTableViewer.refresh();
				this.editor.checkReleaseMask();
			}
		});

		Button btnSortKeyFrames = new Button(composite_2, SWT.NONE);
		btnSortKeyFrames.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnSortKeyFrames.setText("Sort");
		btnSortKeyFrames.addListener(SWT.Selection, e -> this.editor.onSortKeyFrames());
		new Label(composite_2, SWT.NONE);

		scale = new Scale(shell, SWT.NONE);
		GridData gd_scale = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_scale.widthHint = 826;
		scale.setLayoutData(gd_scale);
		scale.addListener(SWT.Selection, e -> this.editor.animationHandler.setPos(scale.getSelection()));

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

		goDmdGroup = new GoDmdGroup(tabFolder);
		tbtmGodmd.setControl(goDmdGroup.getGrpGoDMDCrtls());

		CTabItem tbtmPropertyText = new CTabItem(tabFolder, SWT.NONE);
		tbtmPropertyText.setText(TabMode.PROP.label);

		textProperty = new Text(tabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		tbtmPropertyText.setControl(textProperty);

		tabFolder.setSelection(tbtmKeyframe);
		tabFolder.addListener(SWT.Selection, e -> {
			PinDmdEditor.log.debug("tab changed: {}", tabFolder.getSelection().getText());
			// this.tabMode =
			// TabMode.fromLabel(tabFolder.getSelection().getText());
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
				if (event.keyCode == SWT.CR) {
					String val = txtDelayVal.getText();
					int delay = StringUtils.isEmpty(val) ? 0 : Integer.parseInt(val);
					if (View.this.editor.selectedScene.isPresent()) {
						CompiledAnimation ani = View.this.editor.selectedScene.get();
						if (View.this.editor.actFrameOfSelectedAni < ani.frames.size()) {
							PinDmdEditor.log.debug("Setting delay of frame {} to {}", View.this.editor.actFrameOfSelectedAni, delay);
							ani.frames.get(View.this.editor.actFrameOfSelectedAni).delay = delay;
						}
						View.this.editor.project.dirty = true;
					}
				}
			}
		});

		txtDelayVal.addListener(SWT.Verify, e -> e.doit = Pattern.matches("^[0-9]*$", e.text));

		Label lblPlanes = new Label(grpDetails, SWT.NONE);
		lblPlanes.setText("Planes:");

		lblPlanesVal = new Label(grpDetails, SWT.NONE);
		lblPlanesVal.setText("---");
		new Label(grpDetails, SWT.NONE);

		btnLivePreview = new Button(grpDetails, SWT.CHECK);
		btnLivePreview.setToolTipText("controls live preview to real display device");
		btnLivePreview.setText("Live Preview");
		btnLivePreview.addListener(SWT.Selection, e -> this.editor.onLivePreviewSwitched(btnLivePreview.getSelection()));

		Composite composite = new Composite(shell, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 779;
		composite.setLayoutData(gd_composite);
		composite.setLayout(new GridLayout(11, false));

		btnStartStop = new Button(composite, SWT.NONE);
		btnStartStop.setText("Start");
		btnStartStop.addListener(SWT.Selection, e -> this.editor.onStartStopClicked(this.editor.animationHandler.isStopped()));

		btnPrev = new Button(composite, SWT.NONE);
		btnPrev.setText("<");
		btnPrev.addListener(SWT.Selection, e -> this.editor.onPrevFrameClicked());

		btnNext = new Button(composite, SWT.NONE);
		btnNext.setText(">");
		btnNext.addListener(SWT.Selection, e -> this.editor.onNextFrameClicked());

		btnMarkStart = new Button(composite, SWT.NONE);
		btnMarkStart.setToolTipText("Marks start of scene for cutting");
		btnMarkEnd = new Button(composite, SWT.NONE);
		btnCut = new Button(composite, SWT.NONE);
		btnCut.setToolTipText("Cuts out a new scene for editing and use a replacement or color mask");

		btnMarkStart.setText("Mark Start");
		btnMarkStart.addListener(SWT.Selection, e -> {
			this.editor.cutInfo.setStart(this.editor.selectedRecording.get().actFrame);
		});

		btnMarkEnd.setText("Mark End");
		btnMarkEnd.addListener(SWT.Selection, e -> {
			this.editor.cutInfo.setEnd(this.editor.selectedRecording.get().actFrame);
		});

		btnCut.setText("Cut");
		btnCut.addListener(
				SWT.Selection,
				e -> {
					// respect number of planes while cutting / copying
					this.editor.cutScene(this.editor.selectedRecording.get(), this.editor.cutInfo.getStart(), this.editor.cutInfo.getEnd(),
							this.editor.buildUniqueName(this.editor.scenes));
					PinDmdEditor.log.info("cutting out scene from {}", this.editor.cutInfo);
					this.editor.cutInfo.reset();
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
		bookmarkComboViewer.setLabelProvider(new LabelProviderAdapter<Bookmark>(o -> o.name + " - " + o.pos));
		bookmarkComboViewer.addSelectionChangedListener(e -> {
			Bookmark bm = getFirstSelected(e);
			if (bm != null && this.editor.selectedRecording.isPresent()) {
				this.editor.animationHandler.setPos(bm.pos);
			}
		});

		Button btnNewBookMark = new Button(composite, SWT.NONE);
		btnNewBookMark.setText("New");
		btnNewBookMark.addListener(SWT.Selection, e -> {
			if (this.editor.selectedRecording.isPresent()) {
				Animation r = this.editor.selectedRecording.get();
				Set<Bookmark> set = this.editor.project.bookmarksMap.get(r.getDesc());
				if (set == null) {
					set = new TreeSet<Bookmark>();
					this.editor.project.bookmarksMap.put(r.getDesc(), set);

				}
				String bookmarkName = bookmarkComboViewer.getCombo().getText();
				set.add(new Bookmark(bookmarkName, r.actFrame));
				bookmarkComboViewer.setInput(set);
				bookmarkComboViewer.refresh();
			}
		});

		Button btnDelBookmark = new Button(composite, SWT.NONE);
		btnDelBookmark.setText("Del.");
		btnDelBookmark.addListener(SWT.Selection, e -> {
			if (this.editor.selectedRecording.isPresent()) {
				Animation r = this.editor.selectedRecording.get();
				Set<Bookmark> set = this.editor.project.bookmarksMap.get(r.getDesc());
				if (set != null) {
					set.remove(this.editor.getSelectionFromViewer(bookmarkComboViewer));
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
		combo = paletteComboViewer.getCombo();
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_combo.widthHint = 166;
		combo.setLayoutData(gd_combo);
		//paletteComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		//paletteComboViewer.setLabelProvider(new LabelProviderAdapter<Palette>(o -> o.index + " - " + o.name));
		//paletteComboViewer.setInput(this.editor.project.palettes);
		paletteComboViewer.addSelectionChangedListener(event -> this.editor.onPaletteChanged(getFirstSelected(event)));
		paletteComboViewer.getCombo().select(0);

		paletteTypeComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
		Combo combo_1 = paletteTypeComboViewer.getCombo();
		combo_1.setToolTipText("Type of palette. Default palette is choosen at start and after timed switch is expired");
		GridData gd_combo_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo_1.widthHint = 96;
		combo_1.setLayoutData(gd_combo_1);
		paletteTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		paletteTypeComboViewer.setInput(PaletteType.values());
		//this.editor.setViewerSelection(paletteTypeComboViewer, this.palHandler.getActivePalette().type);
		paletteTypeComboViewer.addSelectionChangedListener(e -> this.editor.onPaletteTypeChanged(getFirstSelected(e)));

		Button btnApplyPalette = new Button(grpPalettes, SWT.NONE);
		btnApplyPalette.setText("Apply");
		btnApplyPalette.addListener(SWT.Selection, e -> this.editor.onApplyPalette(this.palHandler.getActivePalette()));

		btnNewPalette = new Button(grpPalettes, SWT.NONE);
		btnNewPalette.setToolTipText("Creates a new palette by copying the actual colors");
		btnNewPalette.setText("New");
		btnNewPalette.addListener(SWT.Selection, e -> this.editor.paletteHandler.onNewPalette());

		btnRenamePalette = new Button(grpPalettes, SWT.NONE);
		btnRenamePalette.setToolTipText("Confirms the new palette name");
		btnRenamePalette.setText("Rename");
		btnRenamePalette.addListener(SWT.Selection, e -> {
			String newName = paletteComboViewer.getCombo().getText();
			if (newName.contains(" - ")) {
				this.palHandler.getActivePalette().name = newName.split(" - ")[1];
				this.editor.setPaletteViewerByIndex(this.palHandler.getActivePalette().index);
				paletteComboViewer.refresh();
			} else {
				this.editor.msgUtil.warn("Illegal palette name",
						"Palette names must consist of palette index and name.\nName format therefore must be '<idx> - <name>'");
				paletteComboViewer.getCombo().setText(this.palHandler.getActivePalette().index + " - " + this.palHandler.getActivePalette().name);
			}

		});

		Button btnDeletePalette = new Button(grpPalettes, SWT.NONE);
		btnDeletePalette.setText("Delete");
		btnDeletePalette.addListener(SWT.Selection, e -> this.editor.paletteHandler.onDeletePalette());

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
		this.editor.paletteTool = new PaletteTool(shell, grpPal, SWT.FLAT | SWT.RIGHT);
		this.editor.paletteTool.addListener(dmdWidget);

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
		tltmPen.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("pencil")));

		ToolItem tltmFill = new ToolItem(drawToolBar, SWT.RADIO);
		tltmFill.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/color-fill.png")));
		tltmFill.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("fill")));

		ToolItem tltmRect = new ToolItem(drawToolBar, SWT.RADIO);
		tltmRect.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/rect.png")));
		tltmRect.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("rect")));

		ToolItem tltmLine = new ToolItem(drawToolBar, SWT.RADIO);
		tltmLine.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/line.png")));
		tltmLine.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("line")));

		ToolItem tltmCircle = new ToolItem(drawToolBar, SWT.RADIO);
		tltmCircle.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/oval.png")));
		tltmCircle.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("circle")));

		ToolItem tltmFilledCircle = new ToolItem(drawToolBar, SWT.RADIO);
		tltmFilledCircle.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/oval2.png")));
		tltmFilledCircle.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("filledCircle")));

		// ToolItem tltmColorize = new ToolItem(drawToolBar, SWT.RADIO);
		// tltmColorize.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class,
		// "/icons/colorize.png")));
		// tltmColorize.addListener(SWT.Selection, e ->
		// dmdWidget.setDrawTool(drawTools.get("colorize")));

		ToolItem tltmMark = new ToolItem(drawToolBar, SWT.RADIO);
		tltmMark.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/select.png")));
		tltmMark.addListener(SWT.Selection, e -> dmdWidget.setDrawTool(this.editor.drawTools.get("select")));

		this.editor.drawTools.put("pencil", new SetPixelTool(this.editor.paletteTool.getSelectedColor()));
		this.editor.drawTools.put("fill", new FloodFillTool(this.editor.paletteTool.getSelectedColor()));
		this.editor.drawTools.put("rect", new RectTool(this.editor.paletteTool.getSelectedColor()));
		this.editor.drawTools.put("line", new LineTool(this.editor.paletteTool.getSelectedColor()));
		this.editor.drawTools.put("circle", new CircleTool(this.editor.paletteTool.getSelectedColor(), false));
		this.editor.drawTools.put("filledCircle", new CircleTool(this.editor.paletteTool.getSelectedColor(), true));
		// drawTools.put("colorize", new
		// ColorizeTool(paletteTool.getSelectedColor()));
		this.editor.drawTools.put("select", new SelectTool(this.editor.paletteTool.getSelectedColor(), dmdWidget));
		// notify draw tool on color changes
		this.editor.drawTools.values().forEach(d -> this.editor.paletteTool.addIndexListener(d));
		// let draw tools notify when draw action is finished
		this.editor.drawTools.values().forEach(d -> d.addObserver((dm, o) -> this.editor.updateHashes(dm)));

		this.editor.paletteTool.addListener(palette -> {
			if (this.editor.livePreviewActive) {
				this.editor.connector.upload(this.palHandler.getActivePalette(), this.editor.handle);
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
		/*
		 * 1 - Replacement 2 - AddCol 3 - AddCol mit Follow Hash
		 * 
		 * könnte man wenn Mode = 3 und Mask = checked die Maske vom Frame
		 * editieren (Auswahl 1-10 wäre da ausgegraut)
		 * 
		 * In Modus 1+2 würde ich die Mask-Checkbox, Maskennummer-Dropdown und
		 * die Hash-Checkboxen alle auch ausgrauen, da das alles editierten
		 * Content kein Sinn macht. -> Die wären dann alle nur bei Dumps aktiv.
		 */

		editModeViewer.setInput(EditMode.values());
		if (this.editor.selectedScene.isPresent()) {
			this.editor.setViewerSelection(editModeViewer, this.editor.selectedScene.get().getEditMode());
		} else {
			this.editor.setViewerSelection(editModeViewer, EditMode.FIXED);
		}
		editModeViewer.addSelectionChangedListener(e -> this.editor.onEditModeChanged(getFirstSelected(e)));
		// btnColorMask.add

		Label lblMaskNo = new Label(grpDrawing, SWT.NONE);
		lblMaskNo.setText("Mask No:");

		maskSpinner = new Spinner(grpDrawing, SWT.BORDER);
		maskSpinner.setToolTipText("select the mask to use");
		maskSpinner.setMinimum(0);
		maskSpinner.setMaximum(9);
		maskSpinner.setEnabled(false);
		maskSpinner.addListener(SWT.Selection, e -> this.editor.onMaskNumberChanged(maskSpinner.getSelection()));

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
		btnMask.addListener(SWT.Selection, e -> this.editor.onMaskChecked(btnMask.getSelection()));

		btnInvert = new Button(grpDrawing, SWT.NONE);
		btnInvert.setText("Invert");
		btnInvert.addListener(SWT.Selection, e -> this.editor.onInvert());
		btnInvert.setEnabled(false);

		btnCopyToPrev = new Button(grpDrawing, SWT.NONE);
		btnCopyToPrev.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnCopyToPrev.setText("CopyToPrev");
		btnCopyToPrev.addListener(SWT.Selection, e -> this.editor.onCopyAndMoveToPrevFrameClicked());

		btnCopyToNext = new Button(grpDrawing, SWT.NONE);
		btnCopyToNext.setToolTipText("copy the actual scene / color mask to next frame and move forward");
		btnCopyToNext.setText("CopyToNext");
		btnCopyToNext.addListener(SWT.Selection, e -> this.editor.onCopyAndMoveToNextFrameClicked());

		btnUndo = new Button(grpDrawing, SWT.NONE);
		btnUndo.setText("&Undo");
		btnUndo.addListener(SWT.Selection, e -> this.editor.onUndoClicked());

		btnRedo = new Button(grpDrawing, SWT.NONE);
		btnRedo.setText("&Redo");
		btnRedo.addListener(SWT.Selection, e -> this.editor.onRedoClicked());

		btnDeleteColMask = new Button(grpDrawing, SWT.NONE);
		btnDeleteColMask.setText("Delete");
		btnDeleteColMask.setEnabled(false);
		new Label(grpDrawing, SWT.NONE);
		btnDeleteColMask.addListener(SWT.Selection, e -> this.editor.onDeleteColMaskClicked());
		m_bindingContext = initDataBindings();
	}

	private Composite createKeyFrameGroup(Composite parent) {
		grpKeyframe = new Composite(parent, 0);
		grpKeyframe.setLayout(new GridLayout(5, false));
		GridData gd_grpKeyframe = new GridData(SWT.FILL, SWT.TOP, false, false, 3, 4);
		gd_grpKeyframe.heightHint = 257;
		gd_grpKeyframe.widthHint = 490;
		grpKeyframe.setLayoutData(gd_grpKeyframe);
		// grpKeyframe.setText("KeyFrames");
		// grpKeyframe.setVisible(!ApplicationProperties.getBoolean(ApplicationProperties.GODMD_ENABLED_PROP_KEY,
		// false));

		Composite composite_hash = new Composite(grpKeyframe, SWT.NONE);
		// gd_composite_hash.widthHint = 105;
		GridData gd_composite_hash = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_composite_hash.widthHint = 148;
		composite_hash.setLayoutData(gd_composite_hash);
		this.editor.createHashButtons(composite_hash, 10, 0);

		previewDmd = new DMDWidget(grpKeyframe, SWT.DOUBLE_BUFFERED, this.editor.dmd, false);
		GridData gd_dmdPreWidget = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
		gd_dmdPreWidget.heightHint = 64;
		gd_dmdPreWidget.widthHint = 235;
		previewDmd.setLayoutData(gd_dmdPreWidget);
		previewDmd.setDrawingEnabled(false);
		previewDmd.setPalette(this.editor.previewPalettes.get(0));
		previewDmd.setMaskOut(true);
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
		btnAddKeyframe.addListener(SWT.Selection, e -> this.editor.onAddKeyFrameClicked(SwitchMode.PALETTE));

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
		frameSeqViewer.setInput(this.editor.frameSeqList);
		frameSeqViewer.addSelectionChangedListener(event -> this.editor.onFrameSeqChanged(getFirstSelected(event)));

		btnAddFrameSeq = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnAddFrameSeq = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddFrameSeq.widthHint = btnWidth;
		btnAddFrameSeq.setLayoutData(gd_btnAddFrameSeq);
		btnAddFrameSeq.setToolTipText("Adds a keyframe that triggers playback of a scene");
		btnAddFrameSeq.setText("Add ColorScene Switch");
		// TODO add switch mode depend on ani scene
		btnAddFrameSeq.addListener(SWT.Selection, e -> this.editor.onAddFrameSeqClicked(SwitchMode.REPLACE));
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
			if (this.editor.selectedPalMapping != null) {
				this.editor.selectedPalMapping.durationInMillis = Integer.parseInt(txtDuration.getText());
				this.editor.selectedPalMapping.durationInFrames = (int) this.editor.selectedPalMapping.durationInMillis / 40;
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
			if (this.editor.selectedPalMapping != null) {
				this.editor.selectedPalMapping.durationInMillis = this.editor.lastTimeCode - this.editor.saveTimeCode;
				this.editor.selectedPalMapping.durationInFrames = (int) this.editor.selectedPalMapping.durationInMillis / PinDmdEditor.FRAME_RATE;
				txtDuration.setText(this.editor.selectedPalMapping.durationInMillis + "");
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
		spinnerDeviceId.addModifyListener(e -> this.editor.onEventSpinnerChanged(spinnerDeviceId, 8));

		spinnerEventId = new Spinner(composite_5, SWT.BORDER);
		spinnerEventId.setMaximum(255);
		spinnerEventId.setMinimum(0);
		spinnerEventId.addModifyListener(e -> this.editor.onEventSpinnerChanged(spinnerEventId, 0));

		btnAddEvent = new Button(grpKeyframe, SWT.NONE);
		GridData gd_btnAddEvent = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddEvent.widthHint = btnWidth;
		btnAddEvent.setLayoutData(gd_btnAddEvent);
		btnAddEvent.setText("Add Event");
		new Label(grpKeyframe, SWT.NONE);
		btnAddEvent.addListener(SWT.Selection, e -> this.editor.onAddKeyFrameClicked(SwitchMode.EVENT));

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
			if (this.editor.dirtyCheck()) {
				this.editor.onNewProject();
			}
		});

		MenuItem mntmLoadProject = new MenuItem(menu_1, SWT.NONE);
		mntmLoadProject.setText("Load Project\tCtrl-O");
		mntmLoadProject.setAccelerator(SWT.MOD1 + 'O');
		mntmLoadProject.addListener(SWT.Selection, e -> this.editor.onLoadProjectSelected());

		mntmSaveProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveProject.setText("Save Project\tCrtl-S");
		mntmSaveProject.setAccelerator(SWT.MOD1 + 'S');
		mntmSaveProject.addListener(SWT.Selection, e -> this.editor.onSaveProjectSelected(false));

		MenuItem mntmSaveAsProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveAsProject.setText("Save Project as\tShift-Crtl-S");
		mntmSaveAsProject.setAccelerator(SWT.MOD1 | SWT.MOD2 + 'S');
		mntmSaveAsProject.addListener(SWT.Selection, e -> this.editor.onSaveProjectSelected(true));

		MenuItem mntmRecentProjects = new MenuItem(menu_1, SWT.CASCADE);
		mntmRecentProjects.setText("Recent Projects");

		menuPopRecentProjects = new Menu(mntmRecentProjects);
		mntmRecentProjects.setMenu(menuPopRecentProjects);

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmImportProject = new MenuItem(menu_1, SWT.NONE);
		mntmImportProject.setText("Import Project");
		mntmImportProject.addListener(SWT.Selection, e -> this.editor.onImportProjectSelected());

		MenuItem mntmExportRealPinProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportRealPinProject.setText("Export Project (real pin)");
		mntmExportRealPinProject.addListener(SWT.Selection, e -> this.editor.onExportRealPinProject());

		MenuItem mntmExportVpinProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportVpinProject.setText("Export Project (virt pin)");
		mntmExportVpinProject.addListener(SWT.Selection, e -> this.editor.onExportVirtualPinProject());

		mntmUploadProject = new MenuItem(menu_1, SWT.NONE);
		mntmUploadProject.setText("Upload Project");
		mntmUploadProject.addListener(SWT.Selection, e -> this.editor.onUploadProjectSelected());

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setText("Exit\tCtrl-Q");
		mntmExit.addListener(SWT.Selection, e -> {
			if (this.editor.dirtyCheck()) {
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
		mntmCut.addListener(SWT.Selection, e -> this.editor.clipboardHandler.onCut(this.palHandler.getActivePalette()));

		MenuItem mntmCopy = new MenuItem(menu_5, SWT.NONE);
		mntmCopy.setText("Copy \tCtrl-C");
		mntmCopy.setAccelerator(SWT.MOD1 + 'C');
		mntmCopy.addListener(SWT.Selection, e -> this.editor.clipboardHandler.onCopy(this.palHandler.getActivePalette()));

		MenuItem mntmPaste = new MenuItem(menu_5, SWT.NONE);
		mntmPaste.setText("Paste\tCtrl-V");
		mntmPaste.setAccelerator(SWT.MOD1 + 'V');
		mntmPaste.addListener(SWT.Selection, e -> {
			this.editor.clipboardHandler.onPaste();
			this.editor.dmdRedraw();
		});

		MenuItem mntmPasteWithHover = new MenuItem(menu_5, SWT.NONE);
		mntmPasteWithHover.setText("Paste Over\tShift-Ctrl-V");
		mntmPasteWithHover.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'V');
		mntmPasteWithHover.addListener(SWT.Selection, e -> this.editor.clipboardHandler.onPasteHoover());

		MenuItem mntmSelectAll = new MenuItem(menu_5, SWT.NONE);
		mntmSelectAll.setText("Select All\tCtrl-A");
		mntmSelectAll.setAccelerator(SWT.MOD1 + 'A');
		mntmSelectAll.addListener(SWT.Selection, e -> this.editor.onSelectAll());

		MenuItem mntmDeSelect = new MenuItem(menu_5, SWT.NONE);
		mntmDeSelect.setText("Remove Selection\tShift-Ctrl-A");
		mntmDeSelect.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'A');
		mntmDeSelect.addListener(SWT.Selection, e -> this.editor.onRemoveSelection());

		new MenuItem(menu_5, SWT.SEPARATOR);

		mntmUndo = new MenuItem(menu_5, SWT.NONE);
		mntmUndo.setText("Undo\tCtrl-Z");
		mntmUndo.setAccelerator(SWT.MOD1 + 'Z');
		mntmUndo.addListener(SWT.Selection, e -> this.editor.onUndoClicked());

		mntmRedo = new MenuItem(menu_5, SWT.NONE);
		mntmRedo.setText("Redo\tShift-Ctrl-Z");
		mntmRedo.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'Z');
		mntmRedo.addListener(SWT.Selection, e -> this.editor.onRedoClicked());

		MenuItem mntmAnimations = new MenuItem(menu, SWT.CASCADE);
		mntmAnimations.setText("&Animations");

		Menu menu_2 = new Menu(mntmAnimations);
		mntmAnimations.setMenu(menu_2);

		MenuItem mntmLoadAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmLoadAnimation.setText("Load Animation(s)");
		mntmLoadAnimation.addListener(SWT.Selection, e -> this.editor.aniAction.onLoadAniWithFC(true));

		MenuItem mntmLoadRecordings = new MenuItem(menu_2, SWT.NONE);
		mntmLoadRecordings.setText("Load Recording(s)");
		mntmLoadRecordings.addListener(SWT.Selection, e -> this.editor.aniAction.onLoadAniWithFC(true));

		MenuItem mntmSaveAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveAnimation.setText("Save Animation(s) ...");
		mntmSaveAnimation.addListener(SWT.Selection, e -> this.editor.aniAction.onSaveAniWithFC(1));

		MenuItem mntmSaveSingleAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveSingleAnimation.setText("Save single Animation");
		mntmSaveSingleAnimation.addListener(SWT.Selection, e -> this.editor.aniAction.onSaveSingleAniWithFC(1));

		MenuItem mntmRecentAnimationsItem = new MenuItem(menu_2, SWT.CASCADE);
		mntmRecentAnimationsItem.setText("Recent Animations");

		mntmRecentAnimations = new Menu(mntmRecentAnimationsItem);
		mntmRecentAnimationsItem.setMenu(mntmRecentAnimations);

		new MenuItem(menu_2, SWT.SEPARATOR);

		MenuItem mntmExportAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmExportAnimation.setText("Export Animation as GIF");

		mntmExportAnimation.addListener(SWT.Selection, e -> {
			Animation ani = this.editor.playingAnis.get(0);
			Palette pal = this.editor.project.palettes.get(ani.getPalIndex());
			GifExporter exporter = new GifExporter(shell, pal, ani);
			exporter.open();
		});

		MenuItem mntmExportForGodmd = new MenuItem(menu_2, SWT.NONE);
		mntmExportForGodmd.setText("Export for goDMD ...");
		mntmExportForGodmd.addListener(SWT.Selection, e -> {
			ExportGoDdmd exportGoDdmd = new ExportGoDdmd(shell, 0);
			Pair<String, Integer> res = exportGoDdmd.open();
			if (res != null) {
				this.editor.exportForGoDMD(res.getLeft(), res.getRight());
			}
		});

		MenuItem mntmpalettes = new MenuItem(menu, SWT.CASCADE);
		mntmpalettes.setText("&Palettes / Mode");
		Menu menu_3 = new Menu(mntmpalettes);
		mntmpalettes.setMenu(menu_3);

		MenuItem mntmLoadPalette = new MenuItem(menu_3, SWT.NONE);
		mntmLoadPalette.setText("Load Palette");
		mntmLoadPalette.addListener(SWT.Selection, e -> { 
			String file = this.editor.paletteHandler.loadPalette();
			recentPalettesMenuManager.populateRecent(file);
		});

		MenuItem mntmSavePalette = new MenuItem(menu_3, SWT.NONE);
		mntmSavePalette.setText("Save Palette");
		mntmSavePalette.addListener(SWT.Selection, e -> this.editor.paletteHandler.savePalette());

		MenuItem mntmRecentPalettesItem = new MenuItem(menu_3, SWT.CASCADE);
		mntmRecentPalettesItem.setText("Recent Palettes");

		mntmRecentPalettes = new Menu(mntmRecentPalettesItem);
		mntmRecentPalettesItem.setMenu(mntmRecentPalettes);

		new MenuItem(menu_3, SWT.SEPARATOR);

		mntmUploadPalettes = new MenuItem(menu_3, SWT.NONE);
		mntmUploadPalettes.setText("Upload Palettes");
		mntmUploadPalettes.addListener(SWT.Selection, e -> this.editor.connector.upload(this.palHandler.getActivePalette()));

		new MenuItem(menu_3, SWT.SEPARATOR);

		MenuItem mntmConfig = new MenuItem(menu_3, SWT.NONE);
		mntmConfig.setText("Configuration");
		mntmConfig.addListener(SWT.Selection, e -> {
			Config config = new Config(shell);
			config.open(this.editor.pin2dmdAdress);
			if (config.okPressed) {
				this.editor.refreshPin2DmdHost(config.getPin2DmdHost());
				if (!this.editor.dmdSize.equals(config.getDmdSize())) {
					this.editor.refreshDmdSize(config.getDmdSize());
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
		mntmGetHelp.addListener(SWT.Selection, e -> Program.launch(PinDmdEditor.HELP_URL));

		MenuItem mntmRegister = new MenuItem(menu_4, SWT.NONE);
		mntmRegister.setText("Register");
		mntmRegister.addListener(SWT.Selection, e -> new RegisterLicense(shell).open());

		new MenuItem(menu_4, SWT.SEPARATOR);

		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");
		mntmAbout.addListener(SWT.Selection, e -> new About(shell).open(this.editor.pluginsPath, this.editor.loadedPlugins));
	}

	@SuppressWarnings("unchecked")
	<T> T getFirstSelected(SelectionChangedEvent e) {
		IStructuredSelection selection = (IStructuredSelection) e.getSelection();
		return selection.isEmpty() ? null : (T) selection.getFirstElement();
	}

	public void setEditor(PinDmdEditor pinDmdEditor) {
		this.editor = pinDmdEditor;
	}

	public void setViewModel(ViewModel vm) {
		viewModel = vm;;
	}

	public void setProject(Project project) {
		  this.project = project;
	}
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeTextText_1ObserveWidget = WidgetProperties.text().observe(combo);
		IObservableValue textModelObserveValue = PojoProperties.value("paletteName").observe(viewModel);
		bindingContext.bindValue(observeTextText_1ObserveWidget, textModelObserveValue, null, null);
		//
		ObservableListContentProvider listContentProvider = new ObservableListContentProvider();
		IObservableMap observeMap = PojoObservables.observeMap(listContentProvider.getKnownElements(), Palette.class, "label");
		paletteComboViewer.setLabelProvider(new ObservableMapLabelProvider(observeMap));
		paletteComboViewer.setContentProvider(listContentProvider);
		//
		IObservableList palettesProjectObserveList = PojoProperties.list("palettes").observe(project);
		paletteComboViewer.setInput(palettesProjectObserveList);
		//
		return bindingContext;
	}
}