/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.outlier;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.util.LocalizableMessageList.COMMA;
import static com.evolveum.midpoint.util.LocalizableMessageList.SPACE;

public class OutlierExplanationResolver {

    public record ExplanationAttribute(
        ItemPathType path,
        ItemDefinition<?> def,
        String value
    ) {}

    public record RoleStats(
            double frequency,
            Integer memberCount
    ) {}

    public record AnomalyExplanationInput(
            Long id,
            RoleStats roleOverallStats,
            RoleStats roleClusterStats,
            List<ExplanationAttribute> unusualAttributes // should contain only unusual = true attributes
    ) {}

    public record OutlierExplanationInput(
            Long id,
            @NotNull List<AnomalyExplanationInput> anomalies,
            @NotNull List<ExplanationAttribute> groupByAttributes,
            @NotNull Double outlierScore // value <0, 100>
    ) {}

    public record AnomalyExplanationResult(
            Long id,
            List<ExplanationResult> explanations
    ) {}

    public record OutlierExplanationResult(
            Long id,
            ExplanationResult explanation,
            ExplanationResult shortExplanation,
            List<AnomalyExplanationResult> anomalies
    ) {}

    public record ExplanationResult(
            LocalizableMessage message,
            List<OutlierDetectionExplanationCategory> categories
    ) {}

    public enum OutlierDetectionExplanationCategory {
        UNUSUAL_ACCESS,
        IRREGULAR_ATTRIBUTES
    }

    private final OutlierExplanationInput outlier;
    private static final int FEW_ENOUGH_USERS = 3;
    private static final LocalizableMessage COLON = LocalizableMessageBuilder.buildFallbackMessage(": ");
    private static final LocalizableMessage NOTHING = LocalizableMessageBuilder.buildFallbackMessage("");
    private static final LocalizableMessage DOT = LocalizableMessageBuilder.buildFallbackMessage(".");
    private static final LocalizableMessage AND = joinMessage(List.of(SPACE, new SingleLocalizableMessage("OutlierDetection.explanation.common.andSeparator"), SPACE), null);
    private static final DecimalFormat PERCENTAGE_FORMAT_WHOLE = new DecimalFormat("#");
    private static final DecimalFormat PERCENTAGE_FORMAT_FRACTION = new DecimalFormat("#.#");

    public OutlierExplanationResolver(OutlierExplanationInput outlier) {
        this.outlier = outlier;
    }

    private ExplanationResult makeExplanation(LocalizableMessage message, List<OutlierDetectionExplanationCategory> categories) {
        return new ExplanationResult(message, categories);
    }

    private static LocalizableMessage joinMessage(List<LocalizableMessage> messages, LocalizableMessage separator) {
        return new LocalizableMessageList(messages, separator, null, null);
    }

    private static LocalizableMessage joinMessage(List<LocalizableMessage> messages) {
        return new LocalizableMessageList(messages, NOTHING, null, null);
    }

    private String formatOutlierScore() {
        return String.format("%.2f", outlier.outlierScore()) + "%";
    }

    private ExplanationResult explainOverallOutlier() {
        var anomalyCount = outlier.anomalies().size();
        List<OutlierDetectionExplanationCategory> categories = new ArrayList<>();
        LocalizableMessage finalMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.unusualAccess", new Object[] {anomalyCount});
        categories.add(OutlierDetectionExplanationCategory.UNUSUAL_ACCESS);
        if (!outlier.groupByAttributes().isEmpty()) {
            var localizedAttributes = outlier.groupByAttributes().stream()
                    .map(groupByAttribute -> {
                        var attributeName = localize(groupByAttribute);
                        var attributeValue = LocalizableMessageBuilder.buildFallbackMessage("'" + groupByAttribute.value() + "'");
                        return joinMessage(List.of(attributeValue, attributeName), SPACE);
                    })
                    .toList();
            var localizedAttributesMessage = joinMessage(localizedAttributes, AND);
            var groupByMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.groupByExplanation", new Object[] {localizedAttributesMessage});
            finalMessage = joinMessage(List.of(finalMessage, groupByMessage), SPACE);
        }
        var outlierScore = formatOutlierScore();
        var scoreMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.scoreExplanation", new Object[] {outlierScore, outlierScore});
        finalMessage = joinMessage(List.of(finalMessage, DOT, SPACE, scoreMessage, DOT));
        return makeExplanation(finalMessage, categories);
    }

