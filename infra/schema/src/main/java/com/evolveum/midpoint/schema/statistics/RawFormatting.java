/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Formatting that - in fact - does nothing.
 *
 * It just provides abstract Formatting functionality, namely maintains a list of column names.
 */
@Experimental
public class RawFormatting extends Formatting {

    public String apply(Data data) {
        return "Raw formatting has no String representation";
    }
}
