package com.rinke.solutions.pinball;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.api.LicenseException;
import com.rinke.solutions.pinball.util.HttpUtil;
import com.rinke.solutions.pinball.util.VersionUtil;

@Slf4j
public class GlobalExceptionHandler {

    private static final String REPORT_URL = "http://go-dmd.de/report.php";
	private static final String USERNAME = "steve";
	private static final String PW = "gbbataPRE7";
	private static final String PKG = "com.rinke.solutions.pinball";
	private static final int MAX_NO_OF_LOGLINES = 500;
	public static final int UPLOAD = 2048;
	private static GlobalExceptionHandler instance;
    
    public static synchronized GlobalExceptionHandler getInstance() {
        if( instance == null ) instance = new GlobalExceptionHandler();
        return instance;
    }

    private Display display;
    private Shell shell;
    private Exception lastException;

    public void setException(Exception e) {
        this.lastException = e;
        showError(lastException);
    }
    
    private String getMessage(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()+": ");
        sb.append(e.getMessage());
        sb.append("\n@   ");
        StackTraceElement ste = e.getStackTrace()[0];
        sb.append(ste.getClassName()+"."
                +ste.getMethodName()+"("
                +ste.getFileName()+":"+ste.getLineNumber()+")");
        
        return sb.toString();
    }
    
    private int returnCode;
    private MultiStatus multiStatus;
    
    public void showError(Exception e) {
        display.asyncExec(new Runnable() {
            
            @Override
            public void run() {
            	if( e instanceof LicenseException ) {
            		MessageBox messageBox = new MessageBox(shell,
            		        SWT.ICON_WARNING | SWT.OK  );
            		
            		messageBox.setText("License problem");
            		messageBox.setMessage("This action requires a license. Error message was: "+ e.getMessage() + 
            				"\nIf you hav a license file please register your key file via menu Help/Register.");
            		messageBox.open();
            	} else {
                    MultiStatus status = createMultiStatus(e.getLocalizedMessage(), e);
                    setMultiStatus(status);
                    ErrorDialog errorDialog = new ErrorDialog(Display.getCurrent().getActiveShell(),
                            "Error", "Ein unerwarteter Fehler ist aufgetreten", status,
                            IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR) {
                    	
                        protected void createButtonsForButtonBar(Composite parent) {
                            createButton(parent, IDialogConstants.ABORT_ID, IDialogConstants.ABORT_LABEL, true);
                            createButton(parent, IDialogConstants.PROCEED_ID, IDialogConstants.PROCEED_LABEL, false);
                            createButton(parent, UPLOAD, "Upload Error Report", false);
                            createDetailsButton(parent);
                        }

                        @Override
                        protected void buttonPressed(int id) {
                            super.buttonPressed(id);
                            if( id == UPLOAD || id == IDialogConstants.ABORT_ID || id == IDialogConstants.PROCEED_ID) {
                                setReturnCode(id);
                                close();
                            }
                        }
                        
                    };
                    int ret = errorDialog.open();
                    setReturnCode(errorDialog.getReturnCode());
                    if( ret == IDialogConstants.ABORT_ID ) System.exit(1);
    				if( ret == UPLOAD) {
    					uploadReport();
    				}

                }
                //System.out.println(ret); // cancel = 1
                lastException=null;
            }

			void uploadReport() {
				try {
					HttpUtil httpUtil = new HttpUtil();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_");
					String name = sdf.format(new Date());
					File tempFile = File.createTempFile(name, ".rep.gz");
					log.debug("creating {}", tempFile.getName());
					GZIPOutputStream stream = new GZIPOutputStream(new FileOutputStream(tempFile));
					PrintWriter writer = new PrintWriter(stream);
					IStatus[] children = getMultiStatus().getChildren();
					for( IStatus s: children) {
						//System.out.println(s.getMessage());
						writer.println(s.getMessage());
					}
					writer.close();
					stream.close();
					httpUtil.postFile(tempFile.getPath(), USERNAME, PW, REPORT_URL);
					tempFile.delete();
				} catch ( Exception e1) {
					e1.printStackTrace();
				}
			}
        });
    }
    
    private static MultiStatus createMultiStatus(String msg, Throwable t) {
        java.util.List<Status> childStatuses = new ArrayList<>();
        StackTraceElement[] stackTraces = t.getStackTrace();
        List<String> logLines = new ArrayList<String>();
        try {
			logLines = IOUtils.readLines(new FileReader(PinDmdEditor.logFile));
			if(logLines.size()>MAX_NO_OF_LOGLINES) logLines = logLines.subList(logLines.size()-MAX_NO_OF_LOGLINES, logLines.size()-1);
		} catch (IOException e) {
		}
        for (StackTraceElement stackTrace : stackTraces) {
            Status status = new Status(IStatus.ERROR, PKG, stackTrace.toString());
            childStatuses.add(status);
        }
        Status statusVersion = new Status(IStatus.INFO, PKG, "--- version: "+VersionUtil.getVersion() );
        childStatuses.add(statusVersion);

        Status status = new Status(IStatus.ERROR, PKG, "----- last "+MAX_NO_OF_LOGLINES+" log lines ----");
        childStatuses.add(status);
        
        for(String line : logLines) {
        	status = new Status(IStatus.INFO, PKG, line);
        	childStatuses.add(status);
        }
        Status statusEnv = new Status(IStatus.ERROR, PKG, "----- environment ----");
        childStatuses.add(statusEnv);
        for ( String key: System.getenv().keySet()) {
            childStatuses.add( new Status(IStatus.INFO, PKG, key + ": "+System.getProperty(key) ));
        }
        
        Properties props = System.getProperties();
        Status statusProp = new Status(IStatus.ERROR, PKG, "----- system properties ----");
        childStatuses.add(statusProp);
        for ( Object key: props.keySet()) {
            childStatuses.add( new Status(IStatus.INFO, PKG, key + ": "+System.getProperty((String) key) ));
        }

        MultiStatus ms = new MultiStatus(PKG, IStatus.ERROR, childStatuses.toArray(new Status[] {}),
                t.toString(), t);
        return ms;
    }

    public Display getDisplay() {
        return display;
    }


    public void setDisplay(Display display) {
        this.display = display;
    }


    public Shell getShell() {
        return shell;
    }

    public void setShell(Shell shell) {
        this.shell = shell;
    }

	public Exception getLastException() {
		return lastException;
	}

	public void setLastException(Exception lastException) {
		this.lastException = lastException;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public MultiStatus getMultiStatus() {
		return multiStatus;
	}

	public void setMultiStatus(MultiStatus multiStatus) {
		this.multiStatus = multiStatus;
	}

}
