package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
            List<AnomalyExplanationInput> anomalies,
            ExplanationAttribute groupByAttribute
    ) {}

    public record AnomalyExplanationResult(
            Long id,
            LocalizableMessage explanation
    ) {}

    public record OutlierExplanationResult(
            Long id,
            LocalizableMessage explanation,
            List<AnomalyExplanationResult> anomalies
    ) {}

    private final OutlierExplanationInput outlier;
    private static final int FEW_ENOUGH_USERS = 3;
    private static final DecimalFormat PERCENTAGE_FORMAT_WHOLE = new DecimalFormat("#");
    private static final DecimalFormat PERCENTAGE_FORMAT_FRACTION = new DecimalFormat("#.#");

    public OutlierExplanationResolver(OutlierExplanationInput outlier) {
        this.outlier = outlier;
    }

    private LocalizableMessage explainOverallOutlier() {
        var anomalyCount = outlier.anomalies().size();
        var unusualAttributeCount = outlier.anomalies().stream()
                .mapToInt(a -> a.unusualAttributes().size())
                .sum();
        List<LocalizableMessage> summaryMessages = new ArrayList<>();
        summaryMessages.add(new SingleLocalizableMessage("OutlierDetection.explanation.outlier.unusualAccess", new Object[] {anomalyCount}));
        if (unusualAttributeCount > 0) {
            summaryMessages.add(new SingleLocalizableMessage("OutlierDetection.explanation.outlier.irregularAttributes", new Object[] {unusualAttributeCount}));
        }
        var summaryMessage = new LocalizableMessageList(summaryMessages, LocalizableMessageList.COMMA, null, null);
        if (outlier.groupByAttribute() == null) {
            return summaryMessage;
        }

        var attributeName = localize(outlier.groupByAttribute());
        var attributeValue = outlier.groupByAttribute().value();
        var groupByMessage = new SingleLocalizableMessage("OutlierDetection.explanation.outlier.groupByExplanation", new Object[] {attributeName, attributeValue});
        return new LocalizableMessageList(List.of(summaryMessage, groupByMessage), LocalizableMessageList.SPACE, null, null);
    }

    private Object localize(ExplanationAttribute explanationAttribute) {
        var def = explanationAttribute.def();
        if (def != null) {
            if (StringUtils.isNotEmpty(def.getDisplayName())) {
                return new SingleLocalizableMessage(def.getDisplayName(), new Object[0], def.getDisplayName());
            }
            if (StringUtils.isNotEmpty(def.getItemName().getLocalPart())) {
                return StringUtils.isNotEmpty(def.getItemName().getLocalPart());
            }
        }
        return explanationAttribute.path().toString();
    }

    private String formatPercentage(double value) {
        var percentage = value * 100;
        var rounded = value < 0.01 ? PERCENTAGE_FORMAT_FRACTION.format(percentage) : PERCENTAGE_FORMAT_WHOLE.format(percentage);
        return rounded + "%";
    }

    private LocalizableMessage explainAnomaly(AnomalyExplanationInput anomaly) {
        var inGroupCount = anomaly.roleClusterStats().memberCount();
        var repoCoverage = formatPercentage(anomaly.roleOverallStats().frequency());
        var groupCoverage = inGroupCount <= FEW_ENOUGH_USERS
                ? inGroupCount.toString()
                : formatPercentage(anomaly.roleClusterStats().frequency());
        var isUnique = inGroupCount == 1;
        if (isUnique) {
            return new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.uniqueAccess", new Object[] {repoCoverage});
        }
        return new SingleLocalizableMessage("OutlierDetection.explanation.anomaly.unusualAccess", new Object[] {groupCoverage, repoCoverage});
    }

    public OutlierExplanationResult explain() {
        var anomalyExplanations = outlier.anomalies()
                .stream()
                .map(anomaly -> {
                    var anomalyExplanation = explainAnomaly(anomaly);
                    return new AnomalyExplanationResult(anomaly.id(), anomalyExplanation);
                })
                .toList();
        var overallExplanation = explainOverallOutlier();
        return new OutlierExplanationResult(outlier.id(), overallExplanation, anomalyExplanations);
    }

}
