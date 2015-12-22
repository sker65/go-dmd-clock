package com.rinke.solutions.pinball.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservableCollectionTest implements Observer {
	
	private Collection<Object> delegate = new ArrayList<>();
	
	private ObservableCollection<Object> uut;

	private boolean changed;

	private Object o;

	@Before
	public void setup() {
		uut = new ObservableCollection<Object>(delegate);
		o = new Object();
		uut.add(o);
		uut.addObserver(this);
		changed = false;
	}
	
	@Test
	public void testSize() throws Exception {
		assertThat(uut.size(),equalTo(1));
		assertFalse(changed);
	}

	@Test
	public void testIsEmpty() throws Exception {
		assertThat(uut.isEmpty(), equalTo(false));
		assertFalse(changed);
	}

	@Test
	public void testRemove() throws Exception {
		uut.remove(o);
		assertTrue(changed);
	}

	@Test
	public void testAddAll() throws Exception {
		uut.addAll(new ArrayList<Object>());
		assertTrue(changed);
	}

	@Test
	public void testRemoveAll() throws Exception {
		uut.removeAll(new ArrayList<>());
		assertTrue(changed);
	}

	@Test
	public void testRemoveIf() throws Exception {
		uut.removeIf(o->o.equals(o));
		assertTrue(changed);
	}

	@Test
	public void testClear() throws Exception {
		uut.clear();
		assertTrue(changed);
	}

	@Override
	public void update(Observable o, Object arg) {
		changed = true;
	}

}
