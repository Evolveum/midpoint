/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation;

import com.evolveum.midpoint.common.outlier.OutlierExplanationResolver;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation.ExplanationUtil.*;

public class AnomalyExplanationUtil {

    public AnomalyExplanationUtil() {
    }

    public static OutlierExplanationResolver.AnomalyExplanationInput prepareOutlierExplanationAnomalyInput(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull DetectedAnomalyResultType detectedAnomalyResult,
            @NotNull Task task,
            @NotNull OperationResult result) {

        DetectedAnomalyStatisticsType statistics = detectedAnomalyResult.getStatistics();
        OutlierExplanationResolver.RoleStats repoRoleStats = createRoleStats(statistics, true);
        OutlierExplanationResolver.RoleStats groupRoleStats = createRoleStats(statistics, false);

        List<OutlierExplanationResolver.ExplanationAttribute> explanationAttributes =
                prepareOutlierExplanationAttributeInput(roleAnalysisService, detectedAnomalyResult, task, result);

        return new OutlierExplanationResolver.AnomalyExplanationInput(
                detectedAnomalyResult.getId(),
                repoRoleStats,
                groupRoleStats,
                explanationAttributes
        );
    }

    private static @NotNull List<OutlierExplanationResolver.ExplanationAttribute> prepareOutlierExplanationAttributeInput(
            @NotNull RoleAnalysisService roleAnalysisService,
            DetectedAnomalyResultType detectedAnomalyResult,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var userAttributeAnalysisResults = getUserAttributeAnalysis(detectedAnomalyResult);

        if (userAttributeAnalysisResults == null) {
            return new ArrayList<>();
        }

        return userAttributeAnalysisResults.stream()
                .flatMap(userAttributeAnalysisResult ->
                        userAttributeAnalysisResult.getAttributeStatistics().stream()
                                .filter(attributeStatistic -> Boolean.TRUE.equals(attributeStatistic.getIsUnusual()))
                                .map(attributeStatistic -> {
                                    ItemPathType itemPath = userAttributeAnalysisResult.getItemPath();
                                    return createExplanationAttribute(
                                            roleAnalysisService,
                                            attributeStatistic,
                                            itemPath,
                                            getUserItemDefinition(itemPath),
                                            task,
                                            result);
                                }))
                .collect(Collectors.toList());
    }

    @Contract("_, _ -> new")
    private static OutlierExplanationResolver.@NotNull RoleStats createRoleStats(
            @Nullable DetectedAnomalyStatisticsType statistics,
            boolean isOverall) {
        if (statistics == null) {
            return new OutlierExplanationResolver.RoleStats(0, 0);
        }

        FrequencyType frequency = isOverall ? statistics.getMemberCoverageConfidenceStat() : statistics.getGroupFrequency();
        if (frequency == null) {
            return new OutlierExplanationResolver.RoleStats(0, 0);
        }

        return new OutlierExplanationResolver.RoleStats(
                frequency.getPercentageRatio() * 0.01,
                frequency.getEntiretyCount()
        );
    }

}
