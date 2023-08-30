/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.clusterMigrationRecompute;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.getRoleTypeObject;
import static com.evolveum.midpoint.repo.api.RepositoryService.LOGGER;

/**
 * @author lskublik
 */
public class BusinessRoleWizardPanel extends AbstractWizardPanel<RoleType, AbstractRoleDetailsModel<RoleType>> {

    public BusinessRoleWizardPanel(String id, WizardPanelHelper<RoleType, AbstractRoleDetailsModel<RoleType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicInformationStepPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                BusinessRoleWizardPanel.this.onExitPerformed(target);
            }
        });

        BusinessRoleApplicationDto patterns = getAssignmentHolderModel().getPatternDeltas();
        if (patterns != null && CollectionUtils.isNotEmpty(patterns.getBusinessRoleDtos())) {
            steps.add(new ExsitingAccessApplicationRoleStepPanel<>(getAssignmentHolderModel()) {

                @Override
                protected void onExitPerformed(AjaxRequestTarget target) {
                    BusinessRoleWizardPanel.this.onExitPerformed(target);
                }

                @Override
                public VisibleEnableBehaviour getBackBehaviour() {
                    return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
                }
            });

            steps.add(new CandidateMembersPanel<>(getAssignmentHolderModel()) {

                @Override
                protected void onExitPerformed(AjaxRequestTarget target) {
                    BusinessRoleWizardPanel.this.onExitPerformed(target);
                }

                @Override
                public VisibleEnableBehaviour getBackBehaviour() {
                    return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
                }
            });

            steps.add(new AccessApplicationRoleStepPanel(getHelper().getDetailsModel()) {
                @Override
                protected void onSubmitPerformed(AjaxRequestTarget target) {
                    //TODO
                    businessRoleMigrationPerform(target);
                }

                private void businessRoleMigrationPerform(AjaxRequestTarget target) {
                    OperationResult result = new OperationResult("Migration");
                    Task task = getPageBase().createSimpleTask("executeMigration");

                    Collection<ObjectDelta<? extends ObjectType>> deltas;
                    try {
                        deltas = getHelper().getDetailsModel().collectDeltas(result);
                    } catch (Throwable ex) {
                        result.recordFatalError(getString("pageAdminObjectDetails.message.cantCreateObject"), ex);
                        showResult(result);
                        target.add(getFeedbackPanel());
                        return;
                    }

                    BusinessRoleApplicationDto patternDeltas = getHelper().getDetailsModel().getPatternDeltas();

                    if (patternDeltas != null && !patternDeltas.getBusinessRoleDtos().isEmpty()) {
                        ModelService modelService = getPageBase().getModelService();
                        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ObjectChangesExecutorImpl()
                                .executeChanges(deltas, false, task, result, target);

                        String roleOid = ObjectDeltaOperation.findAddDeltaOidRequired(executedDeltas, RoleType.class);
                        clusterMigrationRecompute(result, patternDeltas.getCluster().getOid(), roleOid, getPageBase(), task);

                        PrismObject<RoleType> roleObject = getRoleTypeObject(modelService, roleOid, result, task);
                        if (roleObject != null) {
                            executeMigrationTask(result, task, patternDeltas.getBusinessRoleDtos(), roleObject);
                        }

                    } else {
                        result.recordWarning(getString("BusinessRoleMigration.message.no.changes", patternDeltas));
                    }
                    showResult(result);
                    target.add(getFeedbackPanel());

                    if (!result.isError()) {
                        exitToPreview(target);
                    }
                }

                @Override
                protected boolean isSubmitEnable() {
                    return getHelper().getDetailsModel().getPatternDeltas() != null;
                }

                @Override
                protected void onExitPerformed(AjaxRequestTarget target) {
                    BusinessRoleWizardPanel.this.onExitPerformed(target);
                }
            });

        }

        return steps;
    }

    private void executeMigrationTask(OperationResult result, Task task, List<BusinessRoleDto> patternDeltas, PrismObject<RoleType> roleObject) {
        try {
            ActivityDefinitionType activity = createActivity(patternDeltas, roleObject.getOid());

            getPageBase().getModelInteractionService().submit(
                    activity,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .name("Migration role (" + roleObject.getName().toString() + ")"))
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);

        } catch (CommonException e) {
            LOGGER.error("Failed to execute role {} migration activity: ", roleObject.getOid(), e);
        }
    }

    private ActivityDefinitionType createActivity(List<BusinessRoleDto> patternDeltas, String roleOid) throws SchemaException {

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


    private void exitToPreview(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new RoleWizardPreviewPanel<>(getIdOfChoicePanel(), getHelper().getDetailsModel(), PreviewTileType.class) {
                    @Override
                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
                        switch (value) {
                            case CONFIGURE_MEMBERS -> showMembersPanel(target);
                            case CONFIGURE_GOVERNANCE_MEMBERS -> showGovernanceMembersPanel(target);
                        }
                    }
                });
    }

    private void showGovernanceMembersPanel(AjaxRequestTarget target) {
        showChoiceFragment(target, new GovernanceMembersWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });
    }

    private void showMembersPanel(AjaxRequestTarget target) {
        showChoiceFragment(target, new MembersWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });
    }

    enum PreviewTileType implements TileEnum {

        CONFIGURE_GOVERNANCE_MEMBERS("fa fa-users"),
        CONFIGURE_MEMBERS("fa fa-users");

        private final String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }
}