    private ExplanationResult explainOverallOutlierShort() {
        var anomalyCount = outlier.anomalies().size();
        List<OutlierDetectionExplanationCategory> categories = List.of(OutlierDetectionExplanationCategory.UNUSUAL_ACCESS);
        var outlierScore = formatOutlierScore();
        LocalizableMessage finalMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.shortDescription", new Object[] {anomalyCount, outlierScore});
        finalMessage = joinMessage(List.of(finalMessage, DOT));
        return makeExplanation(finalMessage, categories);
    }

    private LocalizableMessage localize(ExplanationAttribute explanationAttribute) {
        var def = explanationAttribute.def();
        if (def != null) {
            if (StringUtils.isNotEmpty(def.getDisplayName())) {
                return new SingleLocalizableMessage(def.getDisplayName(), new Object[0], def.getDisplayName());
            }
            if (StringUtils.isNotEmpty(def.getItemName().getLocalPart())) {
                return LocalizableMessageBuilder.buildFallbackMessage(def.getItemName().getLocalPart());
            }
        }
        return LocalizableMessageBuilder.buildFallbackMessage(explanationAttribute.path().toString());
    }

    private LocalizableMessage localizeAttributePair(ExplanationAttribute explanationAttribute) {
        var key = localize(explanationAttribute);
        var value = LocalizableMessageBuilder.buildFallbackMessage(explanationAttribute.value());
        return joinMessage(List.of(key, value), COLON);
    }

    private String formatPercentage(double value) {
        var percentage = value * 100;
        var rounded = value < 0.01 ? PERCENTAGE_FORMAT_FRACTION.format(percentage) : PERCENTAGE_FORMAT_WHOLE.format(percentage);
        return rounded + "%";
    }

    public List<ExplanationResult> explainAnomaly(AnomalyExplanationInput anomaly) {
        var inGroupCount = anomaly.roleClusterStats().memberCount();
        var repoCoverage = formatPercentage(anomaly.roleOverallStats().frequency());
        var groupCoverage = inGroupCount <= FEW_ENOUGH_USERS
                ? inGroupCount.toString()
                : formatPercentage(anomaly.roleClusterStats().frequency());
        var isUnique = inGroupCount == 1;
        List<ExplanationResult> explanations = new ArrayList<>();
        if (isUnique) {
            explanations.add(makeExplanation(
                    new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.uniqueAccess", new Object[] {repoCoverage}),
                    List.of(OutlierDetectionExplanationCategory.UNUSUAL_ACCESS))
            );
        } else {
            explanations.add(makeExplanation(
                    new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.unusualAccess", new Object[] {groupCoverage, repoCoverage}),
                    List.of(OutlierDetectionExplanationCategory.UNUSUAL_ACCESS))
            );
        }
        if (!anomaly.unusualAttributes().isEmpty()) {
            var localizedAttributesList = anomaly.unusualAttributes().stream()
                    .map(this::localizeAttributePair)
                    .toList();
            var localizedAttributes = joinMessage(localizedAttributesList, COMMA);
            explanations.add(makeExplanation(
                    new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.irregularAttributes", new Object[] {localizedAttributes}),
                    List.of(OutlierDetectionExplanationCategory.IRREGULAR_ATTRIBUTES))
            );
        }
        return explanations;
    }

    public OutlierExplanationResult explain() {
        var anomalyExplanations = outlier.anomalies()
                .stream()
                .map(anomaly -> {
                    var explanations = explainAnomaly(anomaly);
                    return new AnomalyExplanationResult(anomaly.id(), explanations);
                })
                .toList();
        var overallExplanation = explainOverallOutlier();
        var shortExplanation = explainOverallOutlierShort();
        return new OutlierExplanationResult(outlier.id(), overallExplanation, shortExplanation, anomalyExplanations);
    }

}
