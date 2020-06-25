/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.F_CREATE_TIMESTAMP;
import static java.util.Collections.emptyList;

import java.util.*;

import com.evolveum.midpoint.gui.api.PredefinedDashboardWidgetId;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.page.admin.cases.CaseWorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.cases.CasesListPanel;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAssignmentsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.web.page.self.component.DashboardSearchPanel;
import com.evolveum.midpoint.web.page.self.component.LinksPanel;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * @author Viliam Repan (lazyman)
 * @author Kate Honchar
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self", matchUrlForSecurity = "/self"),
                @Url(mountUrl = "/self/dashboard")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL,
                        label = "PageSelfDashboard.auth.dashboard.label",
                        description = "PageSelfDashboard.auth.dashboard.description")
        })
public class PageSelfDashboard extends PageSelf {
    private static final Trace LOGGER = TraceManager.getTrace(PageSelfDashboard.class);

    private static final String ID_LINKS_PANEL = "linksPanel";
    private static final String ID_WORK_ITEMS_PANEL = "workItemsPanel";
    private static final String ID_SEARCH_PANEL = "searchPanel";
    private static final String ID_REQUESTS_PANEL = "requestPanel";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";

    private static final String DOT_CLASS = PageSelfDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEMS = DOT_CLASS + "loadWorkItems";
    private static final String OPERATION_LOAD_REQUESTS = DOT_CLASS + "loadRequests";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";

    private static final int MAX_WORK_ITEMS = 1000;
    private static final int MAX_REQUESTS = 1000;

    private final Model<PrismObject<? extends FocusType>> principalModel = new Model<>();
    private CompiledGuiProfile compiledGuiProfile;

    public PageSelfDashboard() {
        compiledGuiProfile = getPrincipal().getCompiledGuiProfile();
        principalModel.setObject(loadFocus());
        setTimeZone(PageSelfDashboard.this);
        initLayout();
    }

