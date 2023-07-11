/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.startTimer;

import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

public class UserBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<ClusterType>> executeClustering(ClusterOptions clusterOptions) {

        OperationResult operationResult = new OperationResult("Map UserType object for clustering");

        long start = startTimer(" prepare clustering object");

        int minIntersections = clusterOptions.getMinIntersections();
        int assignThreshold = clusterOptions.getAssignThreshold();
        int threshold = Math.max(assignThreshold, minIntersections);
        clusterOptions.setAssignThreshold(threshold);
        clusterOptions.setMinIntersections(minIntersections);

        PageBase pageBase = clusterOptions.getPageBase();
        //roles //users
        ListMultimap<List<String>, String> chunkMap = loadData(operationResult, pageBase,
                threshold, clusterOptions.getQuery());
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        double eps = 1 - clusterOptions.getSimilarity();
        int minGroupSize = clusterOptions.getMinGroupSize();

        //TODO
        if (eps == 0.0) {
            return new ClusterAlgorithmUtils().processIdenticalGroup(pageBase, dataPoints, clusterOptions);
        }
        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils().processClusters(pageBase, dataPoints, clusters, clusterOptions);
    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, PageBase pageBase,
            int threshold, ObjectFilter userQuery) {

        //role //user
        ListMultimap<List<String>, String> userTypeMap = ArrayListMultimap.create();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                List<String> element = ClusterObjectUtils.getRolesOid(object.asObjectable());
                if (element.size() >= threshold) {
                    Collections.sort(element);
                    userTypeMap.putAll(element, Collections.singletonList(object.asObjectable().getOid()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
        RepositoryService repositoryService = pageBase.getRepositoryService();
        ObjectQuery objectQuery = pageBase.getPrismContext().queryFactory().createQuery(userQuery);

        try {
            repositoryService.searchObjectsIterative(UserType.class, objectQuery, resultHandler, optionsBuilder.build(),
                    true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        return userTypeMap;
    }
}
