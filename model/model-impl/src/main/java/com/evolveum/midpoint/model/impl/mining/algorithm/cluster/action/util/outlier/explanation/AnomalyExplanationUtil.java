/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver;
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

    AnomalyExplanationUtil() {
    }

    public OutlierExplanationResolver.AnomalyExplanationInput prepareOutlierExplanationAnomalyInput(
            @NotNull DetectedAnomalyResult detectedAnomalyResult) {

        DetectedAnomalyStatistics statistics = detectedAnomalyResult.getStatistics();
        OutlierExplanationResolver.RoleStats repoRoleStats = createRoleStats(statistics, true);
        OutlierExplanationResolver.RoleStats groupRoleStats = createRoleStats(statistics, false);

        List<OutlierExplanationResolver.ExplanationAttribute> explanationAttributes =
                prepareOutlierExplanationAttributeInput(detectedAnomalyResult);

        return new OutlierExplanationResolver.AnomalyExplanationInput(
                detectedAnomalyResult.getId(),
                repoRoleStats,
                groupRoleStats,
                explanationAttributes
        );
    }

    private @NotNull List<OutlierExplanationResolver.ExplanationAttribute> prepareOutlierExplanationAttributeInput(
            DetectedAnomalyResult detectedAnomalyResult) {
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
                                            attributeStatistic,
                                            itemPath,
                                            getUserItemDefinition(itemPath));
                                }))
                .collect(Collectors.toList());
    }

    @Contract("_, _ -> new")
    private OutlierExplanationResolver.@NotNull RoleStats createRoleStats(
            @Nullable DetectedAnomalyStatistics statistics,
            boolean isOverall) {
        if (statistics == null) {
            return new OutlierExplanationResolver.RoleStats(0, 0);
        }

        FrequencyType frequency = isOverall ? statistics.getMemberCoverageConfidence() : statistics.getGroupFrequency();
        if (frequency == null) {
            return new OutlierExplanationResolver.RoleStats(0, 0);
        }

        return new OutlierExplanationResolver.RoleStats(
                frequency.getPercentageRatio() * 0.01,
                frequency.getEntiretyCount()
        );
    }

}
