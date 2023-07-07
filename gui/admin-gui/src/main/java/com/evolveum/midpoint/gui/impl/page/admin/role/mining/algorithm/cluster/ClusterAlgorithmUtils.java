/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.startTimer;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.JaccardSorter;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.UUIDToDoubleConverter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterAlgorithmUtils {

    @NotNull
    public List<PrismObject<ClusterType>> processClusters(PageBase pageBase, String identifier, List<DataPoint> dataPoints,
            List<Cluster<DataPoint>> clusters) {
        long start;
        start = startTimer("generate clusters mp objects");
        List<PrismObject<ClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, clusters.size())
                .mapToObj(i -> prepareClusters(pageBase, clusters.get(i).getPoints(), String.valueOf(i), identifier, dataPoints))
                .collect(Collectors.toList());
        endTimer(start, "generate clusters mp objects");

        start = startTimer("generate outliers mp objects");
        PrismObject<ClusterType> clusterTypePrismObject = prepareOutlierClusters(pageBase, dataPoints, identifier);
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
            dataPoints.add(new DataPoint(vectorPoints, elements.get(0), elements, points));
        }
        return dataPoints;
    }

    public ClusterStatistic statisticLoad(List<DataPoint> sortedClusterDataPoints, List<DataPoint> allDataPoints,
            String clusterIndex, String identifier) {

        int totalPoints = 0;
        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;
        Set<String> occupiedUsers = new HashSet<>();
        int clusterSize = sortedClusterDataPoints.size();
        HashMap<String, Double> frequencyMap = new HashMap<>();

        for (DataPoint clusterDataPoint : sortedClusterDataPoints) {
            allDataPoints.remove(clusterDataPoint);
            List<String> points = clusterDataPoint.getPoints();
            List<String> elements = clusterDataPoint.getElements();
            occupiedUsers.addAll(elements);

            int pointsCount = points.size();
            totalPoints += pointsCount;
            minVectorPoint = Math.min(minVectorPoint, pointsCount);
            maxVectorPoint = Math.max(maxVectorPoint, pointsCount);

            for (String point : points) {
                frequencyMap.compute(point, (key, counter) -> counter == null ? 1 : counter + 1);
            }

        }

        double meanPoints = (double) totalPoints / clusterSize;
        double density = (totalPoints / (double) (sortedClusterDataPoints.size() * frequencyMap.size())) * 100;

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);

        int finalTotalPoints = totalPoints;
        frequencyMap.replaceAll((key, value) -> value / finalTotalPoints);

        List<String> occupiedRoles = frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        return new ClusterStatistic(name, identifier, occupiedRoles, occupiedUsers, totalPoints, minVectorPoint,
                maxVectorPoint, clusterSize, meanPoints, density, frequencyMap, null);
    }

    public PrismObject<ClusterType> prepareClusters(PageBase pageBase, List<DataPoint> dataPointCluster, String clusterIndex,
            String identifier, List<DataPoint> dataPoints) {
        List<DataPoint> sortedDataPoints = JaccardSorter.sort(dataPointCluster);

        ClusterStatistic clusterStatistic = statisticLoad(sortedDataPoints, dataPoints, clusterIndex, identifier);
        return generateClusterObject(pageBase, clusterStatistic);
    }

    public PrismObject<ClusterType> prepareOutlierClusters(PageBase pageBase, List<DataPoint> dataPoints,
            String identifier) {

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalDataPoints = dataPoints.size();
        int sumPoints = 0;

        Set<String> elementsOid = new HashSet<>();
        Set<String> pointsSet = new HashSet<>();
        for (DataPoint dataPoint : dataPoints) {
            List<String> points = dataPoint.getPoints();
            pointsSet.addAll(points);
            List<String> elements = dataPoint.getElements();
            elementsOid.addAll(elements);

            int pointsSize = points.size();
            sumPoints += pointsSize;
            minVectorPoint = Math.min(minVectorPoint, pointsSize);
            maxVectorPoint = Math.max(maxVectorPoint, pointsSize);
        }

        double meanPoints = (double) sumPoints / totalDataPoints;

        double density = (sumPoints / (double) (dataPoints.size() * pointsSet.size())) * 100;

        PolyStringType name = PolyStringType.fromOrig("outliers");

        ClusterStatistic clusterStatistic = new ClusterStatistic(name, identifier, new ArrayList<>(pointsSet), elementsOid,
                sumPoints, minVectorPoint, maxVectorPoint, dataPoints.size(), meanPoints, density, new HashMap<>(), null);

        return generateClusterObject(pageBase, clusterStatistic);
    }

    private @NotNull PrismObject<ClusterType> generateClusterObject(PageBase pageBase, ClusterStatistic clusterStatistic) {
        PrismObject<ClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }
        assert clusterTypePrismObject != null;

        Set<String> occupiedUsers = clusterStatistic.getOccupiedUsers();
        List<String> occupiedRoles = clusterStatistic.getOccupiedRoles();

        ClusterType clusterType = clusterTypePrismObject.asObjectable();
        clusterType.setOid(String.valueOf(UUID.randomUUID()));
        clusterType.setElementCount(occupiedUsers.size());
        clusterType.getElements().addAll(occupiedUsers);
        clusterType.getPoints().addAll(occupiedRoles);
        clusterType.setPointCount(occupiedRoles.size());
        clusterType.setMean(String.format("%.3f", clusterStatistic.getMeanPoints()));
        clusterType.setDensity(String.format("%.3f", clusterStatistic.getDensity()));
        clusterType.setMinOccupation(clusterStatistic.getMinVectorPoint());
        clusterType.setMaxOccupation(clusterStatistic.getMaxVectorPoint());
        clusterType.setName(clusterStatistic.getName());
        clusterType.setIdentifier(clusterStatistic.getIdentifier());

        return clusterTypePrismObject;
    }

}
