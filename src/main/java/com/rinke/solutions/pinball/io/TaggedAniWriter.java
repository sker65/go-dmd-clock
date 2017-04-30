package com.rinke.solutions.pinball.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Palette;

import static com.rinke.solutions.pinball.io.TagType.*;

public class TaggedAniWriter {
	
	public List<Tag> write( List<Animation> anisToWrite, List<Palette> palettes) {
		List<Tag> tags = new ArrayList<>();
		try {
			tags.add(Tag.create(VersionInfo).writeShort(4).build());
			// placeholder not needed, because it can be inserted before writing
			// tags.add(new Tag(AniIndex, new byte[4*anisToWrite.size()], CompressionType.UNCOMPRESSED));
			tags.add(Tag.create(Sequence).writeShort(anisToWrite.size()).build());
			for( Animation a : anisToWrite ) {
				// create offset map based on tag list
				writeAni( tags, a, palettes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tags;
	}

	private void writeAni(List<Tag> tags, Animation a, List<Palette> palettes) throws IOException {
		tags.add(
		Tag.create(Animation)
		.writeUTF(a.getDesc())
		.writeShort(a.getCycles())
		.writeShort(a.getHoldCycles())
		// clock while animating
		.writeShort(a.getClockFrom()-a.getStart())
		.writeBoolean(a.isClockSmall())
		.writeBoolean(a.isClockInFront())
		.writeShort(a.getClockXOffset())
		.writeShort(a.getClockYOffset())
		
		.writeShort(a.getRefreshDelay())
		.writeByte(a.getType().ordinal())
		
		.writeByte(a.getFsk())
		.build() );

	}

}
