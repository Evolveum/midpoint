/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.MiningDataUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.startTimer;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DataPoint;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;

public class RoleBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(ClusterOptions clusterOptions) {
        PageBase pageBase = clusterOptions.getPageBase();

//        ts2(clusterOptions.getPageBase());
        long start = startTimer(" prepare clustering object");
        int assignThreshold = clusterOptions.getMinProperties();
        int minIntersections = clusterOptions.getMinIntersections();
        int threshold = Math.max(assignThreshold, minIntersections);
        int maxProperties = Math.max(clusterOptions.getMaxProperties(), threshold);

        ObjectFilter query = clusterOptions.getQuery();
        OperationResult operationResult = new OperationResult("ExecuteRoleBasedClustering");
        ListMultimap<List<String>, String> chunkMap = loadData(operationResult, pageBase,
                threshold, maxProperties, query);
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        start = startTimer("clustering");
        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minIntersections);
        double eps = 1 - clusterOptions.getSimilarity();

        //TODO
        if (eps == 0.0) {
            return new ClusterAlgorithmUtils().processExactMatch(pageBase, dataPoints, clusterOptions);
        }

        int minGroupSize = clusterOptions.getMinGroupSize();
        DBSCANClusterer<DataPoint> dbscan = new DBSCANClusterer<>(eps, minGroupSize, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils().processClusters(pageBase, dataPoints, clusters, clusterOptions);
    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, @NotNull PageBase pageBase,
            int minProperties, int maxProperties, ObjectFilter userQuery) {

        Set<String> existingRolesOidsSet = getExistingRolesOidsSet(result, pageBase);

        //role //user
        ListMultimap<String, String> roleToUserMap = getRoleBasedRoleToUserMap(result, pageBase, userQuery, existingRolesOidsSet);

        //user //role
        return getRoleBasedUserToRoleMap(minProperties, maxProperties, roleToUserMap);
    }

//    public ActivityDefinitionType ts(){
//        return new ActivityDefinitionType().identifier("Role-mining event")
//                .work(new WorkDefinitionsType().sessionProcess(new NoOpWorkDefinitionType().stepInterruptibility(NoOpActivityStepInterruptibilityType.NONE).steps(1).delay(1)));
//    }
//
//    public void ts2(PageBase pageBase){
//        Task task = pageBase.createSimpleTask("executeTask");
//        OperationResult result = task.getResult();
//
//        try {
//            pageBase.getModelInteractionService().submit(
//                    ts(),
//                    ActivitySubmissionOptions.create()
//                            .withTaskTemplate(new TaskType()
//                                    .name("Delete all objects")
//                                    .cleanupAfterCompletion(XmlTypeConverter.createDuration("P1D"))),
//                    task, result);
//        } catch (CommonException e) {
//            throw new RuntimeException(e);
//        }
//    }

}
