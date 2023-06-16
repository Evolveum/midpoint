package com.evolveum.midpoint.gui.impl.page.admin.role.test.cluster;/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.filterMiningTypeObjects;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.getMiningObject;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.*;

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

    public List<PrismObject<ClusterType>> executeClustering(double eps, int minGroupSize, int minIntersections) {
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
        System.out.println("Elapsed time: " + elapsedSeconds + " seconds END");

        Cluster<DataPoint> outliers = getOutliers(dataPointList, clusters);
        clusters.add(outliers);


        return getClusterTypeObjectWithStatistic(clusters);
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

    public List<PrismObject<ClusterType>> getClusterTypeObjectWithStatistic(@NotNull List<Cluster<DataPoint>> clusters) {
        List<PrismObject<ClusterType>> miningTypeList = new ArrayList<>();

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

            miningTypeList.add(generateClusterObject(membersId, meanRoles, minRoleCount, maxRoleCount,
                    isLastCluster, clusterId, density));
        }

        return miningTypeList;
    }


    public PrismObject<ClusterType> generateClusterObject(List<String> groups, double meanRoles, int minRoleCount,
            int maxRoleCount, boolean isLastCluster, String clusterId, double density) {
        PrismObject<ClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while generate ClusterType object,{}", e.getMessage(), e);
        }
        assert clusterTypePrismObject != null;

        Collections.sort(groups);
        UUID uuid = UUID.randomUUID();
        clusterTypePrismObject.asObjectable().setName(PolyStringType.fromOrig(String.valueOf(uuid)));
        clusterTypePrismObject.asObjectable().setOid(String.valueOf(uuid));
        clusterTypePrismObject.asObjectable().setSimilarGroupsCount(groups.size());
        clusterTypePrismObject.asObjectable().getSimilarGroupsId().addAll(groups);

        OperationResult result = new OperationResult("Get ClusterType object");

        Set<String> roles = new HashSet<>();
        Set<String> members = new HashSet<>();
        for (String group : groups) {
            PrismObject<MiningType> miningObject = getMiningObject(pageBase, group, result);
            roles.addAll(miningObject.asObjectable().getRoles());
            members.addAll(miningObject.asObjectable().getMembers());
        }

//        clusterTypePrismObject.asObjectable().getRoles().addAll(roles);
        clusterTypePrismObject.asObjectable().setRolesCount(roles.size());

//        clusterTypePrismObject.asObjectable().getMembers().addAll(members);
        clusterTypePrismObject.asObjectable().setMembersCount(members.size());

        clusterTypePrismObject.asObjectable().setMean(String.format("%.3f", meanRoles));
        clusterTypePrismObject.asObjectable().setDensity(String.format("%.3f", density));

        clusterTypePrismObject.asObjectable().setMinOccupation(minRoleCount);
        clusterTypePrismObject.asObjectable().setMaxOccupation(maxRoleCount);

        if (isLastCluster) {
            clusterTypePrismObject.asObjectable().setIdentifier("outliers");
        } else {
            clusterTypePrismObject.asObjectable().setIdentifier(clusterId);
        }
        return clusterTypePrismObject;
    }

}
