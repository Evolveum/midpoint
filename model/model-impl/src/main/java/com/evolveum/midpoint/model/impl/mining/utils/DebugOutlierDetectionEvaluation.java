package com.evolveum.midpoint.model.impl.mining.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierClusterCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Evaluates outlier detection on RBAC generated dataset which contains outlier labels.
 */
public class DebugOutlierDetectionEvaluation {

    public record ConfusionMatrix(
            Long tp,
            Long tn,
            Long fp,
            Long fn
    ) {}

    private record PredictionRecord(
            String name,
            Boolean groundTruth,
            Boolean prediction
    ) {}

    private final List<String> OUTLIER_PREFIXES = Arrays.asList("Matuzalem", "Jumper", "Zombie", "Mask", "Irregular");
    private final String sessionId;
    private final ModelService modelService;
    private final RoleAnalysisService roleAnalysisService;
    private final Task task;

    private ConfusionMatrix confusionMatrix;
    private double precision, recall, f1score;

    public DebugOutlierDetectionEvaluation(
            String sessionId,
            ModelService modelService,
            RoleAnalysisService roleAnalysisService,
            Task parentTask
    ) {
        this.sessionId = sessionId;
        this.modelService = modelService;
        this.roleAnalysisService = roleAnalysisService;
        task = parentTask.createSubtask();
    }

    public DebugOutlierDetectionEvaluation evaluate() throws Exception {
        var users = getAllUsers();
        var innerOutliers = getOutliers(sessionId, OutlierClusterCategoryType.INNER_OUTLIER);
        var outerOutliers = getOutliers(sessionId, OutlierClusterCategoryType.OUTER_OUTLIER);
        var outliers = Stream.concat(innerOutliers.stream(), outerOutliers.stream()).toList();

        var records = labelData(users, outliers);

        confusionMatrix = computeConfusionMatrix(records);
        var tp = confusionMatrix.tp().doubleValue();
        var fn = confusionMatrix.fn().doubleValue();
        var fp = confusionMatrix.fp().doubleValue();
        recall = tp / (tp + fn);
        precision = tp / (tp + fp);
        f1score = 2.0 * (precision * recall) / (precision + recall);
        return this;
    }

    public ConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }

    public Double getF1Score() {
        return f1score;
    }

    public Double getRecall() {
        return recall;
    }

    public Double getPrecision() {
        return precision;
    }

    @Override
    public String toString() {
        return "DebugOutlierDetectionEvaluation{" +
                "\n  OUTLIER_PREFIXES=" + OUTLIER_PREFIXES +
                ",\n  sessionId='" + sessionId + "'" +
                ",\n  confusionMatrix=" + confusionMatrix +
                ",\n  precision=" + precision +
                ",\n  recall=" + recall +
                ",\n  f1score=" + f1score +
                "\n}";
    }

    private ConfusionMatrix computeConfusionMatrix(List<PredictionRecord> records) {
        var tp = records.stream().filter(r -> r.groundTruth() && r.prediction()).count();
        var tn = records.stream().filter(r -> !r.groundTruth() && !r.prediction()).count();
        var fp = records.stream().filter(r -> !r.groundTruth() && r.prediction()).count();
        var fn = records.stream().filter(r -> r.groundTruth() && !r.prediction()).count();
        return new ConfusionMatrix(tp, tn, fp, fn);
    }

    private List<PredictionRecord> labelData(List<String> users, List<String> outliers) {
        return users
                .stream()
                .map(name -> {
                    var groundTruth = OUTLIER_PREFIXES.stream().anyMatch(name::startsWith);
                    var prediction = outliers.contains(name);
                    return new PredictionRecord(name, groundTruth, prediction);
                })
                .toList();
    }

    private List<String> getAllUsers() throws Exception {
        return modelService
                .searchObjects(UserType.class, null, null, task, task.getResult())
                .stream()
                .map(result -> result.asObjectable().getName().toString())
                .toList();
    }

    private List<String> getOutliers(String sessionOid, OutlierClusterCategoryType category) throws Exception {
        return roleAnalysisService.getSessionOutliers(sessionOid, category, task, task.getResult())
                .stream()
                .map(outlier -> {
                    try {
                        var userOid = outlier.getTargetObjectRef().getOid();
                        var user = modelService.getObject(UserType.class, userOid, null, task, task.getResult()).asObjectable();
                        return user.getName().toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

}
