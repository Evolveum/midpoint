/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import java.io.Serializable;
import java.util.Set;

public class IntersectionObject implements Serializable {

    public static final String F_METRIC = "metric";

    public static final String F_TYPE = "type";

    Set<String> rolesId;
    String type;
    int members;
    Integer totalMembers;
    double metric;

    public IntersectionObject(Set<String> rolesId, double metric, String type, int members, Integer totalMembers) {
        this.rolesId = rolesId;
        this.metric = metric;
        this.type = type;
        this.members = members;
        this.totalMembers = totalMembers;
    }

    public Set<String> getRolesId() {
        return rolesId;
    }

    public double getMetric() {
        return metric;
    }

    public String getType() {
        return type;
    }

    public int getMembers() {
        return members;
    }

    public Integer getTotalMembers() {
        return totalMembers;
    }

}
