package com.rinke.solutions.pinball;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.core.databinding.Binding;

public class Test {
	private Binding foo;
	
	private DataBindingContext m_bindingContext;

	protected Shell shell;
	
	public static class Model {
		protected String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			 this.text = text;
		}
	}
	
	private Model model = new Model();
	
	private Text text_1;
	
	public static void main(String[] args) {
		Test test = new Test();
		test.open();
	}

	/**
	 * Open the window.
	 * @wbp.parser.entryPoint
	 */
	public void open() {
		Display display = Display.getDefault();
		Realm.runWithDefault(SWTObservables.getRealm(Display.getCurrent()), ()->{
			createContents();
			shell.open();
			shell.layout();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		});

	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("SWT Application");
		
		text_1 = new Text(shell, SWT.BORDER);
		text_1.setBounds(37, 30, 64, 19);
		
		Button btnOk = new Button(shell, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println(model.text);
			}
		});
		btnOk.setBounds(295, 222, 94, 28);
		btnOk.setText("ok");
		m_bindingContext = initDataBindings();

	}
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeTextText_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(text_1);
		IObservableValue textModelObserveValue = PojoProperties.value("text").observe(model);
		foo = bindingContext.bindValue(observeTextText_1ObserveWidget, textModelObserveValue, null, null);
		//
		return bindingContext;
	}
}
