package com.satyam.urlshortner.idgen;

public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    public static String encode(long value) {
        if (value == 0) return String.valueOf(ALPHABET.charAt(0));

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }
        return sb.reverse().toString();
    }
}
