package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;

public class BookmarkHandlerTest extends HandlerTest {

	BookmarkHandler uut;
	
	@Before
	public void setUp() throws Exception {
		uut = new BookmarkHandler(vm);
	}

	@Test
	public void testOnSelectedBookmarkChanged() throws Exception {
		Bookmark n = new Bookmark("foo", 10);
		uut.onSelectedBookmarkChanged(null, n );
		assertEquals(0, vm.selectedFrame);
		
		setRecording();
		uut.onSelectedBookmarkChanged(null, n );
		assertEquals(10, vm.selectedFrame);
	}

	void setRecording() {
		CompiledAnimation ani = new CompiledAnimation(AnimationType.COMPILED, "foo", 0, 0, 0, 0, 0);
		ani.setDesc("ani");
		vm.setSelectedRecording(ani);
	}

	@Test
	public void testOnNewBookmark() throws Exception {
		uut.onNewBookmark();
		setRecording();
		uut.onNewBookmark();
		assertTrue( vm.bookmarksMap.containsKey("ani") );
		vm.setEditedBookmarkName("b1");
		vm.setSelectedFrame(120);
		uut.onNewBookmark();
		Set<Bookmark> set = vm.bookmarksMap.get("ani");
		assertTrue( set.size() == 2);
		Optional<Bookmark> bookmark = set.stream().filter(b->b.name.equals("b1")).findFirst();
		assertEquals(120,bookmark.get().pos);
	}

	@Test
	public void testOnDelBookmark() throws Exception {
		uut.onDelBookmark();
	}

	@Test
	public void testAddBookmark() throws Exception {
		CompiledAnimation ani = new CompiledAnimation(AnimationType.COMPILED, "foo", 0, 0, 0, 0, 0);
		ani.setDesc("foo");
		uut.addBookmark(ani, "mark", 20);
	}

}
