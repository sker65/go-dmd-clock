package com.rinke.solutions.pinball.view.handler;

import java.util.TreeSet;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.util.ObservableSet;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class BookmarkHandler extends ViewHandler {

	public BookmarkHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}
	
	public void onSelectedRecordingChanged(Animation ov, Animation nv) {
		if( nv != null ) {
			ObservableSet<Bookmark> bookmarks = model.bookmarksMap.get(nv.getDesc());
			vm.setBookmarks(bookmarks);
		}
	}
	
	public void onSelectedBookmark() {	
		// need when empty list gets selected
	}
	
	public void onSelectedBookmark(Bookmark bookmark) {	
		if( bookmark != null ) {
			vm.setSelectedFrame(bookmark.pos);
		}
		if( changed(bookmark, vm.selectedBookmark) ) vm.setSelectedBookmark(bookmark);
	}
	
	private boolean changed(Bookmark b0, Bookmark b1) {
		if( b0 == null && b1 == null ) return false;
		if( b0 != null && !b0.equals(b1)) return true;
		if( b1 != null && !b1.equals(b0)) return true;
		return false;
	}

	public void onNewBookmark(int pos) {
		vm.bookmarks.add( new Bookmark(vm.editedBookmarkName, vm.selectedFrame));
	}
	
	public void onDeleteBookmark(Bookmark b) {
		vm.bookmarks.remove(b);
		vm.setEditedBookmarkName("");
	}
	
}
