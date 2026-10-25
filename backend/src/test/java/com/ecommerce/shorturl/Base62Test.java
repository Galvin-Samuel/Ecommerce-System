package com.ecommerce.shorturl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    @Test
    @DisplayName("Should encode and decode zero correctly")
    void testZero() {
        String encoded = Base62.encode(0);
        assertThat(encoded).isEqualTo("0");
        assertThat(Base62.decode(encoded)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should encode and decode arbitrary positive integers bidirectionally")
    void testBidirectional() {
        long[] values = {1L, 61L, 62L, 63L, 3844L, 100_000L, 1767225600000L, Long.MAX_VALUE};
        for (long v : values) {
            String encoded = Base62.encode(v);
            assertThat(encoded).isNotBlank();
            long decoded = Base62.decode(encoded);
            assertThat(decoded).isEqualTo(v);
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on negative input for encode")
    void testNegativeInputRejected() {
        assertThatThrownBy(() -> Base62.encode(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on invalid character for decode")
    void testInvalidCharacterRejected() {
        assertThatThrownBy(() -> Base62.decode("abc-123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Base62.decode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
