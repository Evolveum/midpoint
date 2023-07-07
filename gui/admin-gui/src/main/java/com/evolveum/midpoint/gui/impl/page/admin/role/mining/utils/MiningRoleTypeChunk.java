/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import java.io.Serializable;
import java.util.List;

public class MiningRoleTypeChunk implements Serializable {

    List<String> roles;
    List<String> users;
    String chunkName;
    double frequency;

    public void setStatus(ClusterObjectUtils.Status status) {
        this.status = status;
    }

    ClusterObjectUtils.Status status;

    public MiningRoleTypeChunk(List<String> roles, List<String> users, String chunkName, double frequency, ClusterObjectUtils.Status status) {
        this.roles = roles;
        this.users = users;
        this.chunkName = chunkName;
        this.frequency = frequency;
        this.status = status;
    }

    public ClusterObjectUtils.Status getStatus() {
        return status;
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
