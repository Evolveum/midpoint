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
import java.util.Set;

import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

/**
 * The `MiningBaseTypeChunk` class represents a common base for role and user based analysis data chunks.
 */
public abstract class MiningBaseTypeChunk implements Serializable {

    protected final List<String> users;
    protected final List<String> roles;
    protected String chunkName;
    protected FrequencyItem frequency;
    protected RoleAnalysisObjectStatus objectStatus;
    private String iconColor;

    public MiningBaseTypeChunk(
            @NotNull List<String> roles,
            @NotNull List<String> users,
            @NotNull String chunkName,
            FrequencyItem frequency,
            @NotNull RoleAnalysisObjectStatus objectStatus) {
        this.roles = new ArrayList<>(roles);
        this.users = new ArrayList<>(users);
        this.chunkName = chunkName;
        this.frequency = frequency;
        this.objectStatus = new RoleAnalysisObjectStatus(objectStatus);
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

    public void setMembers(@NotNull List<String> members) {

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

    public FrequencyItem getFrequencyItem() {
        return frequency;
    }

    public double getFrequencyValue() {
        return frequency.getFrequency();
    }


    public void setFrequency(FrequencyItem frequency) {
        this.frequency = frequency;
    }

    public void setObjectStatus(@NotNull RoleAnalysisObjectStatus objectStatus) {
        this.objectStatus = objectStatus;
    }

    public void setStatus(@NotNull RoleAnalysisOperationMode roleAnalysisOperationMode) {
        this.objectStatus = new RoleAnalysisObjectStatus(roleAnalysisOperationMode);
    }

    public RoleAnalysisOperationMode getStatus() {
        return objectStatus.getRoleAnalysisOperationMode();
    }

    public RoleAnalysisObjectStatus getObjectStatus() {
        return objectStatus;
    }

    public Set<String> getCandidateRolesIds() {
        if (objectStatus != null) {
            return objectStatus.getContainerId();
        }
        return null;
    }

    public String getIconColor() {
        return iconColor;
    }

    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    public void setChunkName(String chunkName) {
        this.chunkName = chunkName;
    }
}
