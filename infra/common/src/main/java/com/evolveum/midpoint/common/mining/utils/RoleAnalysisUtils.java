/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.util.roles.RoleManagementUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The `RoleAnalysisUtils` class provides utility methods for various operations related to role analysis.
 */
public class RoleAnalysisUtils {

    public static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisUtils.class);

    public static AbstractAnalysisSessionOptionType getSessionOptionType(RoleAnalysisSessionType roleAnalysisSession) {
        if (roleAnalysisSession == null || roleAnalysisSession.getAnalysisOption() == null) {
            return null;
        }
        RoleAnalysisOptionType analysisOption = roleAnalysisSession.getAnalysisOption();
        if (analysisOption.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return roleAnalysisSession.getRoleModeOptions();
        }
        return roleAnalysisSession.getUserModeOptions();
    }

    @NotNull
    public static PatternDetectionOption loadPatternDetectionOption(@NotNull RoleAnalysisDetectionOptionType detectionOptionType) {
        RangeType frequencyRange = detectionOptionType.getFrequencyRange();
        Double min = 0.0;
        Double max = 100.0;
        if(frequencyRange != null && frequencyRange.getMin() != null && frequencyRange.getMax() != null) {
            min = frequencyRange.getMin();
            max = frequencyRange.getMax();
        }
        return new PatternDetectionOption(
                min,
                max,
                detectionOptionType.getMinUserOccupancy(),
                detectionOptionType.getMinRolesOccupancy()
        );
    }

    public static @NotNull List<String> getRolesOidAssignment(@NotNull AssignmentHolderType object) {
        List<AssignmentType> assignments = object.getAssignment();

        return assignments.stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .filter(targetRef -> targetRef.getType().equals(RoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid)
                .sorted()
                .collect(Collectors.toList());
    }

    public static @NotNull List<String> getRolesOidInducement(@NotNull RoleType object) {
        List<AssignmentType> inducement = object.getInducement();

        return inducement.stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .filter(targetRef -> targetRef.getType().equals(RoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid)
                .sorted()
                .collect(Collectors.toList());
    }

    public static @NotNull List<String> getRoleMembershipRefAssignment(
            @NotNull AssignmentHolderType object,
            @NotNull QName complexType) {
        List<ObjectReferenceType> refs = object.getRoleMembershipRef();
        return refs.stream()
                .filter(ref -> ref.getType().equals(complexType))
                .map(AbstractReferencable::getOid)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<String> getRolesOidInducements(@NotNull PrismObject<RoleType> object) {
        return RoleManagementUtil.getInducedRolesOids(object.asObjectable()).stream()
                .sorted() // do we need this?
                .toList();
    }

    public static XMLGregorianCalendar getCurrentXMLGregorianCalendar() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        DatatypeFactory datatypeFactory;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
    }

    public static @NotNull String resolveDateAndTime(@NotNull XMLGregorianCalendar xmlGregorianCalendar) {

        int year = xmlGregorianCalendar.getYear();
        int month = xmlGregorianCalendar.getMonth();
        int day = xmlGregorianCalendar.getDay();
        int hours = xmlGregorianCalendar.getHour();
        int minutes = xmlGregorianCalendar.getMinute();

        String dateString = String.format("%04d:%02d:%02d", year, month, day);

        String amPm = (hours < 12) ? "AM" : "PM";
        hours = hours % 12;
        if (hours == 0) {
            hours = 12;
        }
        String timeString = String.format("%02d:%02d %s", hours, minutes, amPm);

        return dateString + ", " + timeString;
    }

    public static @NotNull String resolveDateAndTime(@NotNull RoleType role) {

        if (role.getMetadata() == null || role.getMetadata().getCreateTimestamp() == null) {
            return "";
        }

        XMLGregorianCalendar createTimestamp = role.getMetadata().getCreateTimestamp();
        int year = createTimestamp.getYear();
        int month = createTimestamp.getMonth();
        int day = createTimestamp.getDay();
        int hours = createTimestamp.getHour();
        int minutes = createTimestamp.getMinute();

        String dateString = String.format("%04d:%02d:%02d", year, month, day);

        String amPm = (hours < 12) ? "AM" : "PM";
        hours = hours % 12;
        if (hours == 0) {
            hours = 12;
        }
        String timeString = String.format("%02d:%02d %s", hours, minutes, amPm);

        return dateString + ", " + timeString;
    }

    public static @NotNull List<RoleAnalysisDetectionPatternType> loadIntersections(
            @NotNull List<DetectedPattern> possibleBusinessRole) {
        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();

        loadSimpleIntersection(possibleBusinessRole,
                roleAnalysisClusterDetectionTypeList);

        return roleAnalysisClusterDetectionTypeList;
    }

    private static void loadSimpleIntersection(@NotNull List<DetectedPattern> possibleBusinessRole,
            List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList) {
        RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType;
        for (DetectedPattern detectedPattern : possibleBusinessRole) {
            roleAnalysisClusterDetectionType = new RoleAnalysisDetectionPatternType();

            Set<String> users = detectedPattern.getUsers();
            Set<String> roles = detectedPattern.getRoles();

            mapPatternRefs(users, roleAnalysisClusterDetectionType, roles);

            roleAnalysisClusterDetectionType.setClusterMetric(detectedPattern.getMetric());
            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType);
        }
    }

    public static void mapPatternRefs(@NotNull Set<String> users, RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType, Set<String> roles) {
        for (String usersRef : users) {
            roleAnalysisClusterDetectionType.getUserOccupancy().add(
                    new ObjectReferenceType().oid(usersRef).type(UserType.COMPLEX_TYPE));

        }

        for (String rolesRef : roles) {
            roleAnalysisClusterDetectionType.getRolesOccupancy().add(
                    new ObjectReferenceType().oid(rolesRef).type(RoleType.COMPLEX_TYPE)
            );
        }
    }

    public static @NotNull AnalysisClusterStatisticType createClusterStatisticType(
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisProcessModeType processMode) {
        AnalysisClusterStatisticType abstractAnalysisClusterStatistic = new AnalysisClusterStatisticType();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            abstractAnalysisClusterStatistic.setRolesCount(clusterStatistic.getMembersCount());
            abstractAnalysisClusterStatistic.setUsersCount(clusterStatistic.getPropertiesCount());
        } else {
            abstractAnalysisClusterStatistic.setUsersCount(clusterStatistic.getMembersCount());
            abstractAnalysisClusterStatistic.setRolesCount(clusterStatistic.getPropertiesCount());
        }

        abstractAnalysisClusterStatistic.setMembershipMean(clusterStatistic.getPropertiesMean());
        abstractAnalysisClusterStatistic.setMembershipDensity(clusterStatistic.getPropertiesDensity());
        abstractAnalysisClusterStatistic.setMembershipRange(new RangeType()
                .min((double) clusterStatistic.getMinVectorPoint())
                .max((double) clusterStatistic.getMaxVectorPoint()));

        return abstractAnalysisClusterStatistic;
    }

    @Nullable
    public static PrismObject<RoleAnalysisClusterType> prepareClusterPrismObject() {
        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = PrismContext.get()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleAnalysisClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }
        return clusterTypePrismObject;
    }

}
