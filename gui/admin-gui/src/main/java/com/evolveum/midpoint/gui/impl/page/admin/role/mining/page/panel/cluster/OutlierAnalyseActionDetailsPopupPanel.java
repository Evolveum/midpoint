/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;

import java.util.*;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableDouble;
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
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutlierAnalyseActionDetailsPopupPanel extends BasePanel<String> implements Popupable {

    List<String> elements;
    Map<String, RoleAnalysisAttributeStatistics> map;
    String userOid;
    String clusterOid;
    int minMembers;
    int anomalyAssignmentCount = 0;

    public OutlierAnalyseActionDetailsPopupPanel(String id,
            IModel<String> messageModel,
            String userOid,
            @NotNull String clusterOid,
            int minMembers) {
        super(id, messageModel);
        this.userOid = userOid;
        this.clusterOid = clusterOid;
        this.minMembers = minMembers;
    }

    //TODO just for testing case (remove later)
    @Override
    protected void onInitialize() {
        super.onInitialize();

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Idk");
        OperationResult result = task.getResult();

        PrismObject<RoleAnalysisClusterType> originalCluster = roleAnalysisService.getClusterTypeObject(clusterOid, task, result);
        List<String> outliersMembers = new ArrayList<>();

        if (originalCluster == null) {
            LOGGER.error("Cluster with oid {} not found", clusterOid);
            return;
        }

        ObjectReferenceType roleAnalysisSessionRef = originalCluster.asObjectable().getRoleAnalysisSessionRef();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService.getSessionTypeObject(
                roleAnalysisSessionRef.getOid(), task, result);

        if (sessionTypeObject == null) {
            LOGGER.error("Session with oid {} not found", roleAnalysisSessionRef.getOid());
            return;
        }

        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        RangeType propertiesRange = userModeOptions.getPropertiesRange();
        Integer minMembersCount = userModeOptions.getMinMembersCount();
        RoleAnalysisDetectionOptionType defaultDetectionOption = session.getDefaultDetectionOption();

        double minFrequency = 2;
        double maxFrequency = 2;

        if (defaultDetectionOption != null) {
            if (defaultDetectionOption.getFrequencyRange() != null) {
                RangeType frequencyRange = defaultDetectionOption.getFrequencyRange();
                if (frequencyRange.getMin() != null) {
                    minFrequency = frequencyRange.getMin().intValue();
                }
                if (frequencyRange.getMax() != null) {
                    maxFrequency = frequencyRange.getMax().intValue();
                }
            }
        }

        List<ObjectReferenceType> member = originalCluster.asObjectable().getMember();
        for (ObjectReferenceType objectReferenceType : member) {
            outliersMembers.add(objectReferenceType.getOid());
        }

        double minThreshold = 0.5;

        ListMultimap<List<String>, String> chunkMap = roleAnalysisService.loadUserForOutlierComparison(
                roleAnalysisService,
                outliersMembers,
                propertiesRange.getMin().intValue(), propertiesRange.getMax().intValue(),
                userModeOptions.getQuery(), result, task);

        MutableDouble usedFrequency = new MutableDouble(minThreshold);

        elements = roleAnalysisService.findJaccardCloseObject(userOid,
                chunkMap,
                usedFrequency,
                outliersMembers, minThreshold, minMembersCount, task,
                result
        );
        elements.add(userOid);

        DisplayValueOption displayValueOption = new DisplayValueOption();
        displayValueOption.setProcessMode(RoleAnalysisProcessModeType.USER);
        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
        displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
        displayValueOption.setChunkAction(RoleAnalysisChunkAction.EXPLORE_DETECTION);
        RoleAnalysisClusterType cluster = new RoleAnalysisClusterType();
        for (String element : elements) {
            cluster.getMember().add(new ObjectReferenceType()
                    .oid(element).type(UserType.COMPLEX_TYPE));
        }

        RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
        detectionOption.setFrequencyRange(new RangeType().min(minFrequency).max(maxFrequency));
        cluster.setDetectionOption(detectionOption);

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareMiningStructure(cluster, displayValueOption,
                RoleAnalysisProcessModeType.USER, result, task);

        RangeType frequencyRange = detectionOption.getFrequencyRange();
        Double sensitivity = detectionOption.getSensitivity();

        RoleAnalysisSortMode sortMode = displayValueOption.getSortMode();
        if (sortMode == null) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
            sortMode = RoleAnalysisSortMode.NONE;
        }

        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(sortMode);

        if (frequencyRange != null) {
            roleAnalysisService.resolveOutliersZScore(roles, frequencyRange, sensitivity);
        }

        for (MiningRoleTypeChunk role : roles) {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            FrequencyItem.Status status = frequencyItem.getStatus();
            if (status == FrequencyItem.Status.NEGATIVE_EXCLUDE) {
                anomalyAssignmentCount++;
            }
        }

        RoleAnalysisUserBasedTable table = loadTable(miningOperationChunk, displayValueOption, cluster);
        add(table);

        RepeatingView headerItems = new RepeatingView("header-items");
        headerItems.setOutputMarkupId(true);
        add(headerItems);

        initOutlierAnalysisHeaderPanel(headerItems, userOid, usedFrequency, originalCluster.asObjectable());
    }

    @NotNull
    private RoleAnalysisUserBasedTable loadTable(
            MiningOperationChunk miningOperationChunk,
            DisplayValueOption displayValueOption,
            @NotNull RoleAnalysisClusterType cluster) {
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
        return table;
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
        return null;
    }

    public Map<String, RoleAnalysisAttributeStatistics> getMap() {
        return map;
    }

    public void setMap(@Nullable Map<String, RoleAnalysisAttributeStatistics> map) {
        this.map = map;
    }

    private void initOutlierAnalysisHeaderPanel(RepeatingView headerItems,
            String userOid,
            MutableDouble usedFrequency,
            RoleAnalysisClusterType cluster) {
        int similarObjectCount = elements.size() - 1;

        List<ObjectReferenceType> clusterMembers = cluster.getMember();

        String outlierTypeIdentification = "unknown";
        for (ObjectReferenceType clusterMember : clusterMembers) {
            String oid = clusterMember.getOid();
            if (oid.equals(userOid)) {
                outlierTypeIdentification = clusterMember.getDescription();
                break;
            }
        }

        InfoBoxModel infoBoxResolvedPatterns = new InfoBoxModel(
                GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Similar users",
                String.valueOf(similarObjectCount),
                100,
                "Number of similar users");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxResolvedPatterns)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }

        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(
                GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Threshold",
                String.valueOf(usedFrequency.doubleValue()),
                100,
                "Threshold for similarity between users");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);

        InfoBoxModel anomalyAssignmentLabelModel = new InfoBoxModel(
                GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Assignment anomalies",
                String.valueOf(anomalyAssignmentCount),
                100,
                "Number of users with assignment anomalies");

        RoleAnalysisInfoBox anomalyAssignmentLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(anomalyAssignmentLabelModel)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        anomalyAssignmentLabel.add(AttributeModifier.replace("class", "col-md-6"));
        anomalyAssignmentLabel.setOutputMarkupId(true);
        headerItems.add(anomalyAssignmentLabel);

        InfoBoxModel outlierType = new InfoBoxModel(
                GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Outlier type",
                String.valueOf(outlierTypeIdentification),
                100,
                "Reason for outlier identification");

        RoleAnalysisInfoBox outlierTypeLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(outlierType)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        outlierTypeLabel.add(AttributeModifier.replace("class", "col-md-6"));
        outlierTypeLabel.setOutputMarkupId(true);
        headerItems.add(outlierTypeLabel);
    }

}
