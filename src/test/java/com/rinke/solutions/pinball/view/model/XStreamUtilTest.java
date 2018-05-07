package com.rinke.solutions.pinball.view.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class XStreamUtilTest {

	private XStreamUtil uut;

	@Before
	public void setUp() throws Exception {
		uut = new XStreamUtil();
	}

	@Test
	public void testInit() throws Exception {
		uut.init();
	}

	@Test
	public void testToXML() throws Exception {
		uut.init();
		String xml = uut.toXML("foo");
		assertEquals("<string>foo</string>", xml);
		xml = uut.toXML(new ViewModel());
		System.out.println(xml);
	}

}
