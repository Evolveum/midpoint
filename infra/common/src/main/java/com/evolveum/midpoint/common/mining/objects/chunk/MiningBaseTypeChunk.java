/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The `MiningBaseTypeChunk` class represents a common base for role and user based analysis data chunks.
 */
public abstract class MiningBaseTypeChunk implements Serializable {

    protected final List<String> users;
    protected final List<String> roles;
    protected final String chunkName;
    protected double frequency;
    protected RoleAnalysisOperationMode roleAnalysisOperationMode;

    public MiningBaseTypeChunk(
            @NotNull List<String> roles,
            @NotNull List<String> users,
            @NotNull String chunkName,
            double frequency,
            @NotNull RoleAnalysisOperationMode roleAnalysisOperationMode) {
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

    public boolean isMemberPresent(@NotNull String member) {
        return getMembers().contains(member);
    }

    public boolean isPropertiesPresent(@NotNull String member) {
        return getProperties().contains(member);
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

    public void setStatus(@NotNull RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.roleAnalysisOperationMode = roleAnalysisOperationMode;
    }
}
