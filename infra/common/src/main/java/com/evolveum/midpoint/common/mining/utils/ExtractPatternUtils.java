/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

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
                (users.size() * roles.size()) - users.size(), null);
    }

    public static DetectedPattern prepareDetectedPattern(@NotNull Set<String> roles, @NotNull Set<String> users, Long patternId) {
        return new DetectedPattern(
                roles,
                users,
                (users.size() * roles.size()) - users.size(), patternId);
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
            Long id = roleAnalysisClusterDetectionType.getId();
            DetectedPattern detectedPattern = prepareDetectedPattern(roles,
                    users, id);

            detectedPattern.setRoleAttributeAnalysisResult(roleAnalysisClusterDetectionType.getRoleAttributeAnalysisResult());
            detectedPattern.setUserAttributeAnalysisResult(roleAnalysisClusterDetectionType.getUserAttributeAnalysisResult());
            Double itemConfidence = roleAnalysisClusterDetectionType.getItemConfidence();
            if (itemConfidence != null) {
                detectedPattern.setItemsConfidence(itemConfidence);
            }
            Double reductionConfidence = roleAnalysisClusterDetectionType.getReductionConfidence();
            if (reductionConfidence != null) {
                detectedPattern.setReductionFactorConfidence(reductionConfidence);
            }

            detectedPattern.setClusterRef(new ObjectReferenceType().oid(cluster.getOid()).type(RoleAnalysisClusterType.COMPLEX_TYPE));
            mergedIntersection.add(detectedPattern);

        }

        return mergedIntersection;
    }

    public static @NotNull DetectedPattern transformPattern(@NotNull RoleAnalysisDetectionPatternType pattern) {
        List<ObjectReferenceType> rolesRef = pattern.getRolesOccupancy();
        List<ObjectReferenceType> usersRef = pattern.getUserOccupancy();

        Set<String> roles = rolesRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
        Set<String> users = usersRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

        return new DetectedPattern(roles, users, (users.size() * roles.size()) - users.size(), pattern.getId());
    }

    public static @NotNull DetectedPattern transformPatternWithAttributes(@NotNull RoleAnalysisDetectionPatternType pattern) {
        List<ObjectReferenceType> rolesRef = pattern.getRolesOccupancy();
        List<ObjectReferenceType> usersRef = pattern.getUserOccupancy();

        Set<String> roles = rolesRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
        Set<String> users = usersRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

        DetectedPattern detectedPattern = new DetectedPattern(roles, users, (users.size() * roles.size()) - users.size(), pattern.getId());
        detectedPattern.setRoleAttributeAnalysisResult(pattern.getRoleAttributeAnalysisResult());
        detectedPattern.setUserAttributeAnalysisResult(pattern.getUserAttributeAnalysisResult());
        Double itemConfidence = pattern.getItemConfidence();
        if (itemConfidence != null) {
            detectedPattern.setItemsConfidence(itemConfidence);
        }
        return detectedPattern;
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
