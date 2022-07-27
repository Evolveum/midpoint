/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import static java.util.Collections.emptyList;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.F_CREATE_TIMESTAMP;

import java.util.*;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.PredefinedDashboardWidgetId;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.CaseWorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAssignmentsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.web.page.admin.server.CasesTablePanel;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.DashboardSearchPanel;
import com.evolveum.midpoint.web.page.self.component.LinksPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Viliam Repan (lazyman)
 * @author Kate Honchar
 */
@PageDescriptor(
        urls = {
//                @Url(mountUrl = "/self", matchUrlForSecurity = "/self"),
                @Url(mountUrl = "/self/dashboardOld")
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
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";

    private transient Application application;

    private Session session;

    public PageSelfDashboard() {
        setTimeZone();
        initLayout();


        session = getSession();
        application = session.getApplication();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model<>("fa fa-tachometer-alt"));
    }

    private void initLayout() {
        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL);
        List<String> searchPanelActions = Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL, AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL);
        dashboardSearchPanel.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SEARCH);
            return WebComponentUtil.getElementVisibility(visibility, searchPanelActions);
        }));
        add(dashboardSearchPanel);

        LinksPanel linksPanel = new LinksPanel(ID_LINKS_PANEL, Model.ofList(loadLinksList()));
        linksPanel.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SHORTCUTS);
            return WebComponentUtil.getElementVisibility(visibility);
        }));
        add(linksPanel);

        AsyncDashboardPanel<Object, List<CaseWorkItemType>> workItemsPanel = new AsyncDashboardPanel<>(
                ID_WORK_ITEMS_PANEL,
                createStringResource("PageSelfDashboard.workItems"),
                GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON,
                GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_BOX_CSS_CLASSES,
                true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected SecurityContextAwareCallable<CallableResult<List<CaseWorkItemType>>> createCallable(
                    Authentication auth, IModel callableParameterModel) {

                return new SecurityContextAwareCallable<>(
                        getSecurityContextManager(), auth) {

                    @Override
                    public CallableResult<List<CaseWorkItemType>> callWithContextPrepared() {
                        return new CallableResult<>(emptyList(), null); // it is ignored anyway - FIXME
                    }
                };
            }

            @Override
            protected Component getMainComponent(String markupId) {
                CaseWorkItemsPanel workItemsPanel = new CaseWorkItemsPanel(markupId, CaseWorkItemsPanel.View.DASHBOARD) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectFilter getCaseWorkItemsFilter() {
                        return QueryUtils.filterForNotClosedStateAndAssignees(getPrismContext().queryFor(CaseWorkItemType.class),
                                        AuthUtil.getPrincipalUser(),
                                        OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
                                .desc(F_CREATE_TIMESTAMP)
                                .buildFilter();
                    }
                };
                workItemsPanel.setOutputMarkupId(true);
                return workItemsPanel;
            }
        };

        workItemsPanel.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.MY_WORKITEMS);
            return getCaseManager().isEnabled() && WebComponentUtil.getElementVisibility(visibility);
        }));
        add(workItemsPanel);

        AsyncDashboardPanel<Object, List<CaseType>> myRequestsPanel =
                new AsyncDashboardPanel<>(ID_REQUESTS_PANEL,
                        createStringResource("PageSelfDashboard.myRequests"),
                        GuiStyleConstants.CLASS_SHADOW_ICON_REQUEST,
                        GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES, true) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<CaseType>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<>(
                                getSecurityContextManager(), auth) {

                            @Override
                            public CallableResult<List<CaseType>> callWithContextPrepared() {
                                return new CallableResult<>(emptyList(), null); // it is ignored anyway - FIXME
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new CasesTablePanel(markupId) {

                            @Override
                            protected void onBeforeRender() {
                                super.onBeforeRender();

                                getTable().setShowAsCard(false);
                            }

                            @Override
                            protected ObjectFilter getCasesFilter() {
                                return QueryUtils.filterForMyRequests(getPrismContext().queryFor(CaseType.class),
                                                AuthUtil.getPrincipalUser().getOid())
                                        .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                                        .buildFilter();
                            }

                            @Override
                            protected boolean isDashboard() {
                                return true;
                            }

                            @Override
                            protected UserProfileStorage.TableId getTableId() {
                                return UserProfileStorage.TableId.PAGE_CASE_CHILD_CASES_TAB;
                            }
                        };
                    }
                };

        myRequestsPanel.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.MY_REQUESTS);
            return getCaseManager().isEnabled() && WebComponentUtil.getElementVisibility(visibility);
        }));
        add(myRequestsPanel);

        initMyAccounts();
        initAssignments();
    }

    private List<RichHyperlinkType> loadLinksList() {
        return ((PageBase) getPage()).getCompiledGuiProfile().getUserDashboardLink();
    }

    private void initMyAccounts() {
        AsyncDashboardPanel<Object, List<SimpleAccountDto>> accounts = new AsyncDashboardPanel<>(ID_ACCOUNTS,
                createStringResource("PageDashboard.accounts"),
                GuiStyleConstants.CLASS_SHADOW_ICON_ACCOUNT,
                GuiStyleConstants.CLASS_OBJECT_SHADOW_BOX_CSS_CLASSES,
                true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected SecurityContextAwareCallable<CallableResult<List<SimpleAccountDto>>> createCallable(
                    Authentication auth, IModel<Object> callableParameterModel) {

                return new SecurityContextAwareCallable<>(
                        getSecurityContextManager(), auth) {

                    @Override
                    public AccountCallableResult<List<SimpleAccountDto>> callWithContextPrepared() {
                        setupContext(application, session);
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
        accounts.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.MY_ACCOUNTS);
            return WebComponentUtil.getElementVisibility(visibility);
        }));
        add(accounts);
    }

    private AccountCallableResult<List<SimpleAccountDto>> loadAccounts() {
        LOGGER.debug("Loading accounts.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<SimpleAccountDto> list = new ArrayList<>();
        callableResult.setValue(list);
        FocusType focus = AuthUtil.getPrincipalUser().getFocus();

        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .root().resolveNames().noFetch()
                .item(ShadowType.F_RESOURCE_REF).resolve().noFetch()
                .build();
        List<ObjectReferenceType> references = focus.getLinkRef();
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
        AsyncDashboardPanel<Object, List<AssignmentItemDto>> assignedOrgUnits = new AsyncDashboardPanel<>(ID_ASSIGNMENTS,
                createStringResource("PageDashboard.assignments"),
                GuiStyleConstants.CLASS_ICON_ASSIGNMENTS,
                GuiStyleConstants.CLASS_OBJECT_ROLE_BOX_CSS_CLASSES,
                true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected SecurityContextAwareCallable<CallableResult<List<AssignmentItemDto>>> createCallable(
                    Authentication auth, IModel callableParameterModel) {

                return new SecurityContextAwareCallable<>(
                        getSecurityContextManager(), auth) {

                    @Override
                    public CallableResult<List<AssignmentItemDto>> callWithContextPrepared() {
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
        assignedOrgUnits.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.MY_ASSIGNMENTS);
            return WebComponentUtil.getElementVisibility(visibility);
        }));
        add(assignedOrgUnits);
    }

    private CallableResult<List<AssignmentItemDto>> loadAssignments() {
        LOGGER.debug("Loading assignments.");
        CallableResult callableResult = new CallableResult();
        List<AssignmentItemDto> list = new ArrayList<>();
        callableResult.setValue(list);

        FocusType focus = AuthUtil.getPrincipalUser().getFocus();
        if (focus.getAssignment().isEmpty()) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);

        List<AssignmentType> assignments = focus.getAssignment();
        for (AssignmentType assignment : assignments) {
            if (assignment.getTargetRef() != null && ArchetypeType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
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

    private AssignmentItemDto createAssignmentItem(AssignmentType assignment,
            Task task, OperationResult result) {
        ActivationType activation = assignment.getActivation();
        if (activation != null && activation.getAdministrativeStatus() != null
                && !activation.getAdministrativeStatus().equals(ActivationStatusType.ENABLED)) {
            return null;
        }
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef == null) {
            // account construction
            ConstructionType construction = assignment.getConstruction();
            String name = null;
            String description = "";
            if (construction != null) {
                if (construction.getResourceRef() != null) {
                    ObjectReferenceType resourceRef = construction.getResourceRef();

                    PrismObject resource = WebModelServiceUtils.loadObject(ResourceType.class,
                            resourceRef.getOid(), this, task, result);
                    name = WebComponentUtil.getName(resource, false);
                    description = construction.getDescription();
                }
            }

            return new AssignmentItemDto(AssignmentEditorDtoType.CONSTRUCTION, name, description, null);
        }

        if (RelationTypes.APPROVER.getRelation().equals(assignment.getTargetRef().getRelation()) ||
                RelationTypes.OWNER.getRelation().equals(assignment.getTargetRef().getRelation())) {
            return null;
        }
        PrismObject value = targetRef.getObject();
        if (value == null) {
            // resolve reference
            value = WebModelServiceUtils.loadObject(ObjectType.class, targetRef.getOid(), this, task, result);
        }

        if (value == null) {
            // we couldn't resolve assignment details
            return new AssignmentItemDto(null, null, null, null);
        }

        String name = WebComponentUtil.getDisplayNameOrName(value, false);
        AssignmentEditorDtoType type = AssignmentEditorDtoType.getType(value.getCompileTimeClass());
        String relation = targetRef.getRelation() != null ? targetRef.getRelation().getLocalPart() : null;

        return new AssignmentItemDto(type, name, getAssignmentDescription(value), relation);
    }

    private String getAssignmentDescription(PrismObject value) {
        Object orgIdentifier = null;
        if (OrgType.class.isAssignableFrom(value.getCompileTimeClass())) {
            orgIdentifier = value.getPropertyRealValue(OrgType.F_IDENTIFIER, String.class);
        }
        return (orgIdentifier != null ? orgIdentifier + " " : "") +
                (value.asObjectable() instanceof ObjectType && value.asObjectable().getDescription() != null ?
                        value.asObjectable().getDescription() : "");
    }

    private UserInterfaceElementVisibilityType getComponentVisibility(PredefinedDashboardWidgetId componentId) {
        CompiledGuiProfile compiledGuiProfile = getCompiledGuiProfile();
        if (compiledGuiProfile.getUserDashboard() == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        List<DashboardWidgetType> widgetsList = compiledGuiProfile.getUserDashboard().getWidget();
        if (widgetsList == null || widgetsList.size() == 0) {
            return UserInterfaceElementVisibilityType.VACANT;
        }
        DashboardWidgetType widget = compiledGuiProfile.findUserDashboardWidget(componentId.getUri());
        if (widget == null || widget.getVisibility() == null) {
            return UserInterfaceElementVisibilityType.HIDDEN;
        } else {
            return widget.getVisibility();
        }
    }
}
