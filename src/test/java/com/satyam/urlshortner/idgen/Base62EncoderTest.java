package com.satyam.urlshortner.idgen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Base62EncoderTest {

    @Test
    void encodesZeroAsFirstAlphabetCharacter() {
        assertEquals("0", Base62Encoder.encode(0L));
    }

    @Test
    void encodingIsDeterministic() {
        long value = 123_456_789L;
        assertEquals(Base62Encoder.encode(value), Base62Encoder.encode(value));
    }

    @Test
    void distinctValuesProduceDistinctCodes() {
        Set<String> codes = new HashSet<>();
        for (long i = 0; i < 10_000; i++) {
            String code = Base62Encoder.encode(i);
            assertEquals(true, codes.add(code), "duplicate code for value " + i);
        }
    }

    @Test
    void largeValuesEncodeToLongerCodes() {
        String small = Base62Encoder.encode(1L);
        String large = Base62Encoder.encode(Long.MAX_VALUE);
        assertNotEquals(small, large);
    }
}
