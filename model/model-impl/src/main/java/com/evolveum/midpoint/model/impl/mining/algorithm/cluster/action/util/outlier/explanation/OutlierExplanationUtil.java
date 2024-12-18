/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation.ExplanationUtil.getUserItemDefinition;

public class OutlierExplanationUtil {

    OutlierExplanationUtil() {
    }

    @Contract("_, _, _, _, _ -> new")
    public static OutlierExplanationResolver.@NotNull OutlierExplanationInput prepareOutlierExplanationInput(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            int partitionCount,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<DetectedAnomalyResult> detectedAnomalyResults = partition.getDetectedAnomalyResult();
        partition.setId((long) partitionCount);

        List<OutlierExplanationResolver.AnomalyExplanationInput> anomalyExplanationInputs = new ArrayList<>(
                detectedAnomalyResults.size());
        AnomalyExplanationUtil anomalyExplanationUtil = new AnomalyExplanationUtil();

        long id = 1;
        for (DetectedAnomalyResult anomalyResult : detectedAnomalyResults) {
            anomalyResult.setId(id++);
            anomalyExplanationInputs.add(anomalyExplanationUtil.prepareOutlierExplanationAnomalyInput(anomalyResult));
        }

        @NotNull List<OutlierExplanationResolver.@Nullable ExplanationAttribute> explanationAttribute =
                extractGroupByExplanationAttributes(roleAnalysisService, partition, task, result);

        return new OutlierExplanationResolver.OutlierExplanationInput(
                partition.getId(),
                anomalyExplanationInputs,
                explanationAttribute,
                partitionCount
        );
    }

    private static @NotNull List<OutlierExplanationResolver.@Nullable ExplanationAttribute> extractGroupByExplanationAttributes(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<OutlierExplanationResolver.ExplanationAttribute> explanationAttributes = new ArrayList<>();
        @Nullable List<ItemPathType> groupByPath = getSessionGroupByAttributeRule(roleAnalysisService, partition, task, result);
        if (groupByPath == null || groupByPath.isEmpty()) {
            return Collections.emptyList();
        }

        groupByPath.forEach(path -> Optional.ofNullable(getOutlierUserGroupByValue(partition, path))
                .map(userGroupByValue -> new OutlierExplanationResolver.ExplanationAttribute(
                        path, getUserItemDefinition(path), userGroupByValue)).ifPresent(explanationAttributes::add));

        return explanationAttributes;
    }

    private static @Nullable String getOutlierUserGroupByValue(
            @NotNull RoleAnalysisOutlierPartitionType partition,
            ItemPathType groupByPath) {
        List<RoleAnalysisAttributeAnalysis> attributeAnalyses = Optional.ofNullable(partition.getPartitionAnalysis())
                .map(RoleAnalysisPartitionAnalysisType::getAttributeAnalysis)
                .map(AttributeAnalysis::getUserAttributeAnalysisResult)
                .map(RoleAnalysisAttributeAnalysisResult::getAttributeAnalysis)
                .orElse(null);

        if (attributeAnalyses == null) {
            return null;
        }

        return attributeAnalyses.stream()
                .filter(attributeAnalysis -> groupByPath.equivalent(attributeAnalysis.getItemPath()))
                .findFirst()
                .map(roleAnalysisAttributeAnalysis -> extractFirstAttributeValue(
                        roleAnalysisAttributeAnalysis.getAttributeStatistics()))  //TODO support multivalued
                .orElse(null);
    }

    private static @Nullable List<ItemPathType> getSessionGroupByAttributeRule(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ObjectReferenceType targetSessionRef = partition.getTargetSessionRef();
        if (targetSessionRef == null || targetSessionRef.getOid() == null) {
            return null;
        }

        PrismObject<RoleAnalysisSessionType> sessionPrismObject = roleAnalysisService.getSessionTypeObject(
                targetSessionRef.getOid(), task, result);
        if (sessionPrismObject == null) {
            return null;
        }

        return getClusteringAttributePaths(sessionPrismObject.asObjectable());
    }

    private static @Nullable List<ItemPathType> getClusteringAttributePaths(RoleAnalysisSessionType sessionObject) {
        return Optional.ofNullable(sessionObject)
                .map(RoleAnalysisSessionType::getUserModeOptions)
                .map(UserAnalysisSessionOptionType::getClusteringAttributeSetting)
                .map(ClusteringAttributeSettingType::getClusteringAttributeRule)
                .map(rules -> rules.stream()
                        .map(ClusteringAttributeRuleType::getPath)
                        .toList())
                .orElse(null);
    }

    private static @Nullable String extractFirstAttributeValue(@NotNull List<RoleAnalysisAttributeStatistics> attributeStatistics) {
        return attributeStatistics.stream()
                .map(RoleAnalysisAttributeStatistics::getAttributeValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}
