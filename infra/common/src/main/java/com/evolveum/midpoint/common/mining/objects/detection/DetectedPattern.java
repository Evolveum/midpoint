/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.detection;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * The `DetectedPattern` class represents a detected pattern in role analysis. It contains information about the roles,
 * users, and the cluster metric associated with the detected pattern.
 */
public class DetectedPattern extends BasePattern implements Serializable {

    public static final String F_METRIC = "metric";

    public static final String F_TYPE = "searchMode";
    ObjectReferenceType clusterRef;


    public DetectedPattern(Set<String> roles, Set<String> users, double clusterMetric, Long patternId) {
        super(roles, users, clusterMetric, patternId);
    }

    public DetectedPattern(Set<String> roles, Set<String> users, double clusterMetric, Long patternId, String roleOid, PatternType patternType) {
        super(roles, users, clusterMetric, patternId, roleOid, patternType);
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public void setClusterRef(ObjectReferenceType clusterRef) {
        this.clusterRef = clusterRef;
    }

    public void setSessionRef(ObjectReferenceType sessionRef) {
        this.sessionRef = sessionRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }



}
