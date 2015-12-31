package com.rinke.solutions.pinball.util;

import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ObservableMap<K,V> extends Observable implements Map<K,V> {
	
	protected Map<K,V> delegate;

	public ObservableMap(Map<K, V> delegate) {
		super();
		this.delegate = delegate;
	}

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	public V get(Object key) {
		return delegate.get(key);
	}

	public V put(K key, V value) {
		V r = delegate.put(key, value);
		setChanged(); notifyObservers();
		return r;
	}

	public V remove(Object key) {
		V r = delegate.remove(key);
		setChanged(); notifyObservers();
		return r;
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		delegate.putAll(m);
		setChanged(); notifyObservers();
	}

	public void clear() {
		delegate.clear();
		setChanged(); notifyObservers();
	}

	public Set<K> keySet() {
		return delegate.keySet();
	}

	public Collection<V> values() {
		return delegate.values();
	}

	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return delegate.entrySet();
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public V getOrDefault(Object key, V defaultValue) {
		return delegate.getOrDefault(key, defaultValue);
	}

	public void forEach(BiConsumer<? super K, ? super V> action) {
		delegate.forEach(action);
	}

	public void replaceAll(
			BiFunction<? super K, ? super V, ? extends V> function) {
		delegate.replaceAll(function);
		setChanged(); notifyObservers();
	}

	public V putIfAbsent(K key, V value) {
		V r = delegate.putIfAbsent(key, value);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean remove(Object key, Object value) {
		boolean r = delegate.remove(key, value);
		setChanged(); notifyObservers();
		return r;
	}

	public boolean replace(K key, V oldValue, V newValue) {
		boolean r = delegate.replace(key, oldValue, newValue);
		setChanged(); notifyObservers();
		return r;
	}

	public V replace(K key, V value) {
		V r = delegate.replace(key, value);
		setChanged(); notifyObservers();
		return r;
	}

	public V computeIfAbsent(K key,
			Function<? super K, ? extends V> mappingFunction) {
		return delegate.computeIfAbsent(key, mappingFunction);
	}

	public V computeIfPresent(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return delegate.computeIfPresent(key, remappingFunction);
	}

	public V compute(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return delegate.compute(key, remappingFunction);
	}

	public V merge(K key, V value,
			BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return delegate.merge(key, value, remappingFunction);
	}
}
