package com.rinke.solutions.pinball;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
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

@RunWith(MockitoJUnitRunner.class)
public class EditorViewBindingTest extends HandlerTest {
	
	@Mock AnimationHandler animationHandler;
	@Mock ClipboardHandler clipboardHandler;
	@Mock RecentMenuManager recentAnimationsMenuManager;
	@Mock Shell shell;
	
	@InjectMocks
	private EditorViewBinding uut = new EditorViewBinding(vm);
	
	private Palette pal;
	private EditorView editorView;

	@Before
	public void setUp() throws Exception {
		pal = Palette.getDefaultPalettes().get(0);
		editorView = Mockito.mock(EditorView.class);
		DMDWidget dmdWidget = Mockito.mock(DMDWidget.class);
		when( editorView.getDmdWidget() ).thenReturn(dmdWidget );
		when( editorView.getClipboardHandler() ).thenReturn(clipboardHandler);
		uut.setEditorView(editorView);
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
		Button[] btns = new Button[]{};
		when( editorView.getBtnHash() ).thenReturn(btns);
		uut.viewModelChanged("selectedHashIndex", 0, 1);
	}

	@Test
	public void testOnAnimationIsPlayingChanged() throws Exception {
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
		when(editorView.getShell()).thenReturn(shell);
		uut.onProjectFilenameChanged("foo", "bar");
	}

}
