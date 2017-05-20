package com.rinke.solutions.pinball.view.handler;

import org.eclipse.core.databinding.observable.Realm;

public class TestingRealm extends Realm {

	@Override
	public boolean isCurrent() {
		return true;
	}
	
	public static void createAndSetDefault() {
		TestingRealm r = new TestingRealm();
		Realm.setDefault(r);
	}

}
