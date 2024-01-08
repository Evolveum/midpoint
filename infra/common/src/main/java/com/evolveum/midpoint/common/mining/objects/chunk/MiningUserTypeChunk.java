/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

/**
 * The `MiningUserTypeChunk` class represents a chunk of role analysis data for a specific user. It contains information
 * about the users, roles, chunk name, frequency, and the role analysis operation mode.
 */
public class MiningUserTypeChunk extends MiningBaseTypeChunk implements Serializable {

    public MiningUserTypeChunk(List<String> users, List<String> roles, String chunkName, double frequency,
            RoleAnalysisOperationMode roleAnalysisOperationMode) {
        super(roles, users, chunkName, frequency, roleAnalysisOperationMode);
    }

    @Override
    public List<String> getMembers() {
        return users;
    }

    @Override
    public List<String> getProperties() {
        return roles;
    }
}
