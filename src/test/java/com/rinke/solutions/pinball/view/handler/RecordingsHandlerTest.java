package com.rinke.solutions.pinball.view.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.util.ObservableSet;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class RecordingsHandlerTest {
	
	private static final String ANIFILENAME = "name";

	private static final String ANINAME = "foo";

	@Mock
	private CmdDispatcher d;
	
	private Model model;

	private ViewModel vm;
	
	private RecordingsHandler recordingsHandler;

	private Map<String, Animation> recordings;
	
	Animation ani;
		
	@Before
	public void setup() {
		TestingRealm.createAndSetDefault();
		
		ani = new Animation(AnimationType.COMPILED, ANIFILENAME, 0, 1, 1, 0, 0);
		ani.setDesc(ANINAME);
		
		model = new Model();
		recordings = new HashMap<>();
		recordings.put(ANINAME, ani);
		
		model.recordings = new ObservableMap<>(recordings);
		model.bookmarksMap.put(ANINAME, new ObservableSet<Bookmark>(new TreeSet<>()));
		model.inputFiles.add(ANIFILENAME);
		
		vm = new ViewModel();
		
		recordingsHandler = new RecordingsHandler(vm, model, d);
		recordingsHandler.init();
	}

	@Test
	public void testPopulate() throws Exception {
		recordingsHandler.populate();
	}

	@Test
	public void testOnSortRecordings() throws Exception {
		// for sorting add a few more
		vm.recordings.add( new Animation(AnimationType.MAME,"name",0,0,0,0,0,"1"));
		vm.recordings.add( new Animation(AnimationType.MAME,"name",0,0,0,0,0,"2"));
		vm.recordings.add( new Animation(AnimationType.MAME,"name",0,0,0,0,0,"3"));
		vm.recordings.add( new Animation(AnimationType.MAME,"name",0,0,0,0,0,"0"));
		
		recordingsHandler.onSortRecordings();
		assertThat( vm.recordings.get(0).getDesc(), is("0"));
	}

	@Test
	public void testOnSelectedRecordingChanged() throws Exception {
		Animation oldVal = new Animation(AnimationType.MAME,"name",0,0,0,0,0,"0");
		Animation newVal = new Animation(AnimationType.MAME,"name",0,0,0,0,0,"2");
		recordingsHandler.onSelectedRecordingChanged(oldVal , newVal );
	}

	@Test
	public void testOnDeleteRecording() throws Exception {
		vm.setPlayingAni(ani);
		recordingsHandler.onDeleteRecording(ani);
		assertThat( model.bookmarksMap.keySet(), not(contains(ANINAME)));
		assertThat( model.recordings.keySet(), not(contains(ANINAME)));
		assertThat( model.inputFiles, not(contains(ANIFILENAME)));
		assertThat( vm.playingAni, is(nullValue()));
	}

}
