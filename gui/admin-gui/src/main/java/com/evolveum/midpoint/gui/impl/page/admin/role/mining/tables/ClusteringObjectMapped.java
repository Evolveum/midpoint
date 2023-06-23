/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ClusteringObjectMapped implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    String targetOid;
    List<String> roles;
    List<String> members;

    public ClusteringObjectMapped(String targetOid, List<String> roles,List<String> members) {
        this.targetOid = targetOid;
        this.roles = roles;
        this.members = members;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }
    public String getTargetOid() {
        return targetOid;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusteringObjectMapped clusteringObjectMapped = (ClusteringObjectMapped) o;
        return Objects.equals(targetOid, clusteringObjectMapped.targetOid) &&
                Objects.equals(roles, clusteringObjectMapped.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetOid, roles);
    }


    public List<String> getMembers() {
        return members;
    }

}
