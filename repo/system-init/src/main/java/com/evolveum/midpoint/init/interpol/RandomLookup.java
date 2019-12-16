/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init.interpol;

import org.apache.commons.configuration2.interpol.Lookup;

/**
 * Variable is in the form
 *  - (empty)
 *  - number
 *  - number:Y or
 *  - number:X:Y
 *
 * Returns a random number between X and Y (inclusive), with the default values of X = 0, Y = 999999999.
 *
 * TODO consider moving this downwards to make it available for the rest of midPoint (not only to config.xml parsing).
 */
public class RandomLookup implements Lookup {

    public static final String PREFIX = "random";

    @Override
    public Object lookup(String variable) {
        String[] parts = variable.split(":");
        long lower = 0, upper = 999999999;
        if (parts.length == 0) {
            // use the defaults
        } else if (!"number".equals(parts[0])) {
            throw new IllegalArgumentException("Only random numbers are supported yet. Variable = " + PREFIX + ":" + variable);
        } else if (parts.length == 2) {
            upper = Long.parseLong(parts[1]);
        } else if (parts.length == 3) {
            lower = Long.parseLong(parts[1]);
            upper = Long.parseLong(parts[2]);
        } else if (parts.length > 3) {
            throw new IllegalArgumentException("Too many parts in " + PREFIX + ":" + variable);
        }
        return String.valueOf(lower + (long) (Math.random() * (upper+1-lower)));
    }
}
