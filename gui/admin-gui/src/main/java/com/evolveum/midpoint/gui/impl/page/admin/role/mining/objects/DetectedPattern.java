/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import java.io.Serializable;
import java.util.Set;

public class DetectedPattern implements Serializable {

    public static final String F_METRIC = "clusterMetric";

    public static final String F_TYPE = "searchMode";

    Set<String> properties;
    Set<String> members;
    Integer clusterRelatedPropertiesOccupation;
    Integer allPropertiesOccupation;
    Double clusterMetric;
    RoleAnalysisSearchModeType searchMode;

    public DetectedPattern(Set<String> properties, Set<String> members,
            double clusterMetric,
            Integer clusterRelatedPropertiesOccupation,
            Integer allPropertiesOccupation,
            RoleAnalysisSearchModeType searchMode) {
        this.properties = properties;
        this.members = members;
        this.clusterMetric = clusterMetric;
        this.clusterRelatedPropertiesOccupation = clusterRelatedPropertiesOccupation;
        this.allPropertiesOccupation = allPropertiesOccupation;
        this.searchMode = searchMode;
    }

    public Set<String> getProperties() {
        return properties;
    }

    public Set<String> getMembers() {
        return members;
    }

    public double getClusterMetric() {
        return clusterMetric;
    }

    public int getClusterRelatedPropertiesOccupation() {
        return clusterRelatedPropertiesOccupation;
    }

    public Integer getAllPropertiesOccupation() {
        return allPropertiesOccupation;
    }

    public RoleAnalysisSearchModeType getSearchMode() {
        return searchMode;
    }
}
