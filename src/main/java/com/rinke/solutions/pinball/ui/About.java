package com.rinke.solutions.pinball.ui;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.util.VersionUtil;
import com.rinke.solutions.pinball.view.View;

@Bean(name="about", scope=Scope.PROTOTYPE)
public class About extends Dialog implements View {

	@Autowired
	String pluginsPath;
	@Autowired
	List<String> plugins;
	
	protected Shell shlAboutPindmdEditor;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public About(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
		setText("About pin2dmd editor");
	}

	/**
	 * Open the dialog.
	 * @param pluginsPath 
	 * @return the result
	 */
	public void open() {
		createContents(pluginsPath, plugins);
		shlAboutPindmdEditor.open();
		shlAboutPindmdEditor.layout();
		Display display = getParent().getDisplay();
		while (!shlAboutPindmdEditor.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the dialog.
	 * @param pluginsPath 
	 * @param plugins 
	 */
	private void createContents(String pluginsPath, List<String> plugins) {
	    
	    
		shlAboutPindmdEditor = new Shell(getParent(), getStyle());
		shlAboutPindmdEditor.setSize(385, 310);
		shlAboutPindmdEditor.setText("About pin2dmd editor");

		ResourceManager resManager = 
                new LocalResourceManager(JFaceResources.getResources(),shlAboutPindmdEditor);
		
		Label logo = new Label(shlAboutPindmdEditor, SWT.PUSH);
		logo.setImage(resManager.createImage(ImageDescriptor.createFromFile(About.class, "/logo.png")));
		logo.setBounds(10, 10, 195, 114);
		logo.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				if( e.x > 100 && e.x < 108 && e.y > 66 && e.y < 78 ) {
					MessageBox b = new MessageBox(shlAboutPindmdEditor, SWT.OK);
					b.setText("Credits");
					b.setMessage("Thanks to all my supporters. Especially Elke.");
					b.open();				
				}
			}
			
			@Override public void mouseDown(MouseEvent e) {}
			@Override public void mouseDoubleClick(MouseEvent e) {}
		});
		
		Button btnOk = new Button(shlAboutPindmdEditor, SWT.NONE);
		btnOk.addListener(SWT.Selection, e->shlAboutPindmdEditor.close());
		btnOk.setBounds(148, 243, 94, 28);
		btnOk.setText("OK");
		
		Link lblBySteve = new Link(shlAboutPindmdEditor, SWT.NONE);
		lblBySteve.setBounds(211, 22, 144, 103);
		lblBySteve.setText("by Steve\r\n(C) 2016-2020\r\n\r\n\r\n<a href=\"https://github.com/sker65/go-dmd-clock\">https://github.com/sker65/go-dmd-clock</a>");
		
		Label lblVersion = new Label(shlAboutPindmdEditor, SWT.NONE);
		lblVersion.setBounds(72, 126, 283, 111);
		String lbl = VersionUtil.getVersion()+"\nPluginPath: "+pluginsPath+"\nLoaded Plugins:";
		for( String p : plugins ) {
			lbl += "\n"+p;
		}
		lblVersion.setText(lbl);
		
	}

}
