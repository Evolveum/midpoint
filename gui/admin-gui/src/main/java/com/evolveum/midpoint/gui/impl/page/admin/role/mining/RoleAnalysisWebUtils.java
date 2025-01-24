/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.outlier.OutlierExplanationResolver;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar.RoleAnalysisBasicProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar.RoleAnalysisInlineProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateMessage;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;

public class RoleAnalysisWebUtils {

    public static final String TITLE_CSS = "title";
    public static final String CLASS_CSS = "class";
    public static final String STYLE_CSS = "style";

    public static final String TEXT_MUTED = "text-muted";
    public static final String TEXT_TONED = "txt-toned";
    public static final String TEXT_TRUNCATE = "text-truncate";
    public static final String FONT_WEIGHT_BOLD = "font-weight-bold";

    public static final String PANEL_ID = "panelId";

    private static final String EXPLANATION_NONE_MESSAGE_KEY = "RoleAnalysis.outlier.no.explanation";

    private RoleAnalysisWebUtils() {
    }

    public static String getRoleAssignmentCount(@NotNull RoleType role, @NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("countRoleMembers");
        OperationResult result = task.getResult();

        Integer roleMembersCount = pageBase.getRoleAnalysisService()
                .countUserTypeMembers(null, role.getOid(),
                        task, result);
        return String.valueOf(roleMembersCount);
    }

    public static @NotNull String getRoleInducementsCount(@NotNull RoleType role) {
        return String.valueOf(role.getInducement().size());
    }

    public static ActivityDefinitionType createRoleMigrationActivity(@NotNull List<BusinessRoleDto> patternDeltas, String roleOid) {

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleType.COMPLEX_TYPE);
        objectReferenceType.setOid(roleOid);

        RoleMembershipManagementWorkDefinitionType roleMembershipManagementWorkDefinitionType = new RoleMembershipManagementWorkDefinitionType();
        roleMembershipManagementWorkDefinitionType.setRoleRef(objectReferenceType);

        ObjectSetType members = new ObjectSetType();
        for (BusinessRoleDto patternDelta : patternDeltas) {
            if (!patternDelta.isInclude()) {
                continue;
            }

            PrismObject<UserType> prismObjectUser = patternDelta.getPrismObjectUser();
            ObjectReferenceType userRef = new ObjectReferenceType();
            userRef.setOid(prismObjectUser.getOid());
            userRef.setType(UserType.COMPLEX_TYPE);
            members.getObjectRef().add(userRef);
        }
        roleMembershipManagementWorkDefinitionType.setMembers(members);

