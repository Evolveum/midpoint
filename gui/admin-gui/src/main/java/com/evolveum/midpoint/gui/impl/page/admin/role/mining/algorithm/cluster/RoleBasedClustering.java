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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jetbrains.annotations.NotNull;

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

public class RoleBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<ClusterType>> executeClustering(ClusterOptions clusterOptions) {
        long start = startTimer(" prepare clustering object");

        OperationResult operationResult = new OperationResult("Map UserType object for clustering");

        int assignThreshold = clusterOptions.getAssignThreshold();
        int minIntersections = clusterOptions.getMinIntersections();
        int threshold = Math.max(assignThreshold, minIntersections);

        PageBase pageBase = clusterOptions.getPageBase();
        ObjectFilter query = clusterOptions.getQuery();
        ListMultimap<List<String>, String> chunkMap = loadData(operationResult, pageBase,
                threshold, minIntersections, query);
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        double eps = 1 - clusterOptions.getSimilarity();
        int minGroupSize = clusterOptions.getMinGroupSize();
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        String identifier = clusterOptions.getIdentifier();
        return new ClusterAlgorithmUtils().processClusters(pageBase, identifier, dataPoints, clusters);
    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, @NotNull PageBase pageBase,
            int threshold, int minIntersection, ObjectFilter roleQuery) {

        //role //user
        ListMultimap<String, String> userTypeMap = ArrayListMultimap.create();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                UserType user = object.asObjectable();
                List<String> element = ClusterObjectUtils.getRolesOid(user);
                for (String roleId : element) {
                    userTypeMap.putAll(roleId, Collections.singletonList(user.getOid()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
        RepositoryService repositoryService = pageBase.getRepositoryService();
        ObjectQuery objectQuery = pageBase.getPrismContext().queryFactory().createQuery(roleQuery);

        try {
            repositoryService.searchObjectsIterative(UserType.class, objectQuery, resultHandler, optionsBuilder.build(),
                    true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        //user //role
        ListMultimap<List<String>, String> flippedMap = ArrayListMultimap.create();
        for (String key : userTypeMap.keySet()) {
            List<String> values = userTypeMap.get(key);
            if (threshold <= values.size()) {
                flippedMap.put(values, key);
            }
        }
        return flippedMap;
    }

}
