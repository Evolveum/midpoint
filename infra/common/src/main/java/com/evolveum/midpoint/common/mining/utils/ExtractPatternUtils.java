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

    public static DetectedPattern prepareDetectedPattern(Set<String> properties, Set<String> members) {
        return new DetectedPattern(
                properties,
                members,
                members.size() * properties.size());
    }

    public static List<DetectedPattern> transformDefaultPattern(RoleAnalysisClusterType clusterType) {
        List<RoleAnalysisDetectionPatternType> defaultDetection = clusterType.getDetectedPattern();
        List<DetectedPattern> mergedIntersection = new ArrayList<>();

        for (RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType : defaultDetection) {

            List<ObjectReferenceType> propertiesRef = roleAnalysisClusterDetectionType.getRolesOccupancy();
            List<ObjectReferenceType> membersObject = roleAnalysisClusterDetectionType.getUserOccupancy();

            Set<String> members = new HashSet<>();
            for (ObjectReferenceType objectReferenceType : membersObject) {
                members.add(objectReferenceType.getOid());
            }

            Set<String> properties = new HashSet<>();
            for (ObjectReferenceType objectReferenceType : propertiesRef) {
                properties.add(objectReferenceType.getOid());
            }

            mergedIntersection.add(prepareDetectedPattern(properties,
                    members));

        }

        return mergedIntersection;
    }
}