        return new ActivityDefinitionType()
                .work(new WorkDefinitionsType()
                        .roleMembershipManagement(roleMembershipManagementWorkDefinitionType));
    }

    public static void businessRoleMigrationPerform(
            @NotNull PageBase pageBase,
            @NotNull BusinessRoleApplicationDto businessRoleApplicationDto,
            @NotNull Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull AjaxRequestTarget target) {

        String roleOid = ObjectDeltaOperation.findAddDeltaOidRequired(executedDeltas, RoleType.class);

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<RoleType> roleObject = roleAnalysisService
                .getRoleTypeObject(roleOid, task, result);

        if (roleObject != null) {
            PrismObject<RoleAnalysisClusterType> cluster = businessRoleApplicationDto.getCluster();
            if (!businessRoleApplicationDto.isCandidate()) {

                List<BusinessRoleDto> businessRoleDtos = businessRoleApplicationDto.getBusinessRoleDtos();

                Set<ObjectReferenceType> candidateMembers = new HashSet<>();

                for (BusinessRoleDto businessRoleDto : businessRoleDtos) {
                    PrismObject<UserType> prismObjectUser = businessRoleDto.getPrismObjectUser();
                    if (prismObjectUser != null) {
                        candidateMembers.add(new ObjectReferenceType()
                                .oid(prismObjectUser.getOid())
                                .type(UserType.COMPLEX_TYPE).clone());
                    }
                }

                RoleAnalysisCandidateRoleType candidateRole = new RoleAnalysisCandidateRoleType();
                candidateRole.getCandidateMembers().addAll(candidateMembers);
                candidateRole.setAnalysisMetric(0.0);
                candidateRole.setCandidateRoleRef(new ObjectReferenceType()
                        .oid(roleOid)
                        .type(RoleType.COMPLEX_TYPE).clone());

                roleAnalysisService.addCandidateRole(
                        cluster.getOid(), candidateRole, task, result);
                return;
            }

            roleAnalysisService.clusterObjectMigrationRecompute(
                    cluster.getOid(), roleOid, task, result);

            String taskOid = UUID.randomUUID().toString();

            ActivityDefinitionType activity;
            activity = createRoleMigrationActivity(businessRoleApplicationDto.getBusinessRoleDtos(), roleOid);
            if (activity != null) {
                ModelInteractionService modelInteractionService = pageBase.getModelInteractionService();
                roleAnalysisService.executeRoleAnalysisRoleMigrationTask(modelInteractionService,
                        cluster, activity, roleObject, taskOid, null, task, result);
                if (result.isWarning()) {
                    pageBase.warn(result.getMessage());
                    target.add(pageBase.getFeedbackPanel());
                }
            }

        }
    }

    public static void navigateToClusterOperationPanel(
            @NotNull PageBase pageBase,
            @Nullable BusinessRoleApplicationDto roleAnalysisPatternDeltas) {
        if (roleAnalysisPatternDeltas == null) {
            return;
        }
        PrismObject<RoleAnalysisClusterType> cluster = roleAnalysisPatternDeltas.getCluster();
        if (cluster == null) {
            return;
        }
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, cluster.getOid());
        parameters.add(PANEL_ID, "clusterDetails");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        pageBase.navigateToNext(detailsPageClass, parameters);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull LoadableDetachableModel<PrismObjectWrapper<UserType>> loadUserWrapperForMarkAction(
            @NotNull String userOid,
            @NotNull PageBase pageBase,
            @NotNull OperationResult result) {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismObjectWrapper<UserType> load() {
                Task task = pageBase.createSimpleTask("createWrapper");
                task.setResult(result);
                ModelService modelService = pageBase.getModelService();

                Collection<SelectorOptions<GetOperationOptions>> options = pageBase.getOperationOptionsBuilder()
                        .noFetch()
                        .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_MARK_REF)).resolve()
                        .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_LIFECYCLE_STATE)).resolve()
                        .build();

                try {
                    PrismObject<UserType> userObject = modelService.getObject(UserType.class, userOid, options, task, result);
                    PrismObjectWrapperFactory<UserType> factory = pageBase.findObjectWrapperFactory(userObject.getDefinition());
                    OperationResult result = task.getResult();
                    WrapperContext ctx = new WrapperContext(task, result);
                    ctx.setCreateIfEmpty(true);

                    return factory.createObjectWrapper(userObject, ItemStatus.NOT_CHANGED, ctx);
                } catch (ExpressionEvaluationException | SecurityViolationException | CommunicationException |
                        ConfigurationException | ObjectNotFoundException | SchemaException e) {
                    throw new SystemException("Cannot create wrapper for " + userOid, e);

                }
            }
        };
    }

    @NotNull
    public static RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> loadRoleAnalysisTempTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @Nullable List<DetectedAnomalyResult> detectedAnomalyResult,
            RoleAnalysisOutlierPartitionType partition,
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull RoleAnalysisClusterType cluster) {

        return loadRoleAnalysisTempTable(id, pageBase, detectedAnomalyResult, null, partition, outlier, cluster);
    }

    @NotNull
    public static RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> loadRoleAnalysisTempTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @Nullable List<DetectedAnomalyResult> detectedAnomalyResult,
            String uniqRoleOid,
            RoleAnalysisOutlierPartitionType partition,
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull RoleAnalysisClusterType cluster) {

        LoadableModel<RoleAnalysisObjectDto> miningOperationChunk = new LoadableModel<>(false) {

            @Contract(" -> new")
            @Override
            protected @NotNull RoleAnalysisObjectDto load() {
                //TODO refactor
                RoleAnalysisObjectDto roleAnalysisObjectDto = new RoleAnalysisObjectDto(
                        cluster, new ArrayList<>(), 0, pageBase) {
                    @Override
                    public @NotNull RoleAnalysisChunkMode getDefaultChunkMode() {
                        return RoleAnalysisChunkMode.EXPAND;
                    }
                };
                String outlierOid = outlier.getObjectRef().getOid();

                if (detectedAnomalyResult == null) {
                    List<DetectedAnomalyResult> partitionDetectedAnomalyResult = partition.getDetectedAnomalyResult();
                    if (partitionDetectedAnomalyResult == null) {
                        return roleAnalysisObjectDto;
                    } else {
                        loadObjectForMark(roleAnalysisObjectDto, outlierOid);
                    }
                }

                loadObjectForMark(roleAnalysisObjectDto, outlierOid);

                return roleAnalysisObjectDto;
            }

            private void loadObjectForMark(@NotNull RoleAnalysisObjectDto roleAnalysisObjectDto, @NotNull String outlierOid) {
                String cssStyle = "border: 5px solid #206f9d;";
                String cssClass = "p-2 d-flex align-items-center justify-content-center bg-danger";
                String cssClassUniq = cssClass + " corner-hashed-bg";
                if (detectedAnomalyResult != null) {
                    for (DetectedAnomalyResult item : detectedAnomalyResult) {
                        ObjectReferenceType targetObjectRef = item.getTargetObjectRef();
                        if (targetObjectRef == null || targetObjectRef.getOid() == null) {
                            continue;
                        }

                        boolean isUniqueRole = uniqRoleOid != null && targetObjectRef.getOid().equals(uniqRoleOid);

                        if (isUniqueRole) {
                            roleAnalysisObjectDto.addMarkedRelation(outlierOid, targetObjectRef.getOid(), cssStyle, cssClassUniq);
                        } else {
                            roleAnalysisObjectDto.addMarkedRelation(outlierOid, targetObjectRef.getOid(), cssStyle, cssClass);
                        }
                    }
                }
            }
        };

        RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> table = new RoleAnalysisTable<>(
                id,
                miningOperationChunk) {
            @Override
            public boolean getMigrationButtonVisibility() {
                return false;
            }
        };

        table.setOutputMarkupId(true);
        return table;
    }

    /**
     * Provides an explanation for the given outlier object.
     *
     * @param roleAnalysisService The role analysis service.
     * @param outlierObject The outlier object containing the explanation details.
     * @param shortExplanation A flag indicating whether to provide a short explanation.
     * @param task The task object.
     * @param result The operation result.
     * @return A model containing the translated explanation message or a default message if no explanation is available.
     */
    public static @NotNull Model<String> explainOutlier(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType outlierObject,
            boolean shortExplanation,
            @NotNull Task task,
            @NotNull OperationResult result) {
        OutlierExplanationResolver.OutlierExplanationResult outlierExplanationResult = roleAnalysisService
                .explainOutlier(outlierObject, task, result);

        OutlierExplanationResolver.ExplanationResult explanation;

        if (shortExplanation) {
            explanation = outlierExplanationResult.shortExplanation();
        } else {
            explanation = outlierExplanationResult.explanation();
        }

        LocalizableMessage message = explanation.message();
        return Model.of(translateMessage(message));
    }

    /**
     * Provides an explanation for the given partition object.
     *
     * @param partition The partition object containing the explanation details.
     * @param shortExplanation A flag indicating whether to provide a short explanation.
     * @return A model containing the translated explanation message or a default message if no explanation is available.
     */
    public static @NotNull Model<String> explainPartition(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            boolean shortExplanation,
            @NotNull Task task,
            @NotNull OperationResult result) {
        OutlierExplanationResolver.OutlierExplanationResult outlierExplanationResult = roleAnalysisService
                .explainOutlierPartition(partition, 1, task, result);

        OutlierExplanationResolver.ExplanationResult explanation;

        if (shortExplanation) {
            explanation = outlierExplanationResult.shortExplanation();
        } else {
            explanation = outlierExplanationResult.explanation();
        }

        LocalizableMessage message = explanation.message();
        return Model.of(translateMessage(message));
    }

    /**
     * Provides an explanation for the given anomaly result.
     *
     * @param anomalyResult The anomaly result containing the explanation details.
     * @return A model containing the translated explanation message or a default message if no explanation is available.
     */
    public static @NotNull Model<String> explainAnomaly(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull DetectedAnomalyResult anomalyResult,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<OutlierExplanationResolver.ExplanationResult> explanations = roleAnalysisService.explainOutlierAnomalyAccess(
                anomalyResult, task, result);

        return resolveAnomalyExplanation(explanations);
    }

    public static @NotNull Model<String> explainAnomaly(List<OutlierExplanationResolver.ExplanationResult> explanations) {
        return resolveAnomalyExplanation(explanations);
    }

    private static @NotNull Model<String> resolveAnomalyExplanation(List<OutlierExplanationResolver.ExplanationResult> explanations) {
        Model<String> noneExplanation = resolveIfNoneExplanation(explanations);
        if (noneExplanation != null) {
            return noneExplanation;
        }

        StringBuilder sb = new StringBuilder();
        for (OutlierExplanationResolver.ExplanationResult explanation : explanations) {
            LocalizableMessage message = explanation.message();
            sb.append(translateMessage(message)).append(". \n");
        }
        return Model.of(sb.toString());
    }

    /**
     * Resolves if there is no explanation available in the provided list of explanations.
     *
     * @param explanation The list of outlier detection explanations.
     * @return A model containing a default message if no explanation is available, or null if an explanation is present.
     */
    private static @Nullable Model<String> resolveIfNoneExplanation(List<OutlierExplanationResolver.ExplanationResult> explanation) {
        if (explanation == null || explanation.isEmpty() || explanation.get(0).message() == null) {
            return Model.of(translate(EXPLANATION_NONE_MESSAGE_KEY));
        }
        return null;
    }

    public static double getTotalSystemPercentageReduction(int totalReduction, int totalAssignmentRoleToUser) {
        double totalSystemPercentageReduction = 0;
        if (totalReduction != 0 && totalAssignmentRoleToUser != 0) {
            totalSystemPercentageReduction = ((double) totalReduction / totalAssignmentRoleToUser) * 100;
            BigDecimal bd = BigDecimal.valueOf(totalSystemPercentageReduction);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            totalSystemPercentageReduction = bd.doubleValue();
        }
        return totalSystemPercentageReduction;
    }

    public static @NotNull RoleAnalysisTabbedPanel<ITab> createRoleAnalysisTabPanel(
            @NotNull PageBase pageBase,
            @NotNull String componentId,
            @NotNull List<ITab> tabs) {
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(componentId, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Contract("_, _ -> new")
            @Override
            protected @NotNull WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(pageBase.getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        assert target != null;
                        target.add(pageBase.getFeedbackPanel());
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        return tabPanel;
    }

    public static @NotNull RoleAnalysisProcessModeType resolveSessionProcessMode(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        return analysisOption.getProcessMode();
    }

    public static RoleAnalysisProcedureType resolveSessionProcedureType(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        return analysisOption.getAnalysisProcedureType();
    }

    public static @NotNull RoleAnalysisInlineProgressBar buildSimpleDensityBasedProgressBar(String id, IModel<String> value) {

        IModel<RoleAnalysisProgressBarDto> model = () -> {
            double actualValue = Double.parseDouble(value.getObject().replace(',', '.'));
            String colorClass = densityBasedColor(
                    Double.parseDouble(value.getObject().replace(',', '.')));

            RoleAnalysisProgressBarDto dto = new RoleAnalysisProgressBarDto(actualValue, colorClass);
            dto.setBarTitle("");
            return dto;
        };

        RoleAnalysisInlineProgressBar progressBar = new RoleAnalysisInlineProgressBar(id, model) {
            @Override
            protected boolean isWider() {
                return true;
            }
        };
        progressBar.setOutputMarkupId(true);
        return progressBar;
    }

    public static RoleAnalysisBasicProgressBar buildDensityProgressPanel(
            @NotNull String componentId,
            @NotNull Double density,
            @NotNull String title) {
        IModel<RoleAnalysisProgressBarDto> model = () -> {
            BigDecimal bd = new BigDecimal(Double.toString(density));
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            double actualValue = bd.doubleValue();

            String colorClass = densityBasedColorOposite(actualValue);

            RoleAnalysisProgressBarDto dto = new RoleAnalysisProgressBarDto(actualValue, colorClass);
            dto.setBarTitle(title);
            return dto;
        };

        RoleAnalysisBasicProgressBar progressBar = new RoleAnalysisBasicProgressBar(componentId, model) {
            @Override
            protected boolean isWider() {
                return true;
            }

            @Override
            protected String getProgressBarContainerCssStyle() {
                return "border-radius: 10px; height:10px;";
            }
        };

        progressBar.setOutputMarkupId(true);
        return progressBar;
    }
}
