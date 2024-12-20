/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ExplanationUtil {

    //TODO update outlier objectExplanation
    public static @Nullable PrismObject<RoleAnalysisOutlierType> uploadOutlierExplanation(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull String userOid,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<RoleAnalysisOutlierType> outlierPrismObject = roleAnalysisService.searchOutlierObjectByUserOid(
                userOid, task, result);

        int finalPartitionCount = computePartitionCount(outlierPrismObject);

        OutlierExplanationResolver.OutlierExplanationInput outlierExplanationInput = OutlierExplanationUtil
                .prepareOutlierExplanationInput(roleAnalysisService, partition, finalPartitionCount, task, result);

        OutlierExplanationResolver.OutlierExplanationResult outlierExplanationResult = new OutlierExplanationResolver(
                outlierExplanationInput).explain();

        List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();

        loadExplanationIntoDetectedAnomalyResult(outlierExplanationResult, detectedAnomalyResult);
        partition.getExplanation().add(outlierExplanationResult.explanation());
        return outlierPrismObject;
    }

    private static void loadExplanationIntoDetectedAnomalyResult(
            OutlierExplanationResolver.@NotNull OutlierExplanationResult outlierExplanationResult,
            @NotNull List<DetectedAnomalyResult> detectedAnomalyResult) {
        List<OutlierExplanationResolver.AnomalyExplanationResult> anomaliesExplanation = outlierExplanationResult.anomalies();
        for (DetectedAnomalyResult anomalyResult : detectedAnomalyResult) {
            anomaliesExplanation.stream()
                    .filter(a -> a.id().equals(anomalyResult.getId()))
                    .findFirst()
                    .ifPresent(a -> anomalyResult.getExplanation().addAll(a.explanations()));
        }
    }

    private static int computePartitionCount(@Nullable PrismObject<RoleAnalysisOutlierType> outlierPrismObject) {
        if (outlierPrismObject == null) {
            return 1;
        }

        RoleAnalysisOutlierType outlierObject = outlierPrismObject.asObjectable();
        List<RoleAnalysisOutlierPartitionType> partitions = outlierObject.getPartition();
        return 1 + partitions.size();
    }

    protected static PrismObjectDefinition<UserType> getUserDefinition() {
        return PrismContext.get()
                .getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    @SuppressWarnings("rawtypes")
    protected static ItemDefinition getUserItemDefinition(@NotNull ItemPathType itemPath) {
        getUserDefinition().findItemDefinition(itemPath.getItemPath());
        return getUserDefinition().findItemDefinition(itemPath.getItemPath());
    }

    protected static List<RoleAnalysisAttributeAnalysis> getUserAttributeAnalysis(@NotNull DetectedAnomalyResult result) {
        return result.getStatistics()
                .getAttributeAnalysis()
                .getUserAttributeAnalysisResult()
                .getAttributeAnalysis();
    }

    protected static Stream<RoleAnalysisAttributeStatistics> getUnusualAttributes(@NotNull RoleAnalysisAttributeAnalysis analysis) {
        return analysis.getAttributeStatistics().stream()
                .filter(attribute -> Boolean.TRUE.equals(attribute.getIsUnusual()));
    }

    @Contract("_, _, _, _, _, _ -> new")
    protected static OutlierExplanationResolver.@NotNull ExplanationAttribute createExplanationAttribute(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisAttributeStatistics attribute,
            ItemPathType itemPath,
            ItemDefinition<?> userItemDefinition,
            @NotNull Task task,
            @NotNull OperationResult result) {

        String attributeValue = attribute.getAttributeValue();
        String displayAttributeValue = resolveAttributeValueRealName(roleAnalysisService, userItemDefinition, attributeValue,
                task, result);
        return new OutlierExplanationResolver.ExplanationAttribute(
                itemPath,
                userItemDefinition,
                displayAttributeValue);
    }

    //check for better solution
    public static String resolveAttributeValueRealName(
            @NotNull RoleAnalysisService service,
            @NotNull ItemDefinition<?> def,
            @NotNull String value,
            @NotNull Task task,
            @NotNull OperationResult result) {

        QName itemTargetType = def.getTypeName();
        if (!itemTargetType.equals(ObjectReferenceType.COMPLEX_TYPE)) {
            return value;
        }

        String itemValue = value;
        UUID uuid = UUID.fromString(value);
        PrismObject<FocusType> focusTypeObject = service.getFocusTypeObject(uuid.toString(), task, result);
        if (focusTypeObject == null) {
            return itemValue;
        }

        FocusType focusObject = focusTypeObject.asObjectable();

        if (focusObject.getName() != null) {
            itemValue = focusTypeObject.getName().getOrig();
        }

        return itemValue;
    }
}
