/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisObjectUtils.*;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.mining.algorithm.BaseAction;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Clustering action.
 * <p>
 * This action is responsible for clustering objects based on selected role analysis mode.
 * <p>
 * It is possible to cluster objects based on user or role. This is set in session object.
 * <p>
 * This action is also responsible for importing clusters into repository.
 * <p>
 * This action also updates session object with information about clusters.
 */
public class ClusteringAction extends BaseAction {

    private Clusterable clusterable;

    private final RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Density Clustering", 7, this::incrementProgress);

    public ClusteringAction(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        super(activityRun);
    }

    public void execute(String sessionOid, @NotNull OperationResult result) {

        var task = activityRun.getRunningTask();
        ModelService modelService = ModelBeans.get().modelService;

        PrismObject<RoleAnalysisSessionType> prismSession = getSessionTypeObject(modelService, sessionOid, task, result);
        if (prismSession != null) {

            deleteRoleAnalysisSessionClusters(modelService, prismSession.getOid(), task, result);

            RoleAnalysisProcessModeType processMode = prismSession.asObjectable().getProcessMode();
            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                this.clusterable = new UserBasedClustering();
            } else if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                this.clusterable = new RoleBasedClustering();
            }

            RoleAnalysisSessionType session = prismSession.asObjectable();
            List<PrismObject<RoleAnalysisClusterType>> clusterObjects =
                    clusterable.executeClustering(session, modelService, handler, task, result);

            if (clusterObjects != null && !clusterObjects.isEmpty()) {
                importObjects(clusterObjects, session, modelService, task, result);
            }

        }
    }

    private void importObjects(
            List<PrismObject<RoleAnalysisClusterType>> clusters,
            @NotNull RoleAnalysisSessionType session,
            ModelService modelService,
            Task task, OperationResult result) {
        String sessionOid = session.getOid();

        ObjectReferenceType sessionRef = new ObjectReferenceType();
        sessionRef.setOid(sessionOid);
        sessionRef.setType(RoleAnalysisSessionType.COMPLEX_TYPE);
        sessionRef.setTargetName(session.getName());

        int processedObjectCount = 0;
        QName complexType = session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        double meanDensity = 0;

        handler.enterNewStep("Importing Clusters");
        handler.setOperationCountToProcess(clusters.size());
        for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
            handler.iterateActualStatus();
            result.subresult("ImportingClusters");

            AnalysisClusterStatisticType clusterStatistic = clusterTypePrismObject.asObjectable().getClusterStatistics();
            meanDensity += clusterStatistic.getMembershipDensity();
            if (session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
                processedObjectCount += clusterStatistic.getRolesCount();
            } else {
                processedObjectCount += clusterStatistic.getUsersCount();

            }

            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setOid(clusterTypePrismObject.getOid());
            objectReferenceType.setType(complexType);

            importRoleAnalysisClusterObject(modelService, clusterTypePrismObject, session.getDefaultDetectionOption(), sessionRef, task, result
            );
        }
        result.getSubresults().get(0).close();

        meanDensity = meanDensity / clusters.size();

        RoleAnalysisSessionStatisticType sessionStatistic = new RoleAnalysisSessionStatisticType();
        sessionStatistic.setProcessedObjectCount(processedObjectCount);
        sessionStatistic.setMeanDensity(meanDensity);
        sessionStatistic.setClusterCount(clusters.size());

        handler.enterNewStep("Update Session");
        handler.setOperationCountToProcess(clusters.size());
        modifySessionAfterClustering(modelService, sessionRef,
                sessionStatistic,
                task, result
        );
    }

}
