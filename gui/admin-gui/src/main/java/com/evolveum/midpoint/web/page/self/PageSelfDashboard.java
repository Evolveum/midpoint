/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.self;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_WORK_ITEM_CREATED_TIMESTAMP;

import java.util.*;

import com.evolveum.midpoint.gui.api.PredefinedDashboardWidgetId;
import com.evolveum.midpoint.schema.util.AdminGuiConfigTypeUtil;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
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
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAssignmentsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.web.page.admin.workflow.ProcessInstancesPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDtoProvider;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.page.self.component.DashboardSearchPanel;
import com.evolveum.midpoint.web.page.self.component.LinksPanel;
import com.evolveum.midpoint.web.security.SecurityUtils;

import javax.xml.namespace.QName;

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

    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();
    private AdminGuiConfigurationType adminGuiConfig;

    public PageSelfDashboard() {
        adminGuiConfig = getPrincipal().getAdminGuiConfiguration();
        principalModel.setObject(loadUser());
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
        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL, null);
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

		AsyncDashboardPanel<Object, List<WorkItemDto>> workItemsPanel = new AsyncDashboardPanel<Object, List<WorkItemDto>>(
                ID_WORK_ITEMS_PANEL,
                createStringResource("PageSelfDashboard.workItems"),
                GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON,
                GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_BOX_CSS_CLASSES,
                true) {

					private static final long serialVersionUID = 1L;

					@Override
                    protected SecurityContextAwareCallable<CallableResult<List<WorkItemDto>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<WorkItemDto>>>(
                                getSecurityEnforcer(), auth) {

                        	private static final long serialVersionUID = 1L;

                            @Override
                            public CallableResult<List<WorkItemDto>> callWithContextPrepared() throws Exception {
								setupContext(application, session);	// TODO is this correct? [med]
                                return loadWorkItems();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
						ISortableDataProvider provider = new ListDataProvider(this,
                                new PropertyModel<List<WorkItemDto>>(getModel(), CallableResult.F_VALUE));
						return new WorkItemsPanel(markupId, provider, null, 10, WorkItemsPanel.View.DASHBOARD);
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

        AsyncDashboardPanel<Object, List<ProcessInstanceDto>> myRequestsPanel =
                new AsyncDashboardPanel<Object, List<ProcessInstanceDto>>(ID_REQUESTS_PANEL,
                        createStringResource("PageSelfDashboard.myRequests"),
                		 GuiStyleConstants.CLASS_SHADOW_ICON_REQUEST,
                        GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES, true) {

        			private static final long serialVersionUID = 1L;

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<ProcessInstanceDto>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<ProcessInstanceDto>>>(
                                getSecurityEnforcer(), auth) {
                        	private static final long serialVersionUID = 1L;

                            @Override
                            public CallableResult<List<ProcessInstanceDto>> callWithContextPrepared() throws Exception {
								setupContext(application, session);
                                return loadMyRequests();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
						ISortableDataProvider provider = new ListDataProvider(this,
                                new PropertyModel<List<ProcessInstanceDto>>(getModel(), CallableResult.F_VALUE));
                        return new ProcessInstancesPanel(markupId, provider, null, 10, ProcessInstancesPanel.View.DASHBOARD, null);
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
        
        initMyAccounts();
        initAssignments();
    }

    private CallableResult<List<WorkItemDto>> loadWorkItems() {

        LOGGER.debug("Loading work items.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<WorkItemDto> list = new ArrayList<>();
        callableResult.setValue(list);

        if (!getWorkflowManager().isEnabled()) {
            return callableResult;
        }

        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_WORK_ITEMS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);

        try {
            ObjectQuery query = QueryBuilder.queryFor(WorkItemType.class, getPrismContext())
                    .item(WorkItemType.F_ASSIGNEE_REF).ref(user.getOid())
                    .desc(F_WORK_ITEM_CREATED_TIMESTAMP)
                    .build();
            List<WorkItemType> workItems = getModelService().searchContainers(WorkItemType.class, query, null, task, result);
            for (WorkItemType workItem : workItems) {
                list.add(new WorkItemDto(workItem));
            }
        } catch (Exception e) {
            result.recordFatalError("Couldn't get list of work items.", e);
        }

        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        LOGGER.debug("Finished work items loading.");

        return callableResult;
    }

    private CallableResult<List<ProcessInstanceDto>> loadMyRequests() {

        LOGGER.debug("Loading requests.");

        AccountCallableResult<List<ProcessInstanceDto>> callableResult = new AccountCallableResult<>();
        List<ProcessInstanceDto> list = new ArrayList<ProcessInstanceDto>();
        callableResult.setValue(list);

        if (!getWorkflowManager().isEnabled()) {
            return callableResult;
        }

		ProcessInstanceDtoProvider provider = new ProcessInstanceDtoProvider(this, true, false);
		provider.iterator(0, Integer.MAX_VALUE);
		callableResult.setValue(provider.getAvailableData());

        LOGGER.debug("Finished requests loading.");

        return callableResult;
    }


    private PrismObject<UserType> loadUser() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        Validate.notNull(principal, "No principal");
        if (principal.getOid() == null) {
            throw new IllegalArgumentException("No OID in principal: "+principal);
        }

        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class,
                principal.getOid(), PageSelfDashboard.this, task, result);
        result.computeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return user;
    }

    private List<RichHyperlinkType> loadLinksList() {
        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return new ArrayList<RichHyperlinkType>();
        } else {
            return ((PageBase)getPage()).loadAdminGuiConfiguration().getUserDashboardLink();
        }
    }
    
    private void initMyAccounts() {
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
                                getSecurityEnforcer(), auth) {

                            @Override
                            public AccountCallableResult<List<SimpleAccountDto>> callWithContextPrepared()
                                    throws Exception {
                                return loadAccounts();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAccountsPanel(markupId,
                                new PropertyModel<List<SimpleAccountDto>>(getModel(), CallableResult.F_VALUE));
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
        List<SimpleAccountDto> list = new ArrayList<SimpleAccountDto>();
        callableResult.setValue(list);
        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);
        GetOperationOptions getOpts = GetOperationOptions.createResolve();
        getOpts.setNoFetch(Boolean.TRUE);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ShadowType.F_RESOURCE, getOpts);

        SelectorOptions<GetOperationOptions> resolveNamesOptions = new SelectorOptions(GetOperationOptions.createResolveNames());
        resolveNamesOptions.getOptions().setNoFetch(Boolean.TRUE);
        options.add(resolveNamesOptions);

        List<ObjectReferenceType> references = user.asObjectable().getLinkRef();
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

            ResourceType resource = accountType.getResource();
            String resourceName = WebComponentUtil.getName(resource);
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
                                getSecurityEnforcer(), auth) {

                            @Override
                            public CallableResult<List<AssignmentItemDto>> callWithContextPrepared() throws Exception {
                                return loadAssignments();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAssignmentsPanel(markupId,
                                new PropertyModel<List<AssignmentItemDto>>(getModel(), CallableResult.F_VALUE));
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
        List<AssignmentItemDto> list = new ArrayList<AssignmentItemDto>();
        callableResult.setValue(list);

        PrismObject<UserType> user = principalModel.getObject();
        if (user == null || user.findContainer(UserType.F_ASSIGNMENT) == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);

        PrismContainer assignments = user.findContainer(UserType.F_ASSIGNMENT);
        List<PrismContainerValue> values = assignments.getValues();
        for (PrismContainerValue assignment : values) {
            AssignmentItemDto item = createAssignmentItem(user, assignment, task, result);
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

	private AssignmentItemDto createAssignmentItem(PrismObject<UserType> user,
                                                   PrismContainerValue<AssignmentType> assignment,
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
			if (construction.getValue().asContainerable() != null && !construction.isEmpty()) {
				ConstructionType constr = (ConstructionType) construction.getValue().asContainerable();

				if (constr.getResourceRef() != null) {
					ObjectReferenceType resourceRef = constr.getResourceRef();

					PrismObject resource = WebModelServiceUtils.loadObject(ResourceType.class,
							resourceRef.getOid(), this, task, result);
					name = WebComponentUtil.getName(resource);
				}
			}

			return new AssignmentItemDto(AssignmentEditorDtoType.CONSTRUCTION, name, null, null);
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

		String name = WebComponentUtil.getDisplayNameOrName(value);
		AssignmentEditorDtoType type = AssignmentEditorDtoType.getType(value.getCompileTimeClass());
		String relation = refValue.getRelation() != null ? refValue.getRelation().getLocalPart() : null;
		String description = null;
		if (OrgType.class.isAssignableFrom(value.getCompileTimeClass())) {
			description = (String) value.getPropertyRealValue(OrgType.F_IDENTIFIER, String.class);
		}

		return new AssignmentItemDto(type, name, description, relation);
	}

	private UserInterfaceElementVisibilityType getComponentVisibility(PredefinedDashboardWidgetId componentId){
        if (adminGuiConfig == null || adminGuiConfig.getUserDashboard() == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        List<DashboardWidgetType> widgetsList = adminGuiConfig.getUserDashboard().getWidget();
        if (widgetsList == null || widgetsList.size() == 0){
            return UserInterfaceElementVisibilityType.VACANT;
        }
        DashboardWidgetType widget = AdminGuiConfigTypeUtil.findWidget(adminGuiConfig.getUserDashboard(),
                componentId.getUri());
        if (widget == null || widget.getVisibility() == null){
            return UserInterfaceElementVisibilityType.HIDDEN;
        } else {
            return widget.getVisibility();
        }
    }
}
