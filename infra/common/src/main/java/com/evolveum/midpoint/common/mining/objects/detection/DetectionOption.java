/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.detection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import java.io.Serializable;

public class DetectionOption implements Serializable {

    private final double minFrequencyThreshold;
    private final Integer minUsers;
    private final double maxFrequencyThreshold;
    private final Integer minRoles;

    public double getMinFrequencyThreshold() {
        return minFrequencyThreshold;
    }

    public Integer getMinUsers() {
        return minUsers;
    }

    public double getMaxFrequencyThreshold() {
        return maxFrequencyThreshold;
    }

    public Integer getMinRoles() {
        return minRoles;
    }

    public DetectionOption(double minFrequencyThreshold, double maxFrequencyThreshold, Integer minUsers, Integer minRoles) {
        this.minFrequencyThreshold = minFrequencyThreshold;
        this.maxFrequencyThreshold = maxFrequencyThreshold;
        this.minUsers = minUsers;
        this.minRoles = minRoles;
    }

    public DetectionOption(RoleAnalysisClusterType cluster) {
        this.minFrequencyThreshold = cluster.getDetectionOption().getFrequencyRange().getMin();
        this.maxFrequencyThreshold = cluster.getDetectionOption().getFrequencyRange().getMax();
        this.minUsers = cluster.getDetectionOption().getMinUserOccupancy();
        this.minRoles = cluster.getDetectionOption().getMinRolesOccupancy();
    }
}
