package com.rinke.solutions.pinball;

import java.util.ArrayList;

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

public class GlobalExceptionHandler {

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
    
    
    public void showError(Exception e) {
        display.asyncExec(new Runnable() {
            
            @Override
            public void run() {
                MultiStatus status = createMultiStatus(e.getLocalizedMessage(), e);
                ErrorDialog errorDialog = new ErrorDialog(Display.getCurrent().getActiveShell(),
                        "Error", "Ein unerwarteter Fehler ist aufgetreten", status,
                        IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR) {
                    protected void createButtonsForButtonBar(Composite parent) {
                        createButton(parent, IDialogConstants.ABORT_ID, IDialogConstants.ABORT_LABEL, true);
                        createButton(parent, IDialogConstants.PROCEED_ID, IDialogConstants.PROCEED_LABEL, false);
                        createDetailsButton(parent);
                    }

                    @Override
                    protected void buttonPressed(int id) {
                        super.buttonPressed(id);
                        if( id == IDialogConstants.ABORT_ID || id == IDialogConstants.PROCEED_ID) {
                            setReturnCode(id);
                            close();
                        }
                    }
                    
                };
                int ret = errorDialog.open();
                if( ret == IDialogConstants.ABORT_ID ) System.exit(1);
                System.out.println(ret); // cancel = 1
                lastException=null;
            }
        });
    }
    
    private static MultiStatus createMultiStatus(String msg, Throwable t) {

        java.util.List<Status> childStatuses = new ArrayList<>();
        StackTraceElement[] stackTraces = t.getStackTrace();

        for (StackTraceElement stackTrace : stackTraces) {
            Status status = new Status(IStatus.ERROR, "com.rinke.solutions.pinball", stackTrace.toString());
            childStatuses.add(status);
        }

        MultiStatus ms = new MultiStatus("com.rinke.solutions.pinball", IStatus.ERROR, childStatuses.toArray(new Status[] {}),
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



}
