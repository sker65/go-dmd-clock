package com.rinke.solutions.pinball.animation;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rinke.solutions.pinball.model.Frame;

public class FrameTest {

    @Test
    public void testTransformByteArray() throws Exception {
        byte[] plane = new byte[1];
        plane[0] = 0b00000001;
        byte[] transform = Frame.transform(plane );
        assertEquals((byte)0b10000000,transform[0]);
    }

}
