/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.startTimer;

import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DataPoint;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(ClusterOptions clusterOptions) {

        long start = startTimer(" prepare clustering object");
        int minIntersections = clusterOptions.getMinIntersections();
        int assignThreshold = clusterOptions.getMinProperties();
        int threshold = Math.max(assignThreshold, minIntersections);
        int maxProperties = Math.max(clusterOptions.getMaxProperties(), threshold);
        clusterOptions.setMinProperties(threshold);
        clusterOptions.setMinIntersections(minIntersections);

        PageBase pageBase = clusterOptions.getPageBase();
        //roles //users
        OperationResult operationResult = new OperationResult("ExecuteUserBasedClustering");
        ListMultimap<List<String>, String> chunkMap = loadData(operationResult, pageBase,
                threshold, maxProperties, clusterOptions.getQuery());
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        double eps = 1 - clusterOptions.getSimilarity();
        int minGroupSize = clusterOptions.getMinGroupSize();

        //TODO
        if (eps == 0.0) {
            return new ClusterAlgorithmUtils().processExactMatch(pageBase, dataPoints, clusterOptions);
        }
        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils().processClusters(pageBase, dataPoints, clusters, clusterOptions);
    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, PageBase pageBase,
            int minProperties, int maxProperties, ObjectFilter userQuery) {

        //role //user
        ListMultimap<List<String>, String> roleToUserMap = ArrayListMultimap.create();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                List<String> members = ClusterObjectUtils.getRolesOid(object.asObjectable());
                int propertiesCount = members.size();
                if (minProperties <= propertiesCount && maxProperties >= propertiesCount) {
                    Collections.sort(members);
                    roleToUserMap.putAll(members, Collections.singletonList(object.asObjectable().getOid()));
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

        return roleToUserMap;
    }
}
