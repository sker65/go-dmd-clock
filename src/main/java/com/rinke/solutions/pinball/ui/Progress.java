package com.rinke.solutions.pinball.ui;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.animation.ProgressEventListener;

@Bean(name="progress", scope=Scope.SINGLETON)
public class Progress extends Dialog implements ProgressEventListener {

	protected Object result;
	protected Shell shell;
	private Display display;
	private ProgressBar progressBar;
	private Label lblJobLabel;
	private Worker worker;
	private Button btnCancel;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public Progress(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
		display = parent.getDisplay();
		setText("Working");
	}
	
	

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open(Worker worker) {
		this.worker = worker;
		createContents();
		shell.open();
		shell.layout();
		
		new ProcessThread(worker).start();
		
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	class ProcessThread extends Thread {

		public ProcessThread(Worker worker) {
			super(worker);
		}

		@Override
		public void run() {
			super.run();
			display.asyncExec(()->shell.close());
		}
		
	}
	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(495, 147);
		shell.setText(getText());
		
		progressBar = new ProgressBar(shell, SWT.NONE);
		progressBar.setBounds(10, 32, 475, 24);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		
		lblJobLabel = new Label(shell, SWT.NONE);
		lblJobLabel.setBounds(10, 60, 475, 14);
		lblJobLabel.setText("New Label");
		
		btnCancel = new Button(shell, SWT.NONE);
		btnCancel.setBounds(201, 91, 94, 28);
		btnCancel.setText("Cancel");
		btnCancel.addListener(SWT.Selection, e->onCancel());

	}

	private void onCancel() {
		btnCancel.setEnabled(false);
		worker.requestCancel();
	}

	public void shouldClose() {
		shell.close();
	}

	@Override
	public void notify(ProgressEvent evt) {
		display.syncExec(()->{
			progressBar.setSelection(evt.progress);
			lblJobLabel.setText(evt.job);
		});
	}



}
