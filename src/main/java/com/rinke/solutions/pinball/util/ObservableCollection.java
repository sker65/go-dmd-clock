package com.rinke.solutions.pinball.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ObservableCollection<T> extends Observable implements Collection<T>{
	
	public ObservableCollection(Collection<T> delegate) {
		super();
		this.delegate = delegate;
	}

	protected Collection<T> delegate;

	public void forEach(Consumer<? super T> action) {
		delegate.forEach(action);
	}

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	public boolean add(T e) {
		boolean r = delegate.add(e);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean remove(Object o) {
		boolean r = delegate.remove(o);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		boolean r = delegate.addAll(c);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean removeAll(Collection<?> c) {
		boolean r = delegate.removeAll(c);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean removeIf(Predicate<? super T> filter) {
		boolean r = delegate.removeIf(filter);
		if( r ){ setChanged(); notifyObservers(); }
		return r;
	}

	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}

	public void clear() {
		delegate.clear();
		setChanged(); notifyObservers();
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public Spliterator<T> spliterator() {
		return delegate.spliterator();
	}

	public Stream<T> stream() {
		return delegate.stream();
	}

	public Stream<T> parallelStream() {
		return delegate.parallelStream();
	}

    @Override
    public String toString() {
        return "ObservableCollection [delegate=" + delegate + "]";
    }
}
