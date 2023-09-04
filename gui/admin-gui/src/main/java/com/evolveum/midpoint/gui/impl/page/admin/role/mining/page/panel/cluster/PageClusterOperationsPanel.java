/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.getSessionTypeObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.Tools.getScaleScript;

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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareExpandStructure;
import com.evolveum.midpoint.model.api.ModelService;
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
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
public class PageClusterOperationsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_DATATABLE = "datatable_extra";
    private final OperationResult result = new OperationResult("GetObject");
    private DetectedPattern analysePattern = null;
    private MiningOperationChunk miningOperationChunk;
    private RoleAnalysisSortMode roleAnalysisSortMode;
    private boolean isRoleMode;
    private boolean compress = true;

    public PageClusterOperationsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        Task task = ((PageBase) getPage()).createSimpleTask("loadObject");
        ModelService modelService = ((PageBase) getPage()).getModelService();

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        PrismObject<RoleAnalysisSessionType> getParent = getSessionTypeObject(modelService, result,
                cluster.getRoleAnalysisSessionRef().getOid(), task);

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
        ModelService modelService = ((PageBase) getPage()).getModelService();

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();

        RoleAnalysisProcessModeType processMode;
        if (isRoleMode) {
            processMode = RoleAnalysisProcessModeType.ROLE;
        } else {
            processMode = RoleAnalysisProcessModeType.USER;
        }

        if (compress) {
            miningOperationChunk = new PrepareChunkStructure().executeOperation(cluster, true,
                    processMode, modelService, result, task);

        } else {
            miningOperationChunk = new PrepareExpandStructure().executeOperation(cluster, true,
                    processMode, modelService, result, task);
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

    private void updateMiningTable(AjaxRequestTarget target, boolean resetStatus) {

        RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
            return;
        }

        List<MiningRoleTypeChunk> simpleMiningRoleTypeChunks = miningOperationChunk.getSimpleMiningRoleTypeChunks();

        List<MiningUserTypeChunk> simpleMiningUserTypeChunks = miningOperationChunk.getSimpleMiningUserTypeChunks();

        if (resetStatus) {
            for (MiningRoleTypeChunk miningRoleTypeChunk : simpleMiningRoleTypeChunks) {
                miningRoleTypeChunk.setStatus(RoleAnalysisOperationMode.NEUTRAL);
            }
            for (MiningUserTypeChunk miningUserTypeChunk : simpleMiningUserTypeChunks) {
                miningUserTypeChunk.setStatus(RoleAnalysisOperationMode.NEUTRAL);
            }
        }

        if (isRoleMode) {
            RoleAnalysisRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(
                    analysePattern
            );
            boxedTablePanel.setOutputMarkupId(true);
            getMiningRoleBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningRoleBasedTable().setOutputMarkupId(true));

        } else {

            RoleAnalysisUserBasedTable boxedTablePanel = generateMiningUserBasedTable(
                    analysePattern
            );
            boxedTablePanel.setOutputMarkupId(true);
            getMiningUserBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
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
                        getPageBase().hideMainPopup(ajaxRequestTarget);
                        analysePattern = rowModel.getObject();
                        updateMiningTable(ajaxRequestTarget, true);
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
                return !compress ? RoleAnalysisChunkMode.EXPAND.getDisplayString() : RoleAnalysisChunkMode.COMPRESS.getDisplayString();
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
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
                        getPageBase().hideMainPopup(ajaxRequestTarget);
                        analysePattern = rowModel.getObject();
                        updateMiningTable(ajaxRequestTarget, true);
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
