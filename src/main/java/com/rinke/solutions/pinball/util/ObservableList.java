package com.rinke.solutions.pinball.util;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class ObservableList<T> extends ObservableCollection<T> implements List<T> {

	List<T> listDelegate;
	
	public ObservableList(List<T> delegate) {
		super(delegate);
		listDelegate = delegate;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		boolean r = listDelegate.addAll(index, c);
		setChanged(); notifyObservers();
		return r;
	}

	@Override
	public T get(int index) {
		return listDelegate.get(index);
	}

	@Override
	public T set(int index, T element) {
		T r = listDelegate.set(index, element);
		setChanged(); notifyObservers();
		return r;
	}

	@Override
	public void add(int index, T element) {
		listDelegate.add(index, element);
		setChanged(); notifyObservers();
	}

	@Override
	public T remove(int index) {
		T r = listDelegate.remove(index);
		setChanged(); notifyObservers();
		return r;
	}

	@Override
	public int indexOf(Object o) {
		return listDelegate.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return listDelegate.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return listDelegate.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return listDelegate.listIterator(index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return listDelegate.subList(fromIndex, toIndex);
	}
	
	public void replaceAll( Collection<? extends T> c ) {
		listDelegate.clear();
		listDelegate.addAll(c);
		setChanged(); notifyObservers();
	}

    @Override
    public String toString() {
        return "ObservableList [listDelegate=" + listDelegate + "]";
    }

}
