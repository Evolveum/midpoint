/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisObjectUtils.*;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.handler.Handler;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ClusteringAction {

    private Clusterable clusterable;

    Handler handler = new Handler("Density Clustering", 7);

    public ClusteringAction() {
    }

    public void execute(@NotNull ModelService modelService, String sessionOid,
            @NotNull OperationResult result, Task task) {

        PrismObject<RoleAnalysisSessionType> prismSession = getSessionTypeObject(modelService, result, sessionOid, task);
        if (prismSession != null) {

            deleteRoleAnalysisSessionClusters(modelService, result, prismSession.getOid(), task);

            RoleAnalysisProcessModeType processMode = prismSession.asObjectable().getProcessMode();
            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                this.clusterable = new UserBasedClustering();
            } else if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                this.clusterable = new RoleBasedClustering();
            }

            RoleAnalysisSessionType session = prismSession.asObjectable();
            List<PrismObject<RoleAnalysisClusterType>> clusterObjects = clusterable.executeClustering(session,
                    result, modelService, handler, task);

            importObjects(clusterObjects, session, modelService, result, handler, task);

        }
    }

    private void importObjects(
            List<PrismObject<RoleAnalysisClusterType>> clusters,
            @NotNull RoleAnalysisSessionType session,
            ModelService modelService,
            OperationResult result,
            Handler handler, Task task) {
        List<ObjectReferenceType> roleAnalysisClusterRef = new ArrayList<>();
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

        handler.setSubTitle("Importing Clusters");
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
            roleAnalysisClusterRef.add(objectReferenceType);

            importRoleAnalysisClusterObject(result, task, modelService,
                    clusterTypePrismObject,
                    sessionRef,
                    session.getDefaultDetectionOption()
            );
        }
        result.getSubresults().get(0).close();

        meanDensity = meanDensity / clusters.size();

        RoleAnalysisSessionStatisticType sessionStatistic = new RoleAnalysisSessionStatisticType();
        sessionStatistic.setProcessedObjectCount(processedObjectCount);
        sessionStatistic.setMeanDensity(meanDensity);
        sessionStatistic.setClusterCount(clusters.size());

        handler.setSubTitle("Update Session");
        handler.setOperationCountToProcess(clusters.size());
        modifySessionAfterClustering(sessionRef,
                sessionStatistic,
                modelService,
                result,
                task
        );
    }

}
