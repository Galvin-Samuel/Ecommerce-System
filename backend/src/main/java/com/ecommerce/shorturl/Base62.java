package com.ecommerce.shorturl;

/**
 * Utility for encoding 64-bit numbers into Base62 strings and decoding them back.
 * Alphabet: [0-9a-zA-Z] (62 characters).
 */
public final class Base62 {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62() {
    }

    /**
     * Encodes a positive 64-bit integer into a Base62 string.
     *
     * @param value positive number
     * @return Base62 encoded string
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot encode negative values: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long num = value;
        while (num > 0) {
            int remainder = (int) (num % BASE);
            sb.append(ALPHABET.charAt(remainder));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string into a 64-bit integer.
     *
     * @param str Base62 string
     * @return decoded positive long
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Input string must not be empty");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}
