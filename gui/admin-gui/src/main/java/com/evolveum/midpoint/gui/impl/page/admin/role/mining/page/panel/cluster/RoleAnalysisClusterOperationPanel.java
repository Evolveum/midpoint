/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidInducements;

import java.io.Serial;
import java.util.*;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.BasePattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItemPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
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

    public static final String PARAM_CANDIDATE_ROLE_ID = "candidateRoleId";
    public static final String PARAM_DETECTED_PATER_ID = "detectedPatternId";
    public static final String PARAM_TABLE_SETTING = "tableSetting";

    private static final String DOT_CLASS = RoleAnalysisClusterOperationPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_OPERATION_PANEL = "panel";

    private final LoadableDetachableModel<OperationPanelModel> operationPanelModel;
    private final LoadableModel<RoleAnalysisObjectDto> miningOperationChunk;

    public RoleAnalysisClusterOperationPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);

        this.operationPanelModel = new LoadableDetachableModel<>() {
            @Override
            protected OperationPanelModel load() {
                OperationPanelModel model = new OperationPanelModel();
                model.createDetectedPatternModel(getClusterPatterns());
                model.createCandidatesRolesRoleModel(getClusterCandidateRoles());
                model.createOutlierPatternModel(getOutlierPatterns());
                model.setCandidateRoleView(!getCandidateRoleContainerId().isEmpty());
                return model;
            }
        };

        this.miningOperationChunk = new LoadableModel<>(false) {

            @Override
            protected RoleAnalysisObjectDto load() {
                //TODO optimize this (preparation of the object)
                return new RoleAnalysisObjectDto(getObjectWrapperObject().asObjectable(), operationPanelModel.getObject().getSelectedPatterns(), getParameterTableSetting(), getPageBase());

            }
        };
    }

    @Override
    protected void initLayout() {
        loadMiningTable();
        initOperationPanel();
    }

    private List<DetectedPattern> getClusterPatterns() {
        RoleAnalysisClusterType clusterType = getObjectWrapperObject().asObjectable();
        return transformDefaultPattern(clusterType, null, getDetectedPatternContainerId());
    }

    private List<DetectedPattern> getClusterCandidateRoles() {
        RoleAnalysisClusterType clusterType = getObjectWrapperObject().asObjectable();
        return loadAllCandidateRoles(clusterType);
    }

    private List<DetectedPattern> getOutlierPatterns() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        RoleAnalysisClusterType cluster = getObjectWrapperObject().asObjectable();

        List<DetectedPattern> outlierPatterns = new ArrayList<>();

        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name?
        OperationResult result = task.getResult();
        List<RoleAnalysisOutlierType> searchResultList = roleAnalysisService.findClusterOutliers(cluster, null, task, result);
        for (RoleAnalysisOutlierType outlier : searchResultList) {
            Set<String> roles = new HashSet<>();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                if (outlierPartition.getClusterRef() != null
                        && outlierPartition.getClusterRef().getOid() != null
                        && outlierPartition.getClusterRef().getOid().equals(cluster.getOid())) {
                    List<DetectedAnomalyResultType> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                    for (DetectedAnomalyResultType detectedAnomaly : detectedAnomalyResult) {
                        roles.add(detectedAnomaly.getTargetObjectRef().getOid());
//                    anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
                    }
                    break;
                }
            }
            Set<String> users = Collections.singleton(outlier.getObjectRef().getOid());
            DetectedPattern detectedPattern = new DetectedPattern(roles, users, outlier.getOverallConfidence(), null) {
                @Override
                public ObjectReferenceType getOutlierRef() {
                    return new ObjectReferenceType()
                            .oid(outlier.getOid())
                            .type(RoleAnalysisOutlierType.COMPLEX_TYPE)
                            .targetName(outlier.getName());
                }

                @Override
                public String getIdentifier() {
                    return outlier.getName().getOrig();
                }

                @Override
                public ObjectReferenceType getClusterRef() {
                    return new ObjectReferenceType().oid(cluster.getOid()).type(RoleAnalysisClusterType.COMPLEX_TYPE);
                }
            };

            detectedPattern.setPatternType(BasePattern.PatternType.OUTLIER);
            outlierPatterns.add(detectedPattern);
        }
        return outlierPatterns;

    }

    private @NotNull List<DetectedPattern> loadAllCandidateRoles(@NotNull RoleAnalysisClusterType cluster) {
        List<RoleAnalysisCandidateRoleType> clusterCandidateRoles = cluster.getCandidateRoles();
        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name?
        OperationResult result = task.getResult();
        return clusterCandidateRoles.stream()
                .filter(candidateRole -> !isRoleMigrated(candidateRole))
                .map(candidateRole -> transformCandidateRole(candidateRole, cluster, task, result))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isRoleMigrated(RoleAnalysisCandidateRoleType candidateRole) {
        RoleAnalysisOperationStatusType operationStatus = candidateRole.getOperationStatus();
        return operationStatus != null
                && operationStatus.getOperationChannel() != null
                && operationStatus.getOperationChannel().equals(RoleAnalysisOperationType.MIGRATION);
    }

    private DetectedPattern transformCandidateRole(RoleAnalysisCandidateRoleType candidateRole,
            RoleAnalysisClusterType cluster,
            Task task,
            OperationResult result) {
        String roleOid = candidateRole.getCandidateRoleRef().getOid();
        PrismObject<RoleType> rolePrismObject = WebModelServiceUtils.loadObject(RoleType.class, roleOid, getPageBase(), task, result);
        if (rolePrismObject == null) {
            return null;
        }

        Set<String> membersOidSet = computeMembersSet(candidateRole, task, result);
        Set<String> accessOidSet = computeAccessSet(rolePrismObject);

        double clusterMetric = (accessOidSet.size() * membersOidSet.size()) - membersOidSet.size();

        DetectedPattern pattern = new DetectedPattern(
                accessOidSet,
                membersOidSet,
                clusterMetric,
                null,
                roleOid,
                BasePattern.PatternType.CANDIDATE) {
            @Override
            public ObjectReferenceType getClusterRef() {
                return new ObjectReferenceType()
                        .oid(cluster.getOid())
                        .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                        .targetName(cluster.getName());
            }
        };

        pattern.setIdentifier(rolePrismObject.getName().getOrig());
        pattern.setId(candidateRole.getId());
        pattern.setPatternSelected(isCandidateRoleSelected(candidateRole));
        return pattern;
    }

    private Set<String> computeMembersSet(RoleAnalysisCandidateRoleType candidateRole, Task task, OperationResult result) {
        String roleOid = candidateRole.getCandidateRoleRef().getOid();
        ListMultimap<String, String> mappedMembers = getPageBase().getRoleAnalysisService().extractUserTypeMembers(new HashMap<>(),
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
        return membersOidSet;
    }

    private Set<String> computeAccessSet(PrismObject<RoleType> rolePrismObject) {
        //TODO this might work only in simple scenarios? what about conditions? role hierarchies?
        List<String> rolesOidInducements = getRolesOidInducements(rolePrismObject);
        List<String> rolesOidAssignment = getRolesOidAssignment(rolePrismObject.asObjectable()); //TODO why assignments

        Set<String> accessOidSet = new HashSet<>(rolesOidInducements);
        accessOidSet.addAll(rolesOidAssignment);
        return accessOidSet;
    }

    private boolean isCandidateRoleSelected(RoleAnalysisCandidateRoleType candidateRole) {
        List<String> candidateRoles = getCandidateRoleContainerId();
        for (String candidateRoleId : candidateRoles) {
            if (candidateRoleId.equals(candidateRole.getId().toString())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public List<String> getCandidateRoleContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
        if (!stringValue.isNull()) {
            String[] split = stringValue.toString().split(",");
            return Arrays.asList(split);
        }
        return new ArrayList<>();
    }

    public Long getDetectedPatternContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_DETECTED_PATER_ID);
        if (!stringValue.isNull()) {
            return Long.valueOf(stringValue.toString());
        }
        return null;
    }

    public Integer getParameterTableSetting() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
        if (!stringValue.isNull()) {
            return Integer.valueOf(stringValue.toString());
        }
        return null;
    }

    private <B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> void loadMiningTable() {
        //TODO what exactly is this?
//    RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
//        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
//            return;
//        }

        RoleAnalysisTable<B, A> boxedTablePanel = new RoleAnalysisTable<>(
                ID_DATATABLE,
                miningOperationChunk) {

            @Override
            protected void onUniquePatternDetectionPerform(AjaxRequestTarget target) {
                RoleAnalysisClusterOperationPanel.this.resetOperationPanel(target);
            }

            @Override
            protected List<DetectedPattern> getSelectedPatterns() {
                return RoleAnalysisClusterOperationPanel.this.getSelectedPatterns();
            }

            @Override
            protected IModel<Map<String, String>> getColorPaletteModel() {
                return () -> operationPanelModel.getObject().getPalletColors();
            }

            @Override
            protected void loadDetectedPattern(AjaxRequestTarget target) {
                RoleAnalysisClusterOperationPanel.this.loadDetectedPattern(target);
            }
        };
        boxedTablePanel.setOutputMarkupId(true);
        add(boxedTablePanel);

    }

    private void initOperationPanel() {
        RoleAnalysisTableOpPanelItemPanel itemPanel = new RoleAnalysisTableOpPanelItemPanel(ID_OPERATION_PANEL, operationPanelModel) {

            @Override
            public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {
                loadDetectedPattern(ajaxRequestTarget);
            }

            @Override
            protected void initHeaderItem(@NotNull RepeatingView headerItems) {

                IModel<String> operationDescription = new LoadableDetachableModel<>() {
                    @Override
                    protected @NotNull String load() {
                        return getModelObject().isShowAsExpandCard() ? "Maximize" : "Minimize";
                    }
                };

                RoleAnalysisTableOpPanelItem refreshIcon = new RoleAnalysisTableOpPanelItem(
                        headerItems.newChildId(), getModel()) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Contract(pure = true)
                    @Override
                    public @NotNull String appendIconPanelCssClass() {
                        return "bg-white";
                    }

                    @Override
                    protected void performOnClick(AjaxRequestTarget target) {
                        boolean showAsExpandCard = getModelObject().isShowAsExpandCard();
                        getModelObject().setShowAsExpandCard(!showAsExpandCard);
//                        showAsExpandCard = !showAsExpandCard;
                        toggleDetailsNavigationPanelVisibility(target);
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String replaceIconCssClass() {
                        if (getModelObject().isShowAsExpandCard()) {
                            return "fa-2x fa fa-compress text-dark";
                        }
                        return "fa-2x fas fa-expand text-dark";
                    }

                    @Override
                    public @NotNull Component getDescriptionTitleComponent(String id) {
                        Label label = new Label(id, "Table view"); //TODO string resource model
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected void addDescriptionComponents() {
                        appendText(operationDescription, null); //TODO string resource model
                    }
                };

                headerItems.add(refreshIcon);
            }

        };
        itemPanel.setOutputMarkupId(true);
        add(itemPanel);
    }

    @SuppressWarnings("rawtypes")
    protected void toggleDetailsNavigationPanelVisibility(AjaxRequestTarget target) {
        PageBase page = getPageBase();
        if (page instanceof AbstractPageObjectDetails) {
            AbstractPageObjectDetails<?, ?> pageObjectDetails = ((AbstractPageObjectDetails) page);
            pageObjectDetails.toggleDetailsNavigationPanelVisibility(target);
        }
    }

    protected void loadDetectedPattern(AjaxRequestTarget target) {
        miningOperationChunk.getObject().updateWithPatterns(getSelectedPatterns(), getPageBase());
        target.add(get(ID_DATATABLE));
    }

    private List<DetectedPattern> getSelectedPatterns() {
        return operationPanelModel.getObject().getSelectedPatterns();
    }

    private void resetOperationPanel(@NotNull AjaxRequestTarget target) {
        operationPanelModel.getObject().clearSelectedPatterns();
        target.add(get(ID_OPERATION_PANEL));
    }

}
