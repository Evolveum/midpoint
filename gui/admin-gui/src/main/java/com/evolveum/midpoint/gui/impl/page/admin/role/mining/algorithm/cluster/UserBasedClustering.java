/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.MiningDataUtils.getExistingRolesOidsSet;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.MiningDataUtils.getUserBasedRoleToUserMap;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.startTimer;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.test.DBSCANClusterer2;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DataPoint;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;

public class UserBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(ClusterOptions clusterOptions,
            OperationResult result, PageBase pageBase) {

        long start = startTimer(" prepare clustering object");
        int minIntersections = clusterOptions.getMinIntersections();
        int assignThreshold = clusterOptions.getMinProperties();
        int threshold = Math.max(assignThreshold, minIntersections);
        int maxProperties = Math.max(clusterOptions.getMaxProperties(), threshold);
        clusterOptions.setMinProperties(threshold);
        clusterOptions.setMinIntersections(minIntersections);

        //roles //users
        ListMultimap<List<String>, String> chunkMap = loadData(result, pageBase,
                threshold, maxProperties, clusterOptions.getQuery());
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        double eps = 1 - clusterOptions.getSimilarity();
        int minGroupSize = clusterOptions.getMinGroupSize();

        //TODO
        if (eps == 0.0) {
            return new ClusterAlgorithmUtils().processExactMatch(pageBase, result, dataPoints, clusterOptions);
        }
        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
//        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
//        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);

        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils().processClusters(pageBase, result, dataPoints, clusters, clusterOptions);
    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, PageBase pageBase,
            int minProperties, int maxProperties, ObjectFilter userQuery) {

        Set<String> existingRolesOidsSet = getExistingRolesOidsSet(result, pageBase);

        //role //user
        return getUserBasedRoleToUserMap(result, pageBase, minProperties, maxProperties, userQuery, existingRolesOidsSet);
    }

}
