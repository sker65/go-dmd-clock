package com.rinke.solutions.pinball;

import java.util.Observable;

/**
 * simple dto for holding cut marks (start & end). Implements observable which is used from UI
 * @author stefanri
 */
public class CutInfo extends Observable {
    
    public CutInfo() {
        super();
    }

    private int start;
    private int end;
    
    public int getStart() {
        return start;
    }
    
    public void setStart(int start) {
        this.start = start;
        setChanged();
        notifyObservers();
    }

    public int getEnd() {
        return end;
    }
    public void setEnd(int end) {
        this.end = end;
        setChanged();
        notifyObservers();
    }
    
    public void reset() {
        start=0;
        end=0;
        setChanged();
        notifyObservers();
    }
    
    @Override
    public String toString() {
        return "CutInfo [start=" + start + ", end=" + end + "]";
    }

}
