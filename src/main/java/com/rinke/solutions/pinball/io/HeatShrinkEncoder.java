package com.rinke.solutions.pinball.io;

public class HeatShrinkEncoder {
	
	enum SinkRes {
		OK,               /* data sunk into input buffer */
	    ERROR_NULL,    /* NULL argument */
	    ERROR_MISUSE,
	}
	
	enum PollRes {
		EMPTY,            /* input exhausted */
	    MORE,             /* poll again for more output  */
	    ERROR_NULL,    /* NULL argument */
	    ERROR_MISUSE,  /* API misuse */
	}
	
	enum FinishRes {
		DONE,           /* encoding is complete */
	    MORE,           /* more output remaining; use poll */
	    ERROR_NULL,  /* NULL argument */
	}
	
	enum State {
		    HSES_NOT_FULL,              /* input buffer not full enough */
		    HSES_FILLED,                /* buffer is full */
		    HSES_SEARCH,                /* searching for patterns */
		    HSES_YIELD_TAG_BIT,         /* yield tag bit */
		    HSES_YIELD_LITERAL,         /* emit literal byte */
		    HSES_YIELD_BR_INDEX,        /* yielding backref index */
		    HSES_YIELD_BR_LENGTH,       /* yielding backref length */
		    HSES_SAVE_BACKLOG,          /* copying buffer to backlog */
		    HSES_FLUSH_BITS,            /* flush bit buffer */
		    HSES_DONE,                  /* done */
	}

	private static final int HEATSHRINK_STATIC_INPUT_BUFFER_SIZE = 32;
	private static final int HEATSHRINK_STATIC_WINDOW_BITS = 10;
	private static final int  HEATSHRINK_STATIC_LOOKAHEAD_BITS = 5;
	    
	private static final int FLAG_IS_FINISHING = 1;
	
    int input_size;        /* bytes in input buffer */
    int match_scan_index;
    int match_length;
    int match_pos;
    int outgoing_bits;     /* enqueued outgoing bits */
    int outgoing_bits_count;
    int flags;
    int state;              /* current state machine node */
    int current_byte;       /* current byte of output */
    int bit_index;          /* current bit index */

    /* input buffer and / sliding window for expansion */
    byte buffer[] = new byte[2 << HEATSHRINK_STATIC_WINDOW_BITS];
    
    public void reset() {
    	
    }
    
    /* Sink up to SIZE bytes from IN_BUF into the encoder.
     * INPUT_SIZE is set to the number of bytes actually sunk (in case a
     * buffer was filled.). */
    public SinkRes sink(byte[] in_buf /*, size_t *input_size*/) {
    	return SinkRes.OK;
    }
    
    /* Poll for output from the encoder, copying at most OUT_BUF_SIZE bytes into
     * OUT_BUF (setting *OUTPUT_SIZE to the actual amount copied). */
    public PollRes poll(byte[]out_buf /* size_t *output_size*/) {
    	return PollRes.EMPTY;
    }
    
    /* Notify the encoder that the input stream is finished.
     * If the return value is HSER_FINISH_MORE, there is still more output, so
     * call heatshrink_encoder_poll and repeat. */
    public FinishRes finish() {
    	return FinishRes.DONE;
    }
}
