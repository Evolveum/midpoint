/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.startTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.prepareExactRolesSetByUsers;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

public class ClusterAlgorithmByRole {

    public ClusterAlgorithmByRole(PageBase pageBase) {
        ClusterAlgorithmByRole.pageBase = pageBase;
    }

    static PageBase pageBase;
    OperationResult operationResult = new OperationResult("Map UserType object for clustering");

    public List<PrismObject<ClusterType>> executeClustering(double eps, int minGroupSize,
            int minIntersections, String identifier, ObjectFilter roleQuery) {

        long start = startTimer(" prepare clustering object");

        Map<List<String>, List<String>> chunkMap = prepareExactRolesSetByUsers(operationResult, pageBase,
                minIntersections, roleQuery);
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils(pageBase, operationResult).processClusters(identifier, dataPoints, clusters);
    }

}
