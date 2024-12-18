package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierDetectionExplanationCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierDetectionExplanationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.LocalizableMessageList.*;

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
            int partitionCount
    ) {}

    public record AnomalyExplanationResult(
            Long id,
            List<OutlierDetectionExplanationType> explanations
    ) {}

    public record OutlierExplanationResult(
            Long id,
            OutlierDetectionExplanationType explanation,
            List<AnomalyExplanationResult> anomalies
    ) {}

    private final OutlierExplanationInput outlier;
    private static final int FEW_ENOUGH_USERS = 3;
    private static final LocalizableMessage COLON = LocalizableMessageBuilder.buildFallbackMessage(": ");
    private static final LocalizableMessage AND = joinMessage(List.of(SPACE, new SingleLocalizableMessage("OutlierDetection.explanation.common.andSeparator"), SPACE), null);
    private static final DecimalFormat PERCENTAGE_FORMAT_WHOLE = new DecimalFormat("#");
    private static final DecimalFormat PERCENTAGE_FORMAT_FRACTION = new DecimalFormat("#.#");

    public OutlierExplanationResolver(OutlierExplanationInput outlier) {
        this.outlier = outlier;
    }

    private OutlierDetectionExplanationType makeExplanation(LocalizableMessage message, List<OutlierDetectionExplanationCategoryType> categories) {
        var explanation = new OutlierDetectionExplanationType();
        explanation.message(LocalizationUtil.createLocalizableMessageType(message));
        explanation.getCategory().addAll(categories);
        return explanation;
    }

    private static LocalizableMessage joinMessage(List<LocalizableMessage> messages, LocalizableMessage separator) {
        return new LocalizableMessageList(messages, separator, null, null);
    }

    private OutlierDetectionExplanationType explainOverallOutlier() {
        var anomalyCount = outlier.anomalies().size();
        var unusualAttributeCount = outlier.anomalies().stream()
                .flatMap(ano -> ano.unusualAttributes().stream().map(ExplanationAttribute::path))
                .collect(Collectors.toSet())
                .size();
        List<OutlierDetectionExplanationCategoryType> categories = new ArrayList<>();
        LocalizableMessage finalMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.unusualAccess", new Object[] {anomalyCount});
        categories.add(OutlierDetectionExplanationCategoryType.UNUSUAL_ACCESS);
        if (unusualAttributeCount > 0) {
            var unusualAttributeMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.irregularAttributes", new Object[] {unusualAttributeCount});
            finalMessage = joinMessage(List.of(finalMessage, unusualAttributeMessage), COMMA);
            categories.add(OutlierDetectionExplanationCategoryType.IRREGULAR_ATTRIBUTES);
        }
        if (!outlier.groupByAttributes().isEmpty()) {
            var localizedAttributes = outlier.groupByAttributes().stream()
                    .map(groupByAttribute -> {
                        var attributeName = localize(groupByAttribute);
                        var attributeValue = LocalizableMessageBuilder.buildFallbackMessage(groupByAttribute.value());
                        return joinMessage(List.of(attributeName, attributeValue), COLON);
                    })
                    .toList();
            var localizedAttributesMessage = joinMessage(localizedAttributes, AND);
            var groupByMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.groupByExplanation", new Object[] {localizedAttributesMessage});
            finalMessage = joinMessage(List.of(finalMessage, groupByMessage), SPACE);
        }
        if (outlier.partitionCount() >  1) {
            var partitionMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.overPartitions", new Object[] {outlier.partitionCount()});
            finalMessage = joinMessage(List.of(finalMessage, partitionMessage), SPACE);
        }
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

    private List<OutlierDetectionExplanationType> explainAnomaly(AnomalyExplanationInput anomaly) {
        var inGroupCount = anomaly.roleClusterStats().memberCount();
        var repoCoverage = formatPercentage(anomaly.roleOverallStats().frequency());
        var groupCoverage = inGroupCount <= FEW_ENOUGH_USERS
                ? inGroupCount.toString()
                : formatPercentage(anomaly.roleClusterStats().frequency());
        var isUnique = inGroupCount == 1;
        List<OutlierDetectionExplanationType> explanations = new ArrayList<>();
        if (isUnique) {
            explanations.add(makeExplanation(
                    new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.uniqueAccess", new Object[] {repoCoverage}),
                    List.of(OutlierDetectionExplanationCategoryType.UNUSUAL_ACCESS))
            );
        } else {
            explanations.add(makeExplanation(
                    new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.unusualAccess", new Object[] {groupCoverage, repoCoverage}),
                    List.of(OutlierDetectionExplanationCategoryType.UNUSUAL_ACCESS))
            );
        }
        if (!anomaly.unusualAttributes().isEmpty()) {
            var localizedAttributesList = anomaly.unusualAttributes().stream()
                    .map(this::localizeAttributePair)
                    .toList();
            var localizedAttributes = joinMessage(localizedAttributesList, COMMA);
            explanations.add(makeExplanation(
                    new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.irregularAttributes", new Object[] {localizedAttributes}),
                    List.of(OutlierDetectionExplanationCategoryType.IRREGULAR_ATTRIBUTES))
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
        return new OutlierExplanationResult(outlier.id(), overallExplanation, anomalyExplanations);
    }

}
