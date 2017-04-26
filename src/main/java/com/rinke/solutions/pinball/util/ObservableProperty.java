package com.rinke.solutions.pinball.util;

import java.util.Observable;
import java.util.function.Consumer;

import com.rinke.solutions.pinball.animation.Animation;


public class ObservableProperty<T> extends Observable {
	
	protected T delegate;

	public ObservableProperty(T def) {
		this.delegate = def;
	}

	public T get() {
		return delegate;
	}

	public void set(T delegate) {
		//firePropertyChange("this.delegate", this.delegate, this.delegate = delegate);
		this.delegate = delegate;
		setChanged(); notifyObservers();
	}

	public boolean isPresent() {
		return delegate != null;
	}

	public T orElse(T def) {
		return delegate == null ? def : delegate;
	}
	
	public void ifPresent(Consumer<? super T> consumer) {
        if (delegate != null)
            consumer.accept(delegate);
    }

	@Override
	public String toString() {
		return String.format("ObservableProperty [delegate=%s]", delegate);
	}

}
