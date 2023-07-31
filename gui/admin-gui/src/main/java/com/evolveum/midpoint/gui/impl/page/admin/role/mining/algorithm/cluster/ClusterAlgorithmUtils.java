/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractIntersections.businessRoleDetection;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.addDetectedObjectIntersection;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ExtractPatternUtils.addDetectedObjectJaccard;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.startTimer;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractJaccard;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.UUIDToDoubleConverter;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningOperationChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

public class ClusterAlgorithmUtils {

    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processClusters(PageBase pageBase, List<DataPoint> dataPoints,
            List<Cluster<DataPoint>> clusters, ClusterOptions clusterOptions) {
        long start;

        QName complexType;
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            complexType = RoleType.COMPLEX_TYPE;
        } else {complexType = UserType.COMPLEX_TYPE;}

        start = startTimer("generate clusters mp objects");
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, clusters.size())
                .mapToObj(i -> prepareClusters(pageBase, clusters.get(i).getPoints(), String.valueOf(i),
                        dataPoints, clusterOptions, complexType))
                .collect(Collectors.toList());
        endTimer(start, "generate clusters mp objects");

        start = startTimer("generate outliers mp objects");
        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareOutlierClusters(pageBase, dataPoints, complexType);
        clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        endTimer(start, "generate outliers mp objects");
        return clusterTypeObjectWithStatistic;
    }

    public static List<DataPoint> prepareDataPoints(ListMultimap<List<String>, String> chunkMap) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (List<String> points : chunkMap.keySet()) {
            List<String> elements = chunkMap.get(points);
            double[] vectorPoints = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                String point = points.get(i);
                vectorPoints[i] = UUIDToDoubleConverter.convertUUid(point);
            }

            // TODO if wanna execute cluster on non grouped data
