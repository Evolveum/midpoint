/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisRoleBasedTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisUserBasedTable;
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
    private final OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);
    private DetectedPattern analysePattern = null;
    private MiningOperationChunk miningOperationChunk;
    private RoleAnalysisSortMode roleAnalysisSortMode;
    private boolean isRoleMode;
    private boolean compress = true;

    public RoleAnalysisClusterOperationPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        Task task = ((PageBase) getPage()).createSimpleTask("loadObject");

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        PrismObject<RoleAnalysisSessionType> getParent = getPageBase().getRoleAnalysisService().
                getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);

        if (getParent != null) {
            isRoleMode = getParent.asObjectable().getProcessMode().equals(RoleAnalysisProcessModeType.ROLE);
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        int max = Math.max(clusterStatistics.getRolesCount(), clusterStatistics.getUsersCount());

        if (max <= 500) {
            roleAnalysisSortMode = RoleAnalysisSortMode.JACCARD;
        } else {
            roleAnalysisSortMode = RoleAnalysisSortMode.FREQUENCY;
        }

        loadMiningTableData();

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer(ID_MAIN_PANEL);
        webMarkupContainer.setOutputMarkupId(true);

        loadMiningTable(webMarkupContainer);
        add(webMarkupContainer);

    }

    private void loadMiningTableData() {
        Task task = ((PageBase) getPage()).createSimpleTask("loadMiningTableData");

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();

        RoleAnalysisProcessModeType processMode;
        if (isRoleMode) {
            processMode = RoleAnalysisProcessModeType.ROLE;
        } else {
            processMode = RoleAnalysisProcessModeType.USER;
        }

        if (compress) {
            miningOperationChunk = getPageBase().getRoleAnalysisService().prepareCompressedMiningStructure(cluster, true,
                    processMode, result, task);

        } else {
            miningOperationChunk = getPageBase().getRoleAnalysisService().prepareExpandedMiningStructure(cluster, true,
                    processMode, result, task);
        }

    }

    private void loadMiningTable(WebMarkupContainer webMarkupContainer) {
        RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
            return;
        }

        if (isRoleMode) {
            RoleAnalysisRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(
                    null
            );
            boxedTablePanel.setOutputMarkupId(true);
            webMarkupContainer.add(boxedTablePanel);
        } else {
            RoleAnalysisUserBasedTable boxedTablePanel = generateMiningUserBasedTable(
                    null
            );
            boxedTablePanel.setOutputMarkupId(true);
            webMarkupContainer.add(boxedTablePanel);
        }

    }

    //TODO - check reset
    private void updateMiningTable(AjaxRequestTarget target, boolean resetStatus) {
        RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
            return;
        }

        if (resetStatus) {

            List<MiningRoleTypeChunk> simpleMiningRoleTypeChunks = miningOperationChunk.getSimpleMiningRoleTypeChunks();

            List<MiningUserTypeChunk> simpleMiningUserTypeChunks = miningOperationChunk.getSimpleMiningUserTypeChunks();


            for (MiningRoleTypeChunk miningRoleTypeChunk : simpleMiningRoleTypeChunks) {
                miningRoleTypeChunk.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
            for (MiningUserTypeChunk miningUserTypeChunk : simpleMiningUserTypeChunks) {
                miningUserTypeChunk.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

        if (isRoleMode) {
            RoleAnalysisRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(
                    analysePattern
            );
            boxedTablePanel.setOutputMarkupId(true);
            getMiningRoleBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(applyTableScaleScript());
            target.add(getMiningRoleBasedTable().setOutputMarkupId(true));

        } else {

            RoleAnalysisUserBasedTable boxedTablePanel = generateMiningUserBasedTable(
                    analysePattern
            );
            boxedTablePanel.setOutputMarkupId(true);
            getMiningUserBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(applyTableScaleScript());
            target.add(getMiningUserBasedTable().setOutputMarkupId(true));
        }

    }

    public RoleAnalysisUserBasedTable generateMiningUserBasedTable(DetectedPattern selectedPattern) {

        return new RoleAnalysisUserBasedTable(ID_DATATABLE, miningOperationChunk,
                selectedPattern,
                roleAnalysisSortMode,
                getObjectWrapperObject()) {
            @Override
            public void resetTable(AjaxRequestTarget target) {
                updateMiningTable(target, false);
            }

            @Override
            protected String getCompressStatus() {
                return !compress ? RoleAnalysisChunkMode.EXPAND.getDisplayString() : RoleAnalysisChunkMode.COMPRESS.getDisplayString();
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
                roleAnalysisSortMode = getMiningUserBasedTable().getRoleAnalysisSortMode();
                compress = !compress;
                loadMiningTableData();
                updateMiningTable(ajaxRequestTarget, false);
            }

            @Override
            protected void showDetectedPatternPanel(AjaxRequestTarget target) {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                List<DetectedPattern> detectedPatternList = transformDefaultPattern(cluster);
                PatternDetailsPanel detailsPanel = new PatternDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"), detectedPatternList, cluster) {
                    @Override
                    public void onLoadPerform(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {
                        RoleAnalysisUserBasedTable miningUserBasedTable = getMiningUserBasedTable();
                        analysePattern = rowModel.getObject();
                        miningUserBasedTable.loadDetectedPattern(ajaxRequestTarget, analysePattern);
                        getPageBase().hideMainPopup(ajaxRequestTarget);
                    }
                };

                getPageBase().showMainPopup(detailsPanel, target);
            }
        };
    }

    public RoleAnalysisRoleBasedTable generateMiningRoleBasedTable(DetectedPattern intersection) {

        return new RoleAnalysisRoleBasedTable(ID_DATATABLE,
                miningOperationChunk,
                intersection,
                roleAnalysisSortMode, getObjectWrapperObject()) {
            @Override
            public void resetTable(AjaxRequestTarget target) {
                updateMiningTable(target, false);
            }

            @Override
            protected String getCompressStatus() {
                return !compress
                        ? RoleAnalysisChunkMode.EXPAND.getDisplayString()
                        : RoleAnalysisChunkMode.COMPRESS.getDisplayString();
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
                roleAnalysisSortMode = getMiningRoleBasedTable().getRoleAnalysisSortMode();
                compress = !compress;
                loadMiningTableData();
                updateMiningTable(ajaxRequestTarget, false);
                ajaxRequestTarget.add(this);
            }

            @Override
            protected void showDetectedPatternPanel(AjaxRequestTarget target) {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                List<DetectedPattern> detectedPatternList = transformDefaultPattern(cluster);
                PatternDetailsPanel detailsPanel = new PatternDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"), detectedPatternList, cluster) {
                    @Override
                    public void onLoadPerform(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {
                        RoleAnalysisRoleBasedTable miningRoleBasedTable = getMiningRoleBasedTable();
                        analysePattern = rowModel.getObject();
                        miningRoleBasedTable.loadDetectedPattern(ajaxRequestTarget, analysePattern);
                        getPageBase().hideMainPopup(ajaxRequestTarget);
                    }
                };

                getPageBase().showMainPopup(detailsPanel, target);
            }
        };
    }

    protected RoleAnalysisRoleBasedTable getMiningRoleBasedTable() {
        return (RoleAnalysisRoleBasedTable) get(((PageBase) getPage()).createComponentPath(ID_MAIN_PANEL, ID_DATATABLE));
    }

    protected RoleAnalysisUserBasedTable getMiningUserBasedTable() {
        return (RoleAnalysisUserBasedTable) get(((PageBase) getPage()).createComponentPath(ID_MAIN_PANEL, ID_DATATABLE));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }
}
