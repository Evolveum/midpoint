/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.simple.Tools.*;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.BusinessRoleApplicationDto;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectionActionExecutor;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.MiningIntersectionTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.MiningRoleBasedTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.MiningUserBasedTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareExpandStructure;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
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
    private static final String ID_PROCESS_BUTTON = "process_selections_id";
    OperationResult result = new OperationResult("GetObject");

    DetectionOption detectionOption;
    List<DetectedPattern> detectedPatternList = new ArrayList<>();
    AjaxButton processButton;
    DetectedPattern detectedPattern = null;
    List<ObjectReferenceType> reductionObjects = new ArrayList<>();
    MiningOperationChunk miningOperationChunk;
    List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
    List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
    SORT sortMode;
    RoleAnalysisProcessModeType processMode;
    boolean compress = true;

    public PageClusterOperationsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        PrismObject<RoleAnalysisSessionType> getParent = getParentClusterByOid((PageBase) getPage(),
                cluster.getRoleAnalysisSessionRef().getOid(), result);
        reductionObjects = cluster.getResolvedPattern();

        if (reductionObjects != null && !reductionObjects.isEmpty()) {
            for (ObjectReferenceType referenceType : reductionObjects) {
                if (referenceType.getOid() == null) {
                    reductionObjects = new ArrayList<>();
                }
            }
        }

        processMode = getParent.asObjectable().getProcessMode();
        miningRoleTypeChunks = new ArrayList<>();
        miningUserTypeChunks = new ArrayList<>();

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        int max = Math.max(clusterStatistics.getRolesCount(), clusterStatistics.getUsersCount());

        if (max <= 500) {
            sortMode = SORT.JACCARD;
        } else {
            sortMode = SORT.NONE;
        }

        if (cluster.getDetectionOption() != null) {
            detectionOption = loadDetectionOption(cluster.getDetectionOption());
        } else {
            detectionOption = new DetectionOption(30, 100, 10, 10);
        }

        long start = startTimer("LOAD DATA");
        detectedPatternList = transformDefaultPattern(cluster);
        loadMiningTableData(sortMode);
        endTimer(start, "LOAD DATA");

        initOperationPart();
    }

    private void initOperationPart() {

        long start = startTimer("LOAD TABLE");
        loadMiningTable(miningRoleTypeChunks, miningUserTypeChunks);
        endTimer(start, "LOAD TABLE");

        Component component = generateTableIntersection(ID_DATATABLE_INTERSECTIONS, detectedPatternList);
        component.setOutputMarkupId(true);
        add(component);

        AjaxButton ajaxButton = executeBusinessSearchPanel();
        add(ajaxButton);

        AjaxButton sortButton = executeJaccardSorting();
        add(sortButton);

        initializeProcessButton();
        add(processButton);
    }

    private void initializeProcessButton() {
        processButton = new AjaxButton(ID_PROCESS_BUTTON,
                createStringResource("RoleMining.button.title.process")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                List<AssignmentType> roleAssignments = new ArrayList<>();

                for (MiningRoleTypeChunk roleChunk : miningRoleTypeChunks) {
                    if (roleChunk.getStatus().equals(Status.ADD)) {
                        for (String roleOid : roleChunk.getRoles()) {
                            PrismObject<RoleType> roleObject = getRoleTypeObject(getPageBase(), roleOid, result);
                            if (roleObject != null) {
                                roleAssignments.add(ObjectTypeUtil.createAssignmentTo(roleOid, ObjectTypes.ROLE));
                            }
                        }
                    }
                }

                PrismObject<RoleType> businessRole = generateBusinessRole((PageBase) getPage(), roleAssignments, "");

                List<BusinessRoleApplicationDto> roleApplicationDtos = new ArrayList<>();

                for (MiningUserTypeChunk userChunk : miningUserTypeChunks) {
                    if (userChunk.getStatus().equals(Status.ADD)) {
                        for (String userOid : userChunk.getUsers()) {
                            PrismObject<UserType> userObject = getUserTypeObject(getPageBase(), userOid, result);
                            if (userObject != null) {
                                String clusterOid = getObjectDetailsModels().getObjectType().getOid();
                                roleApplicationDtos.add(new BusinessRoleApplicationDto(clusterOid, userObject,
                                        businessRole, getPageBase()));
                            }
                        }
                    }
                }

                PageRole pageRole = new PageRole(businessRole, roleApplicationDtos);
                setResponsePage(pageRole);
            }
        };

        processButton.setOutputMarkupId(true);
        processButton.setOutputMarkupPlaceholderTag(true);
        processButton.setVisible(false);
    }

    @NotNull
    private AjaxButton executeBusinessSearchPanel() {
        AjaxButton ajaxButton = new AjaxButton("business_role_mining") {
            @Override
            public void onClick(AjaxRequestTarget target) {

                PrismContainerWrapperModel<RoleAnalysisClusterType, RoleAnalysisDetectionOptionType> detectionOptionsModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), RoleAnalysisClusterType.F_DETECTION_OPTION);

                ExecuteDetectionPanel detailsPanel = new ExecuteDetectionPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"), detectionOptionsModel, detectionOption) {
                    @Override
                    public void performAction(AjaxRequestTarget target, DetectionOption newDetectionOption) {
                        detectedPattern = null;

                        detectedPatternList = new DetectionActionExecutor(newDetectionOption).executeDetection(miningRoleTypeChunks,
                                miningUserTypeChunks, processMode);
                        detectionOption = newDetectionOption;

                        getIntersectionTable().replaceWith(generateTableIntersection(ID_DATATABLE_INTERSECTIONS,
                                detectedPatternList));
                        target.add(getIntersectionTable().setOutputMarkupId(true));

                        updateMiningTable(target, true, miningRoleTypeChunks, miningUserTypeChunks);

                        replaceRoleAnalysisClusterDetection(getObjectDetailsModels().getObjectWrapper().getOid(), getPageBase(),
                                result,
                                detectedPatternList, processMode, newDetectionOption);
                    }

                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }
        };
        ajaxButton.setOutputMarkupId(true);
        return ajaxButton;
    }

    private AjaxButton executeJaccardSorting() {

        AjaxButton ajaxButton = new AjaxButton("jaccard_sort") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                sortMode = SORT.JACCARD;

                miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(sortMode);
                miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(sortMode);

                updateMiningTable(target, true, miningRoleTypeChunks, miningUserTypeChunks);
                target.add(this.setVisible(false));
            }
        };

        ajaxButton.setOutputMarkupId(true);
        ajaxButton.setVisible(sortMode.equals(SORT.NONE));
        return ajaxButton;
    }

    private void loadMiningTableData(SORT sortMode) {
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        String stateHandler = "";
        if (compress) {
            miningOperationChunk = new PrepareChunkStructure().executeOperation(cluster, true,
                    processMode, (PageBase) getPage(), result, stateHandler);

        } else {
            miningOperationChunk = new PrepareExpandStructure().executeOperation(cluster, true,
                    processMode, (PageBase) getPage(), result, stateHandler);
        }
        this.miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(sortMode);
        this.miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(sortMode);

    }

    private void loadMiningTable(List<MiningRoleTypeChunk> miningRoleTypeChunks, List<MiningUserTypeChunk> miningUserTypeChunks) {
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            MiningRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false,
                    detectionOption.getMinFrequencyThreshold(),
                    null,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            add(boxedTablePanel);
        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            MiningUserBasedTable boxedTablePanel = generateMiningUserBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false,
                    detectionOption.getMinFrequencyThreshold(),
                    null,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            add(boxedTablePanel);
        }

    }

    private void updateMiningTable(AjaxRequestTarget target, boolean resetStatus,
            List<MiningRoleTypeChunk> miningRoleTypeChunks, List<MiningUserTypeChunk> miningUserTypeChunks) {

        if (resetStatus) {
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                miningRoleTypeChunk.setStatus(Status.NEUTRAL);
            }
            for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
                miningUserTypeChunk.setStatus(Status.NEUTRAL);
            }
        }

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            MiningRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false,
                    detectionOption.getMinFrequencyThreshold(),
                    detectedPattern,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            getMiningRoleBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningRoleBasedTable().setOutputMarkupId(true));

        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {

            MiningUserBasedTable boxedTablePanel = generateMiningUserBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false,
                    detectionOption.getMinFrequencyThreshold(),
                    detectedPattern,
                    detectionOption.getMaxFrequencyThreshold());
            boxedTablePanel.setOutputMarkupId(true);
            getMiningUserBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningUserBasedTable().setOutputMarkupId(true));
        }

    }

    public MiningUserBasedTable generateMiningUserBasedTable(List<MiningRoleTypeChunk> roles,
            List<MiningUserTypeChunk> users, boolean sortable, double frequency, DetectedPattern intersection, double maxFrequency) {
        return new MiningUserBasedTable(ID_DATATABLE, roles, users, sortable, frequency / 100, intersection, maxFrequency / 100, reductionObjects) {
            @Override
            public void resetTable(AjaxRequestTarget target) {
                updateMiningTable(target, false, miningRoleTypeChunks, miningUserTypeChunks);
            }

            @Override
            protected String getCompressStatus() {
                return !compress ? CHUNK.EXPAND.getDisplayString() : CHUNK.COMPRESS.getDisplayString();
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
                compress = !compress;
                loadMiningTableData(sortMode);
                updateMiningTable(ajaxRequestTarget, false, miningRoleTypeChunks, miningUserTypeChunks);
                ajaxRequestTarget.add(this);
            }
        };
    }

    public MiningRoleBasedTable generateMiningRoleBasedTable(List<MiningRoleTypeChunk> roles,
            List<MiningUserTypeChunk> users, boolean sortable, double minFrequency, DetectedPattern intersection,
            double maxFrequency) {
        return new MiningRoleBasedTable(ID_DATATABLE,
                roles, users,
                minFrequency / 100, maxFrequency / 100,
                intersection,
                reductionObjects,
                sortable) {
            @Override
            public void resetTable(AjaxRequestTarget target) {
                updateMiningTable(target, false, miningRoleTypeChunks, miningUserTypeChunks);
            }

            @Override
            protected String getCompressStatus() {
                return !compress ? CHUNK.EXPAND.getDisplayString() : CHUNK.COMPRESS.getDisplayString();
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
                compress = !compress;
                loadMiningTableData(sortMode);
                updateMiningTable(ajaxRequestTarget, false, miningRoleTypeChunks, miningUserTypeChunks);
                ajaxRequestTarget.add(this);
            }
        };
    }

    public Component generateTableIntersection(String id, List<DetectedPattern> miningSets) {

        MiningIntersectionTable components = new MiningIntersectionTable(id, miningSets, processMode) {
            @Override
            protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {
                detectedPattern = rowModel.getObject();

                updateMiningTable(ajaxRequestTarget, true, miningRoleTypeChunks, miningUserTypeChunks);
                processButton.setVisible(true);
                ajaxRequestTarget.add(processButton);
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
