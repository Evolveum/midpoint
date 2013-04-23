/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.async.CallableResult;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.component.*;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.MyWorkItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.wf.api.WorkItem;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author lazyman
 */
public class PageDashboard extends PageAdminHome {

    private static final Trace LOGGER = TraceManager.getTrace(PageDashboard.class);

    private static final String DOT_CLASS = PageDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
    private static final String OPERATION_LOAD_WORK_ITEMS = DOT_CLASS + "loadWorkItems";

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_WORK_ITEMS = "workItems";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";

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

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return user;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(PageDashboard.class, "PageDashboard.css")));
    }

    private void initLayout() {
        initPersonalInfo();
        initMyWorkItems();
        initMyAccounts();
        initAssignments();
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
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);

            PrismObject<ShadowType> account = WebModelUtils.loadObjectAsync(ShadowType.class, reference.getOid(),
                    options, subResult, this, user);
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
                createStringResource("PageDashboard.personalInfo")) {

            @Override
            protected Component getMainComponent(String componentId) {
                return new PersonalInfoPanel(componentId);
            }
        };
        add(personalInfo);
    }

    private CallableResult<List<MyWorkItemDto>> loadWorkItems() {
        LOGGER.debug("Loading work items.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<MyWorkItemDto> list = new ArrayList<MyWorkItemDto>();
        callableResult.setValue(list);
        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return callableResult;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_WORK_ITEMS);
        callableResult.setResult(result);

        try {
            List<WorkItem> workItems = getWorkflowService().listWorkItemsRelatedToUser(user.getOid(),
                    true, 0, MAX_WORK_ITEMS, result);
            for (WorkItem workItem : workItems) {
                list.add(new MyWorkItemDto(workItem));
            }
        } catch (WorkflowException e) {
            result.recordFatalError("Couldn't get list of work items.", e);
        }

        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        LOGGER.debug("Finished work items loading.");

        return callableResult;
    }

    private void initMyWorkItems() {
        AsyncDashboardPanel<Object, List<MyWorkItemDto>> workItems =
                new AsyncDashboardPanel<Object, List<MyWorkItemDto>>(ID_WORK_ITEMS,
                        createStringResource("PageDashboard.workItems")) {

                    @Override
                    protected Callable<CallableResult<List<MyWorkItemDto>>> createCallable(IModel callableParameterModel) {
                        return new Callable<CallableResult<List<MyWorkItemDto>>>() {

                            @Override
                            public CallableResult<List<MyWorkItemDto>> call() throws Exception {
                                return loadWorkItems();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyWorkItemsPanel(markupId,
                                new PropertyModel<List<MyWorkItemDto>>(getModel(), CallableResult.F_VALUE));
                    }
                };
        add(workItems);
    }

    private void initMyAccounts() {
        AsyncDashboardPanel<Object, List<SimpleAccountDto>> accounts =
                new AsyncDashboardPanel<Object, List<SimpleAccountDto>>(ID_ACCOUNTS,
                        createStringResource("PageDashboard.accounts")) {

                    @Override
                    protected Callable<CallableResult<List<SimpleAccountDto>>> createCallable(
                            IModel<Object> callableParameterModel) {

                        return new Callable<CallableResult<List<SimpleAccountDto>>>() {

                            @Override
                            public AccountCallableResult<List<SimpleAccountDto>> call() throws Exception {
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
                new AsyncDashboardPanel<Object, List<AssignmentItemDto>>(ID_ASSIGNMENTS,
                        createStringResource("PageDashboard.assignments")) {

                    @Override
                    protected Callable<CallableResult<List<AssignmentItemDto>>> createCallable(IModel callableParameterModel) {
                        return new Callable<CallableResult<List<AssignmentItemDto>>>() {

                            @Override
                            public CallableResult<List<AssignmentItemDto>> call() throws Exception {
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
            PrismProperty construction = assignment.findProperty(AssignmentType.F_ACCOUNT_CONSTRUCTION);
            String name = null;
            String description = null;
            if (construction != null && !construction.isEmpty()) {
                ConstructionType constr = (ConstructionType)
                        construction.getRealValue(ConstructionType.class);
                description = constr.getDescription();

                if (constr.getResourceRef() != null) {
                    ObjectReferenceType resourceRef = constr.getResourceRef();
                    OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
                    subResult.addParam("targetRef", resourceRef.getOid());

                    PrismObject resource = WebModelUtils.loadObjectAsync(
                            ResourceType.class, resourceRef.getOid(), subResult, this, user);
                    name = WebMiscUtil.getName(resource);
                }
            }

            return new AssignmentItemDto(AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION, name, description, null);
        }

        PrismReferenceValue refValue = targetRef.getValue();
        PrismObject value = refValue.getObject();
        if (value == null) {
            //resolve reference
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
            subResult.addParam("targetRef", refValue.getOid());
            value = WebModelUtils.loadObjectAsync(ObjectType.class, refValue.getOid(), subResult, this, user);
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
}
