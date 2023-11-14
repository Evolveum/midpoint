/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;

/**
 * Enumeration representing different chunk modes that can be used for role mining data set generation.
 * <p>
 * It is used in the context of role analysis.
 * <p>
 * This enum provides chunk modes for role analysis, including EXPAND and COMPRESS.
 */
public enum RoleAnalysisChunkMode implements Serializable {
    EXPAND("EXPAND"),
    COMPRESS("COMPRESS");

    private final String displayString;

    RoleAnalysisChunkMode(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

}
