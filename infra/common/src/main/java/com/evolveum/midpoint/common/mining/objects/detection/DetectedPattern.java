/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.detection;

import java.io.Serializable;
import java.util.Set;

/**
 * The `DetectedPattern` class represents a detected pattern in role analysis. It contains information about the roles,
 * users, and the cluster metric associated with the detected pattern.
 */
public class DetectedPattern implements Serializable {

    public static final String F_METRIC = "clusterMetric";

    public static final String F_TYPE = "searchMode";

    private final Set<String> roles;
    private final Set<String> users;
    private final Double clusterMetric;

    public DetectedPattern(Set<String> roles, Set<String> users,
            double clusterMetric) {
        this.roles = roles;
        this.users = users;
        this.clusterMetric = clusterMetric;
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

}
