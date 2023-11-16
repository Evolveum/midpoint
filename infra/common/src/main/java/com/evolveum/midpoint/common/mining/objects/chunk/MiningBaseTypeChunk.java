/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

/**
 * The `MiningBaseTypeChunk` class represents a common base for role and user based analysis data chunks.
 */
public abstract class MiningBaseTypeChunk implements Serializable {

    protected final List<String> users;
    protected final List<String> roles;
    protected final String chunkName;
    protected double frequency;
    protected RoleAnalysisOperationMode roleAnalysisOperationMode;

    public MiningBaseTypeChunk(List<String> roles, List<String> users, String chunkName, double frequency,
            RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roles = new ArrayList<>(roles);
        this.users = new ArrayList<>(users);
        this.chunkName = chunkName;
        this.frequency = frequency;
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getUsers() {
        return users;
    }

    public List<String> getMembers() {
        return new ArrayList<>();
    }

    public List<String> getProperties() {
        return new ArrayList<>();
    }

    public String getChunkName() {
        return chunkName;
    }

    public double getFrequency() {
        return frequency;
    }

    public RoleAnalysisOperationMode getStatus() {
        return roleAnalysisOperationMode;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public void setStatus(RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }
}
