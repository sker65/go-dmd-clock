package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.model.Mask;

@RunWith(MockitoJUnitRunner.class)
public class HashCmdHandlerTest extends HandlerTest {
	
	private static final String PRINT_HASH = "16212C37";

	@Mock KeyframeHandler keyframeHandler;
	
	@InjectMocks
	HashCmdHandler uut = new HashCmdHandler(vm);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testOnHashSelected() throws Exception {
		uut.onHashSelected(0, true);
	}

	@Test
	public void testOnHashSelectedOff() throws Exception {
		uut.onHashSelected(0, false);
	}

	@Test
	public void testOnSelectedHashIndexChanged() throws Exception {
		uut.onSelectedHashIndexChanged(0, 1);
	}

	@Test
	public void testGetPrintableHashes() throws Exception {
		String hashes = uut.getPrintableHashes(new byte[]{22,33,44,55});
		assertEquals(PRINT_HASH, hashes);
	}

	@Test
	public void testRefreshHashButtons() throws Exception {
		List<byte[]> hashes = new ArrayList<>();
		hashes.add(new byte[]{22,33,44,55});
		uut.refreshHashButtons(hashes, null, 0);
		assertEquals(PRINT_HASH, vm.hashLbl[0]);
	}

	@Test
	public void testRefreshHashButtonsWithMask() throws Exception {
		List<byte[]> hashes = new ArrayList<>();
		hashes.add(new byte[]{22,33,44,55});
		uut.refreshHashButtons(hashes, new Mask(DmdSize.Size128x32.planeSize), 1);
		assertEquals("M1 "+PRINT_HASH, vm.hashLbl[0]);
	}

	@Test
	public void testGetEmptyHash() throws Exception {
		assertEquals(DmdSize.Size128x32, vm.dmdSize);
		assertEquals("B2AA7578",uut.getEmptyHash());
	}

	@Test
	public void testUpdateKeyFrameButtons() throws Exception {
		uut.updateKeyFrameButtons(getScene(""), getScene(""), 0);
	}

}
