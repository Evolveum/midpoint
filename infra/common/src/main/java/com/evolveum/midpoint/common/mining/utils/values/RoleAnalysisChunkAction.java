
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
