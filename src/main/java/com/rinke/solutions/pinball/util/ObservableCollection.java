package com.rinke.solutions.pinball.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.rinke.solutions.pinball.view.model.PropertyChangeSupported;

public class ObservableCollection<T> extends Observable implements Collection<T>, PropertyChangeListener{
	
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
		if( e instanceof PropertyChangeSupported ) {
			((PropertyChangeSupported) e).addPropertyChangeListener(this);
		}
		boolean r = delegate.add(e);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean remove(Object o) {
		if( o instanceof PropertyChangeSupported ) {
			((PropertyChangeSupported) o).removePropertyChangeListener(this);
		}
		boolean r = delegate.remove(o);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		for(Object e: c) {
			if( e instanceof PropertyChangeSupported ) {
				((PropertyChangeSupported) e).addPropertyChangeListener(this);
			}
		}
		boolean r = delegate.addAll(c);
		setChanged(); notifyObservers();
		return r;
	}
	
	public boolean replace(Collection<? extends T> newCollection) {
		// calc a diff
		ArrayList<T> toAdd = new ArrayList<>();
		ArrayList<T> toRemove = new ArrayList<>();
		for( T newItem : newCollection) {
			if( !this.contains(newItem) ) {
				toAdd.add(newItem);
			}
		}
		for( T currentItem : delegate) {
			if( !newCollection.contains(currentItem) ) {
				toRemove.add(currentItem);
			}
		}
		if( toAdd.isEmpty() && toRemove.isEmpty() ) return false;
		for( T i : toAdd ) this.add(i);
		for( T i : toRemove ) this.remove(i);
		return true;
	}

	public boolean removeAll(Collection<?> c) {
		for(Object e: c) {
			if( e instanceof PropertyChangeSupported ) {
				((PropertyChangeSupported) e).removePropertyChangeListener(this);
			}
		}
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
		for(Object e : delegate) {
			if( e instanceof PropertyChangeSupported ) {
				((PropertyChangeSupported) e).removePropertyChangeListener(this);
			}			
		}
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

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		setChanged(); notifyObservers();
	}
}
