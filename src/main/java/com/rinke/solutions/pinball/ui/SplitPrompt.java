package com.rinke.solutions.pinball.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

//@Slf4j
@Bean(name="splitPrompt", scope=Scope.PROTOTYPE)
public class SplitPrompt extends Dialog implements View {

	protected Object result;
	protected Shell shell;
	private Text nameInput;
	@Setter
	private String itemName;
	@Getter @Setter
	private String prompt;
	@Getter
	private int size;
	@Getter
	private boolean okay;

	private Combo comboSize;
	private Label labelSize;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public SplitPrompt(Shell parent) {
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
		shell.setSize(450, 177);
		shell.setText("New "+itemName);
		
		
		Group grpName = new Group(shell, SWT.NONE);
		grpName.setText("     Name     ");
		grpName.setBounds(21, 10, 393, 69);
		
		nameInput = new Text(grpName, SWT.BORDER);
		nameInput.setBounds(10, 21, 371, 23);
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
		btnOkayButton.setBounds(320, 108, 93, 30);
		btnOkayButton.setText("Okay");
		btnOkayButton.addListener(SWT.Selection, e->ok());
		
		Button btnCancelButton = new Button(shell, SWT.NONE);
		btnCancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		btnCancelButton.setBounds(205, 108, 93, 30);
		btnCancelButton.setText("Cancel");
		
		comboSize = new Combo(shell, SWT.NONE);
		comboSize.setBounds(106, 113, 42, 23);
		comboSize.setItems(new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"});
		comboSize.select(0);
		
		labelSize = new Label(shell, SWT.NONE);
		labelSize.setBounds(45, 116, 42, 15);
		labelSize.setText("Size");
		btnCancelButton.addListener(SWT.Selection, e->cancel());
		
	}

	private void cancel() {
		this.okay = false;
		shell.close();
	}

	private void ok() {
		this.prompt = nameInput.getText();
		this.size = comboSize.getSelectionIndex();
		this.okay = true;
		shell.close();
	}
	
	

}
