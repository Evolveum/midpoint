/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.context;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.model.impl.mining.utils.DebugOutlierDetectionEvaluation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.util.exception.*;

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

        roleAnalysisService.deleteSessionClustersMembers(prismSession.getOid(), task, result, false);

        this.clusterable = new ClusteringBehavioralResolver();

        clusterable.executeClustering(
                roleAnalysisService, modelService, session, handler, attributeAnalysisCache, objectCategorisationCache, task, result);

        updateSessionStatistics(roleAnalysisService, modelService, session, task, result);
    }

    int processedObjectCount = 0;
    double meanDensity = 0;
    int countOutliers = 0;

    private void updateSessionStatistics(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<String> sessionClusterOids = new ArrayList<>();

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();

        handler.enterNewStep("Load session statistics");
        handler.setOperationCountToProcess(1);

        loadSessionStatisticsFromClusters(modelService, session, task, result, sessionClusterOids, analysisOption);

        int clusterCount = sessionClusterOids.size();
        meanDensity = meanDensity / (clusterCount - countOutliers);

        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        meanDensity = Math.min(meanDensity, 100.000);
        handler.iterateActualStatus();
        result.subresult("UpdatingStatistics");

        sessionStatistic.setProcessedObjectCount(processedObjectCount);
        sessionStatistic.setMeanDensity(meanDensity);
        sessionStatistic.setClusterCount(clusterCount);


        handler.enterNewStep("Update Session");
        handler.setOperationCountToProcess(clusterCount);

        roleAnalysisService
                .updateSessionStatistics(session, sessionStatistic, task, result);

        RoleAnalysisIdentifiedCharacteristicsType characteristicsContainer = objectCategorisationCache.build(session);

        roleAnalysisService
                .updateSessionIdentifiedCharacteristics(session, characteristicsContainer, task, result);

        executeOutlierPart(roleAnalysisService, modelService, session, task, result, sessionClusterOids, analysisOption);
        result.getSubresults().get(0).close();
        // Development only helper method - DO NOT RUN IN REAL ENVIRONMENT!
        //logDebugOutlierDetectionEvaluation(sessionOid, ModelBeans.get().modelService, roleAnalysisService, task);
    }

    private void executeOutlierPart(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<String> sessionClusterOids,
            RoleAnalysisOptionType analysisOption) {
        for (String clusterOid : sessionClusterOids) {
            PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = roleAnalysisService.getClusterTypeObject(
                    clusterOid, task, result);
            if (clusterTypePrismObject == null) {
                LOGGER.warn("Cluster with oid {} not found", clusterOid);
                continue;
            }

            long startTime = System.currentTimeMillis();
            RoleAnalysisClusterType cluster = clusterTypePrismObject.asObjectable();
            OutlierDetectionActionExecutor detectionExecutionUtil = new OutlierDetectionActionExecutor(roleAnalysisService);
            detectionExecutionUtil.executeOutlierDetection(
                    cluster, session, analysisOption, attributeAnalysisCache, objectCategorisationCache, task, result);
            long endTime = System.currentTimeMillis();
            double processingTimeInSeconds = (double) (endTime - startTime) / 1000.0;
            LOGGER.debug("Processing time for outlier detection cluster " + cluster.getName()
                    + ": " + processingTimeInSeconds + " seconds");
        }

        //part of object categorization (expensive)
        RoleAnalysisProcedureType analysisProcedureType = analysisOption.getAnalysisProcedureType();
        if (analysisProcedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
            resolveAnomalyNoise(modelService, session, attributeAnalysisCache, roleAnalysisService, task, result);
        }
    }

    private void loadSessionStatisticsFromClusters(
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result,
            List<String> sessionClusterOids,
            RoleAnalysisOptionType analysisOption) {
        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(session.getOid()).build();

        try {
            ResultHandler<RoleAnalysisClusterType> resultHandler = (prismCluster, lResult) -> {
                result.subresult("extractClusterStatistics");
                sessionClusterOids.add(prismCluster.getOid());

                RoleAnalysisClusterType cluster = prismCluster.asObjectable();
                if (cluster.getCategory() != null && cluster.getCategory().equals(RoleAnalysisClusterCategory.OUTLIERS)) {
                    countOutliers++;
                }

                AnalysisClusterStatisticType clusterStatistic = prismCluster.asObjectable().getClusterStatistics();
                meanDensity += clusterStatistic.getMembershipDensity();
                if (analysisOption.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
                    processedObjectCount += clusterStatistic.getRolesCount();
                } else {
                    processedObjectCount += clusterStatistic.getUsersCount();
                }

                return true;
            };

            modelService.searchObjectsIterative(RoleAnalysisClusterType.class, query, resultHandler, null,
                    task, result);

        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't search  search and load cluster object iterative for session: {}", session, e);
        }
    }

    public void resolveAnomalyNoise(
            @NotNull ModelService modelService,
            RoleAnalysisSessionType session,
            AttributeAnalysisCache analysisCache,
            RoleAnalysisService roleAnalysisService,
            Task task,
            OperationResult result) {
        ListMultimap<String, String> roleMemberCache = analysisCache.getRoleMemberCache();

        ListMultimap<String, String> userRolesMap = reverseMap(roleMemberCache);

        String sessionOid = session.getOid();

        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> allSessionOutlierPartitions = roleAnalysisService
                .getSessionOutlierPartitionsMap(sessionOid, null, true, null, task, result);
        ListMultimap<String, String> outlierAnomalyMap = ArrayListMultimap.create();
        Set<String> sessionAnomalyOids = new HashSet<>();
        Set<String> sessionOutlierOids = new HashSet<>();
        allSessionOutlierPartitions.forEach((partition, outlier) -> {
            sessionOutlierOids.add(outlier.getObjectRef().getOid());
            List<DetectedAnomalyResultType> detectedAnomalyResult = partition.getDetectedAnomalyResult();
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

        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(session.getOid()).build();

        try {
            ResultHandler<RoleAnalysisClusterType> resultHandler = (prismCluster, lResult) -> {
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
                return true;
            };

            modelService.searchObjectsIterative(RoleAnalysisClusterType.class, query, resultHandler, null,
                    task, result);

        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't search  search and load cluster object iterative for session: {}", session, e);
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