    private transient Application application;

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model("fa fa-dashboard"));
    }

    private void initLayout(){
        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL);
        List<String> searchPanelActions = Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL, AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL);
        dashboardSearchPanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                UserInterfaceElementVisibilityType visibilityType = getComponentVisibility(PredefinedDashboardWidgetId.SEARCH);
                return WebComponentUtil.getElementVisibility(visibilityType, searchPanelActions);
            }
        });
        add(dashboardSearchPanel);

        LinksPanel linksPanel = new LinksPanel(ID_LINKS_PANEL, Model.ofList(loadLinksList()));
        linksPanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                UserInterfaceElementVisibilityType visibilityType = getComponentVisibility(PredefinedDashboardWidgetId.SHORTCUTS);
                return WebComponentUtil.getElementVisibility(visibilityType);
            }
        });
        add(linksPanel);

        // TODO is this correct? [med]
        application = getApplication();
        final Session session = Session.get();

        AsyncDashboardPanel<Object, List<CaseWorkItemType>> workItemsPanel = new AsyncDashboardPanel<Object, List<CaseWorkItemType>>(
                ID_WORK_ITEMS_PANEL,
                createStringResource("PageSelfDashboard.workItems"),
                GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON,
                GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_BOX_CSS_CLASSES,
                true) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<CaseWorkItemType>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<CaseWorkItemType>>>(
                                getSecurityContextManager(), auth) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public CallableResult<List<CaseWorkItemType>> callWithContextPrepared() {
                                return new CallableResult<>(emptyList(), null); // it is ignored anyway - FIXME
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        CaseWorkItemsPanel workItemsPanel = new CaseWorkItemsPanel(markupId, CaseWorkItemsPanel.View.DASHBOARD){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected ObjectFilter getCaseWorkItemsFilter(){
                                return QueryUtils.filterForNotClosedStateAndAssignees(getPrismContext().queryFor(CaseWorkItemType.class),
                                        SecurityUtils.getPrincipalUser(),
                                        OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
                                        .desc(F_CREATE_TIMESTAMP)
                                        .buildFilter();
                            }
                        };
                        workItemsPanel.setOutputMarkupId(true);
                        return workItemsPanel;
                    }
                };

        workItemsPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                UserInterfaceElementVisibilityType visibilityType = getComponentVisibility(PredefinedDashboardWidgetId.MY_WORKITEMS);
                return getWorkflowManager().isEnabled() && WebComponentUtil.getElementVisibility(visibilityType);
            }
        });
        add(workItemsPanel);

        AsyncDashboardPanel<Object, List<CaseType>> myRequestsPanel =
                new AsyncDashboardPanel<Object, List<CaseType>>(ID_REQUESTS_PANEL,
                        createStringResource("PageSelfDashboard.myRequests"),
                         GuiStyleConstants.CLASS_SHADOW_ICON_REQUEST,
                        GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES, true) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<CaseType>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<CaseType>>>(
                                getSecurityContextManager(), auth) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public CallableResult<List<CaseType>> callWithContextPrepared() {
                                return new CallableResult<>(emptyList(), null); // it is ignored anyway - FIXME
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        CasesListPanel casesPanel = new CasesListPanel(markupId) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected ObjectFilter getCasesFilter() {
                                return QueryUtils.filterForMyRequests(getPrismContext().queryFor(CaseType.class),
                                        SecurityUtils.getPrincipalUser().getOid())
                                        .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                                        .buildFilter();
                            }

                            @Override
                            protected boolean isDashboard(){
                                return true;
                            }
                        };
                        return casesPanel;
                    }
                };

        myRequestsPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                UserInterfaceElementVisibilityType visibilityType = getComponentVisibility(PredefinedDashboardWidgetId.MY_REQUESTS);
                return getWorkflowManager().isEnabled() && WebComponentUtil.getElementVisibility(visibilityType);

            }
        });
        add(myRequestsPanel);

        initMyAccounts(session);
        initAssignments();
    }

    private PrismObject<? extends FocusType> loadFocus() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        Validate.notNull(principal, "No principal");
        if (principal.getOid() == null) {
            throw new IllegalArgumentException("No OID in principal: "+principal);
        }

        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<? extends FocusType> focus = WebModelServiceUtils.loadObject(FocusType.class,
                principal.getOid(), PageSelfDashboard.this, task, result);
        result.computeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return focus;
    }

    private List<RichHyperlinkType> loadLinksList() {
        PrismObject<? extends FocusType> focus = principalModel.getObject();
        if (focus == null) {
            return new ArrayList<>();
        } else {
            return ((PageBase) getPage()).getCompiledGuiProfile().getUserDashboardLink();
        }
    }

    private void initMyAccounts(Session session) {
        AsyncDashboardPanel<Object, List<SimpleAccountDto>> accounts = new AsyncDashboardPanel<Object, List<SimpleAccountDto>>(ID_ACCOUNTS,
                        createStringResource("PageDashboard.accounts"),
                        GuiStyleConstants.CLASS_SHADOW_ICON_ACCOUNT,
                        GuiStyleConstants.CLASS_OBJECT_SHADOW_BOX_CSS_CLASSES,
                        true) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<SimpleAccountDto>>> createCallable(
                            Authentication auth, IModel<Object> callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<SimpleAccountDto>>>(
                                getSecurityContextManager(), auth) {

                            @Override
                            public AccountCallableResult<List<SimpleAccountDto>> callWithContextPrepared()
                                    throws Exception {
                                setupContext(application, session);    // TODO is this correct? [med]
                                return loadAccounts();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAccountsPanel(markupId,
                            new PropertyModel<>(getModel(), CallableResult.F_VALUE));
                    }

                    @Override
                    protected void onPostSuccess(AjaxRequestTarget target) {
                        showFetchResult();
                        super.onPostSuccess(target);
                    }

                    @Override
                    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
                        showFetchResult();
                        super.onUpdateError(target, ex);
                    }

                    private void showFetchResult() {
                        AccountCallableResult<List<SimpleAccountDto>> result =
                                (AccountCallableResult<List<SimpleAccountDto>>) getModel().getObject();

                        PageBase page = (PageBase) getPage();
                        for (OperationResult res : result.getFetchResults()) {
                            if (!WebComponentUtil.isSuccessOrHandledError(res)) {
                                page.showResult(res);
                            }
                        }
                    }
                };
        accounts.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                UserInterfaceElementVisibilityType visibilityType = getComponentVisibility(PredefinedDashboardWidgetId.MY_ACCOUNTS);
                return WebComponentUtil.getElementVisibility(visibilityType);
            }
        });
        add(accounts);
    }

    private AccountCallableResult<List<SimpleAccountDto>> loadAccounts() throws Exception {
        LOGGER.debug("Loading accounts.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<SimpleAccountDto> list = new ArrayList<>();
        callableResult.setValue(list);
        PrismObject<? extends FocusType> focus = principalModel.getObject();
        if (focus == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .root().resolveNames().noFetch()
                .item(ShadowType.F_RESOURCE_REF).resolve().noFetch()
                .build();
        List<ObjectReferenceType> references = focus.asObjectable().getLinkRef();
        for (ObjectReferenceType reference : references) {
            PrismObject<ShadowType> account = WebModelServiceUtils.loadObject(ShadowType.class, reference.getOid(),
                    options, this, task, result);
            if (account == null) {
                continue;
            }

            ShadowType accountType = account.asObjectable();

            OperationResultType fetchResult = accountType.getFetchResult();

            if (fetchResult != null) {
                callableResult.getFetchResults().add(OperationResult.createOperationResult(fetchResult));
            }

            String resourceName = WebComponentUtil.getName(accountType.getResourceRef());
            list.add(new SimpleAccountDto(WebComponentUtil.getOrigStringFromPoly(accountType.getName()), resourceName));
        }
        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        LOGGER.debug("Finished accounts loading.");

        return callableResult;
    }

    private void initAssignments() {
        AsyncDashboardPanel<Object, List<AssignmentItemDto>> assignedOrgUnits = new AsyncDashboardPanel<Object, List<AssignmentItemDto>>(ID_ASSIGNMENTS,
                createStringResource("PageDashboard.assignments"),
                GuiStyleConstants.CLASS_ICON_ASSIGNMENTS,
                GuiStyleConstants.CLASS_OBJECT_ROLE_BOX_CSS_CLASSES,
                true) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<AssignmentItemDto>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<AssignmentItemDto>>>(
                                getSecurityContextManager(), auth) {

                            @Override
                            public CallableResult<List<AssignmentItemDto>> callWithContextPrepared() throws Exception {
                                return loadAssignments();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAssignmentsPanel(markupId,
                            new PropertyModel<>(getModel(), CallableResult.F_VALUE));
                    }
                };
        assignedOrgUnits.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                UserInterfaceElementVisibilityType visibilityType = getComponentVisibility(PredefinedDashboardWidgetId.MY_ASSIGNMENTS);
                return WebComponentUtil.getElementVisibility(visibilityType);
            }
        });
        add(assignedOrgUnits);

    }

    private CallableResult<List<AssignmentItemDto>> loadAssignments() throws Exception {
        LOGGER.debug("Loading assignments.");
        CallableResult callableResult = new CallableResult();
        List<AssignmentItemDto> list = new ArrayList<>();
        callableResult.setValue(list);

        PrismObject<? extends FocusType> focus = principalModel.getObject();
        if (focus == null || focus.findContainer(UserType.F_ASSIGNMENT) == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);

        PrismContainer assignments = focus.findContainer(UserType.F_ASSIGNMENT);
        List<PrismContainerValue> values = assignments.getValues();
        for (PrismContainerValue assignment : values) {
            AssignmentType assignmentType = (AssignmentType)assignment.asContainerable();
            if (assignmentType.getTargetRef() != null && ArchetypeType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())){
                continue;
            }
            AssignmentItemDto item = createAssignmentItem(assignment, task, result);
            if (item != null) {
                list.add(item);
            }
        }
        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        Collections.sort(list);

        LOGGER.debug("Finished assignments loading.");

        return callableResult;
    }

    private AssignmentItemDto createAssignmentItem(PrismContainerValue<AssignmentType> assignment,
            Task task, OperationResult result) {
        ActivationType activation = assignment.asContainerable().getActivation();
        if (activation != null && activation.getAdministrativeStatus() != null
                && !activation.getAdministrativeStatus().equals(ActivationStatusType.ENABLED)) {
            return null;
        }
        PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
        if (targetRef == null || targetRef.isEmpty()) {
            // account construction
            PrismContainer construction = assignment.findContainer(AssignmentType.F_CONSTRUCTION);
            String name = null;
            String description = "";
            if (construction.getRealValue() != null && !construction.isEmpty()) {
                ConstructionType constr = (ConstructionType) construction.getRealValue();

                if (constr.getResourceRef() != null) {
                    ObjectReferenceType resourceRef = constr.getResourceRef();

                    PrismObject resource = WebModelServiceUtils.loadObject(ResourceType.class,
                            resourceRef.getOid(), this, task, result);
                    name = WebComponentUtil.getName(resource, false);
                    description = constr.getDescription();
                }
            }

            return new AssignmentItemDto(AssignmentEditorDtoType.CONSTRUCTION, name, description, null);
        }

        if (RelationTypes.APPROVER.getRelation().equals(assignment.getValue().getTargetRef().getRelation()) ||
                RelationTypes.OWNER.getRelation().equals(assignment.getValue().getTargetRef().getRelation())){
            return null;
        }
        PrismReferenceValue refValue = targetRef.getValue();
        PrismObject value = refValue.getObject();
        if (value == null) {
            // resolve reference
            value = WebModelServiceUtils.loadObject(ObjectType.class, refValue.getOid(), this, task, result);
        }

        if (value == null) {
            // we couldn't resolve assignment details
            return new AssignmentItemDto(null, null, null, null);
        }

        String name = WebComponentUtil.getDisplayNameOrName(value, false);
        AssignmentEditorDtoType type = AssignmentEditorDtoType.getType(value.getCompileTimeClass());
        String relation = refValue.getRelation() != null ? refValue.getRelation().getLocalPart() : null;

        return new AssignmentItemDto(type, name, getAssignmentDescription(value), relation);
    }

    private String getAssignmentDescription(PrismObject value){
        Object orgIdentifier = null;
        if (OrgType.class.isAssignableFrom(value.getCompileTimeClass())) {
            orgIdentifier = value.getPropertyRealValue(OrgType.F_IDENTIFIER, String.class);
        }
        String description = (orgIdentifier != null ? orgIdentifier + " " : "") +
                (value.asObjectable() instanceof ObjectType && value.asObjectable().getDescription() != null ?
                        value.asObjectable().getDescription() : "");
        return description;
    }

    private UserInterfaceElementVisibilityType getComponentVisibility(PredefinedDashboardWidgetId componentId){
        if (compiledGuiProfile == null || compiledGuiProfile.getUserDashboard() == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        List<DashboardWidgetType> widgetsList = compiledGuiProfile.getUserDashboard().getWidget();
        if (widgetsList == null || widgetsList.size() == 0){
            return UserInterfaceElementVisibilityType.VACANT;
        }
        DashboardWidgetType widget = compiledGuiProfile.findUserDashboardWidget(componentId.getUri());
        if (widget == null || widget.getVisibility() == null){
            return UserInterfaceElementVisibilityType.HIDDEN;
        } else {
            return widget.getVisibility();
        }
    }
}
