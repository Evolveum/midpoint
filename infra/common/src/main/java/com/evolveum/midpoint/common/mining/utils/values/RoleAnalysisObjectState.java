/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils.values;

import org.jetbrains.annotations.NotNull;

/**
 * The `RoleAnalysisObjectState` enum represents the state of the role analysis object.
 * It can be either stable or processing.
 * It should be used to determine if operations can be performed on the object.
 */
//TODO we should use different method for this (remove RoleAnalysisObjectState, RoleAnalysisOperationStatusType
// and all related component and compute all thinks direct above existing task?)
public enum RoleAnalysisObjectState {
    STABLE("Stable"),
    PROCESSING("Processing"),
    CLOSED("Closed"),
    SUSPENDED("Suspended"),
    RUNNING("Running");

    private final String displayString;

    RoleAnalysisObjectState(@NotNull String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static boolean isStable(@NotNull String value) {
        return !value.toLowerCase().contains(RUNNING.getDisplayString().toLowerCase());
//        String lowerCaseValue = value.toLowerCase();
//        return lowerCaseValue.equals(STABLE.displayString.toLowerCase())
//                || lowerCaseValue.contains(CLOSED.displayString.toLowerCase())
//                || lowerCaseValue.contains(SUSPENDED.displayString.toLowerCase())
//                || lowerCaseValue.contains("(7/7) runnable");
    }

    public static boolean isProcessing(@NotNull String value) {
        return value.toLowerCase().contains(PROCESSING.displayString.toLowerCase());
    }

}