//            for(int i =0; i < elements.size();i++){
//
//                dataPoints.add(new DataPoint(vectorPoints, Collections.singletonList(elements.get(i)), points));
//
//            }
            dataPoints.add(new DataPoint(vectorPoints, elements, points));

        }
        return dataPoints;
    }

    public ClusterStatistic statisticLoad(List<DataPoint> clusterDataPoints, List<DataPoint> allDataPoints,
            String clusterIndex, QName complexType) {

        int totalPoints = 0;
        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;
        int clusterGroupSize = clusterDataPoints.size();

        Set<String> elementsOids = new HashSet<>();
        Set<String> occupiedPoints = new HashSet<>();

        for (DataPoint clusterDataPoint : clusterDataPoints) {
            allDataPoints.remove(clusterDataPoint);
            List<String> points = clusterDataPoint.getPoints();
            List<String> elements = clusterDataPoint.getElements();
            elementsOids.addAll(elements);

            int pointsCount = points.size();
            totalPoints += pointsCount;
            minVectorPoint = Math.min(minVectorPoint, pointsCount);
            maxVectorPoint = Math.max(maxVectorPoint, pointsCount);

            occupiedPoints.addAll(points);
        }

        int pointsCount = occupiedPoints.size();

        double meanPoints = (double) totalPoints / clusterGroupSize;

        double density = Math.min((totalPoints / (double) (clusterDataPoints.size() * pointsCount)) * 100, 100);

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);

        Set<ObjectReferenceType> processedObjectsRef = new HashSet<>();
        ObjectReferenceType objectReferenceType;
        for (String element : elementsOids) {
            objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(complexType);
            objectReferenceType.setOid(element);
            processedObjectsRef.add(objectReferenceType);
        }

        return new ClusterStatistic(name, processedObjectsRef, elementsOids.size(), pointsCount, minVectorPoint,
                maxVectorPoint, clusterGroupSize, meanPoints, density);
    }

    @NotNull
    public List<PrismObject<RoleAnalysisClusterType>> processIdenticalGroup(PageBase pageBase, List<DataPoint> dataPoints,
            ClusterOptions clusterOptions) {
        long start;

        QName processedObjectComplexType;
        QName propertiesComplexType;
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            processedObjectComplexType = RoleType.COMPLEX_TYPE;
            propertiesComplexType = UserType.COMPLEX_TYPE;
        } else {
            processedObjectComplexType = UserType.COMPLEX_TYPE;
            propertiesComplexType = RoleType.COMPLEX_TYPE;
        }

        start = startTimer("generate clusters mp objects");
        List<DataPoint> dataPointsOutliers = new ArrayList<>();
        List<PrismObject<RoleAnalysisClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, dataPoints.size())
                .mapToObj(i -> prepareIdenticalGroup(pageBase, dataPoints.get(i), String.valueOf(i),
                        clusterOptions, dataPointsOutliers, processedObjectComplexType, propertiesComplexType))
                .filter(Objects::nonNull) // Filter out null elements
                .collect(Collectors.toList());

        endTimer(start, "generate clusters mp objects");

        start = startTimer("generate outliers mp objects");
        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = prepareOutlierClusters(pageBase, dataPoints, processedObjectComplexType);
        clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        endTimer(start, "generate outliers mp objects");
        return clusterTypeObjectWithStatistic;
    }

    public ClusterStatistic statisticIdenticalLoad(DataPoint clusterDataPoints,
            String clusterIndex, int minGroupSize, List<DataPoint> dataPointsOutliers, QName processedObjectComplexType, QName propertiesComplexType) {

        int totalPoints = 0;
        int minVectorPoint;
        int maxVectorPoint;
        int clusterGroupSize = clusterDataPoints.getElements().size();

        Set<String> elementsOids = new HashSet<>(clusterDataPoints.getElements());
        Set<String> occupiedPoints = new HashSet<>(clusterDataPoints.getPoints());

        if (elementsOids.size() < minGroupSize) {
            dataPointsOutliers.add(clusterDataPoints);
            return null;
        }

        int elementsCount = elementsOids.size();
        int pointsCount = occupiedPoints.size();
        totalPoints += pointsCount * elementsCount;
        minVectorPoint = pointsCount;
        maxVectorPoint = pointsCount;

        double density = Math.min((totalPoints / (double) (elementsCount * pointsCount)) * 100, 100);

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);

        Set<ObjectReferenceType> processedObjectsRef = new HashSet<>();
        ObjectReferenceType objectReferenceType;
        for (String element : elementsOids) {
            objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(processedObjectComplexType);
            objectReferenceType.setOid(element);
            processedObjectsRef.add(objectReferenceType);
        }

        Set<ObjectReferenceType> propertiesObjectRef = new HashSet<>();
        for (String element : occupiedPoints) {
            objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(propertiesComplexType);
            objectReferenceType.setOid(element);
            propertiesObjectRef.add(objectReferenceType);
        }

        return new ClusterStatistic(name, propertiesObjectRef, processedObjectsRef, elementsCount, pointsCount, minVectorPoint,
                maxVectorPoint, clusterGroupSize, pointsCount, density);
    }

    public PrismObject<RoleAnalysisClusterType> prepareIdenticalGroup(PageBase pageBase, DataPoint dataPointCluster,
            String clusterIndex, ClusterOptions clusterOptions, List<DataPoint> dataPointsOutliers, QName processedObjectComplexType, QName propertiesComplexType) {

        int minGroupSize = clusterOptions.getMinGroupSize();
        ClusterStatistic clusterStatistic = statisticIdenticalLoad(dataPointCluster, clusterIndex, minGroupSize,
                dataPointsOutliers, processedObjectComplexType, propertiesComplexType);

        if (clusterStatistic != null) {
            RoleAnalysisClusterStatisticType roleAnalysisClusterStatisticType = new RoleAnalysisClusterStatisticType();
            roleAnalysisClusterStatisticType.setMemberCount(clusterStatistic.getTotalElements());
            roleAnalysisClusterStatisticType.setPropertiesCount(clusterStatistic.getTotalPoints());
            roleAnalysisClusterStatisticType.setPropertiesMinOccupation(clusterStatistic.getMinVectorPoint());
            roleAnalysisClusterStatisticType.setPropertiesMaxOccupation(clusterStatistic.getMaxVectorPoint());
            roleAnalysisClusterStatisticType.setPropertiesMean(clusterStatistic.getMeanPoints());
            roleAnalysisClusterStatisticType.setPropertiesDensity(clusterStatistic.getDensity());

            return generateClusterObject(pageBase, clusterStatistic, clusterOptions, roleAnalysisClusterStatisticType);
        } else {return null;}
    }

    public PrismObject<RoleAnalysisClusterType> prepareClusters(PageBase pageBase, List<DataPoint> dataPointCluster, String clusterIndex,
            List<DataPoint> dataPoints, ClusterOptions clusterOptions, QName complexType) {

        ClusterStatistic clusterStatistic = statisticLoad(dataPointCluster, dataPoints, clusterIndex, complexType);

        RoleAnalysisClusterStatisticType roleAnalysisClusterStatisticType = new RoleAnalysisClusterStatisticType();
        roleAnalysisClusterStatisticType.setMemberCount(clusterStatistic.getTotalElements());
        roleAnalysisClusterStatisticType.setPropertiesCount(clusterStatistic.getTotalPoints());
        roleAnalysisClusterStatisticType.setPropertiesMinOccupation(clusterStatistic.getMinVectorPoint());
        roleAnalysisClusterStatisticType.setPropertiesMaxOccupation(clusterStatistic.getMaxVectorPoint());
        roleAnalysisClusterStatisticType.setPropertiesMean(clusterStatistic.getMeanPoints());
        roleAnalysisClusterStatisticType.setPropertiesDensity(clusterStatistic.getDensity());

        return generateClusterObject(pageBase, clusterStatistic, clusterOptions, roleAnalysisClusterStatisticType);
    }

    public PrismObject<RoleAnalysisClusterType> prepareOutlierClusters(PageBase pageBase, List<DataPoint> dataPoints, QName complexType) {

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalDataPoints = dataPoints.size();
        int sumPoints = 0;

        Set<String> elementsOid = new HashSet<>();
        Set<String> pointsSet = new HashSet<>();
        for (DataPoint dataPoint : dataPoints) {
            List<String> points = dataPoint.getPoints();
            pointsSet.addAll(points);
            elementsOid.addAll(dataPoint.getElements());

            int pointsSize = points.size();
            sumPoints += pointsSize;
            minVectorPoint = Math.min(minVectorPoint, pointsSize);
            maxVectorPoint = Math.max(maxVectorPoint, pointsSize);
        }

        double meanPoints = (double) sumPoints / totalDataPoints;

        int pointsSize = pointsSet.size();
        double density = (sumPoints / (double) (dataPoints.size() * pointsSize)) * 100;

        PolyStringType name = PolyStringType.fromOrig("outliers");

        Set<ObjectReferenceType> processedObjectsRef = new HashSet<>();
        ObjectReferenceType objectReferenceType;
        for (String element : elementsOid) {
            objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(complexType);
            objectReferenceType.setOid(element);
            processedObjectsRef.add(objectReferenceType);
        }

        ClusterStatistic clusterStatistic = new ClusterStatistic(name, processedObjectsRef, elementsOid.size(),
                pointsSize, minVectorPoint, maxVectorPoint, dataPoints.size(), meanPoints, density);

        RoleAnalysisClusterStatisticType roleAnalysisClusterStatisticType = new RoleAnalysisClusterStatisticType();
        roleAnalysisClusterStatisticType.setMemberCount(elementsOid.size());
        roleAnalysisClusterStatisticType.setPropertiesCount(pointsSize);
        roleAnalysisClusterStatisticType.setPropertiesMinOccupation(minVectorPoint);
        roleAnalysisClusterStatisticType.setPropertiesMaxOccupation(maxVectorPoint);
        roleAnalysisClusterStatisticType.setPropertiesMean(meanPoints);
        roleAnalysisClusterStatisticType.setPropertiesDensity(density);

        return generateClusterObject(pageBase, clusterStatistic, null, roleAnalysisClusterStatisticType);
    }

    private @NotNull PrismObject<RoleAnalysisClusterType> generateClusterObject(PageBase pageBase, ClusterStatistic clusterStatistic,
            ClusterOptions clusterOptions, RoleAnalysisClusterStatisticType roleAnalysisClusterStatisticType) {

        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleAnalysisClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }
        assert clusterTypePrismObject != null;

        Set<ObjectReferenceType> occupiedUsers = clusterStatistic.getProcessedObjectRef();

        RoleAnalysisClusterType clusterType = clusterTypePrismObject.asObjectable();
        clusterType.setOid(String.valueOf(UUID.randomUUID()));
        clusterType.setClusterStatistic(roleAnalysisClusterStatisticType);
        clusterType.getMember().addAll(occupiedUsers);
        clusterType.setName(clusterStatistic.getName());

        //TODO any change everything, currently only due to testing.
        if (clusterOptions != null && clusterOptions.getSimilarity() == 1) {
            QName processedObjectComplexType;
            QName propertiesComplexType;
            if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.USER)) {
                processedObjectComplexType = UserType.COMPLEX_TYPE;
                propertiesComplexType = RoleType.COMPLEX_TYPE;
            } else {
                processedObjectComplexType = RoleType.COMPLEX_TYPE;
                propertiesComplexType = UserType.COMPLEX_TYPE;
            }

            RoleAnalysisClusterDetectionType roleAnalysisClusterDetectionType;
            roleAnalysisClusterDetectionType = new RoleAnalysisClusterDetectionType();
            roleAnalysisClusterDetectionType.setSearchMode(clusterOptions.getSearchMode());

            ObjectReferenceType objectReferenceType;
            Set<ObjectReferenceType> properties = clusterStatistic.getPropertiesRef();
            for (ObjectReferenceType propertiesRef : properties) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(propertiesRef.getOid());
                objectReferenceType.setType(propertiesComplexType);
                roleAnalysisClusterDetectionType.getProperties().add(objectReferenceType);

            }

            Set<ObjectReferenceType> members = clusterStatistic.getProcessedObjectRef();
            for (ObjectReferenceType processedObjectOid : members) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(processedObjectOid.getOid());
                objectReferenceType.setType(processedObjectComplexType);
                roleAnalysisClusterDetectionType.getMember().add(objectReferenceType);
            }

            int propertiesCount = properties.size();
            int membersCount = members.size();
            roleAnalysisClusterDetectionType.setClusterRelatedPropertiesOccupation(propertiesCount);
            roleAnalysisClusterDetectionType.setClusterMetric((double) propertiesCount * membersCount);
            clusterType.getDetection().add(roleAnalysisClusterDetectionType);

        } else {
            List<DetectedPattern> possibleBusinessRole = new ArrayList<>();
            OperationResult operationResult = new OperationResult("Prepare data for intersection");
            resolveDefaultIntersection(pageBase, clusterType, possibleBusinessRole,
                    operationResult, clusterOptions);
        }

        return clusterTypePrismObject;
    }

    private static void resolveDefaultIntersection(PageBase pageBase, RoleAnalysisClusterType clusterType,
            List<DetectedPattern> possibleBusinessRole,
            OperationResult operationResult, ClusterOptions clusterOptions) {

        if (clusterOptions != null) {
            RoleAnalysisProcessModeType mode = clusterOptions.getMode();
            int group = Math.min(clusterOptions.getDefaultOccupancySearch(), clusterOptions.getMinGroupSize());
            int intersection = Math.min(clusterOptions.getDefaultIntersectionSearch(), clusterOptions.getMinIntersections());
            double defaultMaxFrequency = clusterOptions.getDefaultMaxFrequency();
            double defaultMinFrequency = clusterOptions.getDefaultMinFrequency();
            RoleAnalysisSearchModeType searchMode = clusterOptions.getSearchMode();

            if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
                MiningOperationChunk miningOperationChunk = new MiningOperationChunk(clusterType, pageBase,
                        RoleAnalysisProcessModeType.ROLE, operationResult, true, false);
                List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                        ClusterObjectUtils.SORT.NONE);
                List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                        ClusterObjectUtils.SORT.NONE);

                if (searchMode.equals(RoleAnalysisSearchModeType.JACCARD)) {
                    possibleBusinessRole = ExtractJaccard.businessRoleDetection(miningRoleTypeChunks, miningUserTypeChunks,
                            defaultMinFrequency,
                            defaultMaxFrequency,
                            group, intersection, mode, clusterOptions.getDefaultJaccardThreshold());
                } else {
                    possibleBusinessRole = businessRoleDetection(miningRoleTypeChunks, miningUserTypeChunks, defaultMinFrequency,
                            defaultMaxFrequency,
                            group, intersection, mode);
                }

            } else if (mode.equals(RoleAnalysisProcessModeType.USER)) {
                MiningOperationChunk miningOperationChunk = new MiningOperationChunk(clusterType, pageBase,
                        RoleAnalysisProcessModeType.USER, operationResult, true, false);
                List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                        ClusterObjectUtils.SORT.NONE);
                List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                        ClusterObjectUtils.SORT.NONE);

                if (searchMode.equals(RoleAnalysisSearchModeType.JACCARD)) {
                    possibleBusinessRole = ExtractJaccard.businessRoleDetection(miningRoleTypeChunks, miningUserTypeChunks,
                            defaultMinFrequency,
                            defaultMaxFrequency,
                            intersection, group, mode, clusterOptions.getDefaultJaccardThreshold());
                } else {
                    possibleBusinessRole = businessRoleDetection(miningRoleTypeChunks, miningUserTypeChunks, defaultMinFrequency,
                            defaultMaxFrequency,
                            intersection, group, mode);
                }

            }

            QName processedObjectComplexType;
            QName propertiesObjectComplexType;
            if (mode.equals(RoleAnalysisProcessModeType.USER)) {
                processedObjectComplexType = UserType.COMPLEX_TYPE;
                propertiesObjectComplexType = RoleType.COMPLEX_TYPE;
            } else {
                processedObjectComplexType = RoleType.COMPLEX_TYPE;
                propertiesObjectComplexType = UserType.COMPLEX_TYPE;
            }

            List<RoleAnalysisClusterDetectionType> roleAnalysisClusterDetectionTypeList = loadIntersections(possibleBusinessRole,
                    processedObjectComplexType, propertiesObjectComplexType, searchMode);

            clusterType.getDetection().addAll(roleAnalysisClusterDetectionTypeList);
        }
    }

    private static List<RoleAnalysisClusterDetectionType> loadIntersections(List<DetectedPattern> possibleBusinessRole,
            QName processedObjectComplexType, QName propertiesObjectComplexType, RoleAnalysisSearchModeType searchMode) {
        List<RoleAnalysisClusterDetectionType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();
        RoleAnalysisClusterDetectionType roleAnalysisClusterDetectionType;
        for (DetectedPattern detectedPattern : possibleBusinessRole) {
            roleAnalysisClusterDetectionType = new RoleAnalysisClusterDetectionType();
            roleAnalysisClusterDetectionType.setSearchMode(searchMode);

            ObjectReferenceType objectReferenceType;
            Set<String> members = detectedPattern.getMembers();
            for (String propertiesRef : members) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(propertiesRef);
                objectReferenceType.setType(processedObjectComplexType);
                roleAnalysisClusterDetectionType.getMember().add(objectReferenceType);

            }

            if (searchMode.equals(RoleAnalysisSearchModeType.JACCARD)) {
                Set<String> properties = detectedPattern.getProperties();
                for (String propertiesRef : properties) {
                    objectReferenceType = new ObjectReferenceType();
                    objectReferenceType.setOid(propertiesRef);
                    objectReferenceType.setType(propertiesObjectComplexType);
                    roleAnalysisClusterDetectionType.getProperties().add(objectReferenceType);

                }
            }

            int propertiesCount = detectedPattern.getClusterRelatedPropertiesOccupation();
            roleAnalysisClusterDetectionType.setClusterRelatedPropertiesOccupation(propertiesCount);
            roleAnalysisClusterDetectionType.setClusterMetric(detectedPattern.getClusterMetric());
            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType);
        }
        return roleAnalysisClusterDetectionTypeList;
    }

    public static List<DetectedPattern> loadDefaultIntersection(RoleAnalysisClusterType clusterType) {
        List<RoleAnalysisClusterDetectionType> defaultDetection = clusterType.getDetection();
        List<DetectedPattern> mergedIntersection = new ArrayList<>();
        for (RoleAnalysisClusterDetectionType roleAnalysisClusterDetectionType : defaultDetection) {
            RoleAnalysisSearchModeType searchMode = roleAnalysisClusterDetectionType.getSearchMode();

            List<ObjectReferenceType> propertiesRef = roleAnalysisClusterDetectionType.getProperties();
            Integer clusterRelatedPropertiesOccupation = roleAnalysisClusterDetectionType.getClusterRelatedPropertiesOccupation();
            List<ObjectReferenceType> membersObject = roleAnalysisClusterDetectionType.getMember();
            Set<String> members = new HashSet<>();
            for (ObjectReferenceType objectReferenceType : membersObject) {
                members.add(objectReferenceType.getOid());
            }
            if (searchMode.equals(RoleAnalysisSearchModeType.JACCARD)) {
                Set<String> properties = new HashSet<>();
                for (ObjectReferenceType objectReferenceType : propertiesRef) {
                    properties.add(objectReferenceType.getOid());
                }

                mergedIntersection.add(addDetectedObjectJaccard(properties, members, null));
            } else {

                mergedIntersection.add(addDetectedObjectIntersection(clusterRelatedPropertiesOccupation,
                        members, null));
            }
        }

        return mergedIntersection;
    }
}
