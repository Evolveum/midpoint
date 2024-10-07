/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.detection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;

import java.io.Serializable;

/**
 * The `DetectionOption` class represents detection options for role analysis. It includes parameters such as minimum
 * and maximum frequency thresholds, minimum users, and minimum roles.
 */
public class PatternDetectionOption implements Serializable {

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

    public PatternDetectionOption(double minFrequencyThreshold, double maxFrequencyThreshold, Integer minUsers, Integer minRoles) {
        this.minFrequencyThreshold = minFrequencyThreshold;
        this.maxFrequencyThreshold = maxFrequencyThreshold;
        this.minUsers = minUsers;
        this.minRoles = minRoles;
    }

    public PatternDetectionOption(RoleAnalysisClusterType cluster) {
        RoleAnalysisDetectionOptionType detectionOption = cluster.getDetectionOption();
        RangeType frequencyRange = detectionOption.getFrequencyRange();
        if (frequencyRange == null) {
            this.minFrequencyThreshold = 0;
            this.maxFrequencyThreshold = 100;
        } else {
            this.minFrequencyThreshold = frequencyRange.getMin();
            this.maxFrequencyThreshold = frequencyRange.getMax();
        }

        this.minUsers = detectionOption.getMinUserOccupancy();
        this.minRoles = detectionOption.getMinRolesOccupancy();
    }
}
