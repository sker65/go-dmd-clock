package com.rinke.solutions.pinball.view.handler;

import java.util.TreeSet;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class BookmarkHandler extends ViewHandler {

	public BookmarkHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}
	
	TreeSet<Bookmark> saveGetBookmarkByLabel(TypedLabel nv) {
		TreeSet<Bookmark> bookmarks = new TreeSet<Bookmark>();
		if( nv != null ) {
			if( model.bookmarksMap.containsKey(nv.label)) {
				bookmarks.addAll(model.bookmarksMap.get(nv.label));
			}
		}
		return bookmarks;
	}
	
	public void onSelectedRecordingChanged(TypedLabel ov, TypedLabel nv) {
		TreeSet<Bookmark> bookmarks = saveGetBookmarkByLabel(nv);
		vm.setBookmarks(bookmarks);
	}
	
	public void onSelectedBookmark(Bookmark bookmark) {	
		if( bookmark != null ) {
			vm.setActFrame(bookmark.pos);
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
		TreeSet<Bookmark> bookmarks = saveGetBookmarkByLabel(vm.selectedRecording);
		bookmarks.add( new Bookmark(vm.editedBookmarkName, vm.actFrame));
		model.bookmarksMap.put(vm.selectedRecording.label, bookmarks);
		vm.setBookmarks(bookmarks);
	}
	
	public void onDeleteBookmark(Bookmark b) {
		TreeSet<Bookmark> bookmarks = saveGetBookmarkByLabel(vm.selectedRecording);
		bookmarks.remove(b);
		model.bookmarksMap.put(vm.selectedRecording.label, bookmarks);
		vm.setBookmarks(bookmarks);	
		vm.setEditedBookmarkName("");
	}
	
}
