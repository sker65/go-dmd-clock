package com.rinke.solutions.pinball.view.handler;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class BookmarkHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired AnimationHandler animationHandler;

	public BookmarkHandler(ViewModel vm ) {
		super(vm);
	}
	
	public void onSelectedBookmarkChanged(Bookmark o, Bookmark n) {
		if( n != null && vm.selectedRecording != null ) {
			vm.setSelectedFrame(n.pos);
			// TODO should ne decoupled
			animationHandler.setPos(n.pos);
		}
	}

	public void onNewBookmark() {
		if( vm.selectedRecording!=null ) {
			addBookmark(vm.selectedRecording, vm.editedBookmarkName, vm.selectedFrame);
		}
	};

	public void onDelBookmark() {
		if( vm.selectedRecording != null ) {
			Set<Bookmark> set = vm.bookmarksMap.get(vm.selectedRecording.getDesc());
			if( set != null ) {
				set.remove(vm.selectedBookmark);
				vm.bookmarks.clear();
				vm.bookmarks.addAll(set);
			}
		}
	};

	void addBookmark(Animation animation, String bookmarkName, int pos) {
		if( StringUtils.isEmpty(bookmarkName)) bookmarkName = "Bookmark";
		Set<Bookmark> set = vm.bookmarksMap.get(animation.getDesc());
		if( set == null ) {
			set = new TreeSet<Bookmark>();
			vm.bookmarksMap.put(animation.getDesc(),set);
		}
		set.add(new Bookmark(bookmarkName, pos));
		vm.bookmarks.clear();
		vm.bookmarks.addAll(set);
	}


}
