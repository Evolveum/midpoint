/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsTaskCreator;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.ApplicationRoleWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.BusinessRoleWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;

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
    boolean isRmWizard = false;

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
        this.isRmWizard = true;
    }

    @Override
    protected void postProcessModel(AbstractRoleDetailsModel<RoleType> objectDetailsModels) {
        if (patternDeltas != null && !patternDeltas.getUserMembers().isEmpty()) {
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

    @Override
    protected boolean isApplicableTemplate() {
        if (isCreateFromRoleMining()) {
            return true;
        }
        return super.isApplicableTemplate();
    }

    @Override
    protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
        Collection<CompiledObjectCollectionView> applicableArchetypeViews = super.findAllApplicableArchetypeViews();
        if (!isCreateFromRoleMining()) {
            return applicableArchetypeViews;
        }
        //TODO restrict to only of business roles?
        return applicableArchetypeViews.stream()
                .filter(this::isBusinessRole)
                .toList();
//                .forEach(view -> view.getCollection().getCollectionRef().setFilter(null));
    }

    private boolean isBusinessRole(CompiledObjectCollectionView view) {
        String archetypeOid = view.getArchetypeOid();
        if (archetypeOid == null) {
            return false;
        }
        return getModelInteractionService().isSubarchetypeOrArchetype(archetypeOid, SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(), new OperationResult("check archetype"));
    }

    private boolean isCreateFromRoleMining() {
        return patternDeltas != null || getObjectDetailsModels().getPatternDeltas() != null;
    }

    @Override
    protected boolean canShowWizard() {
        return (!isHistoryPage() && !isEditObject()
                && isApplicationOrBusinessRole()) || isRmWizard;
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
            navigateToClusterOperationPanel((PageBase) getPage(), getPatternDeltas());
            return;
        }
        super.exitFromWizard();
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

    //TODO this is part of role mining. Consider moving it to different place
    @Override
    protected void postProcessResultForWizard(
            @NotNull OperationResult result,
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
            AjaxRequestTarget target) {

        if (result.isFatalError()) {
            return;
        }

        if (existPatternDeltas()) {
            List<String> userMembersOid = new ArrayList<>();

            Set<ObjectReferenceType> userMembers = getPatternDeltas().getUserMembers();
            userMembers.forEach(member -> {
                if (member != null && member.getOid() != null) {
                    userMembersOid.add(member.getOid());
                }
            });

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

            PageBase pageBase = (PageBase) getPage();
            businessRoleMigrationPerform(pageBase, getPatternDeltas(), executedDeltas, task, result, target);
        }

        result.computeStatus();
        showResult(result);
        super.postProcessResultForWizard(result, executedDeltas, target);
    }

    @Override
    protected void reloadObjectDetailsModel(PrismObject<RoleType> prismObject) {
        BusinessRoleApplicationDto patterns = getObjectDetailsModels().getPatternDeltas();
        super.reloadObjectDetailsModel(prismObject);
        getObjectDetailsModels().setPatternDeltas(patterns);
    }

    protected ObjectQuery createInOidQuery(List<String> selectedObjectsList) {
        return getPrismContext().queryFactory().createQuery(getPrismContext().queryFactory()
                .createInOid(selectedObjectsList != null ? selectedObjectsList : List.of()));
    }

    private boolean existPatternDeltas() {
        return getPatternDeltas() != null && !getPatternDeltas().getUserMembers().isEmpty();
    }

    private BusinessRoleApplicationDto getPatternDeltas() {
        return getObjectDetailsModels().getPatternDeltas();
    }

    protected boolean isHistoryPage() {
        return false;
    }

    @Override
    protected void addButtons(RepeatingView repeatingView) {
        super.addButtons(repeatingView);
        initMigrationButton(repeatingView);
    }

    private void initMigrationButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_MIGRATION_ICON,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("RoleMining.button.title.execute.migration")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageBase pageBase = (PageBase) getPage();
                ConfirmationPanel dialog = new DeleteConfirmationPanel(pageBase.getMainPopupBodyId(), createStringResource(
                        "RoleMining.button.title.execute.migration.confirmation.message")) {
                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        performMigration(target, pageBase);
                    }
                };
                pageBase.showMainPopup(dialog, target);
            }

            private void performMigration(AjaxRequestTarget target, @NotNull PageBase pageBase) {
                Task task = pageBase.createSimpleTask(OP_PERFORM_MIGRATION);
                OperationResult result = task.getResult();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                PrismObject<RoleType> prismObject = getPrismObject();
                roleAnalysisService.executeRoleMigrationProcess(pageBase.getModelInteractionService(), prismObject, task, result);
                result.computeStatus();
                if (result.isWarning()) {
                    warn(result.getMessage());
                    target.add(((PageBase) getPage()).getFeedbackPanel());
                } else {
                    success(createStringResource("RoleMining.task.migration.execute.success").getString());
                    target.add(((PageBase) getPage()).getFeedbackPanel());
                }
                refresh(target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append("class", "btn btn-default btn-sm"));
        migrationButton.add(new VisibleBehaviour(() -> !isActiveRole()));
        repeatingView.add(migrationButton);
    }

    private boolean isActiveRole() {
        return getObjectDetailsModels().getObjectType().getLifecycleState() == null
                || SchemaConstants.LIFECYCLE_ACTIVE.equals(getObjectDetailsModels().getObjectType().getLifecycleState());
    }
}
