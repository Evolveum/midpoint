/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.component.*;
import com.evolveum.midpoint.web.page.admin.home.dto.*;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = {"/admin/dashboard", "/admin"}, action = {
        @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                label = PageAdminHome.AUTH_HOME_ALL_LABEL, description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#dashboard",
                label = "PageDashboard.auth.dashboard.label", description = "PageDashboard.auth.dashboard.description")})
public class PageDashboard extends PageAdminHome {

    private static final Trace LOGGER = TraceManager.getTrace(PageDashboard.class);

    private static final String DOT_CLASS = PageDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_WORK_ITEMS = DOT_CLASS + "loadWorkItems";

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_WORK_ITEMS = "workItems";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_SYSTEM_INFO = "systemInfo";

    private static final int MAX_WORK_ITEMS = 1000;

    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();

    public PageDashboard() {
        principalModel.setObject(loadUser());

        initLayout();
    }

    private PrismObject<UserType> loadUser() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();

        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = WebModelUtils.loadObject(UserType.class,
                principal.getOid(), result, PageDashboard.this);
        result.computeStatus();

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return user;
    }

    private void initLayout() {
        initPersonalInfo();
        initMyWorkItems();
        initMyAccounts();
        initAssignments();
        initSystemInfo();
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

        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        callableResult.setResult(result);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

        List<ObjectReferenceType> references = user.asObjectable().getLinkRef();
        for (ObjectReferenceType reference : references) {
            PrismObject<ShadowType> account = WebModelUtils.loadObject(ShadowType.class, reference.getOid(),
                    options, result, this);
            if (account == null) {
                continue;
            }

            ShadowType accountType = account.asObjectable();

            OperationResultType fetchResult = accountType.getFetchResult();

            if (fetchResult != null) {
                callableResult.getFetchResults().add(OperationResult.createOperationResult(fetchResult));
            }

            ResourceType resource = accountType.getResource();
            String resourceName = WebMiscUtil.getName(resource);
            list.add(new SimpleAccountDto(WebMiscUtil.getOrigStringFromPoly(accountType.getName()), resourceName));
        }
        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        LOGGER.debug("Finished accounts loading.");

        return callableResult;
    }

    private void initPersonalInfo() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO, null,
                createStringResource("PageDashboard.personalInfo"), "fa fa-fw fa-male", DashboardColor.GRAY) {

            @Override
            protected Component getMainComponent(String componentId) {
                return new PersonalInfoPanel(componentId);
            }
        };
        add(personalInfo);
    }

    private CallableResult<List<WorkItemDto>> loadWorkItems() {

        LOGGER.debug("Loading work items.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<WorkItemDto> list = new ArrayList<WorkItemDto>();
        callableResult.setValue(list);

        if (!getWorkflowManager().isEnabled()) {
            return callableResult;
        }

        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return callableResult;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_WORK_ITEMS);
        callableResult.setResult(result);

        try {
            List<WorkItemType> workItems = getWorkflowService().listWorkItemsRelatedToUser(user.getOid(),
                    true, 0, MAX_WORK_ITEMS, result);
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

    private void initSystemInfo() {
        AsyncDashboardPanel<Object, SystemInfoDto> systemInfo =
                new AsyncDashboardPanel<Object, SystemInfoDto>(ID_SYSTEM_INFO, createStringResource("PageDashboard.systemInfo"),
                        "fa fa-fw fa-tachometer", DashboardColor.GREEN) {

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<SystemInfoDto>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<SystemInfoDto>>(
                                getSecurityEnforcer(), auth) {

                            @Override
                            public CallableResult<SystemInfoDto> callWithContextPrepared() throws Exception {
                                CallableResult callableResult = new CallableResult();

                                //TODO - fill correct data in users and tasks graphs[shood]
                                SimplePieChartDto usersDto = new SimplePieChartDto("PageDashboard.activeUsers", 100, 25);
                                SimplePieChartDto tasksDto = new SimplePieChartDto("PageDashboard.activeTasks", 100, 35);
                                SimplePieChartDto loadDto = new SimplePieChartDto("PageDashboard.serverLoad", 100, WebMiscUtil.getSystemLoad(), "%");
                                SimplePieChartDto memDto = new SimplePieChartDto("PageDashboard.usedRam",
                                        WebMiscUtil.getMaxRam(), WebMiscUtil.getRamUsage(), "%");

                                SystemInfoDto sysInfoDto = new SystemInfoDto(usersDto, tasksDto, loadDto, memDto);

                                callableResult.setValue(sysInfoDto);
                                return callableResult;
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new SystemInfoPanel(markupId, new PropertyModel<SystemInfoDto>(getModel(), CallableResult.F_VALUE));
                    }
                };
        add(systemInfo);
    }

    private void initMyWorkItems() {
        AsyncDashboardPanel<Object, List<WorkItemDto>> workItems =
                new AsyncDashboardPanel<Object, List<WorkItemDto>>(ID_WORK_ITEMS, createStringResource("PageDashboard.workItems"),
                        "fa fa-fw fa-tasks", DashboardColor.RED) {

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<WorkItemDto>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<WorkItemDto>>>(
                                getSecurityEnforcer(), auth) {

                            @Override
                            public CallableResult<List<WorkItemDto>> callWithContextPrepared() throws Exception {
                                return loadWorkItems();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new WorkItemsPanel(markupId, new PropertyModel<List<WorkItemDto>>(getModel(), CallableResult.F_VALUE), false);
                    }
                };

        workItems.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getWorkflowManager().isEnabled();
            }
        });
        add(workItems);
    }

    private void initMyAccounts() {
        AsyncDashboardPanel<Object, List<SimpleAccountDto>> accounts =
                new AsyncDashboardPanel<Object, List<SimpleAccountDto>>(ID_ACCOUNTS, createStringResource("PageDashboard.accounts"),
                        "fa fa-fw fa-external-link", DashboardColor.BLUE) {

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
                            if (!WebMiscUtil.isSuccessOrHandledError(res)) {
                                page.showResult(res);
                            }
                        }
                    }
                };
        add(accounts);
    }

    private void initAssignments() {
        AsyncDashboardPanel<Object, List<AssignmentItemDto>> assignedOrgUnits =
                new AsyncDashboardPanel<Object, List<AssignmentItemDto>>(ID_ASSIGNMENTS, createStringResource("PageDashboard.assignments"),
                        "fa fa-fw fa-star", DashboardColor.YELLOW) {

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

        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);
        callableResult.setResult(result);

        PrismContainer assignments = user.findContainer(UserType.F_ASSIGNMENT);
        List<PrismContainerValue> values = assignments.getValues();
        for (PrismContainerValue assignment : values) {
            AssignmentItemDto item = createAssignmentItem(user, result, assignment);
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

    private AssignmentItemDto createAssignmentItem(PrismObject<UserType> user, OperationResult result,
                                                   PrismContainerValue assignment) {
        PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
        if (targetRef == null || targetRef.isEmpty()) {
            //account construction
            PrismContainer construction = assignment.findContainer(AssignmentType.F_CONSTRUCTION);
            String name = null;
            String description = null;
            if (construction.getValue().asContainerable() != null && !construction.isEmpty()) {
                ConstructionType constr = (ConstructionType) construction.getValue().asContainerable();
                description =  (String) construction.getPropertyRealValue(ConstructionType.F_DESCRIPTION, String.class);

                if (constr.getResourceRef() != null) {
                    ObjectReferenceType resourceRef = constr.getResourceRef();

                    PrismObject resource = WebModelUtils.loadObject(ResourceType.class, resourceRef.getOid(), result, this);
                    name = WebMiscUtil.getName(resource);
                }
            }

            return new AssignmentItemDto(AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION, name, description, null);
        }

        PrismReferenceValue refValue = targetRef.getValue();
        PrismObject value = refValue.getObject();
        if (value == null) {
            //resolve reference
            value = WebModelUtils.loadObject(ObjectType.class, refValue.getOid(), result, this);
        }

        if (value == null) {
            //we couldn't resolve assignment details
            return new AssignmentItemDto(null, null, null, null);
        }

        String name = WebMiscUtil.getName(value);
        AssignmentEditorDtoType type = AssignmentEditorDtoType.getType(value.getCompileTimeClass());
        String relation = refValue.getRelation() != null ? refValue.getRelation().getLocalPart() : null;
        String description = null;
        if (RoleType.class.isAssignableFrom(value.getCompileTimeClass())) {
            description = (String) value.getPropertyRealValue(RoleType.F_DESCRIPTION, String.class);
        }

        return new AssignmentItemDto(type, name, description, relation);
    }

    @Override
    public PageBase reinitialize() {
        return new PageDashboard();
    }
}
