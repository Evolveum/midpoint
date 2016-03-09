/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardColor;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAssignmentsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.component.SystemInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
@PageDescriptor(url = {"/admin/dashboard", "/admin"}, action = {
        @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                label = PageAdminHome.AUTH_HOME_ALL_LABEL, description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                label = "PageDashboard.auth.dashboard.label", description = "PageDashboard.auth.dashboard.description")})
public class PageDashboard extends PageAdminHome {

    private static final Trace LOGGER = TraceManager.getTrace(PageDashboard.class);

    private static final String DOT_CLASS = PageDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_SYSTEM_INFO = "systemInfo";

    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();

    public PageDashboard() {
        principalModel.setObject(loadUserSelf(PageDashboard.this));
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getSessionStorage().peekBreadcrumb();
        bc.setIcon(new Model("fa fa-dashboard"));
    }

    private void initLayout() {
        initPersonalInfo();
        initMyAccounts();
        initAssignments();
        initSystemInfo();
        
//        DraggablesAcceptedByDroppable defaultValues = new DraggablesAcceptedByDroppable("one", "two", "three");
//        DraggableElement draggable1 = new DraggableElement("draggable1");
//        add(draggable1);
//        
//       
//        draggable1.setOutputMarkupId(true);
//
//
//        DroppableElement droppable1 = new DroppableElement("droppable1", defaultValues);
//        add(droppable1);
//        
//        add(new DraggableAndDroppableElement("draggableDroppable1", new Model<String>("Drag me!"), defaultValues));
//
//        
//        DraggableElement draggable2 = new DraggableElement("draggable2");
//        add(draggable2);
//        draggable2.setOutputMarkupId(true);
//
//        add(new DraggableAndDroppableElement("draggableDroppable2", new Model<String>("Drag me!"), defaultValues));
//
//        add(new DroppableElement("droppable2", defaultValues));
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
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

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

    private void initSystemInfo() {
        DashboardPanel systemInfo = new DashboardPanel(ID_SYSTEM_INFO, null,
                createStringResource("PageDashboard.systemInfo"), "fa fa-tachometer", DashboardColor.GREEN) {

            @Override
            protected Component getMainComponent(String componentId) {
                return new SystemInfoPanel(componentId);
            }
        };
        add(systemInfo);
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
                            if (!WebComponentUtil.isSuccessOrHandledError(res)) {
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
                                                   PrismContainerValue assignment, Task task, OperationResult result) {
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

                    PrismObject resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceRef.getOid(), this, task, result);
                    name = WebComponentUtil.getName(resource);
                }
            }

            return new AssignmentItemDto(AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION, name, description, null);
        }

        PrismReferenceValue refValue = targetRef.getValue();
        PrismObject value = refValue.getObject();
        if (value == null) {
            //resolve reference
            value = WebModelServiceUtils.loadObject(ObjectType.class, refValue.getOid(), this, task, result);
        }

        if (value == null) {
            //we couldn't resolve assignment details
            return new AssignmentItemDto(null, null, null, null);
        }

        String name = WebComponentUtil.getName(value);
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
