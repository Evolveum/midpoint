/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsTaskCreator;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.schema.constants.RelationTypes;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.ApplicationRoleWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.BusinessRoleWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    protected boolean canShowWizard(SystemObjectsType archetype) {
        return !isHistoryPage() && (!isEditObject()) && WebComponentUtil.hasArchetypeAssignment(
                getObjectDetailsModels().getObjectType(),
                archetype.value());
    }

    @Override
    protected boolean canShowWizard() {

        return !isHistoryPage() && !isEditObject()
                && isApplicationOrBusinessRole();
    }

    private boolean isApplicationOrBusinessRole() {
        OperationResult result = new OperationResult("determineArchetype");
        RoleType roleToTest = getObjectDetailsModels().getObjectType();
        try {
            return getModelInteractionService().isOfArchetype(roleToTest, SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value(), result)
                    || getModelInteractionService().isOfArchetype(roleToTest, SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(), result);
        } catch (Exception e) {
            result.recordFatalError("Couldn't determine archetype for role: " + roleToTest, e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine archetype for role: " + roleToTest, e);
            return false;
        }
    }

    @Override
    protected void exitFromWizard() {
        if (existPatternDeltas()) {
            navigateToClusterOperationPanel();
            return;
        }
        super.exitFromWizard();
    }

    private void navigateToClusterOperationPanel() {
        if (!existPatternDeltas()) {
            return;
        }
        PageParameters parameters = new PageParameters();
        BusinessRoleApplicationDto patternDeltas = getObjectDetailsModels().getPatternDeltas();
        PrismObject<RoleAnalysisClusterType> cluster = patternDeltas.getCluster();
        if (cluster == null) {
            return;
        }
        parameters.add(OnePageParameterEncoder.PARAMETER, cluster.getOid());
        parameters.add("panelId", "clusterDetails");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        navigateToNext(detailsPageClass, parameters);
    }

    @Override
    protected DetailsFragment createWizardFragment() {

        Class<? extends AbstractWizardPanel> wizardClass = getWizardPanelClass();

        PrismObject<RoleType> obj = getObjectDetailsModels().getObjectWrapper().getObject();
        try {
            obj.findOrCreateProperty(ResourceType.F_LIFECYCLE_STATE).setRealValue(SchemaConstants.LIFECYCLE_DRAFT);
        } catch (SchemaException ex) {
            getFeedbackMessages().error(PageRole.this, ex.getUserFriendlyMessage());
        }

        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRole.this) {
            @Override
            protected void initFragmentLayout() {
                try {
                    Constructor<? extends AbstractWizardPanel> constructor = wizardClass.getConstructor(String.class, WizardPanelHelper.class);
                    AbstractWizardPanel wizard = constructor.newInstance(ID_TEMPLATE, createObjectWizardPanelHelper());
                    add(wizard);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error("Couldn't create panel by constructor for class " + wizardClass.getSimpleName()
                            + " with parameters type: String, WizardPanelHelper");
                }
            }
        };
    }

    private Class<? extends AbstractWizardPanel> getWizardPanelClass() {
        OperationResult result = new OperationResult("determineArchetype");
        try {
            if (getModelInteractionService().isOfArchetype(
                    getObjectDetailsModels().getObjectType(),
                    SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value(),
                    result)) {
                return ApplicationRoleWizardPanel.class;
            }
            if (getModelInteractionService().isOfArchetype(
                    getObjectDetailsModels().getObjectType(),
                    SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(),
                    result)) {
                return BusinessRoleWizardPanel.class;
            }
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine archetype for role: " + getObjectDetailsModels().getObjectType(), e);
            result.recordFatalError("Couldn't determine archetype for role: " + getObjectDetailsModels().getObjectType(), e);
        }
        showResult(result);
        return null;
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

        if (result.isFatalError()) {
            return;
        }

        if (existPatternDeltas()) {
            List<ObjectType> userMembersOid = new ArrayList<>();

            List<BusinessRoleDto> businessRoleDtos = getPatternDeltas().getBusinessRoleDtos();

            for (BusinessRoleDto businessRoleDto : businessRoleDtos) {
                PrismObject<UserType> prismObjectUser = businessRoleDto.getPrismObjectUser();
                if (prismObjectUser != null) {
                    userMembersOid.add(prismObjectUser.asObjectable());
                }
            }
            String roleOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
            Task task = createSimpleTask("load role after save");
            PrismObject<RoleType> object = WebModelServiceUtils.loadObject(
                    getType(),
                    roleOid,
                    getOperationOptions(),
                    (PageBase) getPage(),
                    task,
                    task.getResult());
            if (object != null) {
                getObjectDetailsModels().reset();
                getObjectDetailsModels().reloadPrismObjectModel(object);
            }

            if (object != null) {
                var pageBase = (PageBase) getPage();
                var taskCreator = new MemberOperationsTaskCreator.Assign(
                        object.asObjectable(),
                        UserType.COMPLEX_TYPE,
                        createInOidQuery(userMembersOid),
                        pageBase,
                        RelationTypes.MEMBER.getRelation());

                pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                        .withOpResultOptions(OpResult.Options.create()
                                .withHideTaskLinks(false))
                        .withCustomFeedbackPanel(getFeedbackPanel())
                        .run(taskCreator::createAndSubmitTask);

            }

            businessRoleMigrationPerform(result, executedDeltas, target);
        }

        result.computeStatus();
        showResult(result);
        super.postProcessResultForWizard(result, executedDeltas, target);
    }

    protected ObjectQuery createInOidQuery(List<ObjectType> selectedObjectsList) {
        List<String> oids = new ArrayList<>();
        for (Object selectable : selectedObjectsList) {
            oids.add(((ObjectType) selectable).getOid());
        }

        return getPrismContext().queryFactory().createQuery(getPrismContext().queryFactory().createInOid(oids));
    }

    private void businessRoleMigrationPerform(
            OperationResult result,
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, AjaxRequestTarget target) {

        RoleAnalysisService roleAnalysisService = getRoleAnalysisService();

        Task task = createSimpleTask(OP_PERFORM_MIGRATION);

        String roleOid = ObjectDeltaOperation.findAddDeltaOidRequired(executedDeltas, RoleType.class);

        BusinessRoleApplicationDto patternDeltas = getObjectDetailsModels().getPatternDeltas();

        PrismObject<RoleType> roleObject = roleAnalysisService
                .getRoleTypeObject(roleOid, task, result);

        if (roleObject != null) {
            if (!patternDeltas.isCandidate()) {

                List<BusinessRoleDto> businessRoleDtos = patternDeltas.getBusinessRoleDtos();

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
                        patternDeltas.getCluster().getOid(), candidateRole, task, result);
                return;
            }

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
                roleAnalysisService.executeMigrationTask(getModelInteractionService(),
                        patternDeltas.getCluster(), activity, roleObject, taskOid, null, task, result);
                if (result.isWarning()) {
                    warn(result.getMessage());
                    target.add(((PageBase) getPage()).getFeedbackPanel());
                }
            }

        }
    }

    private boolean existPatternDeltas() {
        BusinessRoleApplicationDto patternDeltas = getPatternDeltas();
        return patternDeltas != null && !patternDeltas.getBusinessRoleDtos().isEmpty();
    }

    private BusinessRoleApplicationDto getPatternDeltas() {
        BusinessRoleApplicationDto patternDeltas = getObjectDetailsModels().getPatternDeltas();
        return patternDeltas;
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
