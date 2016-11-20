package com.rinke.solutions.pinball.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class About extends Dialog {

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
	public void open(String pluginsPath, List<String> plugins) {
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
		shlAboutPindmdEditor.setSize(385, 259);
		shlAboutPindmdEditor.setText("About pin2dmd editor");

		ResourceManager resManager = 
                new LocalResourceManager(JFaceResources.getResources(),shlAboutPindmdEditor);
		
		Label logo = new Label(shlAboutPindmdEditor, SWT.NONE);
		logo.setImage(resManager.createImage(ImageDescriptor.createFromFile(About.class, "/logo.png")));
		logo.setBounds(10, 10, 195, 114);
		
		Button btnOk = new Button(shlAboutPindmdEditor, SWT.NONE);
		btnOk.addListener(SWT.Selection, e->shlAboutPindmdEditor.close());
		btnOk.setBounds(147, 199, 94, 28);
		btnOk.setText("OK");
		
		Label lblBySteve = new Label(shlAboutPindmdEditor, SWT.NONE);
		lblBySteve.setBounds(211, 22, 144, 103);
		lblBySteve.setText("by Steve\n(C) 2016\n\n\nhttp://github.com/\nsker65/go-dmd-clock");
		
		Label lblVersion = new Label(shlAboutPindmdEditor, SWT.NONE);
		lblVersion.setBounds(72, 126, 283, 67);
		String lbl = getVersion()+"\nPluginPath: "+pluginsPath+"\nLoaded Plugins:";
		for( String p : plugins ) {
			lbl += "\n"+p;
		}
		lblVersion.setText(lbl);
		
	}
	
	public synchronized String getVersion() {
		
		String className = this.getClass().getSimpleName() + ".class";
		String classPath = this.getClass().getResource(className).toString();
		if (!classPath.startsWith("jar")) {
		  // Class not from JAR
		  return "";
		}
		String version = "";
		
		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + 
		    "/META-INF/MANIFEST.MF";
		
	    // try to load from maven properties first
	    try {
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			Attributes attr = manifest.getMainAttributes();
	            version = "Version: " + attr.getValue("version");
	            version += "\nat: " + attr.getValue("buildNumber");
	            version += "\nBuild: " + attr.getValue("drone");
	    } catch (Exception e) {
	        // ignore
	    }

	    // fallback to using Java API
	    if (version == null) {
	        Package aPackage = getClass().getPackage();
	        if (aPackage != null) {
	            version = aPackage.getImplementationVersion();
	            if (version == null) {
	                version = aPackage.getSpecificationVersion();
	            }
	        }
	    }

	    if (version == null) {
	        // we could not compute the version so use a blank
	        version = "";
	    }

	    return version;
	} 
}
