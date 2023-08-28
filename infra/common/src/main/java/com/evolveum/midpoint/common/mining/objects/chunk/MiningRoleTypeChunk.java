/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MiningRoleTypeChunk implements Serializable {

    private final List<String> users;
    private final List<String> roles;
    private final String chunkName;
    double frequency;
    RoleAnalysisOperationMode roleAnalysisOperationMode;

    public void setStatus(RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }

    public MiningRoleTypeChunk(List<String> roles, List<String> users, String chunkName, double frequency,
            RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roles = new ArrayList<>(roles);
        this.users = new ArrayList<>(users);
        this.chunkName = chunkName;
        this.frequency = frequency;
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public RoleAnalysisOperationMode getStatus() {
        return roleAnalysisOperationMode;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getUsers() {
        return users;
    }

    public String getChunkName() {
        return chunkName;
    }

    public double getFrequency() {
        return frequency;
    }

}
