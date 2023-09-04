/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

public class ExtractPatternUtils {

    public static DetectedPattern prepareDetectedPattern(Set<String> roles, Set<String> users) {
        return new DetectedPattern(
                roles,
                users,
                users.size() * roles.size());
    }

    public static List<DetectedPattern> transformDefaultPattern(RoleAnalysisClusterType clusterType) {
        List<RoleAnalysisDetectionPatternType> defaultDetection = clusterType.getDetectedPattern();
        List<DetectedPattern> mergedIntersection = new ArrayList<>();

        if (isEmptyDetectionPattern(defaultDetection)) {
            return new ArrayList<>();
        }

        for (RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType : defaultDetection) {

            List<ObjectReferenceType> rolesRef = roleAnalysisClusterDetectionType.getRolesOccupancy();

            List<ObjectReferenceType> usersRef = roleAnalysisClusterDetectionType.getUserOccupancy();

            Set<String> roles = new HashSet<>();
            for (ObjectReferenceType objectReferenceType : rolesRef) {
                roles.add(objectReferenceType.getOid());
            }

            Set<String> users = new HashSet<>();
            for (ObjectReferenceType objectReferenceType : usersRef) {
                users.add(objectReferenceType.getOid());
            }

            DetectedPattern detectedPattern = prepareDetectedPattern(roles,
                    users);

            mergedIntersection.add(detectedPattern);

        }

        return mergedIntersection;
    }

    private static boolean isEmptyDetectionPattern(List<RoleAnalysisDetectionPatternType> defaultDetection) {

        if (defaultDetection == null) {
            return true;
        }

        if (defaultDetection.size() == 1) {
            RoleAnalysisDetectionPatternType detectionPatternType = defaultDetection.get(0);
            return detectionPatternType == null || detectionPatternType.getClusterMetric() == null;
        } else {
            return false;
        }
    }

}
