/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.RoleAnalysisMatrixTable;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "clusterDetails")
@PanelInstance(
        identifier = "clusterDetails",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.operationsPanel",
                icon = GuiStyleConstants.CLASS_ICON_TASK_RESULTS,
                order = 20
        )
)
public class RoleAnalysisClusterOperationPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_DATATABLE = "datatable_extra";
    private static final String DOT_CLASS = RoleAnalysisClusterOperationPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    private boolean isRoleMode;

    private LoadableDetachableModel<DisplayValueOption> displayValueOptionModel;

    public static final String PARAM_CANDIDATE_ROLE_ID = "candidateRoleId";
    public static final String PARAM_DETECTED_PATER_ID = "detectedPatternId";
    public static final String PARAM_TABLE_SETTING = "tableSetting";
    boolean isOutlierDetection = false;
    Set<String> markedUsers = new HashSet<>();


    public Integer getParameterTableSetting() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
        if (!stringValue.isNull()) {
            return Integer.valueOf(stringValue.toString());
        }
        return null;
    }

    public RoleAnalysisClusterOperationPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECTS);
        OperationResult result = task.getResult();

        RoleAnalysisProcessModeType mode;
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        PrismObject<RoleAnalysisSessionType> getParent = roleAnalysisService.
                getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);
        if (getParent != null) {
            RoleAnalysisSessionType session = getParent.asObjectable();
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
            if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
                isOutlierDetection = true;
            }
            mode = analysisOption.getProcessMode();
            isRoleMode = mode.equals(RoleAnalysisProcessModeType.ROLE);
        }

        if (isOutlierDetection) {
            markedUsers = new HashSet<>();
            List<RoleAnalysisOutlierType> searchResultList = roleAnalysisService.findClusterOutliers(
                    cluster, task, result);
            if (searchResultList != null && !searchResultList.isEmpty()) {
                for (RoleAnalysisOutlierType outlier : searchResultList) {
                    ObjectReferenceType targetObjectRef = outlier.getTargetObjectRef();
                    if (targetObjectRef != null && targetObjectRef.getOid() != null) {
                        markedUsers.add(targetObjectRef.getOid());
                    }
                }
            }
        }

        loadDisplayValueOptionModel();

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer(ID_MAIN_PANEL);
        webMarkupContainer.setOutputMarkupId(true);

        loadMiningTable(webMarkupContainer);

        add(webMarkupContainer);

    }

    private void loadDisplayValueOptionModel() {
        displayValueOptionModel = new LoadableDetachableModel<>() {
            @Override
            protected @NotNull DisplayValueOption load() {

                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

                DisplayValueOption displayValueOption = new DisplayValueOption();
                displayValueOption.setChunkMode(RoleAnalysisChunkMode.COMPRESS);

                displayValueOption.setProcessMode(isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER);

                Integer parameterTableSetting = getParameterTableSetting();
                if (parameterTableSetting != null && parameterTableSetting == 1) {
                    displayValueOption.setFullPage(true);
                }

                Integer rolesCount = clusterStatistics.getRolesCount();
                Integer usersCount = clusterStatistics.getUsersCount();

                if (rolesCount == null || usersCount == null) {
                    displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
                } else {

                    int maxRoles;
                    int maxUsers;

                    if (isRoleMode) {
                        maxRoles = 20;
                        maxUsers = 13;
                    } else {
                        maxRoles = 13;
                        maxUsers = 20;
                    }
                    int max = Math.max(rolesCount, usersCount);

                    if (max <= 500) {
                        displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
                    } else {
                        displayValueOption.setSortMode(RoleAnalysisSortMode.FREQUENCY);
                    }

                    if (rolesCount > maxRoles && usersCount > maxUsers) {
                        displayValueOption.setChunkMode(RoleAnalysisChunkMode.COMPRESS);
                    } else if (rolesCount > maxRoles) {
                        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND_USER);
                    } else if (usersCount > maxUsers) {
                        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND_ROLE);
                    } else {
                        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
                    }
                }

                return displayValueOption;
            }
        };
    }





    private <B extends MiningBaseTypeChunk, A extends  MiningBaseTypeChunk> void loadMiningTable(WebMarkupContainer webMarkupContainer) {
        RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
            return;
        }

        RoleAnalysisMatrixTable<B, A> boxedTablePanel = generateMiningTable();
        boxedTablePanel.setOutputMarkupId(true);
        webMarkupContainer.add(boxedTablePanel);

    }


    public <B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> RoleAnalysisMatrixTable<B, A> generateMiningTable() {

        return new RoleAnalysisMatrixTable<>(
                ID_DATATABLE,
//                analysePattern,
                displayValueOptionModel,
                this::getObjectWrapperObject,
                isRoleMode) {

            @Override
            protected Set<String> getMarkMemberObjects() {
                if (isOutlierDetection()) {
                    return markedUsers;
                }

                return null;
            }

            @Override
            public boolean isOutlierDetection() {
                return isOutlierDetection;
            }


        };
    }

}
