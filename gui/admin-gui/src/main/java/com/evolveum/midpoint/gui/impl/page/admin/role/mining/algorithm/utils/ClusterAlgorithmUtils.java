/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.prepareDetectedPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.createObjectReferences;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getSessionOptionType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism.Cluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism.DataPoint;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterStatistic;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterAlgorithmUtils {

    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processClusters(PageBase pageBase, OperationResult result,
            List<DataPoint> dataPoints, List<Cluster<DataPoint>> clusters,
            @NotNull RoleAnalysisSessionType session, Handler handler) {

        QName complexType = session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        int size = clusters.size();
        handler.setSubTitle("Generate Cluster Statistics model");
        handler.setOperationCountToProcess(size);
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, size)
                .mapToObj(i -> {
                    handler.iterateActualStatus();

                    return prepareClusters(pageBase, result, clusters.get(i).getPoints(), String.valueOf(i),
                            dataPoints, session, complexType);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        handler.setSubTitle("Prepare Outliers");
        handler.setOperationCountToProcess(dataPoints.size());
        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareOutlierClusters(pageBase, result,
                dataPoints, complexType, session.getProcessMode(), handler);
        clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        return clusterTypeObjectWithStatistic;
    }

    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processExactMatch(PageBase pageBase, OperationResult result,
            List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session, Handler handler) {

        QName processedObjectComplexType = session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        QName propertiesComplexType = processedObjectComplexType.equals(RoleType.COMPLEX_TYPE)
                ? UserType.COMPLEX_TYPE
                : RoleType.COMPLEX_TYPE;

        List<DataPoint> dataPointsOutliers = new ArrayList<>();
        int size = dataPoints.size();

        handler.setSubTitle("Generate Cluster Statistics model");
        handler.setOperationCountToProcess(size);
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, size)
                .mapToObj(i -> {
                    handler.iterateActualStatus();

                    return exactPrepareDataPoints(pageBase, result, dataPoints.get(i), String.valueOf(i),
                            session, dataPointsOutliers, processedObjectComplexType, propertiesComplexType);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        handler.setSubTitle("Prepare Outliers");
        handler.setOperationCountToProcess(dataPoints.size());
        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareOutlierClusters(pageBase, result, dataPoints,
                processedObjectComplexType, session.getProcessMode(), handler);
        clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        return clusterTypeObjectWithStatistic;
    }

    public static List<DataPoint> prepareDataPoints(ListMultimap<List<String>, String> chunkMap) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (List<String> points : chunkMap.keySet()) {
            List<String> elements = chunkMap.get(points);

            dataPoints.add(new DataPoint(new HashSet<>(elements), new HashSet<>(points)));

        }
        return dataPoints;
    }

    private ClusterStatistic statisticLoad(List<DataPoint> clusterDataPoints, List<DataPoint> allDataPoints,
            String clusterIndex, QName complexType, PageBase pageBase, OperationResult result) {

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);
        RepositoryService repositoryService = pageBase.getRepositoryService();

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalAssignPropertiesRelation = 0;
        int totalMembersCount = 0;

        Set<String> membersOidsSet = new HashSet<>();
        Set<String> propertiesOidsSet = new HashSet<>();

        for (DataPoint clusterDataPoint : clusterDataPoints) {
            allDataPoints.remove(clusterDataPoint);
            Set<String> properties = clusterDataPoint.getProperties();
            Set<String> members = clusterDataPoint.getMembers();
            membersOidsSet.addAll(members);
            propertiesOidsSet.addAll(properties);

            int groupSize = members.size();
            totalMembersCount += groupSize;

            int occupyPointsCount = properties.size();
            totalAssignPropertiesRelation += (occupyPointsCount * groupSize);
            minVectorPoint = Math.min(minVectorPoint, occupyPointsCount);
            maxVectorPoint = Math.max(maxVectorPoint, occupyPointsCount);
        }

        int existingPropertiesInCluster = propertiesOidsSet.size();

        if (existingPropertiesInCluster == 0 || totalMembersCount == 0) {
            return null;
        }

        int allPossibleRelation = existingPropertiesInCluster * totalMembersCount;

        double meanPoints = (double) totalAssignPropertiesRelation / totalMembersCount;

        double density = Math.min((totalAssignPropertiesRelation / (double) allPossibleRelation) * 100, 100);

        Set<ObjectReferenceType> processedObjectsRef = createObjectReferences(membersOidsSet, complexType, repositoryService,
                result);

        return new ClusterStatistic(name, processedObjectsRef, totalMembersCount, existingPropertiesInCluster, minVectorPoint,
                maxVectorPoint, meanPoints, density);
    }

    private ClusterStatistic exactStatisticLoad(DataPoint clusterDataPoints,
            String clusterIndex, int threshold, List<DataPoint> dataPointsOutliers, QName processedObjectComplexType,
            QName propertiesComplexType, PageBase pageBase, OperationResult result) {

        Set<String> elementsOids = new HashSet<>(clusterDataPoints.getMembers());
        Set<String> occupiedPoints = new HashSet<>(clusterDataPoints.getProperties());

        if (elementsOids.size() < threshold) {
            dataPointsOutliers.add(clusterDataPoints);
            return null;
        }

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);

        RepositoryService repositoryService = pageBase.getRepositoryService();

        Set<ObjectReferenceType> membersObjectsRef = createObjectReferences(elementsOids, processedObjectComplexType,
                repositoryService, result);

        Set<ObjectReferenceType> propertiesObjectRef = createObjectReferences(occupiedPoints, propertiesComplexType,
                repositoryService, result);

        double density = 100;

        int membersCount = membersObjectsRef.size();
        int propertiesCount = propertiesObjectRef.size();

        if (propertiesCount == 0 || membersCount == 0) {
            return null;
        }

        return new ClusterStatistic(name, propertiesObjectRef, membersObjectsRef, membersCount, propertiesCount,
                propertiesCount, propertiesCount, propertiesCount, density);
    }

    private PrismObject<RoleAnalysisClusterType> exactPrepareDataPoints(PageBase pageBase, OperationResult result,
            DataPoint dataPointCluster, String clusterIndex,
            @NotNull RoleAnalysisSessionType session, List<DataPoint> dataPointsOutliers,
            QName processedObjectComplexType, QName propertiesComplexType) {

        AbstractAnalysisSessionOptionType sessionOptionType = getSessionOptionType(session);
        int minMembersCount = sessionOptionType.getMinMembersCount();
        ClusterStatistic clusterStatistic = exactStatisticLoad(dataPointCluster, clusterIndex, minMembersCount,
                dataPointsOutliers, processedObjectComplexType, propertiesComplexType, pageBase, result);

        if (clusterStatistic != null) {
            AnalysisClusterStatisticType roleAnalysisClusterStatisticType = ClusterObjectUtils.createClusterStatisticType(clusterStatistic, session.getProcessMode());

            return generateClusterObject(pageBase, result, clusterStatistic, session,
                    roleAnalysisClusterStatisticType, true);
        } else {return null;}
    }

    private PrismObject<RoleAnalysisClusterType> prepareClusters(PageBase pageBase, OperationResult result,
            List<DataPoint> dataPointCluster, String clusterIndex, List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session, QName complexType) {

        Set<String> elementsOids = new HashSet<>();
        for (DataPoint clusterDataPoint : dataPointCluster) {
            Set<String> elements = clusterDataPoint.getMembers();
            elementsOids.addAll(elements);
        }

        AbstractAnalysisSessionOptionType sessionOptionType = getSessionOptionType(session);

        if (elementsOids.size() < sessionOptionType.getMinMembersCount()) {
            return null;
        }

        ClusterStatistic clusterStatistic = statisticLoad(dataPointCluster, dataPoints, clusterIndex,
                complexType, pageBase, result);

        assert clusterStatistic != null;
        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = ClusterObjectUtils
                .createClusterStatisticType(clusterStatistic, session.getProcessMode());

        boolean detect = true;
        RoleAnalysisDetectionProcessType detectMode = session.getDefaultDetectionOption().getDetectionProcessMode();
        if (detectMode.equals(RoleAnalysisDetectionProcessType.PARTIAL)) {
            if (clusterStatistic.getPropertiesCount() > 300 || clusterStatistic.getMembersCount() > 300) {
                detect = false;
            }
        } else if (detectMode.equals(RoleAnalysisDetectionProcessType.SKIP)) {
            detect = false;
        }
        return generateClusterObject(pageBase, result, clusterStatistic, session, roleAnalysisClusterStatisticType, detect);

    }

    private PrismObject<RoleAnalysisClusterType> prepareOutlierClusters(PageBase pageBase, OperationResult result,
            List<DataPoint> dataPoints, QName complexType, RoleAnalysisProcessModeType processMode, Handler handler) {

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalDataPoints = dataPoints.size();
        int sumPoints = 0;

        Set<String> elementsOid = new HashSet<>();
        Set<String> pointsSet = new HashSet<>();
        for (DataPoint dataPoint : dataPoints) {
            handler.iterateActualStatus();

            Set<String> points = dataPoint.getProperties();
            pointsSet.addAll(points);
            elementsOid.addAll(dataPoint.getMembers());

            int pointsSize = points.size();
            sumPoints += pointsSize;
            minVectorPoint = Math.min(minVectorPoint, pointsSize);
            maxVectorPoint = Math.max(maxVectorPoint, pointsSize);
        }

        double meanPoints = (double) sumPoints / totalDataPoints;

        int pointsSize = pointsSet.size();
        int elementSize = elementsOid.size();
        double density = (sumPoints / (double) (elementSize * pointsSize)) * 100;

        PolyStringType name = PolyStringType.fromOrig("outliers");

        Set<ObjectReferenceType> processedObjectsRef = new HashSet<>();
        ObjectReferenceType objectReferenceType;
        for (String element : elementsOid) {
            objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(complexType);
            objectReferenceType.setOid(element);
            processedObjectsRef.add(objectReferenceType);
        }

        ClusterStatistic clusterStatistic = new ClusterStatistic(name, processedObjectsRef, elementSize,
                pointsSize, minVectorPoint, maxVectorPoint, meanPoints, density);

        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = ClusterObjectUtils
                .createClusterStatisticType(clusterStatistic, processMode);

        return generateClusterObject(pageBase, result, clusterStatistic, null,
                roleAnalysisClusterStatisticType, false);
    }

    private @NotNull PrismObject<RoleAnalysisClusterType> generateClusterObject(PageBase pageBase, OperationResult result,
            ClusterStatistic clusterStatistic,
            RoleAnalysisSessionType session,
            AnalysisClusterStatisticType roleAnalysisClusterStatisticType,
            boolean detectPattern) {

        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = ClusterObjectUtils.prepareClusterPrismObject(pageBase);
        assert clusterTypePrismObject != null;

        Set<ObjectReferenceType> members = clusterStatistic.getMembersRef();

        RoleAnalysisClusterType clusterType = clusterTypePrismObject.asObjectable();
        clusterType.setOid(String.valueOf(UUID.randomUUID()));

        clusterType.getMember().addAll(members);
        clusterType.setName(clusterStatistic.getName());
        double maxReduction = 0;
        if (session != null && detectPattern) {
            RoleAnalysisProcessModeType mode = session.getProcessMode();
            DefaultPatternResolver defaultPatternResolver = new DefaultPatternResolver(mode);

            List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList = defaultPatternResolver
                    .loadPattern(session, clusterStatistic, clusterType, pageBase, result);
            clusterType.getDetectedPattern().addAll(roleAnalysisClusterDetectionTypeList);

            for (RoleAnalysisDetectionPatternType detectionPatternType : roleAnalysisClusterDetectionTypeList) {
                maxReduction = Math.max(maxReduction, detectionPatternType.getClusterMetric());
            }
        }

        roleAnalysisClusterStatisticType.setDetectedReductionMetric(maxReduction);

        clusterType.setClusterStatistics(roleAnalysisClusterStatisticType);

        return clusterTypePrismObject;
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

}
