/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.ApplicationRoleWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.BusinessRoleWizardPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/role", matchUrlForSecurity = "/admin/role")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, label = "PageAdminRoles.auth.roleAll.label", description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageAbstractRole<RoleType, AbstractRoleDetailsModel<RoleType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

    private static final String DOT_CLASS = BusinessRoleWizardPanel.class.getName() + ".";
    private static final String OP_PERFORM_MIGRATION = DOT_CLASS + "performMigration";

    private BusinessRoleApplicationDto patternDeltas;

    public PageRole() {
        super();
    }

    public PageRole(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageRole(PrismObject<RoleType> role) {
        super(role);
    }

    public PageRole(PrismObject<RoleType> role, BusinessRoleApplicationDto patternDeltas) {
        super(role);
        this.patternDeltas = patternDeltas;
    }

    @Override
    protected AbstractRoleDetailsModel<RoleType> createObjectDetailsModels(PrismObject<RoleType> object) {
        return new AbstractRoleDetailsModel<>(createPrismObjectModel(object), this);
    }

    @Override
    protected void postProcessModel(AbstractRoleDetailsModel<RoleType> objectDetailsModels) {
        if (patternDeltas != null && !patternDeltas.getBusinessRoleDtos().isEmpty()) {
            objectDetailsModels.setPatternDeltas(patternDeltas);
        }

        patternDeltas = null;
    }

    @Override
    public Class<RoleType> getType() {
        return RoleType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleType> summaryModel) {
        return new RoleSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    protected DetailsFragment createDetailsFragment() {

        Class<? extends AbstractWizardPanel> wizardClass = null;

        if (canShowWizard(SystemObjectsType.ARCHETYPE_APPLICATION_ROLE)) {
            wizardClass = ApplicationRoleWizardPanel.class;
        }

        if (canShowWizard(SystemObjectsType.ARCHETYPE_BUSINESS_ROLE)) {
            wizardClass = BusinessRoleWizardPanel.class;
        }

        if (wizardClass != null) {
            setShowedByWizard(true);
            PrismObject<RoleType> obj = getObjectDetailsModels().getObjectWrapper().getObject();
            try {
                obj.findOrCreateProperty(ResourceType.F_LIFECYCLE_STATE).setRealValue(SchemaConstants.LIFECYCLE_DRAFT);
            } catch (SchemaException ex) {
                getFeedbackMessages().error(PageRole.this, ex.getUserFriendlyMessage());
            }
            return createRoleWizardFragment(wizardClass);
        }

        return super.createDetailsFragment();
    }

    protected boolean canShowWizard(SystemObjectsType archetype) {
        return !isHistoryPage() && !isEditObject() && WebComponentUtil.hasArchetypeAssignment(
                getObjectDetailsModels().getObjectType(),
                archetype.value());
    }

    @Override
    protected void exitFromWizard() {
        if (existPatternDeltas()) {
            navigateToRoleAnalysis();
            return;
        }
        super.exitFromWizard();
    }

    private void navigateToRoleAnalysis() {
        PageParameters parameters = new PageParameters();
        String clusterOid = getObjectDetailsModels().getPatternDeltas().getCluster().getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        navigateToNext(detailsPageClass, parameters);
    }

    private DetailsFragment createRoleWizardFragment(Class<? extends AbstractWizardPanel> clazz) {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRole.this) {
            @Override
            protected void initFragmentLayout() {
                try {
                    Constructor<? extends AbstractWizardPanel> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);
                    AbstractWizardPanel wizard = constructor.newInstance(ID_TEMPLATE, createObjectWizardPanelHelper());
                    add(wizard);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error("Couldn't create panel by constructor for class " + clazz.getSimpleName()
                            + " with parameters type: String, WizardPanelHelper");
                }
            }
        };
    }

    @Override
    public void savePerformed(AjaxRequestTarget target) {
        super.savePerformed(target);
    }

    @Override
    protected void postProcessResultForWizard(
            OperationResult result,
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
            AjaxRequestTarget target) {
        businessRoleMigrationPerform(result, executedDeltas);

        super.postProcessResult(result, executedDeltas, target);
    }

    private void businessRoleMigrationPerform(
            OperationResult result,
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas) {

        if (result.isFatalError()) {
            return;
        }

        if (!existPatternDeltas()) {
            return;
        }

        Task task = createSimpleTask(OP_PERFORM_MIGRATION);

        String roleOid = ObjectDeltaOperation.findAddDeltaOidRequired(executedDeltas, RoleType.class);

        BusinessRoleApplicationDto patternDeltas = getObjectDetailsModels().getPatternDeltas();
        RoleAnalysisService roleAnalysisService = getRoleAnalysisService();
        roleAnalysisService.clusterObjectMigrationRecompute(
                getRepositoryService(), patternDeltas.getCluster().getOid(), roleOid, task, result);

        PrismObject<RoleType> roleObject = roleAnalysisService
                .getRoleTypeObject( roleOid, task, result);
        if (roleObject != null) {
            executeMigrationTask(result, task, patternDeltas.getBusinessRoleDtos(), roleObject);
        }
    }

    private boolean existPatternDeltas() {
        BusinessRoleApplicationDto patternDeltas = getObjectDetailsModels().getPatternDeltas();
        return patternDeltas != null && !patternDeltas.getBusinessRoleDtos().isEmpty();
    }

    private void executeMigrationTask(OperationResult result, Task task, List<BusinessRoleDto> patternDeltas, PrismObject<RoleType> roleObject) {
        try {
            ActivityDefinitionType activity = createActivity(patternDeltas, roleObject.getOid());

            getModelInteractionService().submit(
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

    protected boolean isHistoryPage() {
        return false;
    }
}
