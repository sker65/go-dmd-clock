package com.rinke.solutions.pinball.ui;

import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.LabelProviderAdapter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;

@Bean(name="editLink", scope=Scope.PROTOTYPE)
public class EditLink extends Dialog implements EditLinkView {

	protected Object result;
	protected Shell shlEditlink;
	private ComboViewer comboViewer;
	private boolean contentCreadted;
	public  boolean okClicked;
	private Text txtStartFrame;
	Map<String,Animation> values;
	RecordingLink recordingLink;
	private Label lblSceneFoo;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public EditLink(Shell parent) {
		super(parent, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		setText("Edit Link");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public void open() {
		createContents();
		shlEditlink.open();
		shlEditlink.layout();
		okClicked = false;
		Display display = getParent().getDisplay();
		while (!shlEditlink.isDisposed() ) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		contentCreadted=false;
		shlEditlink.dispose();
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		if (contentCreadted) return;
		shlEditlink = new Shell(getParent(), getStyle());
		shlEditlink.setSize(350, 200);
		shlEditlink.setText("EditLink");
		
		Button btnOk = new Button(shlEditlink, SWT.NONE);
		btnOk.setBounds(211, 109, 94, 28);
		btnOk.setText("Ok");
		btnOk.addListener(SWT.Selection, e->onOk());
		
		Button btnCancel = new Button(shlEditlink, SWT.NONE);
		btnCancel.setBounds(111, 109, 94, 28);
		btnCancel.setText("Cancel");
		btnCancel.addListener(SWT.Selection, e->onCancel());
		
		lblSceneFoo = new Label(shlEditlink, SWT.NONE);
		lblSceneFoo.setBounds(10, 10, 159, 14);
		lblSceneFoo.setText("Scene: foo");
		
		comboViewer = new ComboViewer(shlEditlink, SWT.NONE);
		Combo combo = comboViewer.getCombo();
		combo.setBounds(10, 50, 159, 33);
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.setLabelProvider(new LabelProviderAdapter<Animation>(o -> o.getDesc()));

		Label lblLinkedRecording = new Label(shlEditlink, SWT.NONE);
		lblLinkedRecording.setBounds(10, 30, 108, 14);
		lblLinkedRecording.setText("Linked Recording");
		
		txtStartFrame = new Text(shlEditlink, SWT.BORDER);
		txtStartFrame.setBounds(188, 50, 117, 19);
		
		Label lblStartFrame = new Label(shlEditlink, SWT.NONE);
		lblStartFrame.setBounds(188, 30, 94, 14);
		lblStartFrame.setText("Start Frame");
		contentCreadted=true;
	}

	private void onOk() {
		okClicked = true;
		IStructuredSelection selection = (IStructuredSelection) comboViewer.getSelection();
		Animation ani = (Animation) selection.getFirstElement();
		if (selection.size() != 0)
			this.recordingLink = new RecordingLink(ani.getDesc() , Integer.parseInt(txtStartFrame.getText()));
		else 
			this.recordingLink = null;
		close();
	}
	
	private void close() {
		contentCreadted=false;
		shlEditlink.dispose();
	}

	private void onCancel() {
		close();
	}
	
	@Override
	public void setRecordingLink(RecordingLink rl) {
		if( rl != null ) {
			txtStartFrame.setText(Integer.toString(rl.startFrame));
			comboViewer.setSelection(new StructuredSelection(values.get(rl.associatedRecordingName)),true);
		} else {
			txtStartFrame.setText("");
			comboViewer.setSelection(null);
			
		}
	}

	@Override
	public void setSceneName(String name) {
		createContents();
		lblSceneFoo.setText("Scene: "+name);
	}
	
	@Override
	public void setRecordings(Map<String,Animation> v) {
		createContents();
		this.values = v;
		comboViewer.setInput(v.values());
	}

	public RecordingLink getRecordingLink() {
		return recordingLink;
	}

	@Override
	public boolean okClicked() {
		return okClicked;
	}

}
