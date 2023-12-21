/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;

/**
 * Enumeration representing different role analysis processing modes.
 * <p>
 * It is used in the context of role analysis.
 * <p>
 * This enum provides processing modes for role analysis, including DETECTION, CLUSTERING, MIGRATION and DEFAULT.
 */
public enum RoleAnalysisChannelMode implements Serializable {
    DETECTION("detection", ""),
    CLUSTERING("clustering", ""),
    MIGRATION("migration", ""),
    DEFAULT("default", "");

    private final String displayString;
    private String objectIdentifier;

    RoleAnalysisChannelMode(String displayString, String objectIdentifier) {
        this.displayString = displayString;
        this.objectIdentifier = objectIdentifier;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getFullChannelIdentifier() {
        if (objectIdentifier == null || objectIdentifier.isEmpty()) {
            return displayString;
        }

        return displayString + " : " + objectIdentifier;
    }

    public void setObjectIdentifier(String objectIdentifier) {
        this.objectIdentifier = objectIdentifier;
    }

}
