/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import java.util.Random;

/**
 *
 * @author Vilo Repan
 */
public class RandomString {

    private static final char[] SYMBOLS = new char[70];
    private static final int READABLE_SYMBOLS_LENGTH = 62;

    static {
        for (int idx = 0; idx < 10; ++idx) {
            SYMBOLS[idx] = (char) ('0' + idx);
        }
        for (int idx = 10; idx < 36; ++idx) {
            SYMBOLS[idx] = (char) ('a' + idx - 10);
            SYMBOLS[idx + 26] = (char) ('A' + idx - 10);
        }
        SYMBOLS[62] = '@';
        SYMBOLS[63] = '#';
        SYMBOLS[64] = '$';
        SYMBOLS[65] = '&';
        SYMBOLS[66] = '!';
        SYMBOLS[67] = '*';
        SYMBOLS[68] = '+';
        SYMBOLS[69] = '=';
    }

    private final Random random = new Random();
    private final char[] buf;
    private boolean readable = false;

    public RandomString(int length) {
        this(length, false);
    }

    public RandomString(int length, boolean readable) {
        if (length < 1) {
            throw new IllegalArgumentException("length < 1: " + length);
        }
        buf = new char[length];
        this.readable = readable;
    }

    public String nextString() {
        int length = SYMBOLS.length;
        if (readable) {
            length = READABLE_SYMBOLS_LENGTH;
        }
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = SYMBOLS[random.nextInt(length)];
        }
        return new String(buf);
    }
}

