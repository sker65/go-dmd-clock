package com.rinke.solutions.pinball.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class ObservableList<T> extends ObservableCollection<T> implements List<T> {

	public ObservableList() {
		super( new ArrayList<T>());
	}
	
	public ObservableList(List<T> delegate) {
		super(delegate);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		boolean r = ((List<T>)delegate).addAll(index, c);
		setChanged(); notifyObservers();
		return r;
	}

	@Override
	public T get(int index) {
		return ((List<T>)delegate).get(index);
	}

	@Override
	public T set(int index, T element) {
		T r = ((List<T>)delegate).set(index, element);
		setChanged(); notifyObservers();
		return r;
	}

	@Override
	public void add(int index, T element) {
		((List<T>)delegate).add(index, element);
		setChanged(); notifyObservers();
	}

	@Override
	public T remove(int index) {
		T r = ((List<T>)delegate).remove(index);
		setChanged(); notifyObservers();
		return r;
	}

	@Override
	public int indexOf(Object o) {
		return ((List<T>)delegate).indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return ((List<T>)delegate).lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return ((List<T>)delegate).listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return ((List<T>)delegate).listIterator(index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return ((List<T>)delegate).subList(fromIndex, toIndex);
	}

    @Override
    public String toString() {
        return "ObservableList [delegate=" + delegate + "]";
    }

}
