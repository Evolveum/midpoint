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

import com.evolveum.midpoint.common.mining.objects.detection.BasePattern;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

import org.jetbrains.annotations.Nullable;

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
        return transformDefaultPattern(cluster, null, null);
    }

    public static @NotNull List<DetectedPattern> transformDefaultPattern(@NotNull RoleAnalysisClusterType cluster, RoleAnalysisSessionType session) {
        return transformDefaultPattern(cluster, session, null);
    }

    public static @NotNull List<DetectedPattern> transformDefaultPattern(@NotNull RoleAnalysisClusterType cluster,
            @Nullable RoleAnalysisSessionType session,
            Long selectedPatternId) {
        List<RoleAnalysisDetectionPatternType> defaultDetection = cluster.getDetectedPattern();
        List<DetectedPattern> mergedIntersection = new ArrayList<>();

        if (isEmptyDetectionPattern(defaultDetection)) {
            return new ArrayList<>();
        }

        for (RoleAnalysisDetectionPatternType roleAnalysisDetectionPattern : defaultDetection) {

            List<ObjectReferenceType> rolesRef = roleAnalysisDetectionPattern.getRolesOccupancy();

            List<ObjectReferenceType> usersRef = roleAnalysisDetectionPattern.getUserOccupancy();

            Set<String> roles = rolesRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

            Set<String> users = usersRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
            Long id = roleAnalysisDetectionPattern.getId();
            DetectedPattern detectedPattern = prepareDetectedPattern(roles,
                    users, id);

            detectedPattern.setRoleAttributeAnalysisResult(roleAnalysisDetectionPattern.getRoleAttributeAnalysisResult());
            detectedPattern.setUserAttributeAnalysisResult(roleAnalysisDetectionPattern.getUserAttributeAnalysisResult());
            Double itemConfidence = roleAnalysisDetectionPattern.getItemConfidence();
            if (itemConfidence != null) {
                detectedPattern.setItemsConfidence(itemConfidence);
            }
            Double reductionConfidence = roleAnalysisDetectionPattern.getReductionConfidence();
            if (reductionConfidence != null) {
                detectedPattern.setReductionFactorConfidence(reductionConfidence);
            }

            detectedPattern.setClusterRef(new ObjectReferenceType()
                    .oid(cluster.getOid())
                    .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                    .targetName(cluster.getName()));

            ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

            if (roleAnalysisSessionRef != null) {
                detectedPattern.setSessionRef(roleAnalysisSessionRef.clone());
            }

            if (session != null) {
                detectedPattern.setSessionRef(new ObjectReferenceType()
                        .oid(session.getOid())
                        .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                        .targetName(session.getName()));
            }

            mergedIntersection.add(detectedPattern);
            detectedPattern.setPatternType(BasePattern.PatternType.PATTERN);
            if (selectedPatternId != null) {
                detectedPattern.setPatternSelected(isPatternSelected(roleAnalysisDetectionPattern, selectedPatternId));
            }
        }

        return mergedIntersection;
    }

    public static @NotNull DetectedPattern transformDefaultPattern(
            @NotNull RoleAnalysisDetectionPatternType roleAnalysisDetectionPattern,
            @Nullable ObjectReferenceType clusterRef,
            @Nullable ObjectReferenceType sessionRef,
            @Nullable Long selectedPatternId) {

        List<ObjectReferenceType> rolesRef = roleAnalysisDetectionPattern.getRolesOccupancy();
        List<ObjectReferenceType> usersRef = roleAnalysisDetectionPattern.getUserOccupancy();

        Set<String> roles = rolesRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
        Set<String> users = usersRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

        Long id = roleAnalysisDetectionPattern.getId();
        DetectedPattern detectedPattern = prepareDetectedPattern(roles,
                users, id);

        detectedPattern.setRoleAttributeAnalysisResult(roleAnalysisDetectionPattern.getRoleAttributeAnalysisResult());
        detectedPattern.setUserAttributeAnalysisResult(roleAnalysisDetectionPattern.getUserAttributeAnalysisResult());
        Double itemConfidence = roleAnalysisDetectionPattern.getItemConfidence();
        if (itemConfidence != null) {
            detectedPattern.setItemsConfidence(itemConfidence);
        }
        Double reductionConfidence = roleAnalysisDetectionPattern.getReductionConfidence();
        if (reductionConfidence != null) {
            detectedPattern.setReductionFactorConfidence(reductionConfidence);
        }

        if (clusterRef != null) {
            detectedPattern.setClusterRef(clusterRef.clone());
        }

        if (sessionRef != null) {
            detectedPattern.setSessionRef(sessionRef.clone());
        }

        detectedPattern.setPatternType(BasePattern.PatternType.PATTERN);
        if (selectedPatternId != null) {
            detectedPattern.setPatternSelected(isPatternSelected(roleAnalysisDetectionPattern, selectedPatternId));
        }

        return detectedPattern;
    }

    private static boolean isPatternSelected(RoleAnalysisDetectionPatternType roleAnalysisDetectionPattern, Long patternId) {
        Long id = roleAnalysisDetectionPattern.getId();
        if (id.equals(patternId)) {
            return true;
        }

        return false;
    }

//    public static @NotNull DetectedPattern transformPattern(@NotNull RoleAnalysisDetectionPatternType pattern) {
//        List<ObjectReferenceType> rolesRef = pattern.getRolesOccupancy();
//        List<ObjectReferenceType> usersRef = pattern.getUserOccupancy();
//
//        Set<String> roles = rolesRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
//        Set<String> users = usersRef.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
//
//        return new DetectedPattern(roles, users, (users.size() * roles.size()) - users.size(), pattern.getId());
//    }

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
            return detectionPatternType == null || detectionPatternType.getReductionCount() == null;
        } else {
            return false;
        }
    }

}
