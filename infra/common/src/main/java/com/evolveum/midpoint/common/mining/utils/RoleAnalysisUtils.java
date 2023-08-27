/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.util.roles.RoleManagementUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.ClassPathUtil.LOGGER;

public class RoleAnalysisUtils {

    public static List<String> extractOid(List<PrismObject<UserType>> roleMembers) {
        List<String> membersOids = new ArrayList<>();
        for (PrismObject<UserType> roleMember : roleMembers) {
            membersOids.add(roleMember.getOid());
        }

        return membersOids;

    }

    public static AbstractAnalysisSessionOptionType getSessionOptionType(RoleAnalysisSessionType roleAnalysisSession) {
        if (roleAnalysisSession == null || roleAnalysisSession.getProcessMode() == null) {
            return null;
        }

        if (roleAnalysisSession.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return roleAnalysisSession.getRoleModeOptions();
        }
        return roleAnalysisSession.getUserModeOptions();
    }

    @NotNull
    public static DetectionOption loadDetectionOption(@NotNull RoleAnalysisDetectionOptionType detectionOptionType) {

        Double min = detectionOptionType.getFrequencyRange().getMin();
        Double max = detectionOptionType.getFrequencyRange().getMax();
        return new DetectionOption(
                min,
                max,
                detectionOptionType.getMinUserOccupancy(),
                detectionOptionType.getMinRolesOccupancy()
        );
    }

    public static List<String> getRolesOidAssignment(AssignmentHolderType object) {
        List<String> oidList;
        List<AssignmentType> assignments = object.getAssignment();
        oidList = assignments.stream().map(AssignmentType::getTargetRef).filter(
                        targetRef -> targetRef.getType().equals(RoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid).sorted()
                .collect(Collectors.toList());
        return oidList;
    }

    public static List<String> getRolesOidInducements(PrismObject<RoleType> object) {
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

    public static String resolveDateAndTime(XMLGregorianCalendar xmlGregorianCalendar) {

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

    public static List<RoleAnalysisDetectionPatternType> loadIntersections(List<DetectedPattern> possibleBusinessRole,
            QName processedObjectComplexType, QName propertiesComplexType) {
        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();

        loadSimpleIntersection(possibleBusinessRole,
                roleAnalysisClusterDetectionTypeList, processedObjectComplexType, propertiesComplexType);

        return roleAnalysisClusterDetectionTypeList;
    }

    private static void loadSimpleIntersection(List<DetectedPattern> possibleBusinessRole,
            List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList,
            QName processedObjectComplexType, QName propertiesComplexType) {
        RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType;
        for (DetectedPattern detectedPattern : possibleBusinessRole) {
            roleAnalysisClusterDetectionType = new RoleAnalysisDetectionPatternType();

            ObjectReferenceType objectReferenceType;
            Set<String> members = detectedPattern.getUsers();
            for (String memberRef : members) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(memberRef);
                objectReferenceType.setType(processedObjectComplexType);
                roleAnalysisClusterDetectionType.getUserOccupancy().add(objectReferenceType);

            }

            Set<String> properties = detectedPattern.getRoles();
            for (String propertiesRef : properties) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(propertiesRef);
                objectReferenceType.setType(propertiesComplexType);
                roleAnalysisClusterDetectionType.getRolesOccupancy().add(objectReferenceType);
            }

            roleAnalysisClusterDetectionType.setClusterMetric(detectedPattern.getClusterMetric());
            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType);
        }
    }

    public static AnalysisClusterStatisticType createClusterStatisticType(ClusterStatistic clusterStatistic,
            RoleAnalysisProcessModeType processMode) {
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
