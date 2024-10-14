package com.evolveum.midpoint.model.impl.mining.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates outlier detection on RBAC generated dataset which contains outlier labels.
 * Metrics: F1 score, precision, recall
 */
public class DebugOutlierDetectionEvaluation {

    public record ConfusionMatrix(
            Long tp,
            Long tn,
            Long fp,
            Long fn
    ) {}

    private record PredictionRecord(
            String oid,
            String name,
            Boolean groundTruth,
            Boolean prediction
    ) {}

    // named outlier: identified by name
    private static final List<String> NAMED_OUTLIER_PREFIXES = Arrays.asList("Matuzalem", "Jumper", "Zombie", "Mask");

    // noise outlier: identified by noise role, see InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole
    private static final List<String> NOISE_ROLES_OIDS = Arrays.asList(
            "c368b9a1-3c58-4d6f-9f86-a23ccf8a4f06",
            "6e42c7ab-4c75-4c17-bf69-63049315680c",
            "f659fe15-9e98-4468-9e7d-80eabe6253c9",
            "f3e4d45c-d311-4f8b-99da-a96313ec7eb0",
            "dd36aaa5-d671-4a5d-b2c0-3af937f5db0c",
            "62231b07-af48-4dfb-8250-a40f13994d0c"
    );

    private final String sessionOid;
    private final ModelService modelService;
    private final RoleAnalysisService roleAnalysisService;
    private final Task task;
    private RoleAnalysisSessionType session;

    private ConfusionMatrix confusionMatrix;
    private double precision, recall, f1score;

    public DebugOutlierDetectionEvaluation(
            String sessionOid,
            ModelService modelService,
            RoleAnalysisService roleAnalysisService,
            Task parentTask
    ) {
        this.sessionOid = sessionOid;
        this.modelService = modelService;
        this.roleAnalysisService = roleAnalysisService;
        task = parentTask.createSubtask();
    }

    public DebugOutlierDetectionEvaluation evaluate() throws Exception {
        session = getSession(sessionOid);
        var users = getSessionUsers(sessionOid);
        var innerOutliers = getOutliers(sessionOid, OutlierClusterCategoryType.INNER_OUTLIER);
        var outerOutliers = getOutliers(sessionOid, OutlierClusterCategoryType.OUTER_OUTLIER);
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
                "\n  sessionOid='" + sessionOid + "'" +
                ",\n  sessionName=" + session.getName().toString() +
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

    private List<PredictionRecord> labelData(List<UserType> users, List<UserType> outliers) throws Exception {
        var usersWithNoise = getUsersWithNoiseRoleOids();
        return users
                .stream()
                .map(user -> {
                    var name = user.getName().toString();
                    var isNamedOutlier = NAMED_OUTLIER_PREFIXES.stream().anyMatch(user.getName().toString()::startsWith);
                    var isNoiseRoleOutlier = usersWithNoise.contains(user.getOid());
                    var groundTruth = isNamedOutlier || isNoiseRoleOutlier;
                    var prediction = outliers.stream().anyMatch(o -> o.getOid().equals(user.getOid()));
                    return new PredictionRecord(user.getOid(), name, groundTruth, prediction);
                })
                .toList();
    }

    private List<String> getUsersWithNoiseRoleOids() throws Exception {
        var query = modelService.getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(NOISE_ROLES_OIDS.toArray(new String[0]))
                .endBlock()
                .build();
        return modelService
                .searchObjects(UserType.class, query, null, task, task.getResult())
                .stream()
                .map(u -> u.asObjectable().getOid())
                .toList();
    }

    private List<UserType> getSessionUsers(String sessionOid) throws Exception {
        // NOTE: it is currently difficult to query users the same way as in session, therefore extracting from all clusters
        var clusters = getClusters(sessionOid);
        return clusters
                .stream()
                .flatMap(cluster -> cluster.getMember().stream())
                .map(ref -> this.getUser(ref.getOid()))
                .toList();
    }

    private List<UserType> getOutliers(String sessionOid, OutlierClusterCategoryType category) throws Exception {
        return roleAnalysisService.getSessionOutliers(sessionOid, category, task, task.getResult())
                .stream()
                .map(outlier -> this.getUser(outlier.getObjectRef().getOid()))
                .toList();
    }

    private List<RoleAnalysisClusterType> getClusters(String sessionOid) throws Exception {
        var query = modelService.getPrismContext().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(sessionOid)
                .build();
        return modelService
                .searchObjects(RoleAnalysisClusterType.class, query, null, task, task.getResult())
                .stream()
                .map(result -> result.asObjectable())
                .toList();
    }

    private UserType getUser(String userOid) {
        try {
            return modelService.getObject(UserType.class, userOid, null, task, task.getResult()).asObjectable();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RoleAnalysisSessionType getSession(String sessionOid) throws Exception {
        return modelService.getObject(RoleAnalysisSessionType.class, sessionOid, null, task, task.getResult()).getValue().asObjectable();
    }


}
