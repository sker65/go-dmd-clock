package com.rinke.solutions.pinball;

import java.util.Observable;

import com.rinke.solutions.beans.Bean;

/**
 * simple dto for holding cut marks (start & end). Implements observable which is used from UI
 * @author stefanri
 */
@Bean
public class CutInfo extends Observable {
    
    public CutInfo() {
        super();
    }

    private Integer start;
    private Integer end;
    
    public int getStart() {
        return start==null?0:start;
    }
    
    public void setStart(int start) {
        this.start = start;
        setChanged();
        notifyObservers();
    }

    public int getEnd() {
        return end==null?0:end;
    }
    public void setEnd(int end) {
        this.end = end;
        setChanged();
        notifyObservers();
    }
    
    public void reset() {
        start=null;
        end=null;
        setChanged();
        notifyObservers();
    }
    
    @Override
    public String toString() {
        return "CutInfo [start=" + start + ", end=" + end + "]";
    }

	public boolean canCut() {
		return end!=null && start != null && end>=start;
	}

	public boolean canMarkEnd(int actFrame) {
		return start!=null&&actFrame>=start;
	}

}
