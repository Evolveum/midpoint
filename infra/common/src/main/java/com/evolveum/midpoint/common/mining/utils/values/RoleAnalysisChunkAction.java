/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;

public enum RoleAnalysisChunkAction implements Serializable {

    SELECTION("SELECTION"),
    DETAILS_DETECTION("DETAILS DETECTION"),
    EXPLORE_DETECTION("EXPLORE DETECTION");

    private final String displayString;

    RoleAnalysisChunkAction(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }
}
