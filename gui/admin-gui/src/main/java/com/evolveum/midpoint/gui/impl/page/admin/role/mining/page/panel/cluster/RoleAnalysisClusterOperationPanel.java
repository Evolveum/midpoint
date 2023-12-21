/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidInducements;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.initRoleBasedDetectionPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.initUserBasedDetectionPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
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

import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Nullable;

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
    private List<DetectedPattern> analysePattern = null;
    private MiningOperationChunk miningOperationChunk;
    private RoleAnalysisSortMode roleAnalysisSortMode;
    private boolean isRoleMode;
    private boolean compress = true;

    public static final String PARAM_CANDIDATE_ROLE_ID = "candidateRoleId";
    public static final String PARAM_DETECTED_PATER_ID = "detectedPatternId";

    public List<String> getCandidateRoleContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
        if (!stringValue.isNull()) {
            String[] split = stringValue.toString().split(",");
            return Arrays.asList(split);
        }
        return null;
    }

    public Long getDetectedPatternContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_DETECTED_PATER_ID);
        if (!stringValue.isNull()) {
            return Long.valueOf(stringValue.toString());
        }
        return null;
    }

    public RoleAnalysisClusterOperationPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = ((PageBase) getPage()).createSimpleTask(OP_PREPARE_OBJECTS);
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        PrismObject<RoleAnalysisSessionType> getParent = roleAnalysisService.
                getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);

        if (getCandidateRoleContainerId() != null) {
            loadCandidateRole(cluster, roleAnalysisService, task);
        } else if (getDetectedPatternContainerId() != null) {
            loadDetectedPattern(cluster);
        }

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

        loadMiningTable(webMarkupContainer, analysePattern);
        add(webMarkupContainer);

    }

    private void loadCandidateRole(RoleAnalysisClusterType cluster,
            RoleAnalysisService roleAnalysisService,
            Task task) {
        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

        analysePattern = new ArrayList<>();
        for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {

            List<String> candidateRoleContainerId = getCandidateRoleContainerId();
            for (String candidateRoleId : candidateRoleContainerId) {

                if (candidateRoleId.equals(candidateRole.getId().toString())) {
                    String roleOid = candidateRole.getCandidateRoleRef().getOid();
                    PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(
                            roleOid, task, result);
                    List<String> rolesOidInducements;
                    if (rolePrismObject == null) {
                        return;
                    }
                    rolesOidInducements = getRolesOidInducements(rolePrismObject);
                    List<String> rolesOidAssignment = getRolesOidAssignment(rolePrismObject.asObjectable());

                    Set<String> accessOidSet = new HashSet<>(rolesOidInducements);
                    accessOidSet.addAll(rolesOidAssignment);

                    ListMultimap<String, String> mappedMembers = roleAnalysisService.extractUserTypeMembers(new HashMap<>(),
                            null,
                            Collections.singleton(roleOid),
                            task,
                            result);

                    List<ObjectReferenceType> candidateMembers = candidateRole.getCandidateMembers();
                    Set<String> membersOidSet = new HashSet<>();
                    for (ObjectReferenceType candidateMember : candidateMembers) {
                        String oid = candidateMember.getOid();
                        if (oid != null) {
                            membersOidSet.add(oid);
                        }
                    }

                    membersOidSet.addAll(mappedMembers.get(roleOid));
                    double clusterMetric = accessOidSet.size() * membersOidSet.size();

                    DetectedPattern pattern = new DetectedPattern(
                            accessOidSet,
                            membersOidSet,
                            clusterMetric,
                            null);
                    pattern.setIdentifier(rolePrismObject.getName().getOrig());
                    pattern.setId(candidateRole.getId());

                    analysePattern.add(pattern);
                }
            }
        }
    }

    private void loadDetectedPattern(RoleAnalysisClusterType cluster) {
        List<RoleAnalysisDetectionPatternType> detectedPattern = cluster.getDetectedPattern();

        for (RoleAnalysisDetectionPatternType pattern : detectedPattern) {
            Long id = pattern.getId();
            if (getDetectedPatternContainerId().equals(id)) {
                analysePattern = Collections.singletonList(transformPattern(pattern));
            }
        }
    }

    private void loadMiningTableData() {
        Task task = ((PageBase) getPage()).createSimpleTask(OP_PREPARE_OBJECTS);
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();

        RoleAnalysisProcessModeType processMode;
        if (isRoleMode) {
            processMode = RoleAnalysisProcessModeType.ROLE;
        } else {
            processMode = RoleAnalysisProcessModeType.USER;
        }

        if (compress) {
            miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, true,
                    processMode, result, task);

        } else {
            miningOperationChunk = roleAnalysisService.prepareExpandedMiningStructure(cluster, true,
                    processMode, result, task);
        }

        RoleAnalysisDetectionOptionType detectionOption = cluster.getDetectionOption();

        RangeType frequencyRange = detectionOption.getFrequencyRange();

        double minFrequency = 0;
        double maxFrequency = 0;
        if (frequencyRange != null) {
            minFrequency = frequencyRange.getMin() / 100;
            maxFrequency = frequencyRange.getMax() / 100;
        }

        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

        if (analysePattern != null && !analysePattern.isEmpty()) {
            if (isRoleMode) {
                initRoleBasedDetectionPattern(getPageBase(), users, roles, analysePattern, minFrequency, maxFrequency, task, result);
            } else {
                initUserBasedDetectionPattern(getPageBase(), users, roles, analysePattern, minFrequency, maxFrequency, task, result);
            }
        }

    }

    private void loadMiningTable(WebMarkupContainer webMarkupContainer, List<DetectedPattern> analysePattern) {
        RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
            return;
        }

        if (isRoleMode) {
            RoleAnalysisRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(
                    analysePattern
            );
            boxedTablePanel.setOutputMarkupId(true);
            webMarkupContainer.add(boxedTablePanel);
        } else {
            RoleAnalysisUserBasedTable boxedTablePanel = generateMiningUserBasedTable(
                    analysePattern
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

    public RoleAnalysisUserBasedTable generateMiningUserBasedTable(List<DetectedPattern> selectedPattern) {

        return new RoleAnalysisUserBasedTable(ID_DATATABLE, miningOperationChunk,
                selectedPattern,
                roleAnalysisSortMode,
                getObjectWrapperObject()) {

            @Override
            protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRole() {
                return getRoleAnalysisCandidateRoleType();
            }

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
                DetectedPatternPopupPanel detailsPanel = new DetectedPatternPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Patterns panel"), cluster, detectedPatternList);

                getPageBase().showMainPopup(detailsPanel, target);
            }

            @Override
            protected void showCandidateRolesPanel(AjaxRequestTarget target) {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();

                Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

                HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate = new HashMap<>();
                List<RoleType> roles = new ArrayList<>();
                for (RoleAnalysisCandidateRoleType candidateRoleType : candidateRoles) {
                    ObjectReferenceType candidateRoleRef = candidateRoleType.getCandidateRoleRef();
                    PrismObject<RoleType> role = getPageBase().getRoleAnalysisService()
                            .getRoleTypeObject(candidateRoleRef.getOid(), task, result);
                    if (Objects.nonNull(role)) {
                        cacheCandidate.put(candidateRoleRef.getOid(), candidateRoleType);
                        roles.add(role.asObjectable());
                    }
                }

                List<String> selectedCandidates = getCandidateRoleContainerId();
                CandidateRolesPopupPanel detailsPanel = new CandidateRolesPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"), cluster, cacheCandidate, roles, selectedCandidates);

                getPageBase().showMainPopup(detailsPanel, target);

            }
        };
    }

    public RoleAnalysisRoleBasedTable generateMiningRoleBasedTable(List<DetectedPattern> intersection) {

        return new RoleAnalysisRoleBasedTable(ID_DATATABLE,
                miningOperationChunk,
                intersection,
                roleAnalysisSortMode, getObjectWrapperObject()) {

            @Override
            protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRole() {
                return getRoleAnalysisCandidateRoleType();
            }

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
                DetectedPatternPopupPanel detailsPanel = new DetectedPatternPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Patterns panel"), cluster, detectedPatternList);

                getPageBase().showMainPopup(detailsPanel, target);
            }

            @Override
            protected void showCandidateRolesPanel(AjaxRequestTarget target) {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();

                Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

                HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate = new HashMap<>();
                List<RoleType> roles = new ArrayList<>();
                for (RoleAnalysisCandidateRoleType candidateRoleType : candidateRoles) {
                    ObjectReferenceType candidateRoleRef = candidateRoleType.getCandidateRoleRef();
                    PrismObject<RoleType> role = getPageBase().getRoleAnalysisService()
                            .getRoleTypeObject(candidateRoleRef.getOid(), task, result);
                    if (Objects.nonNull(role)) {
                        cacheCandidate.put(candidateRoleRef.getOid(), candidateRoleType);
                        roles.add(role.asObjectable());
                    }
                }
                List<String> selectedCandidates = getCandidateRoleContainerId();
                CandidateRolesPopupPanel detailsPanel = new CandidateRolesPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"), cluster, cacheCandidate, roles, selectedCandidates);

                getPageBase().showMainPopup(detailsPanel, target);

            }
        };
    }

    private @Nullable Set<RoleAnalysisCandidateRoleType> getRoleAnalysisCandidateRoleType() {
        List<String> candidateRoleContainerId = getCandidateRoleContainerId();

        Set<RoleAnalysisCandidateRoleType> candidateRoleTypes = new HashSet<>();
        if (candidateRoleContainerId != null && !candidateRoleContainerId.isEmpty()) {
            RoleAnalysisClusterType cluster = getObjectWrapperObject().asObjectable();
            List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

            for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
                if (candidateRoleContainerId.contains(candidateRole.getId().toString())) {
                    candidateRoleTypes.add(candidateRole);
                }
            }
            if (!candidateRoleTypes.isEmpty()) {
                return candidateRoleTypes;
            }
            return null;
        }
        return null;
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
