/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The `ExtractPatternUtils` class provides utility methods for preparing and transforming detected patterns.
 * <p>
 * It's a part of the `Role Analysis`.
 * </p>
 */
public class ExtractPatternUtils {

    public static DetectedPattern prepareDetectedPattern(@NotNull Set<String> roles, @NotNull Set<String> users) {
        return new DetectedPattern(
                roles,
                users,
                users.size() * roles.size());
    }

    public static @NotNull List<DetectedPattern> transformDefaultPattern(@NotNull RoleAnalysisClusterType cluster) {
        List<RoleAnalysisDetectionPatternType> defaultDetection = cluster.getDetectedPattern();
        List<DetectedPattern> mergedIntersection = new ArrayList<>();

        if (isEmptyDetectionPattern(defaultDetection)) {
            return new ArrayList<>();
        }

        for (RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType : defaultDetection) {

            List<ObjectReferenceType> rolesRef = roleAnalysisClusterDetectionType.getRolesOccupancy();

            List<ObjectReferenceType> usersRef = roleAnalysisClusterDetectionType.getUserOccupancy();

            Set<String> roles = rolesRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

            Set<String> users = usersRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

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
