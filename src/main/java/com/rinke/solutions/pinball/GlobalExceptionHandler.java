package com.rinke.solutions.pinball;

import org.eclipse.swt.SWT;
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
                MessageBox messageBox = new MessageBox(shell,
                        SWT.ICON_WARNING | SWT.YES | SWT.NO );
                
                messageBox.setText("Fehler aufgetreten!");
                messageBox.setMessage(getMessage(e)+"\n\nTrotzdem weitermachen?");
                if( SWT.NO == messageBox.open()) System.exit(1);
                lastException=null;
            }
        });
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