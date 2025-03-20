/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.mining.utils.values;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class RoleAnalysisObjectStatus implements Serializable {
    RoleAnalysisOperationMode roleAnalysisOperationMode;
    private Set<String> containerId = new HashSet<>();

    public RoleAnalysisObjectStatus(RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }

    public RoleAnalysisObjectStatus(@Nullable RoleAnalysisObjectStatus objectStatus) {
        if (objectStatus == null) {
            return;
        }

        this.roleAnalysisOperationMode = objectStatus.getRoleAnalysisOperationMode();
        this.containerId = (objectStatus.getContainerId() != null)
                ? new HashSet<>(objectStatus.getContainerId())
                : null;
    }

    public RoleAnalysisOperationMode getRoleAnalysisOperationMode() {
        return roleAnalysisOperationMode;
    }

    public void setRoleAnalysisOperationMode(RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }

    public Set<String> getContainerId() {
        return containerId;
    }

    public void setContainerId(Set<String> containerId) {
        this.containerId = containerId;
    }

    public void addContainerId(String containerId) {
        if (containerId == null || containerId.isEmpty()) {
            return;
        }
        this.containerId.add(containerId);
    }

}
