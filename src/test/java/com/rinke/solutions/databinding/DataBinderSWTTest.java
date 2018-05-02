package com.rinke.solutions.databinding;

import static com.rinke.solutions.databinding.WidgetProp.ENABLED;
import static org.junit.Assert.*;
import lombok.Getter;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.view.model.AbstractModel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class DataBinderSWTTest {
	
	private DataBinder uut;
	
	private Display display;

	private Realm realm;
	
	@Getter
	private static class TestViewModel extends AbstractModel {
		@ViewBinding public boolean deleteRecordingEnabled;

		public void setDeleteRecordingEnabled(boolean deleteRecordingEnabled) {
			firePropertyChange("deleteRecordingEnabled", this.deleteRecordingEnabled, this.deleteRecordingEnabled = deleteRecordingEnabled);
		}
	}
	
	private static class TestView {
		public TestView() {
			super();
			Shell shell = new Shell();
			this.deleteRecording = new Button(shell, 0);
		}

		@GuiBinding(prop=ENABLED) Button deleteRecording;
	}

	@Before
	public void setUp() throws Exception {
		display = Display.getDefault();
		realm = SWTObservables.getRealm(display);
		Realm.runWithDefault(realm, ()->uut = new DataBinder() );
	}

	@Test
	public void testBind() throws Exception {
		TestView testView = new TestView();
		TestViewModel testViewModel = new TestViewModel();
		Realm.runWithDefault(realm, ()->uut.bind(testView, testViewModel));
		assertFalse(testView.deleteRecording.isEnabled());
		testViewModel.setDeleteRecordingEnabled(true);
		assertTrue(testView.deleteRecording.isEnabled());
	}

}
