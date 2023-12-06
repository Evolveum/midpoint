/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.detection;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

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
    private final Long patternId;
    private Long candidateRoleId = null;

    public DetectedPattern(Set<String> roles, Set<String> users,
            double clusterMetric, Long patternId) {
        this.roles = roles;
        this.users = users;
        this.clusterMetric = clusterMetric;
        this.patternId = patternId;
    }

    public DetectedPattern(RoleAnalysisDetectionPatternType detectionPatternType) {
        this.roles = detectionPatternType.getRolesOccupancy()
                .stream().map(AbstractReferencable::getOid).collect(java.util.stream.Collectors.toSet());
        this.users = detectionPatternType.getUserOccupancy()
                .stream().map(AbstractReferencable::getOid).collect(java.util.stream.Collectors.toSet());
        this.clusterMetric = detectionPatternType.getClusterMetric();
        this.patternId = detectionPatternType.getId();
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

    public Long getPatternId() {
        return patternId;
    }

    public Long getCandidateRoleId() {
        return candidateRoleId;
    }

    public void setCandidateRoleId(Long candidateRoleId) {
        this.candidateRoleId = candidateRoleId;
    }

}
