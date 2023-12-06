/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enumeration representing different operation modes for role analysis.
 * <p>
 * This enum provides operation modes for role analysis, including EXCLUDE, INCLUDE, and DISABLE.
 * Additionally, it provides methods for toggling the status and checking the current mode.
 */
public enum RoleAnalysisOperationMode implements Serializable {

    EXCLUDE("fa fa-plus"),
    INCLUDE("fa fa-minus"),
    DISABLE("fa fa-ban");

    public Set<String> getContainerId() {
        return containerId;
    }

    public void setContainerId(Set<String> containerId) {
        this.containerId = containerId;
    }

    public void addContainerId(String containerId) {
      this.containerId.add(containerId);
    }

    private Set<String> containerId = new HashSet<>();

    private final String displayString;

    RoleAnalysisOperationMode(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    /**
     * Toggle the operation mode status. If it is EXCLUDE, it will be changed to INCLUDE.
     * If it is DISABLE, it will remain DISABLE.
     *
     * @return The toggled operation mode.
     */
    public RoleAnalysisOperationMode toggleStatus() {
        if (this == EXCLUDE) {
            return INCLUDE;
        } else if (this == INCLUDE) {
            return EXCLUDE;
        } else {
            return DISABLE;
        }
    }

    public boolean isInclude() {
        return this == INCLUDE;
    }

    public boolean isDisable() {
        return this == DISABLE;
    }

    public boolean isExclude() {
        return this == EXCLUDE;
    }
}
