package com.rinke.solutions.pinball;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.RecentMenuManager;
import com.rinke.solutions.pinball.view.handler.HandlerTest;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.PaletteTool;

@RunWith(MockitoJUnitRunner.class)
public class EditorViewBindingSWTTest extends HandlerTest {
	
	@Mock AnimationHandler animationHandler;
	@Mock ClipboardHandler clipboardHandler;
	@Mock RecentMenuManager recentAnimationsMenuManager;
	@Mock Shell shell;
	@Mock Text txtDuration;
	
	@InjectMocks
	private EditorViewBinding uut = new EditorViewBinding(vm);
	
	private Palette pal;
	private EditorView editorView;
	DMD dmd = new DMD(128,32);
	private Button[] buttons;


	@Before
	public void setUp() throws Exception {
		pal = Palette.getDefaultPalettes().get(0);
		editorView = Mockito.mock(EditorView.class);
		editorView.txtDuration = txtDuration;
		buttons = getBtn();
		when( editorView.getBtnHash() ).thenReturn(buttons);
		DMDWidget dmdWidget = Mockito.mock(DMDWidget.class);
		DMDWidget previewWidget = Mockito.mock(DMDWidget.class);
		editorView.previewDmd = previewWidget;
		when( editorView.getDmdWidget() ).thenReturn(dmdWidget );
		when( editorView.getClipboardHandler() ).thenReturn(clipboardHandler);
		when( editorView.getShell() ).thenReturn(shell);
		uut.setEditorView(editorView);
	}

	private Button[] getBtn() {
		Button[] btns= new Button[4];
		for(int i=0; i<4;i++) {
			btns[i] = Mockito.mock(Button.class);
			when(btns[i].getData()).thenReturn(i);
		}
		return btns;
	}

	@Test
	public void testOnCut() throws Exception {
		uut.onCut(pal);
	}

	@Test
	public void testOnCopy() throws Exception {
		uut.onCopy(pal);
	}

	@Test
	public void testViewModelChanged() throws Exception {
		uut.viewModelChanged("selectedHashIndex", 0, 1);
	}

	@Test
	public void testViewModelChangedLabel() throws Exception {
		uut.viewModelChanged("hashLbl", null, "foo");
	}

	@Test
	public void testViewModelChangedBtnEnabled() throws Exception {
		vm.hashButtonEnabled[0] = true;
		vm.setHashButtonsEnabled(true);
		uut.viewModelChanged("hashButtonEnabled", false, true );
		vm.setHashButtonsEnabled(false);
		uut.viewModelChanged("hashButtonEnabled", false, true );
	}

	@Test
	public void testViewModelChangedPalette() throws Exception {
		editorView.paletteTool =  Mockito.mock(PaletteTool.class);
		uut.viewModelChanged("paletteDirty", false, true );
	}

	@Test
	public void testViewModelChangedHashBtnSelected() throws Exception {
		uut.viewModelChanged("hashButtonSelected", 0, 1);
	}

	@Test
	public void testOnAnimationIsPlayingChanged() throws Exception {
		uut.onAnimationIsPlayingChanged(false, true);
		vm.setMaxFrame(10);
		uut.onAnimationIsPlayingChanged(false, true);
	}

	@Test
	public void testOnRecentAnimationsChanged() throws Exception {
		when( editorView.getRecentAnimationsMenuManager()).thenReturn(recentAnimationsMenuManager);
		uut.onRecentAnimationsChanged("foo", "bar");
	}

	@Test
	public void testOnPaste() throws Exception {
		uut.onPaste();
	}

	@Test
	public void testOnPasteHoover() throws Exception {
		uut.onPasteHoover();
	}

	@Test
	public void testOnProjectFilenameChanged() throws Exception {
		uut.onProjectFilenameChanged("foo", "bar");
	}

	@Test
	public void testOnProjectFilenameChangedToNull() throws Exception {
		uut.onProjectFilenameChanged("foo", null);
	}

	@Test
	public void testOnSelectedPaletteChanged() throws Exception {
		Palette n = Palette.getDefaultPalettes().get(1);
		Palette o = Palette.getDefaultPalettes().get(2);
		uut.onSelectedPaletteChanged(o, n);
	}

	@Test
	public void testRun() throws Exception {
		uut.animation.run();
		vm.setMaxFrame(10);
		uut.animation.run();
	}

	@Test
	public void testOnDurationChanged() throws Exception {
		uut.onDurationChanged(0, 10);
	}

	@Test
	public void testOnDirtyChanged() throws Exception {
		uut.onDirtyChanged(false, true);
	}
	
	@Test
	public void testOnPreviewDMDChanged() throws Exception {
		uut.onPreviewDMDChanged(null, dmd);
		uut.onPreviewDMDChanged(dmd, null);
	}

}
