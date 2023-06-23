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

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortDataPoints;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.startTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.prepareExactUsersSetByRoles;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterAlgorithm {

    public ClusterAlgorithm(PageBase pageBase) {
        ClusterAlgorithm.pageBase = pageBase;
    }

    static PageBase pageBase;
    OperationResult operationResult = new OperationResult("Map UserType object for clustering");

    public List<PrismObject<ClusterType>> executeClustering(double eps, int minGroupSize,
            int minIntersections, String identifier, ObjectFilter userQuery) {

        long start = startTimer(" prepare clustering object");
        Map<List<String>, List<String>> listListMap = prepareExactUsersSetByRoles(operationResult, pageBase,
                minIntersections, userQuery);
        List<DataPoint> dataPointList = prepareDataPoints(listListMap);
        endTimer(start, "prepare clustering object");

        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPointList);
        endTimer(start, "clustering");

        start = startTimer("generate clusters mp objects");
        List<PrismObject<ClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, clusters.size())
                .mapToObj(i -> prepareClusters(clusters.get(i).getPoints(), String.valueOf(i), identifier))
                .collect(Collectors.toList());
        endTimer(start, "generate clusters mp objects");

        start = startTimer("generate outliers mp objects");
        Cluster<DataPoint> outliers = getOutliers(dataPointList, clusters);
        PrismObject<ClusterType> clusterTypePrismObject = prepareOutlierClusters(outliers.getPoints(), identifier);
        clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        endTimer(start, "generate outliers mp objects");

        return clusterTypeObjectWithStatistic;
    }

    private static List<DataPoint> prepareDataPoints(Map<List<String>, List<String>> listListMap) {
        List<DataPoint> dataPointList = new ArrayList<>();
        for (Map.Entry<List<String>, List<String>> entry : listListMap.entrySet()) {
            List<String> roles = entry.getKey();
            List<String> users = entry.getValue();
            double[] rolesArray = new double[roles.size()];
            for (int i = 0; i < roles.size(); i++) {
                String role = roles.get(i);
                rolesArray[i] = UUIDToDoubleConverter.convertUUid(role);
            }
            dataPointList.add(new DataPoint(rolesArray, users.get(0), users, roles));
        }
        return dataPointList;
    }

    public PrismObject<ClusterType> prepareClusters(List<DataPoint> dataPointCluster, String clusterIndex,
            String identifier) {
        List<DataPoint> sortedDataPoints = jaccSortDataPoints(dataPointCluster);

        int minRoleCount = Integer.MAX_VALUE;
        int maxRoleCount = -1;

        int totalDataPoints = sortedDataPoints.size();
        int sumRoles = 0;

        List<String> users = new ArrayList<>();
        Map<String, Integer> roleCountMap = new HashMap<>();

        for (DataPoint dataPoint : sortedDataPoints) {
            List<String> pointRoles = dataPoint.getRoles();

            for (String role : pointRoles) {
                roleCountMap.compute(role, (key, counter) -> counter == null ? 1 : counter + 1);
            }

            List<String> pointMembers = dataPoint.getMembers();

            users.addAll(pointMembers);

            int roleCount = pointRoles.size();
            sumRoles += roleCount;
            minRoleCount = Math.min(minRoleCount, roleCount);
            maxRoleCount = Math.max(maxRoleCount, roleCount);
        }

        double meanRoles = (double) sumRoles / totalDataPoints;

        List<String> sortedRoles = roleCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        double density = (sumRoles / (double) (dataPointCluster.size() * roleCountMap.size())) * 100;

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);

        return generateClusterObject(users, sortedRoles, meanRoles, density, minRoleCount,
                maxRoleCount, name, identifier);
    }

    public PrismObject<ClusterType> prepareOutlierClusters(List<DataPoint> dataPointCluster,
            String identifier) {

        int minRoleCount = Integer.MAX_VALUE;
        int maxRoleCount = -1;

        int totalDataPoints = dataPointCluster.size();
        int sumRoles = 0;

        List<String> users = new ArrayList<>();
        Set<String> roles = new HashSet<>();
        for (DataPoint dataPoint : dataPointCluster) {
            List<String> pointRoles = dataPoint.getRoles();
            roles.addAll(pointRoles);
            List<String> pointMembers = dataPoint.getMembers();
            users.addAll(pointMembers);

            int roleCount = pointRoles.size();
            sumRoles += roleCount;
            minRoleCount = Math.min(minRoleCount, roleCount);
            maxRoleCount = Math.max(maxRoleCount, roleCount);
        }

        double meanRoles = (double) sumRoles / totalDataPoints;

        double density = (sumRoles / (double) (dataPointCluster.size() * roles.size())) * 100;

        PolyStringType name = PolyStringType.fromOrig("outliers");

        return generateClusterObject(users, new ArrayList<>(roles), meanRoles, density, minRoleCount,
                maxRoleCount, name, identifier);
    }

    private @NotNull PrismObject<ClusterType> generateClusterObject(List<String> users, List<String> sortedRoles, double meanRoles,
            double density, int minRoleCount, int maxRoleCount, PolyStringType name, String identifier) {
        PrismObject<ClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }
        assert clusterTypePrismObject != null;

        ClusterType clusterType = clusterTypePrismObject.asObjectable();

        clusterType.setOid(String.valueOf(UUID.randomUUID()));
        clusterType.setSimilarGroupsCount(users.size());
        clusterType.getSimilarGroupsId().addAll(users);
        clusterType.getRoles().addAll(sortedRoles);
        clusterType.setRolesCount(sortedRoles.size());
        clusterType.setMembersCount(users.size());
        clusterType.setMean(String.format("%.3f", meanRoles));
        clusterType.setDensity(String.format("%.3f", density));
        clusterType.setMinOccupation(minRoleCount);
        clusterType.setMaxOccupation(maxRoleCount);
        clusterType.setName(name);
        clusterType.setIdentifier(identifier);

        return clusterTypePrismObject;
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
                outliers.addPoint(dataPoint);
            }
        }
        return outliers;
    }

}
