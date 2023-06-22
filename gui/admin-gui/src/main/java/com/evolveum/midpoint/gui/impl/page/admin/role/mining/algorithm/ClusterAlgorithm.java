/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortMiningSet;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.filterMiningTypeObjects;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.getMiningObject;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterAlgorithm {

    public ClusterAlgorithm(PageBase pageBase) {
        ClusterAlgorithm.pageBase = pageBase;
    }

    static PageBase pageBase;

    public List<PrismObject<ClusterType>> executeClustering(double eps, int minGroupSize, int minIntersections, String identifier) {
        List<DataPoint> dataPointList = new ArrayList<>();
        List<PrismObject<MiningType>> prismObjects = filterMiningTypeObjects(pageBase);

        for (PrismObject<MiningType> prismObject : prismObjects) {
            List<String> roles = prismObject.asObjectable().getRoles();
            double[] rolesArray = new double[roles.size()];

            for (int i = 0; i < roles.size(); i++) {
                String role = roles.get(i);
                rolesArray[i] = UUIDToDoubleConverter.convertUUid(role);
            }
            dataPointList.add(new DataPoint(rolesArray, prismObject.getOid()));
        }

        long startTime = System.currentTimeMillis();

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);

        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPointList);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        System.out.println("Elapsed time: " + elapsedSeconds + " seconds (prepare clusters)");

        Cluster<DataPoint> outliers = getOutliers(dataPointList, clusters);
        clusters.add(outliers);

        startTime = System.currentTimeMillis();
        List<PrismObject<ClusterType>> clusterTypeObjectWithStatistic = getClusterTypeObjectWithStatistic(clusters, identifier);
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        elapsedSeconds = elapsedTime / 1000.0;
        System.out.println("Elapsed time: " + elapsedSeconds + " seconds (prepare clusters objects)");

        return clusterTypeObjectWithStatistic;
    }

    @NotNull
    private static Cluster<DataPoint> getOutliers(@NotNull List<DataPoint> dataPointList, List<Cluster<DataPoint>> clusters) {
        Cluster<DataPoint> outliers = new Cluster<>();
        for (DataPoint dataPoint : dataPointList) {
            boolean isInCluster = false;
            for (Cluster<DataPoint> cluster : clusters) {
                if (cluster.getPoints().contains(dataPoint)) {
                    isInCluster = true;
                    break;
                }
            }
            if (!isInCluster) {
                dataPoint.setNoisePoint(true);
                outliers.addPoint(dataPoint);
            }
        }
        return outliers;
    }

    public List<PrismObject<ClusterType>> getClusterTypeObjectWithStatistic(@NotNull List<Cluster<DataPoint>> clusters, String identifier) {
        List<PrismObject<ClusterType>> clusterTypeList = new ArrayList<>();

        int clustersCount = clusters.size();

        for (int i = 0; i < clusters.size(); i++) {
            Cluster<DataPoint> cluster = clusters.get(i);
            List<DataPoint> dataPointCluster = cluster.getPoints();
            List<String> membersId = new ArrayList<>();

            int minRoleCount = Integer.MAX_VALUE;
            int maxRoleCount = Integer.MIN_VALUE;

            int totalDataPoints = dataPointCluster.size();
            int sumRoles = 0;

            int count = 0;
            double density = 0;

            for (int j = 0; j < dataPointCluster.size(); j++) {
                DataPoint dataPoint = dataPointCluster.get(j);
                membersId.add(dataPoint.getLabel());

                int roleCount = dataPoint.getPoint().length;
                sumRoles += roleCount;
                if (roleCount < minRoleCount) {
                    minRoleCount = roleCount;
                }
                if (roleCount > maxRoleCount) {
                    maxRoleCount = roleCount;
                }

                double[] dataPointA = dataPoint.getPoint();

                HashSet<Double> setA = new HashSet<>();
                for (double num : dataPointA) {
                    setA.add(num);
                }

                for (int k = j + 1; k < dataPointCluster.size(); k++) {
                    double[] dataPointB = dataPointCluster.get(k).getPoint();

                    HashSet<Double> intersection = new HashSet<>();
                    for (double num : dataPointB) {
                        if (setA.contains(num)) {
                            intersection.add(num);
                        }
                    }

                    density += intersection.size();
                    count++;
                }
            }

            density = density / count;
            double meanRoles = (double) sumRoles / totalDataPoints;
            boolean isLastCluster = (i == clustersCount - 1);
            String clusterId = "cluster_" + (i + 1);

            clusterTypeList.add(generateClusterObject(membersId, meanRoles, minRoleCount, maxRoleCount,
                    isLastCluster, clusterId, density,identifier));
        }

        return clusterTypeList;
    }


    public PrismObject<ClusterType> generateClusterObject(List<String> groups, double meanRoles, int minRoleCount,
            int maxRoleCount, boolean isLastCluster, String clusterId, double density, String identifier) {
        PrismObject<ClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while generating ClusterType object: {}", e.getMessage(), e);
        }
        assert clusterTypePrismObject != null;

        UUID uuid = UUID.randomUUID();
        ClusterType clusterType = clusterTypePrismObject.asObjectable();
        clusterType.setOid(String.valueOf(uuid));
        clusterType.setSimilarGroupsCount(groups.size());

        OperationResult result = new OperationResult("Get ClusterType object");

        Set<String> roles = new HashSet<>();
        Set<String> members = new HashSet<>();

        if (!isLastCluster) {
            List<PrismObject<MiningType>> minigList = new ArrayList<>();

            for (String group : groups) {
                PrismObject<MiningType> miningObject = getMiningObject(pageBase, group, result);
                minigList.add(miningObject);
                MiningType miningType = miningObject.asObjectable();
                roles.addAll(miningType.getRoles());
                members.addAll(miningType.getMembers());
            }

            List<PrismObject<MiningType>> jaccSortMiningSet = jaccSortMiningSet(minigList);

            for (PrismObject<MiningType> miningTypePrismObject : jaccSortMiningSet) {
                clusterType.getSimilarGroupsId().add(miningTypePrismObject.getOid());
            }

            Map<String, Long> roleCountMap = roles.stream()
                    .collect(Collectors.toMap(Function.identity(),
                            role -> jaccSortMiningSet.stream()
                                    .filter(miningType -> miningType.asObjectable().getRoles().contains(role))
                                    .count()));

            List<String> sortedRolePrismObjectList = roleCountMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();
            for (String role : sortedRolePrismObjectList) {
                clusterType.getRoles().add(role);
            }
        } else {
            for (String group : groups) {
                PrismObject<MiningType> miningObject = getMiningObject(pageBase, group, result);
                roles.addAll(miningObject.asObjectable().getRoles());
                members.addAll(miningObject.asObjectable().getMembers());
            }
            clusterType.getSimilarGroupsId().addAll(groups);
            clusterType.getRoles().addAll(roles);

        }

        clusterType.setRolesCount(roles.size());
        clusterType.setMembersCount(members.size());
        clusterType.setMean(String.format("%.3f", meanRoles));
        clusterType.setDensity(String.format("%.3f", density));
        clusterType.setMinOccupation(minRoleCount);
        clusterType.setMaxOccupation(maxRoleCount);
        clusterType.setName(PolyStringType.fromOrig(isLastCluster ? "outliers" : clusterId));
        clusterType.setIdentifier(identifier);

        return clusterTypePrismObject;
    }
}
