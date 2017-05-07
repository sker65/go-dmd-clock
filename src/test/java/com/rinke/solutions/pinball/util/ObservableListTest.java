package com.rinke.solutions.pinball.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.junit.Before;
import org.junit.Test;

public class ObservableListTest implements Observer {

	private List<Object> delegate = new ArrayList<>();
	
	private ObservableList<Object> uut;

	private boolean changed;

	private Object o;

	@Before
	public void setup() {
		uut = new ObservableList<Object>(delegate);
		o = new Object();
		uut.add(o);
		uut.addObserver(this);
		changed = false;
	}	
	
	@Test
	public void testAddAll() throws Exception {
		uut.addAll(0, new ArrayList<Object>());
		assertTrue(changed);
	}

	@Test
	public void testGet() throws Exception {
		uut.get(0);
		assertFalse(changed);
	}

	@Test
	public void testSet() throws Exception {
		uut.set(0, o);
		assertTrue(changed);
	}

	@Test
	public void testAdd() throws Exception {
		uut.addAll(0, new ArrayList<Object>());
		assertTrue(changed);
	}

	@Test
	public void testRemove() throws Exception {
		uut.remove(0);
		assertTrue(changed);
	}

	@Test
	public void testIndexOf() throws Exception {
		uut.indexOf(o);
		assertFalse(changed);
	}

	@Test
	public void testLastIndexOf() throws Exception {
		uut.lastIndexOf(o);
		assertFalse(changed);
	}

	@Test
	public void testListIterator() throws Exception {
		uut.listIterator();
		assertFalse(changed);
	}

	@Test
	public void testListIteratorInt() throws Exception {
		uut.listIterator(0);
		assertFalse(changed);
	}

	@Test
	public void testSubList() throws Exception {
		uut.subList(0, 1);
		assertFalse(changed);
	}

	@Override
	public void update(Observable o, Object arg) {
		changed = true;
	}

}
