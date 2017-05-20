package com.rinke.solutions.pinball.util;

import java.util.Collection;
import java.util.Set;

public class ObservableSet<T> extends ObservableCollection<T> implements Set<T> {

	public ObservableSet(Collection<T> delegate) {
		super(delegate);
	}

}
