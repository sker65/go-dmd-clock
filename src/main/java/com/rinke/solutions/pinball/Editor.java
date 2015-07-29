package com.rinke.solutions.pinball;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Editor implements Runnable {
    
    private static Logger LOG = LoggerFactory.getLogger(Editor.class); 

    private static final String NO_TRANS = " - ";
    String[] args;
    private String lastPath;

    public Editor(String[] args) {
        this.args = args;
    }
    
    /**
     * Launch the application.
     * 
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "go-dmd-editor.log");
        // Display display = Display.getDefault();
        Editor editor = new Editor(args);
        try {
            editor.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DMDClock clock = new DMDClock(false);
    Shell shell;

    Animation selectedAnimation;
    int selectedAnimationIndex;

    java.util.List<Animation> sourceAnis = new ArrayList<>();
    java.util.List<Animation> targetAnis = new ArrayList<>();
    java.util.List<Animation> playingAnis = new ArrayList<>();

    List sourceList;

    Text txtTdelay;
    Combo comboTransition;
    Combo comboFsk;
    Spinner spinnerCycle;
    Spinner spinnerHold;
    Text nameText;
    Button btnCut;
    Button btnDelete;

    Display display;
    
    String[] fsks = new String[] { "18", "16", "12", "6" };
    private java.util.List<String> transitions;

    /*
     * private Menu createMenu() {
     * 
     * Menu menuBar = new Menu(shell, SWT.BAR);
     * 
     * MenuItem item = new MenuItem(menuBar, SWT.CASCADE); item.setText("File");
     * Menu fileMenu = new Menu(shell, SWT.DROP_DOWN); item.setMenu(fileMenu);
     * 
     * MenuItem mntmLoad = new MenuItem(fileMenu, SWT.NONE);
     * mntmLoad.addSelectionListener(new SelectionAdapter() {
     * 
     * @Override public void widgetSelected(SelectionEvent e) { load(false); }
     * }); mntmLoad.setText("Load");
     * 
     * MenuItem mntmAdd = new MenuItem(fileMenu, SWT.NONE);
     * mntmAdd.addSelectionListener(new SelectionAdapter() {
     * 
     * @Override public void widgetSelected(SelectionEvent e) { load(true); }
     * }); mntmAdd.setText("Add");
     * 
     * MenuItem mntmSave = new MenuItem(fileMenu, SWT.NONE);
     * mntmSave.addSelectionListener(new SelectionAdapter() {
     * 
     * @Override public void widgetSelected(SelectionEvent e) { save(); } });
     * mntmSave.setText("Save");
     * 
     * MenuItem menuItem = new MenuItem(fileMenu, SWT.SEPARATOR);
     * menuItem.setText("sep1");
     * 
     * MenuItem mntmQuit = new MenuItem(fileMenu, SWT.NONE);
     * mntmQuit.setText("Quit"); return menuBar;
     * 
     * }
     */
    

    /**
     * @wbp.parser.entryPoint
     */
    public void run() {
        
        InputStream stream;
        String version = "";
        try{ 
            stream = this.getClass().getClassLoader().getResourceAsStream("version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            version = reader.readLine();
            reader.close();
        } catch(Exception e) {
            
        }
        
        display = Display.getDefault();
        shell = new Shell();
        shell.setSize(1260, 600);
        shell.setText("Animation Editor - "+version);
        shell.setLayout(new GridLayout(2, false));

        Label lblAnimations = new Label(shell, SWT.NONE);
        lblAnimations.setText("Animations");

        Group grpDetails = new Group(shell, SWT.NONE);
        GridData gd_grpDetails = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
        gd_grpDetails.widthHint = 812;
        grpDetails.setLayoutData(gd_grpDetails);
        grpDetails.setText("Details");

        final Label lblDetails = new Label(grpDetails, SWT.NONE);
        lblDetails.setBounds(10, 20, 115, 17);
        lblDetails.setText("pos");

        final Button btnStart = new Button(grpDetails, SWT.NONE);
        final Button btnStop = new Button(grpDetails, SWT.NONE);

        btnStart.setBounds(145, 10, 60, 29);
        btnStart.setText("Start");
        btnStart.addListener(SWT.Selection, e -> {
            animationHandler.start();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        });
        btnStart.setEnabled(false);

        btnStop.setBounds(206, 10, 60, 29);
        btnStop.setText("Stop");
        btnStop.addListener(SWT.Selection, e -> {
            animationHandler.stop();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        Button btnPrev = new Button(grpDetails, SWT.NONE);
        btnPrev.setBounds(272, 10, 31, 29);
        btnPrev.setText("<");
        btnPrev.addListener(SWT.Selection, e -> animationHandler.prev());

        Button btnNext = new Button(grpDetails, SWT.NONE);
        btnNext.setBounds(303, 10, 31, 29);
        btnNext.setText(">");
        btnNext.addListener(SWT.Selection, e -> animationHandler.next());

        Button btnMarkStart = new Button(grpDetails, SWT.NONE);
        btnMarkStart.setBounds(336, 10, 91, 29);
        btnMarkStart.setText("Mark Start");
        btnMarkStart.addListener(SWT.Selection, e -> {
            markStart = selectedAnimation.actFrame;
            btnCut.setEnabled(markEnd > 0 && markEnd > markStart);
        });

        Button btnMarkEnd = new Button(grpDetails, SWT.NONE);
        btnMarkEnd.setBounds(426, 10, 91, 29);
        btnMarkEnd.setText("Mark End");
        btnMarkEnd.addListener(SWT.Selection, e -> {
            markEnd = selectedAnimation.actFrame;
            btnCut.setEnabled(markEnd > 0 && markEnd > markStart);
        });

        btnCut = new Button(grpDetails, SWT.NONE);
        btnCut.setBounds(523, 10, 91, 29);
        btnCut.setText("Cut");
        btnCut.setEnabled(false);
        btnCut.addListener(SWT.Selection, e -> cutOutNewClip(markStart, markEnd));

        final Button btnShowClock = new Button(grpDetails, SWT.CHECK);
        btnShowClock.addListener(SWT.Selection, e -> animationHandler.setShowClock(btnShowClock.getSelection()));

        btnShowClock.setBounds(632, 13, 115, 24);
        btnShowClock.setText("Show Clock");
        btnShowClock.setSelection(true);

        // shell.setMenuBar(createMenu());

        // Group grpSource = new Group(shell, SWT.NONE);
        // grpSource.setText("Source");
        // grpSource.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
        // 1, 2));
        // grpSource.setLayout(new GridLayout(1, false));

        sourceList = new List(shell, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
        GridData gd_aniList = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 3);
        gd_aniList.verticalSpan = 1;
        gd_aniList.widthHint = 221;
        gd_aniList.heightHint = 280;
        sourceList.setLayoutData(gd_aniList);

        sourceList.addListener(SWT.Selection, e -> {
            if( selectedAnimation != null ) {
                pullFromWidget(selectedAnimation);
                sourceList.setItem(selectedAnimationIndex, selectedAnimation.getDesc());
            }
            selectedAnimationIndex = sourceList.getSelectionIndex();
            selectedAnimation = sourceAnis.get(selectedAnimationIndex);
            playingAnis.clear();
            playingAnis.add(selectedAnimation);
            animationHandler.setAnimations(playingAnis);
            btnDelete.setEnabled(sourceList.getSelectionCount() > 0);
            bindToWidget();
        });

        Canvas canvas = new Canvas(shell, SWT.BORDER|SWT.DOUBLE_BUFFERED);
        GridData gd_canvas = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
        gd_canvas.heightHint = 250;
        gd_canvas.widthHint = 960;
        canvas.setLayoutData(gd_canvas);
        // canvas.setSize(960, 320);
        canvas.addPaintListener(new DmdPaintListener());
        canvas.setBackground(new Color(display, 10, 10, 10));

        animationHandler = new AnimationHandler(playingAnis, clock, dmd, canvas, false);
        new Label(shell, SWT.NONE);

        final Scale scale = new Scale(shell, SWT.NONE);
        GridData gd_scale = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
        gd_scale.widthHint = 967;
        scale.setLayoutData(gd_scale);
        animationHandler.setScale(scale);
        scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));

        Group grpActions = new Group(shell, SWT.NONE);
        GridData gd_grpActions = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
        gd_grpActions.heightHint = 131;
        gd_grpActions.widthHint = 242;
        grpActions.setLayoutData(gd_grpActions);
        grpActions.setText("Actions");

        Button btnSave = new Button(grpActions, SWT.NONE);
        btnSave.setBounds(106, 61, 91, 29);
        btnSave.setText("Save");
        btnSave.addListener(SWT.Selection, e -> save());

        btnDelete = new Button(grpActions, SWT.NONE);
        btnDelete.setText("Delete");
        btnDelete.setEnabled(false);
        btnDelete.setBounds(9, 61, 91, 29);
        btnDelete.addListener(SWT.Selection, e -> deleteFromList(sourceList.getSelection(), sourceAnis));

        Button btnLoad = new Button(grpActions, SWT.NONE);
        btnLoad.addListener(SWT.Selection, e -> load(false));

        btnLoad.setText("Load");
        btnLoad.setBounds(9, 26, 91, 29);

        Button btnAdd = new Button(grpActions, SWT.NONE);
        btnAdd.addListener(SWT.Selection, e -> load(true));
        btnAdd.setText("Add");
        btnAdd.setBounds(106, 26, 91, 29);

        Button btnSelectAll = new Button(grpActions, SWT.NONE);
        btnSelectAll.setText("Select All");
        btnSelectAll.setBounds(9, 98, 91, 29);
        btnSelectAll.addListener(SWT.Selection, e -> sourceList.selectAll());

        Group grpDetails_1 = new Group(shell, SWT.NONE);
        GridData gd_grpDetails_1 = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
        gd_grpDetails_1.heightHint = 134;
        gd_grpDetails_1.widthHint = 861;
        grpDetails_1.setLayoutData(gd_grpDetails_1);
        grpDetails_1.setText("Details");

        Label lblFsk = new Label(grpDetails_1, SWT.NONE);
        lblFsk.setBounds(17, 35, 35, 17);
        lblFsk.setText("FSK");

        Label lblHold = new Label(grpDetails_1, SWT.NONE);
        lblHold.setText("Hold");
        lblHold.setBounds(139, 35, 35, 17);

        Label lblCycle = new Label(grpDetails_1, SWT.NONE);
        lblCycle.setText("Cycle");
        lblCycle.setBounds(268, 35, 35, 17);

        Label lblTransition = new Label(grpDetails_1, SWT.NONE);
        lblTransition.setText("Transition");
        lblTransition.setBounds(405, 35, 75, 17);

        spinnerCycle = new Spinner(grpDetails_1, SWT.BORDER);
        spinnerCycle.setBounds(309, 30, 90, 27);
        // spinnerCycle.setValues(0, 0, 20);

        spinnerHold = new Spinner(grpDetails_1, SWT.BORDER);
        spinnerHold.setBounds(173, 30, 89, 27);

        comboFsk = new Combo(grpDetails_1, SWT.READ_ONLY);
        comboFsk.setBounds(56, 30, 75, 17);
        comboFsk.setItems(fsks);

        comboTransition = new Combo(grpDetails_1, SWT.READ_ONLY);
        comboTransition.setBounds(478, 30, 106, 27);
        comboTransition.addListener(SWT.Selection, e -> pullTransition(selectedAnimation));

        transitions = buildTransitions(transitionsPath, comboTransition);

        Label lblDelay = new Label(grpDetails_1, SWT.NONE);
        lblDelay.setText("Delay");
        lblDelay.setBounds(590, 35, 45, 17);

        txtTdelay = new Text(grpDetails_1, SWT.BORDER);
        txtTdelay.setText("30");
        txtTdelay.setBounds(644, 30, 75, 27);

        Label lblName = new Label(grpDetails_1, SWT.NONE);
        lblName.setText("Name");
        lblName.setBounds(10, 64, 50, 17);

        nameText = new Text(grpDetails_1, SWT.BORDER);
        nameText.setBounds(56, 61, 149, 27);
        animationHandler.setLabelHandler(new EventHandler() {

            @Override
            public void notifyAni(AniEvent evt) {
                switch (evt.evtType) {
                case ANI:
                    lblDetails.setText("Frame: " + evt.actFrame);
                    // sourceList.setSelection(new String[] { evt.actAnimation
                    // .getDesc() });
                    break;
                case CLOCK:
                    lblDetails.setText("");
                    // sourceList.deselectAll();
                    break;
                }
            }

        });

        shell.open();
        shell.layout();

        display.timerExec(animationHandler.getRefreshDelay(), animationHandler);

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }



    /**
     * deletes from the underlying list
     * 
     * @param keys
     *            key to delete
     * @param anis
     *            list to delete from
     */
    protected void deleteFromList(String[] keys, java.util.List<Animation> anis) {
        for (String key : keys) {
            Iterator<Animation> i = anis.iterator();
            while (i.hasNext()) {
                Animation a = i.next();
                if (a.getDesc().equals(key)) {
                    i.remove();
                }
            }
        }
        populateList(sourceList, anis);
    }

    protected void cutOutNewClip(int start, int end) {
        // only works if current is MAME
        Animation ani = buildMameAnimation(selectedAnimation.getBasePath() + selectedAnimation.getName());
        ani.start = start;
        ani.end = end;
        ani.setDesc(selectedAnimation.getDesc() + (cutNameNumber++));
        sourceAnis.add(ani);
        populateList(sourceList, sourceAnis);
    }

    int cutNameNumber = 1; // postfix for names
    int markStart = 0;
    int markEnd = 0;

    private java.util.List<String> buildTransitions(String basePath, Combo transitions) {
        Pattern pattern = Pattern.compile("^([a-z_\\.\\-A-Z]*)([0-9]*)\\.png$");
        String[] list = new File(basePath+"transitions/").list();
        LinkedHashSet<String> trans = new LinkedHashSet<String>();
        if (list != null)
            for (String name : list) {
                Matcher matcher = pattern.matcher(name);
                if (matcher.matches()) {
                    // System.out.println(matcher.group(1));
                    if (!matcher.group(1).isEmpty())
                        LOG.debug("name: "+name+" '"+matcher.group(1)+"'");
                        trans.add(matcher.group(1));
                }
            }
        trans.add(NO_TRANS);
        trans.forEach(key -> transitions.add(key));
        return new ArrayList<>(trans);
    }

    private DMD dmd = new DMD();

    private AnimationHandler animationHandler;
    private String transitionsPath = "./";//home/sr/Downloads/Pinball/";

    private class DmdPaintListener implements PaintListener {

        public void paintControl(PaintEvent e) {
            dmd.draw(e);
            if (animationHandler != null)
                e.display.timerExec(animationHandler.getRefreshDelay(), animationHandler);
            e.gc.dispose();
        }
    }

    protected void save() {
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.ani" });
        fileChooser.setFilterNames(new String[] { "Animationen", "ani" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null)
            return;
        AnimationCompiler.compile(sourceAnis, filename);
    }

    private void bindToWidget() {
        if (selectedAnimation != null) {
            comboFsk.select(comboFsk.indexOf(String.valueOf(selectedAnimation.getFsk())));
            spinnerCycle.setSelection(selectedAnimation.getCycles());
            spinnerHold.setSelection(selectedAnimation.getHoldCycles());
            txtTdelay.setText(String.valueOf(selectedAnimation.getTransitionDelay()));
            nameText.setText(selectedAnimation.getDesc());
            if( selectedAnimation.getTransitionName()==null || selectedAnimation.getTransitionName().isEmpty()) {
                comboTransition.deselectAll();
            } else {
                comboTransition.select(transitions.indexOf(selectedAnimation.getTransitionName()));
            }
        }
    }
    
    private void pullFromWidget(Animation ani) {
        ani.setFsk(Integer.valueOf(fsks[comboFsk.getSelectionIndex()]));
        ani.setCycles(spinnerCycle.getSelection());
        ani.setHoldCycles(spinnerHold.getSelection());
        ani.setDesc(nameText.getText());
        pullTransition(ani);
    }

    private void pullTransition(Animation ani) {
        int index = comboTransition.getSelectionIndex();
        if( index != -1 && !transitions.get(index).equals(NO_TRANS)) {
            ani.setTransitionDelay(Integer.valueOf(txtTdelay.getText()));
            ani.setTransitionName(transitions.get(index));
            ani.setTransitionFrom(ani.end);
            ani.setTransitionsPath(transitionsPath);
        } else {
            ani.setTransitionName(null);
            ani.setTransitionFrom(0);
        }
    }


    protected void load(boolean append) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.properties;*.ani;*.txt.gz" });
        fileChooser.setFilterNames(new String[] { "Animationen", "properties, txt.gz, ani" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null)
            return;

        java.util.List<Animation> loadedList = new ArrayList<>();
        if (filename.endsWith(".ani")) {
            loadedList.addAll(AnimationCompiler.readFromCompiledFile(filename));
        } else if (filename.endsWith(".txt.gz")) {
            loadedList.add(buildMameAnimation(filename));
        } else if (filename.endsWith(".properties")) {
            loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
        }
        // animationHandler.setAnimations(sourceAnis);
        if (!append) {
            sourceAnis.clear();
            selectedAnimation = null;
            selectedAnimationIndex = 0;
            playingAnis.clear();
        }
        sourceAnis.addAll(loadedList);
        populateList(sourceList, sourceAnis);
    }

    private Animation buildMameAnimation(String filename) {
        File file = new File(filename);
        String base = file.getName();
        Animation ani = new Animation(AnimationType.MAME, base, 0, 0, 1, 1, 0);
        ani.setBasePath(file.getParent() + "/");
        ani.setDesc(base.substring(0, base.indexOf('.')));
        return ani;
    }

    private void populateList(List list, java.util.List<Animation> anis) {
        list.removeAll();
        anis.forEach(animation -> list.add(animation.getDesc()));
    }
}
