/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.context;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.model.impl.mining.utils.DebugOutlierDetectionEvaluation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.mining.algorithm.BaseAction;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering.Clusterable;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.context.OutlierDetectionActionExecutor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.model.impl.mining.RoleAnalysisDataServiceUtils.reverseMap;

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

    private static final Trace LOGGER = TraceManager.getTrace(ClusteringActionExecutor.class);

    private final AttributeAnalysisCache attributeAnalysisCache = new AttributeAnalysisCache();

    private final ObjectCategorisationCache objectCategorisationCache = new ObjectCategorisationCache();

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

        if (prismSession == null) {
            LOGGER.debug("nothing to do"); //TODO message
            return;
        }

        RoleAnalysisSessionType session = prismSession.asObjectable();
        objectCategorisationCache.markExcludedObjects(session);

        List<ObjectReferenceType> effectiveMarkRef = session.getEffectiveMarkRef();
        //TODO this is brutal hack. Change it.
        // Start *
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
        //End *

        roleAnalysisService.deleteSessionClustersMembers(prismSession.getOid(), task, result, false);

        this.clusterable = new ClusteringBehavioralResolver();

        List<PrismObject<RoleAnalysisClusterType>> clusterObjects = clusterable.executeClustering(
                roleAnalysisService, modelService, session, handler, attributeAnalysisCache, objectCategorisationCache, task, result);

        if (!clusterObjects.isEmpty()) {
            importObjects(roleAnalysisService, clusterObjects, session, task, result);
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

        QName complexType;
        if (analysisOption.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            complexType = RoleType.COMPLEX_TYPE;
        } else {
            complexType = UserType.COMPLEX_TYPE;
        }

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

        }

        //TODO not just basic it must be connected to in and out outlier analysis (experimental)
        for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
            long startTime = System.currentTimeMillis();
            RoleAnalysisClusterType cluster = clusterTypePrismObject.asObjectable();
            OutlierDetectionActionExecutor detectionExecutionUtil = new OutlierDetectionActionExecutor(roleAnalysisService);
            detectionExecutionUtil.executeOutlierDetection(
                    cluster, session, analysisOption, attributeAnalysisCache, objectCategorisationCache, task, result);
            long endTime = System.currentTimeMillis();
            double processingTimeInSeconds = (double) (endTime - startTime) / 1000.0;
            LOGGER.debug("Processing time for outlier detection cluster " + cluster.getName() + ": " + processingTimeInSeconds + " seconds");
        }

        result.getSubresults().get(0).close();

        meanDensity = meanDensity / (clusters.size() - countOutliers);

        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        meanDensity = Math.min(meanDensity, 100.000);
        sessionStatistic.setProcessedObjectCount(processedObjectCount);
        sessionStatistic.setMeanDensity(meanDensity);
        sessionStatistic.setClusterCount(clusters.size());

        handler.enterNewStep("Update Session");
        handler.setOperationCountToProcess(clusters.size());

        RoleAnalysisProcedureType analysisProcedureType = analysisOption.getAnalysisProcedureType();
        if (analysisProcedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
            resolveAnomalyNoise(clusters, session, attributeAnalysisCache, roleAnalysisService, task, result);
        }

        roleAnalysisService
                .updateSessionStatistics(session, sessionStatistic, task, result);
        RoleAnalysisIdentifiedCharacteristicsType characteristicsContainer = objectCategorisationCache.build(session);

        roleAnalysisService
                .updateSessionIdentifiedCharacteristics(session, characteristicsContainer, task, result);

        // Development only helper method - DO NOT RUN IN REAL ENVIRONMENT!
        //logDebugOutlierDetectionEvaluation(sessionOid, modelService, roleAnalysisService, task);
    }

    public void resolveAnomalyNoise(
            List<PrismObject<RoleAnalysisClusterType>> clusters,
            RoleAnalysisSessionType session,
            AttributeAnalysisCache analysisCache,
            RoleAnalysisService roleAnalysisService,
            Task task,
            OperationResult result) {
        ListMultimap<String, String> roleMemberCache = analysisCache.getRoleMemberCache();

        ListMultimap<String, String> userRolesMap = reverseMap(roleMemberCache);

        String sessionOid = session.getOid();

        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> allSessionOutlierPartitions = roleAnalysisService
                .getSessionOutlierPartitionsMap(sessionOid, null, true, task, result);
        ListMultimap<String, String> outlierAnomalyMap = ArrayListMultimap.create();
        Set<String> sessionAnomalyOids = new HashSet<>();
        Set<String> sessionOutlierOids = new HashSet<>();
        allSessionOutlierPartitions.forEach((partition, outlier) -> {
            sessionOutlierOids.add(outlier.getObjectRef().getOid());
            List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();
            if (detectedAnomalyResult != null) {
                detectedAnomalyResult.forEach(anomaly -> {
                    String anomalyRoleOid = anomaly.getTargetObjectRef().getOid();
                    outlierAnomalyMap.put(outlier.getObjectRef().getOid(), anomalyRoleOid);
                    sessionAnomalyOids.add(anomalyRoleOid);
                });
            }
        });

        loadSessionAnomalyCategorization(objectCategorisationCache, sessionAnomalyOids);
        loadSessionOutlierCategorization(objectCategorisationCache, sessionOutlierOids);

        for (PrismObject<RoleAnalysisClusterType> prismCluster : clusters) {
            RoleAnalysisClusterType clusterObject = prismCluster.asObjectable();
            List<ObjectReferenceType> member = clusterObject.getMember();
            for (ObjectReferenceType userMember : member) {
                String userMemberOid = userMember.getOid();
                List<String> strings = userRolesMap.get(userMemberOid);
                Set<String> userAnomalyOids = new HashSet<>();
                for (String anomalyOid : sessionAnomalyOids) {
                    if (strings.contains(anomalyOid)) {
                        userAnomalyOids.add(anomalyOid);
                    }
                }

                if (!userAnomalyOids.isEmpty()) {
                    HashSet<String> outlierAnomaly = new HashSet<>(outlierAnomalyMap.get(userMemberOid));
                    userAnomalyOids.removeAll(outlierAnomaly);
                    sessionAnomalyOids.removeAll(userAnomalyOids);
                }
            }

        }

        loadSessionExclusiveAnomalyCategorization(objectCategorisationCache, sessionAnomalyOids);
    }

    //Temporary only user mode
    private static void loadSessionAnomalyCategorization(
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Set<String> sessionAnomalyOids) {
        objectCategorisationCache.putAllCategory(sessionAnomalyOids,
                RoleAnalysisObjectCategorizationType.ANOMALY, RoleType.COMPLEX_TYPE);
    }

    //Temporary only user mode
    private static void loadSessionOutlierCategorization(
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Set<String> sessionAnomalyOids) {
        objectCategorisationCache.putAllCategory(sessionAnomalyOids,
                RoleAnalysisObjectCategorizationType.OUTLIER, UserType.COMPLEX_TYPE);
    }

    private static void loadSessionExclusiveAnomalyCategorization(@NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Set<String> sessionAnomalyOids) {
        objectCategorisationCache.putAllCategory(sessionAnomalyOids,
                RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY, RoleType.COMPLEX_TYPE);
    }

    /**
     * Logs outlier detection evaluation, can be used only with RBAC generated test data.
     * Intended to be used for development purposes only.
     */
    private void logDebugOutlierDetectionEvaluation(
            String sessionOid,
            ModelService modelService,
            RoleAnalysisService roleAnalysisService,
            Task task
    ) {
        try {
            var evaluation = new DebugOutlierDetectionEvaluation(
                    sessionOid,
                    modelService,
                    roleAnalysisService,
                    task
            ).evaluate();
            LOGGER.info(evaluation.toString());
        } catch (Exception e) {
            LOGGER.warn("Exception in outlier detection evaluation", e);
        }
    }
}
