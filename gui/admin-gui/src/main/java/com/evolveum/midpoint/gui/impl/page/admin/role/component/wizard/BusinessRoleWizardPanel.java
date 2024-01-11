/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import static com.evolveum.midpoint.repo.api.RepositoryService.LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public class BusinessRoleWizardPanel extends AbstractWizardPanel<RoleType, AbstractRoleDetailsModel<RoleType>> {

    private static final String DOT_CLASS = BusinessRoleWizardPanel.class.getName() + ".";
    private static final String OP_PERFORM_MIGRATION = DOT_CLASS + "performMigration";

    public BusinessRoleWizardPanel(String id, WizardPanelHelper<RoleType, AbstractRoleDetailsModel<RoleType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private boolean isRoleMigration = false;

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
        isRoleMigration = patterns != null && CollectionUtils.isNotEmpty(patterns.getBusinessRoleDtos());

        if (isRoleMigration) {
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

        }

        steps.add(new AccessApplicationRoleStepPanel(getHelper().getDetailsModel()) {
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                //TODO
                if (isRoleMigration) {
                    businessRoleMigrationPerform(target);
                } else {
                    super.onSubmitPerformed(target);
                    BusinessRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
                }
            }

            private void businessRoleMigrationPerform(AjaxRequestTarget target) {
                Task task = getPageBase().createSimpleTask(OP_PERFORM_MIGRATION);
                OperationResult result = task.getResult();

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
                    Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ObjectChangesExecutorImpl()
                            .executeChanges(deltas, false, task, result, target);

                    String roleOid = ObjectDeltaOperation.findAddDeltaOidRequired(executedDeltas, RoleType.class);
                    RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                    PrismObject<RoleType> roleObject = roleAnalysisService
                            .getRoleTypeObject(roleOid, task, result);
                    if (roleObject != null) {
                        roleAnalysisService.clusterObjectMigrationRecompute(
                                patternDeltas.getCluster().getOid(), roleOid, task, result);

                        String taskOid = UUID.randomUUID().toString();

                        ActivityDefinitionType activity = null;
                        try {
                            activity = createActivity(patternDeltas.getBusinessRoleDtos(), roleOid);
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't create activity for role migration: " + roleOid);
                        }
                        if (activity != null) {
                            roleAnalysisService.executeMigrationTask(
                                    patternDeltas.getCluster(), activity, roleObject, taskOid, null, task, result);
                        }
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
                if (isRoleMigration) {
                    return getHelper().getDetailsModel().getPatternDeltas() != null;
                } else {
                    return super.isSubmitEnable();
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                BusinessRoleWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSavePerformed(target);
        if (!result.isError()) {
//            WebComponentUtil.createToastForCreateObject(target, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
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
        if (isRoleMigration) {
            setResponsePage(PageRoleAnalysis.class);
            PageParameters parameters = new PageParameters();
            String clusterOid = getHelper().getDetailsModel().getPatternDeltas().getCluster().getOid();
            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
            parameters.add("panelId", "migratedRoles");
            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
            getPageBase().navigateToNext(detailsPageClass, parameters);
        } else {
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
