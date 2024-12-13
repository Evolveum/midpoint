/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RoleAnalysisWebUtils {

    public static final String TITLE_CSS = "title";
    public static final String CLASS_CSS = "class";
    public static final String STYLE_CSS = "style";

    public static final String TEXT_MUTED = "text-muted";
    public static final String TEXT_TONED = "txt-toned";
    public static final String TEXT_TRUNCATE = "text-truncate";
    public static final String FONT_WEIGHT_BOLD = "font-weight-bold";

    public static final String PANEL_ID = "panelId";

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

        LoadableModel<RoleAnalysisObjectDto> miningOperationChunk = new LoadableModel<>(false) {

            @Contract(" -> new")
            @Override
            protected @NotNull RoleAnalysisObjectDto load() {
                //TODO refactor
                RoleAnalysisObjectDto roleAnalysisObjectDto = new RoleAnalysisObjectDto(
                        cluster, new ArrayList<>(), 0, pageBase);
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

            private void loadObjectForMark(RoleAnalysisObjectDto roleAnalysisObjectDto, String outlierOid) {
                if (detectedAnomalyResult != null) {
                    for (DetectedAnomalyResult item : detectedAnomalyResult) {
                        ObjectReferenceType targetObjectRef = item.getTargetObjectRef();
                        if (targetObjectRef == null) {
                            continue;
                        }

                        roleAnalysisObjectDto.addMarkedRelation(outlierOid, targetObjectRef.getOid());
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
}
