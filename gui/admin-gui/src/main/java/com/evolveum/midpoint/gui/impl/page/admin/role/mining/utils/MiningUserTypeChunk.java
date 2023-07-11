/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import java.io.Serializable;
import java.util.List;

public class MiningUserTypeChunk implements Serializable {

    List<String> users;
    List<String> roles;
    String chunkName;
    double frequency;
    ClusterObjectUtils.Status status;

    public MiningUserTypeChunk(List<String> users, List<String> roles, String chunkName, double frequency, ClusterObjectUtils.Status status) {
        this.users = users;
        this.roles = roles;
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

    public void setStatus(ClusterObjectUtils.Status status) {
        this.status = status;
    }


}
