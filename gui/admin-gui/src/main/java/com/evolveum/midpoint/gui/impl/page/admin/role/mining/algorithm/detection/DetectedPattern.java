/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection;


import java.io.Serializable;
import java.util.Set;

public class DetectedPattern implements Serializable {

    public static final String F_METRIC = "clusterMetric";

    public static final String F_TYPE = "searchMode";

    Set<String> roles;
    Set<String> users;
    Set<String> memberTypeObjectOccupation;
    Double clusterMetric;
    Long id;

    public DetectedPattern(Set<String> roles, Set<String> users,
            double clusterMetric,
            Set<String> memberTypeObjectOccupation, Long id) {
        this.roles = roles;
        this.users = users;
        this.clusterMetric = clusterMetric;
        this.memberTypeObjectOccupation = memberTypeObjectOccupation;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getUsers() {
        return users;
    }

    public double getClusterMetric() {
        return clusterMetric;
    }

    public Set<String> getMemberTypeObjectOccupation() {
        return memberTypeObjectOccupation;
    }

    public void setMemberTypeObjectOccupation(Set<String> memberTypeObjectOccupation) {
        this.memberTypeObjectOccupation = memberTypeObjectOccupation;
    }
}
