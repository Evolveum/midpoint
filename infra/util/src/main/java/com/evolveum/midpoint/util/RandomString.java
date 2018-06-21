/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util;

import java.util.Random;

/**
 *
 * @author Vilo Repan
 */
public class RandomString {

    private static final char[] symbols = new char[70];
    private static final int READABLE_SYMBOLS_LENGTH = 62;

    static {
        for (int idx = 0; idx < 10; ++idx) {
            symbols[idx] = (char) ('0' + idx);
        }
        for (int idx = 10; idx < 36; ++idx) {
            symbols[idx] = (char) ('a' + idx - 10);
            symbols[idx + 26] = (char) ('A' + idx - 10);
        }
        symbols[62] = '@';
        symbols[63] = '#';
        symbols[64] = '$';
        symbols[65] = '&';
        symbols[66] = '!';
        symbols[67] = '*';
        symbols[68] = '+';
        symbols[69] = '=';
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
    	int length = symbols.length;
    	if (readable) {
    		length = READABLE_SYMBOLS_LENGTH;
    	}
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[random.nextInt(length)];
        }
        return new String(buf);
    }
}

