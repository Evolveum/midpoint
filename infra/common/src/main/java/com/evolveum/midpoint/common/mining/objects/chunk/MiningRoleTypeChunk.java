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
 * The `MiningRoleTypeChunk` class represents a chunk of role analysis data for a specific role. It contains information
 * about the roles, users, chunk name, frequency, and the role analysis operation mode.
 */
public class MiningRoleTypeChunk extends MiningBaseTypeChunk implements Serializable {

    public MiningRoleTypeChunk(
            @NotNull List<String> roles,
            @NotNull List<String> users,
            @NotNull String chunkName,
            FrequencyItem frequency,
            @NotNull RoleAnalysisObjectStatus objectStatus) {
        super(roles, users, chunkName, frequency, objectStatus);
    }

    public MiningRoleTypeChunk(MiningBaseTypeChunk chunk){
        super(chunk.getMembers(), chunk.getProperties(), chunk.getChunkName(), chunk.getFrequencyItem(), chunk.getObjectStatus());
    }

    public MiningRoleTypeChunk(
            @NotNull List<String> roles,
            @NotNull List<String> users,
            @NotNull String chunkName,
            FrequencyItem frequency,
            @NotNull RoleAnalysisOperationMode operationMode) {
        super(roles, users, chunkName, frequency, new RoleAnalysisObjectStatus(operationMode));
    }

    @Override
    public List<String> getMembers() {
        return roles;
    }

    @Override
    public void setMembers(@NotNull List<String> members) {
        roles.clear();
        roles.addAll(members);
    }

    @Override
    public List<String> getProperties() {
        return users;
    }

    @Override
    public boolean isMemberPresent(@NotNull String member) {
        return roles.contains(member);
    }

    @Override
    public boolean isPropertiesPresent(@NotNull String member) {
        return users.contains(member);
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
