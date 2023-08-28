/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.loadDetectionOption;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.Tools.*;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.MiningIntersectionTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.MiningRoleBasedTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.MiningUserBasedTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareExpandStructure;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
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
                order = 1
        )
)
public class PageClusterOperationsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_DATATABLE = "datatable_extra";
    private static final String ID_DATATABLE_INTERSECTIONS = "table_intersection";

    private final OperationResult result = new OperationResult("GetObject");
    private DetectionOption detectionOption;
    private DetectedPattern detectedPattern = null;
    private List<ObjectReferenceType> reductionObjects = new ArrayList<>();
    private MiningOperationChunk miningOperationChunk;
    private RoleAnalysisSortMode roleAnalysisSortMode;
    private RoleAnalysisProcessModeType processMode;
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
        reductionObjects = cluster.getResolvedPattern();

        if (reductionObjects != null && !reductionObjects.isEmpty()) {
            for (ObjectReferenceType referenceType : reductionObjects) {
                if (referenceType.getOid() == null) {
                    reductionObjects = new ArrayList<>();
                }
            }
        }

        if (getParent != null) {
            processMode = getParent.asObjectable().getProcessMode();
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        int max = Math.max(clusterStatistics.getRolesCount(), clusterStatistics.getUsersCount());

        if (max <= 500) {
            roleAnalysisSortMode = RoleAnalysisSortMode.JACCARD;
        } else {
            roleAnalysisSortMode = RoleAnalysisSortMode.NONE;
        }

        if (cluster.getDetectionOption() != null) {
            detectionOption = loadDetectionOption(cluster.getDetectionOption());
        } else {
            detectionOption = new DetectionOption(30, 100, 10, 10);
        }

        List<DetectedPattern> detectedPatternList = transformDefaultPattern(cluster);
        loadMiningTableData();
        loadMiningTable();

        Component component = generateTableIntersection(ID_DATATABLE_INTERSECTIONS, detectedPatternList);
        component.setOutputMarkupId(true);
        add(component);
    }

    private void loadMiningTableData() {

        Task task = ((PageBase) getPage()).createSimpleTask("loadMiningTableData");
        ModelService modelService = ((PageBase) getPage()).getModelService();

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        if (compress) {
            miningOperationChunk = new PrepareChunkStructure().executeOperation(cluster, true,
                    processMode, modelService, result, task);

        } else {
            miningOperationChunk = new PrepareExpandStructure().executeOperation(cluster, true,
                    processMode, modelService, result, task);
        }

    }

    private void loadMiningTable() {
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            MiningRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(
                    detectionOption.getMinFrequencyThreshold(),
                    null,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            add(boxedTablePanel);
        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            MiningUserBasedTable boxedTablePanel = generateMiningUserBasedTable(
                    detectionOption.getMinFrequencyThreshold(),
                    null,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            add(boxedTablePanel);
        }

    }

    private void updateMiningTable(AjaxRequestTarget target, boolean resetStatus) {

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

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            MiningRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(
                    detectionOption.getMinFrequencyThreshold(),
                    detectedPattern,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            getMiningRoleBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningRoleBasedTable().setOutputMarkupId(true));

        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {

            MiningUserBasedTable boxedTablePanel = generateMiningUserBasedTable(
                    detectionOption.getMinFrequencyThreshold(),
                    detectedPattern,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            getMiningUserBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningUserBasedTable().setOutputMarkupId(true));
        }

    }

    public MiningUserBasedTable generateMiningUserBasedTable(double frequency, DetectedPattern intersection, double maxFrequency) {
        return new MiningUserBasedTable(ID_DATATABLE, miningOperationChunk, frequency / 100,
                intersection, maxFrequency / 100,
                reductionObjects, roleAnalysisSortMode, getObjectWrapperObject()) {
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
        };
    }

    public MiningRoleBasedTable generateMiningRoleBasedTable(double minFrequency, DetectedPattern intersection,
            double maxFrequency) {

        return new MiningRoleBasedTable(ID_DATATABLE,
                miningOperationChunk,
                minFrequency / 100, maxFrequency / 100,
                intersection,
                reductionObjects,
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
        };
    }

    public Component generateTableIntersection(String id, List<DetectedPattern> miningSets) {

        MiningIntersectionTable components = new MiningIntersectionTable(id, miningSets, processMode) {
            @Override
            protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {
                detectedPattern = rowModel.getObject();

                updateMiningTable(ajaxRequestTarget, true);
            }
        };

        components.setOutputMarkupId(true);
        return components;
    }

    protected Component getIntersectionTable() {
        return get(((PageBase) getPage()).createComponentPath(ID_DATATABLE_INTERSECTIONS));
    }

    protected MiningRoleBasedTable getMiningRoleBasedTable() {
        return (MiningRoleBasedTable) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE));
    }

    protected MiningUserBasedTable getMiningUserBasedTable() {
        return (MiningUserBasedTable) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}