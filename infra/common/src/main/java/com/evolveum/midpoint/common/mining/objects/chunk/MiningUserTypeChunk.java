/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.jetbrains.annotations.NotNull;

/**
 * The `MiningUserTypeChunk` class represents a chunk of role analysis data for a specific user. It contains information
 * about the users, roles, chunk name, frequency, and the role analysis operation mode.
 */
public class MiningUserTypeChunk extends MiningBaseTypeChunk implements Serializable {

    public MiningUserTypeChunk(
            @NotNull List<String> users,
            @NotNull List<String> roles,
            @NotNull String chunkName,
            FrequencyItem frequency,
            @NotNull RoleAnalysisObjectStatus objectStatus) {
        super(roles, users, chunkName, frequency, objectStatus);
    }

    public MiningUserTypeChunk(@NotNull MiningBaseTypeChunk chunk) {
        super(chunk.getProperties(), chunk.getMembers(), chunk.getChunkName(), chunk.getFrequencyItem(), chunk.getObjectStatus());
    }

    public MiningUserTypeChunk(
            @NotNull List<String> users,
            @NotNull List<String> roles,
            @NotNull String chunkName,
            FrequencyItem frequency,
            @NotNull RoleAnalysisOperationMode operationMode) {
        super(roles, users, chunkName, frequency, new RoleAnalysisObjectStatus(operationMode));
    }

    @Override
    public List<String> getMembers() {
        return users;
    }

    @Override
    public void setMembers(@NotNull List<String> members) {
        users.clear();
        users.addAll(members);
    }

    @Override
    public List<String> getProperties() {
        return roles;
    }

    @Override
    public boolean isMemberPresent(@NotNull String member) {
        return users.contains(member);
    }

    @Override
    public boolean isPropertiesPresent(@NotNull String member) {
        return roles.contains(member);
    }

    @Override
    public RoleAnalysisOperationMode getStatus() {
        return super.getStatus();
    }

    @Override
    public RoleAnalysisObjectStatus getObjectStatus() {
        return objectStatus;
    }

    @Override
    public void setObjectStatus(@NotNull RoleAnalysisObjectStatus objectStatus) {
        super.setObjectStatus(objectStatus);
    }

    @Override
    public void setStatus(@NotNull RoleAnalysisOperationMode roleAnalysisOperationMode) {
        super.setStatus(roleAnalysisOperationMode);
    }

}
