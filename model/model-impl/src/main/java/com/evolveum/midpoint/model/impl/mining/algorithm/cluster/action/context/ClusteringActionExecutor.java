/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.context;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.mining.algorithm.BaseAction;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering.Clusterable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils.processOutliersAnalysis;

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
public class ClusteringActionExecutor extends BaseAction {

    private Clusterable clusterable;

    private static final String DECOMISSIONED_MARK_OID = "00000000-0000-0000-0000-000000000801";

    private final RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Density Clustering",
            7, this::incrementProgress);

    public ClusteringActionExecutor(@NotNull AbstractActivityRun<?, ?, ?> activityRun, @NotNull CurrentActivityState<AbstractActivityWorkStateType> activityState) {
        super(activityRun);
    }

    /**
     * Executes the clustering action for the specified session and imports the resulting clusters into the repository.
     *
     * @param sessionOid The OID of the role analysis session to process.
     * @param result The operation result to record the outcome.
     */
    public void execute(@NotNull String sessionOid, @NotNull OperationResult result) {

        var task = activityRun.getRunningTask();
        ModelService modelService = ModelBeans.get().modelService;
        RoleAnalysisService roleAnalysisService = ModelBeans.get().roleAnalysisService;

        PrismObject<RoleAnalysisSessionType> prismSession = roleAnalysisService.getSessionTypeObject(
                sessionOid, task, result);

        if (prismSession != null) {

            RoleAnalysisSessionType session = prismSession.asObjectable();
            List<ObjectReferenceType> effectiveMarkRef = session.getEffectiveMarkRef();
            boolean isDecomissioned = false;
            if (effectiveMarkRef != null && !effectiveMarkRef.isEmpty()) {
                for (ObjectReferenceType ref : effectiveMarkRef) {
                    if (ref.getOid().equals(DECOMISSIONED_MARK_OID)) {
                        String description = ref.getDescription();
                        if (description != null && description.equals("First run")) {
                            ObjectReferenceType mark = new ObjectReferenceType().oid(DECOMISSIONED_MARK_OID)
                                    .type(MarkType.COMPLEX_TYPE)
                                    .description("Second run");
                            roleAnalysisService.replaceSessionMarkRef(prismSession, mark, result, task);
                        } else {
                            isDecomissioned = true;
                        }
                        break;
                    }
                }
            }

            if (isDecomissioned) {
                Task subTask = task.createSubtask();
                PrismObject<TaskType> sessionTask = roleAnalysisService.getSessionTask(sessionOid, subTask, result);
//                roleAnalysisService.stopSessionTask(sessionOid, task1, result);
//                roleAnalysisService.deleteSessionTask(sessionOid, result);
//                sessionTask.asObjectable().cleanupAfterCompletion(XmlTypeConverter.createDuration("PT0S"));

                roleAnalysisService.deleteSession(sessionOid, subTask, result);
                task.setName("Role Analysis Decommissioned");
                if (sessionTask != null) {
                    roleAnalysisService.deleteSessionTask(sessionTask.asObjectable(), result);
                }
                handler.finish();
                return;
            }

            roleAnalysisService.deleteSessionClustersMembers(prismSession.getOid(), task, result);

            this.clusterable = new ClusteringBehavioralResolver();

            List<PrismObject<RoleAnalysisClusterType>> clusterObjects =
                    clusterable.executeClustering(roleAnalysisService, modelService, session, handler, task, result);

            if (!clusterObjects.isEmpty()) {
                importObjects(roleAnalysisService, clusterObjects, session, task, result);
            }

        }
    }

    private void importObjects(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<PrismObject<RoleAnalysisClusterType>> clusters,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String sessionOid = session.getOid();

        ObjectReferenceType sessionRef = new ObjectReferenceType();
        sessionRef.setOid(sessionOid);
        sessionRef.setType(RoleAnalysisSessionType.COMPLEX_TYPE);
        sessionRef.setTargetName(session.getName());
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        int processedObjectCount = 0;
        QName complexType = analysisOption.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        double meanDensity = 0;

        int countOutliers = 0;

        handler.enterNewStep("Importing Clusters");
        handler.setOperationCountToProcess(clusters.size());
        for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
            handler.iterateActualStatus();
            result.subresult("ImportingClusters");
            RoleAnalysisClusterType cluster = clusterTypePrismObject.asObjectable();
            if (cluster.getCategory() != null && cluster.getCategory().equals(RoleAnalysisClusterCategory.OUTLIERS)) {
                countOutliers++;
            }

            AnalysisClusterStatisticType clusterStatistic = clusterTypePrismObject.asObjectable().getClusterStatistics();
            meanDensity += clusterStatistic.getMembershipDensity();
            if (analysisOption.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
                processedObjectCount += clusterStatistic.getRolesCount();
            } else {
                processedObjectCount += clusterStatistic.getUsersCount();

            }

            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setOid(clusterTypePrismObject.getOid());
            objectReferenceType.setType(complexType);

            roleAnalysisService.importCluster(
                    clusterTypePrismObject, session.getDefaultDetectionOption(), sessionRef, task, result
            );

            processOutliersAnalysis(roleAnalysisService, cluster, session, analysisOption, task, result);

        }
        result.getSubresults().get(0).close();

        meanDensity = meanDensity / (clusters.size() - countOutliers);

        meanDensity = Math.min(meanDensity, 100.000);
        RoleAnalysisSessionStatisticType sessionStatistic = new RoleAnalysisSessionStatisticType();
        sessionStatistic.setProcessedObjectCount(processedObjectCount);
        sessionStatistic.setMeanDensity(meanDensity);
        sessionStatistic.setClusterCount(clusters.size());

        handler.enterNewStep("Update Session");
        handler.setOperationCountToProcess(clusters.size());
        roleAnalysisService
                .updateSessionStatistics(sessionRef, sessionStatistic, task, result
                );
    }

}
