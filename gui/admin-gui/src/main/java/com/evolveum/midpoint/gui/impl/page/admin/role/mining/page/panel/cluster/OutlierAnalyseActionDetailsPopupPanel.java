/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter.jacquardSimilarity;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

import java.util.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisInfoBox;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.RoleAnalysisUserBasedTable;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutlierAnalyseActionDetailsPopupPanel extends BasePanel<String> implements Popupable {

    List<PrismObject<UserType>> elements;

    Map<String, RoleAnalysisAttributeStatistics> map;
    String userOid;
    String clusterOid;
    int minMembers;
    int anomalyAssignmentCount = 0;

    public OutlierAnalyseActionDetailsPopupPanel(String id,
            IModel<String> messageModel,
            String userOid,
            String clusterOid,
            int minMembers) {
        super(id, messageModel);
        this.userOid = userOid;
        this.clusterOid = clusterOid;
        this.minMembers = minMembers;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        PageBase pageBase = getPageBase();
        ModelService modelService = pageBase.getModelService();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Idk");
        OperationResult result = task.getResult();

        PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService.getClusterTypeObject(clusterOid, task, result);
        List<String> outliersMembers = new ArrayList<>();
        List<ObjectReferenceType> member = clusterTypeObject.asObjectable().getMember();
        for (ObjectReferenceType objectReferenceType : member) {
            outliersMembers.add(objectReferenceType.getOid());
        }

        Double jaccardFrequencyMetric = getJaccardFrequencyMetric(userOid, roleAnalysisService,
                modelService, null, task, result, outliersMembers, 0.5, 10);

        elements = getJaccardCloseObject(userOid, roleAnalysisService,
                modelService, null, task, result, outliersMembers, jaccardFrequencyMetric);

        DisplayValueOption displayValueOption = new DisplayValueOption();
        displayValueOption.setProcessMode(RoleAnalysisProcessModeType.USER);
        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
        displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
        displayValueOption.setChunkAction(RoleAnalysisChunkAction.EXPLORE_DETECTION);
        RoleAnalysisClusterType cluster = new RoleAnalysisClusterType();
        for (PrismObject<UserType> element : elements) {
            cluster.getMember().add(new ObjectReferenceType()
                    .oid(element.getOid()).type(UserType.COMPLEX_TYPE).targetName(element.getName().getOrig()));
        }

        RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
        detectionOption.setFrequencyRange(new RangeType().min(2.0).max(2.0));
        cluster.setDetectionOption(detectionOption);

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareMiningStructure(cluster, displayValueOption,
                RoleAnalysisProcessModeType.USER, result, task);

        RangeType frequencyRange = detectionOption.getFrequencyRange();

        RoleAnalysisSortMode sortMode = displayValueOption.getSortMode();
        if (sortMode == null) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
            sortMode = RoleAnalysisSortMode.NONE;
        }

        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(sortMode);

        if (frequencyRange != null) {
            roleAnalysisService.resolveOutliersZScore(roles, frequencyRange.getMin(), frequencyRange.getMax());
        }

        for (MiningRoleTypeChunk role : roles) {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            FrequencyItem.Status status = frequencyItem.getStatus();
            if (status == FrequencyItem.Status.NEGATIVE_EXCLUDE) {
                anomalyAssignmentCount++;
            }
        }

        RoleAnalysisUserBasedTable table = new RoleAnalysisUserBasedTable(
                "table",
                miningOperationChunk,
                new ArrayList<>(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected DisplayValueOption load() {
                        return displayValueOption;
                    }
                }, cluster.asPrismObject()) {

            @Override
            public boolean isOutlierDetection() {
                return true;
            }

            @Override
            protected Set<String> getMarkMemberObjects() {
                Set<String> markObjects = new HashSet<>();
                markObjects.add(userOid);
                return markObjects;
            }
        };

        table.setOutputMarkupId(true);
        add(table);

        RepeatingView headerItems = new RepeatingView("header-items");
        headerItems.setOutputMarkupId(true);
        add(headerItems);

        initOutlierAnalysisHeaderPanel(headerItems);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    public int getHeight() {
        return 90;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
//        if (processModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
//            return new StringResourceModel("RoleMining.members.details.panel.title.roles");
//        }
//        return new StringResourceModel("RoleMining.members.details.panel.title.users");
        return null;
    }

    public Map<String, RoleAnalysisAttributeStatistics> getMap() {
        return map;
    }

    public void setMap(@Nullable Map<String, RoleAnalysisAttributeStatistics> map) {
        this.map = map;
    }

    int counterHandlet = 0;
    double foundThreshold = 0;

    public List<PrismObject<UserType>> getJaccardCloseObject(double threshold,
            String userOid,
            RoleAnalysisService roleAnalysisService,
            ModelService modelService,
            ObjectQuery query,
            Task task,
            OperationResult result) {
        List<PrismObject<UserType>> elements = new ArrayList<>();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return elements;
        }
        elements.add(userTypeObject);

        PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService.getClusterTypeObject(clusterOid, task, result);
        if (clusterTypeObject == null) {
            return elements;
        }
        List<String> outliersMembers = new ArrayList<>();
        List<ObjectReferenceType> member = clusterTypeObject.asObjectable().getMember();
        for (ObjectReferenceType objectReferenceType : member) {
            outliersMembers.add(objectReferenceType.getOid());
        }

        List<String> similarUserOidSet = new ArrayList<>();
        Map<Double, Integer> similarityStats = new TreeMap<>(Collections.reverseOrder());

        UserType userObject = userTypeObject.asObjectable();
        List<String> userRolesToCompare = getRolesOidAssignment(userObject);

        //TODO store assignments and user oid? whats about heap? Search takes a lot of time
        //TODO it is not nessesary to search all users, and is not nessesary to search twice
        ResultHandler<UserType> resultHandler = (user, lResult) -> {
            try {
                //TODO think about this ignore outliers?
                // Whats if we want to compare with them?
                // Whats if cluster is not generated because missing one object?
                // Im not sure about it
                if (!outliersMembers.contains(user.getOid()) && !user.getOid().equals(userOid)) {
                    List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
                    double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, rolesOidAssignment);
                    jacquardSimilarity = Math.floor(jacquardSimilarity * 10) / 10.0;

                    if (similarityStats.containsKey(jacquardSimilarity)) {
                        similarityStats.put(jacquardSimilarity, similarityStats.get(jacquardSimilarity) + 1);
                    } else {
                        similarityStats.put(jacquardSimilarity, 1);
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(user.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        ResultHandler<UserType> resultHandler2 = (user, lResult) -> {
            try {
                //TODO think about this ignore outliers?
                // Whats if we want to compare with them?
                // Whats if cluster is not generated because missing one object?
                // Im not sure about it
                if (!outliersMembers.contains(user.getOid()) && !user.getOid().equals(userOid)) {
                    List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
                    double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, rolesOidAssignment);
                    if (jacquardSimilarity >= foundThreshold) {
                        elements.add(user);
                        similarUserOidSet.add(user.getOid());
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(user.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve role members: " + userOid + ": " + ex.getMessage(), ex);
        } finally {
            result.recomputeStatus();
        }

        for (Map.Entry<Double, Integer> entry : similarityStats.entrySet()) {
            Integer value = entry.getValue();
            if (value >= minMembers) {
                foundThreshold = entry.getKey();
                break;
            }
        }

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler2, null,
                    task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve role members: " + userOid + ": " + ex.getMessage(), ex);
        } finally {
            result.recomputeStatus();
        }

        return elements;
    }

    private void initOutlierAnalysisHeaderPanel(RepeatingView headerItems) {

        int similarObjectCount = elements.size() - 1;
        double usedThreshold = foundThreshold;

        InfoBoxModel infoBoxResolvedPatterns = new InfoBoxModel(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Similar users",
                String.valueOf(similarObjectCount),
                100,
                "Number of similar users");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxResolvedPatterns)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }

        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Threshold",
                String.valueOf(usedThreshold),
                100,
                "Threshold for similarity between users");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);

        InfoBoxModel anomalyAssignmentLabelModel = new InfoBoxModel(GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Assignment anomalies",
                String.valueOf(anomalyAssignmentCount),
                100,
                "Number of users with assignment anomalies");

        RoleAnalysisInfoBox anomalyAssignmentLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(anomalyAssignmentLabelModel)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        anomalyAssignmentLabel.add(AttributeModifier.replace("class", "col-md-6"));
        anomalyAssignmentLabel.setOutputMarkupId(true);
        headerItems.add(anomalyAssignmentLabel);
    }

    public static List<PrismObject<UserType>> getJaccardCloseObject(String userOid,
            RoleAnalysisService roleAnalysisService,
            ModelService modelService,
            ObjectQuery query,
            Task task,
            OperationResult result,
            List<String> outliersMembers, Double jaccardFrequencyMetric) {

        List<PrismObject<UserType>> elements = new ArrayList<>();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return elements;
        }
        elements.add(userTypeObject);

        List<String> similarUserOidSet = new ArrayList<>();

        UserType userObject = userTypeObject.asObjectable();
        List<String> userRolesToCompare = getRolesOidAssignment(userObject);

        //TODO store assignments and user oid? whats about heap? Search takes a lot of time
        //TODO it is not nessesary to search all users, and is not nessesary to search twice

        ResultHandler<UserType> resultHandler2 = (user, lResult) -> {
            try {
                //TODO think about this ignore outliers?
                // Whats if we want to compare with them?
                // Whats if cluster is not generated because missing one object?
                // Im not sure about it
                if (!outliersMembers.contains(user.getOid()) && !user.getOid().equals(userOid)) {
                    List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
                    double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, rolesOidAssignment);
                    if (jacquardSimilarity >= jaccardFrequencyMetric) {
                        elements.add(user);
                        similarUserOidSet.add(user.getOid());
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(user.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler2, null,
                    task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve role members: " + userOid + ": " + ex.getMessage(), ex);
        } finally {
            result.recomputeStatus();
        }

        return elements;
    }

    public static Double getJaccardFrequencyMetric(String userOid,
            @NotNull RoleAnalysisService roleAnalysisService,
            ModelService modelService,
            ObjectQuery query,
            Task task,
            OperationResult result,
            List<String> outliersMembers,
            double minThreshold,
            int minMembers) {

        List<PrismObject<UserType>> elements = new ArrayList<>();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return 1.0;
        }
        elements.add(userTypeObject);

        Map<Double, Integer> similarityStats = new TreeMap<>(Collections.reverseOrder());

        UserType userObject = userTypeObject.asObjectable();
        List<String> userRolesToCompare = getRolesOidAssignment(userObject);

        //TODO store assignments and user oid? whats about heap? Search takes a lot of time
        //TODO it is not nessesary to search all users, and is not nessesary to search twice
        ResultHandler<UserType> resultHandler = (user, lResult) -> {
            try {
                //TODO think about this ignore outliers?
                // Whats if we want to compare with them?
                // Whats if cluster is not generated because missing one object?
                // Im not sure about it
                if (!outliersMembers.contains(user.getOid()) && !user.getOid().equals(userOid)) {
                    List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
                    double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, rolesOidAssignment);
                    jacquardSimilarity = Math.floor(jacquardSimilarity * 10) / 10.0;
                    if (jacquardSimilarity >= minThreshold) {
                        if (similarityStats.containsKey(jacquardSimilarity)) {
                            similarityStats.put(jacquardSimilarity, similarityStats.get(jacquardSimilarity) + 1);
                        } else {
                            similarityStats.put(jacquardSimilarity, 1);
                        }
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(user.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve role members: " + userOid + ": " + ex.getMessage(), ex);
        } finally {
            result.recomputeStatus();
        }

        for (Map.Entry<Double, Integer> entry : similarityStats.entrySet()) {
            Integer value = entry.getValue();
            if (value >= minMembers) {
                return entry.getKey();
            }
        }

        return 1.0;
    }

}
