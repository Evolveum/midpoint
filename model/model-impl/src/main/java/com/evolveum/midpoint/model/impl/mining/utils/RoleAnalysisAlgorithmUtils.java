/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.utils;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisObjectUtils.createObjectReferences;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisObjectUtils.getSessionTypeObjectCount;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.Cluster;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.DataPoint;
import com.evolveum.midpoint.model.impl.mining.algorithm.detection.DefaultPatternResolver;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisAlgorithmUtils {

    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processClusters(ModelService modelService, Task task, OperationResult result,
            List<DataPoint> dataPoints, List<Cluster<DataPoint>> clusters,
            @NotNull RoleAnalysisSessionType session, RoleAnalysisProgressIncrement handler) {

        Integer sessionTypeObjectCount = getSessionTypeObjectCount(modelService, result, task);

        QName complexType = session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        int size = clusters.size();
        handler.enterNewStep("Generate Cluster Statistics model");
        handler.setOperationCountToProcess(size);
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, size)
                .mapToObj(i -> {
                    handler.iterateActualStatus();

                    return prepareClusters(modelService, task, result, clusters.get(i).getPoints(), String.valueOf(i),
                            dataPoints, session, complexType, sessionTypeObjectCount);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

         if (!dataPoints.isEmpty()) {
             handler.enterNewStep("Prepare Outliers");
             handler.setOperationCountToProcess(dataPoints.size());
             PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareOutlierClusters(modelService, task, result,
                     dataPoints, complexType, session.getProcessMode(), handler, sessionTypeObjectCount);
             clusterTypeObjectWithStatistic.add(clusterTypePrismObject);

         }
        return clusterTypeObjectWithStatistic;
    }

    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processExactMatch(ModelService modelService, Task task, OperationResult result,
            List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session, RoleAnalysisProgressIncrement handler) {

        Integer sessionTypeObjectCount = getSessionTypeObjectCount(modelService, result, task);

        QName processedObjectComplexType = session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        QName propertiesComplexType = processedObjectComplexType.equals(RoleType.COMPLEX_TYPE)
                ? UserType.COMPLEX_TYPE
                : RoleType.COMPLEX_TYPE;

        List<DataPoint> dataPointsOutliers = new ArrayList<>();
        int size = dataPoints.size();

        handler.enterNewStep("Generate Cluster Statistics model");
        handler.setOperationCountToProcess(size);
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, size)
                .mapToObj(i -> {
                    handler.iterateActualStatus();

                    return exactPrepareDataPoints(modelService, task, result, dataPoints.get(i), String.valueOf(i),
                            session, dataPointsOutliers, processedObjectComplexType, propertiesComplexType, sessionTypeObjectCount);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!dataPoints.isEmpty()) {
            handler.enterNewStep("Prepare Outliers");
            handler.setOperationCountToProcess(dataPoints.size());
            PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareOutlierClusters(modelService, task, result, dataPoints,
                    processedObjectComplexType, session.getProcessMode(), handler, sessionTypeObjectCount);
            clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        }
        return clusterTypeObjectWithStatistic;
    }

    private ClusterStatistic statisticLoad(List<DataPoint> clusterDataPoints, List<DataPoint> allDataPoints,
            String clusterIndex, QName complexType, ModelService modelService, Task task, OperationResult result, Integer sessionTypeObjectCount) {

        PolyStringType name = PolyStringType.fromOrig(sessionTypeObjectCount + "_cluster_" + clusterIndex);

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

        Set<ObjectReferenceType> processedObjectsRef = createObjectReferences(membersOidsSet, complexType, modelService,
                task, result);

        return new ClusterStatistic(name, processedObjectsRef, totalMembersCount, existingPropertiesInCluster, minVectorPoint,
                maxVectorPoint, meanPoints, density);
    }

    private ClusterStatistic exactStatisticLoad(DataPoint clusterDataPoints,
            String clusterIndex, int threshold, List<DataPoint> dataPointsOutliers, QName processedObjectComplexType,
            QName propertiesComplexType, ModelService modelService, Task task, OperationResult result, Integer sessionTypeObjectCount) {

        Set<String> elementsOids = new HashSet<>(clusterDataPoints.getMembers());
        Set<String> occupiedPoints = new HashSet<>(clusterDataPoints.getProperties());

        if (elementsOids.size() < threshold) {
            dataPointsOutliers.add(clusterDataPoints);
            return null;
        }

        PolyStringType name = PolyStringType.fromOrig(sessionTypeObjectCount + "_cluster_" + clusterIndex);

        Set<ObjectReferenceType> membersObjectsRef = createObjectReferences(elementsOids, processedObjectComplexType,
                modelService, task, result);

        Set<ObjectReferenceType> propertiesObjectRef = createObjectReferences(occupiedPoints, propertiesComplexType,
                modelService, task, result);

        double density = 100;

        int membersCount = membersObjectsRef.size();
        int propertiesCount = propertiesObjectRef.size();

        if (propertiesCount == 0 || membersCount == 0) {
            return null;
        }

        return new ClusterStatistic(name, propertiesObjectRef, membersObjectsRef, membersCount, propertiesCount,
                propertiesCount, propertiesCount, propertiesCount, density);
    }

    private PrismObject<RoleAnalysisClusterType> exactPrepareDataPoints(ModelService modelService, Task task, OperationResult result,
            DataPoint dataPointCluster, String clusterIndex,
            @NotNull RoleAnalysisSessionType session, List<DataPoint> dataPointsOutliers,
            QName processedObjectComplexType, QName propertiesComplexType, Integer sessionTypeObjectCount) {

        AbstractAnalysisSessionOptionType sessionOptionType = getSessionOptionType(session);
        int minMembersCount = sessionOptionType.getMinMembersCount();
        ClusterStatistic clusterStatistic = exactStatisticLoad(dataPointCluster, clusterIndex, minMembersCount,
                dataPointsOutliers, processedObjectComplexType, propertiesComplexType, modelService, task, result, sessionTypeObjectCount);

        if (clusterStatistic != null) {
            AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                    session.getProcessMode());

            return generateClusterObject(modelService, task, result, clusterStatistic, session,
                    roleAnalysisClusterStatisticType, true);
        } else {return null;}
    }

    private PrismObject<RoleAnalysisClusterType> prepareClusters(ModelService modelService, Task task, OperationResult result,
            List<DataPoint> dataPointCluster, String clusterIndex, List<DataPoint> dataPoints,
            @NotNull RoleAnalysisSessionType session, QName complexType, Integer sessionTypeObjectCount) {

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
                complexType, modelService, task, result, sessionTypeObjectCount);

        assert clusterStatistic != null;
        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                session.getProcessMode());

        boolean detect = true;
        RoleAnalysisDetectionProcessType detectMode = session.getDefaultDetectionOption().getDetectionProcessMode();

        if (detectMode == null) {
            detectMode = RoleAnalysisDetectionProcessType.FULL;
        }

        if (detectMode.equals(RoleAnalysisDetectionProcessType.PARTIAL)) {
            if (clusterStatistic.getPropertiesCount() > 300 || clusterStatistic.getMembersCount() > 300) {
                detect = false;
            }
        } else if (detectMode.equals(RoleAnalysisDetectionProcessType.SKIP)) {
            detect = false;
        }
        return generateClusterObject(modelService, task, result, clusterStatistic, session, roleAnalysisClusterStatisticType, detect);

    }

    private PrismObject<RoleAnalysisClusterType> prepareOutlierClusters(ModelService modelService, Task task,
            OperationResult result, List<DataPoint> dataPoints, QName complexType,
            RoleAnalysisProcessModeType processMode, RoleAnalysisProgressIncrement handler, Integer sessionTypeObjectCount) {

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

        PolyStringType name = PolyStringType.fromOrig(sessionTypeObjectCount + "_outliers");

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

        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = createClusterStatisticType(clusterStatistic,
                processMode);

        return generateClusterObject(modelService, task, result, clusterStatistic, null,
                roleAnalysisClusterStatisticType, false);
    }

    private @NotNull PrismObject<RoleAnalysisClusterType> generateClusterObject(ModelService modelService, Task task,
            OperationResult result,
            ClusterStatistic clusterStatistic,
            RoleAnalysisSessionType session,
            AnalysisClusterStatisticType roleAnalysisClusterStatisticType,
            boolean detectPattern) {

        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareClusterPrismObject();
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
                    .loadPattern(session, clusterStatistic, clusterType, modelService, result, task);
            clusterType.getDetectedPattern().addAll(roleAnalysisClusterDetectionTypeList);

            for (RoleAnalysisDetectionPatternType detectionPatternType : roleAnalysisClusterDetectionTypeList) {
                maxReduction = Math.max(maxReduction, detectionPatternType.getClusterMetric());
            }
        }

        roleAnalysisClusterStatisticType.setDetectedReductionMetric(maxReduction);

        clusterType.setClusterStatistics(roleAnalysisClusterStatisticType);

        return clusterTypePrismObject;
    }

}
