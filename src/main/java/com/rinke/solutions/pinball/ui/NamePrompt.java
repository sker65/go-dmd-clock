package com.rinke.solutions.pinball.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.view.View;

import lombok.Getter;
import lombok.Setter;

//@Slf4j
@Bean(name="namePrompt", scope=Scope.PROTOTYPE)
public class NamePrompt extends Dialog implements View {

	protected Object result;
	protected Shell shell;
	private Text nameInput;
	@Setter
	private String itemName;
	@Getter @Setter
	private String prompt;
	@Getter
	private boolean okay;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NamePrompt(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
		setText("New Item");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public void open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		//return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(450, 217);
		shell.setText("New "+itemName);
		
		Group grpName = new Group(shell, SWT.NONE);
		grpName.setText("     Name     ");
		grpName.setBounds(21, 30, 393, 72);
		
		nameInput = new Text(grpName, SWT.BORDER);
		nameInput.setBounds(10, 23, 371, 23);
		nameInput.setText(prompt);
		nameInput.setSelection(0, prompt.length());
		
		nameInput.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
				// allow RETURN press to confirm
				if( e.character == '\r' || e.character == '\n' ) ok();
			}
		});
		
		Button btnOkayButton = new Button(shell, SWT.NONE);
		btnOkayButton.setBounds(321, 129, 93, 30);
		btnOkayButton.setText("Okay");
		btnOkayButton.addListener(SWT.Selection, e->ok());
		
		Button btnCancelButton = new Button(shell, SWT.NONE);
		btnCancelButton.setBounds(214, 129, 93, 30);
		btnCancelButton.setText("Cancel");
		btnCancelButton.addListener(SWT.Selection, e->cancel());
		
	}

	private void cancel() {
		this.okay = false;
		shell.close();
	}

	private void ok() {
		this.prompt = nameInput.getText();
		this.okay = true;
		shell.close();
	}
	
	

}
