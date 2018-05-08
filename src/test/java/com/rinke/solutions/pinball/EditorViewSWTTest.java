package com.rinke.solutions.pinball;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class EditorViewSWTTest {
	
	Shell shell = new Shell();
	
	EditorView uut = new EditorView(4, false);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreateMenu() throws Exception {
		uut.createMenu(shell);
	}

	@Test
	public void testCreateContents() throws Exception {
		uut.vm = new ViewModel();
		uut.shell = new Shell();
		Display display = Display.getDefault();
		Realm realm = SWTObservables.getRealm(display);
		Realm.runWithDefault(realm, ()->uut.createContents());
	}

}
