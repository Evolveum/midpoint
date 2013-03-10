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

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dashboard.Dashboard;
import com.evolveum.midpoint.web.component.dashboard.DashboardPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.Collection;
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

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_WORK_ITEMS = "workItems";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";

    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();

    public PageDashboard() {
         principalModel.setObject(loadUser());

        initLayout();
    }

    private PrismObject<UserType> loadUser() {
        PrincipalUser principal = SecurityUtils.getPrincipalUser();

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

    private List<SimpleAccountDto> loadAccounts() throws Exception {
        LOGGER.debug("Loading accounts.");
        List<SimpleAccountDto> list = new ArrayList<SimpleAccountDto>();
        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return list;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(AccountShadowType.F_RESOURCE, GetOperationOptions.createResolve());

        List<ObjectReferenceType> references = user.asObjectable().getAccountRef();
        for (ObjectReferenceType reference : references) {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);

            PrismObject<AccountShadowType> account = WebModelUtils.loadObjectAsync(AccountShadowType.class, reference.getOid(),
                    options, subResult, this, user);
            if (account == null) {
                continue;
            }

            AccountShadowType accountType = account.asObjectable();

            OperationResultType fetchResult = accountType.getFetchResult();
            if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
                showResult(OperationResult.createOperationResult(fetchResult));
            }

            ResourceType resource = accountType.getResource();
            String resourceName = WebMiscUtil.getName(resource);
            list.add(new SimpleAccountDto(WebMiscUtil.getOrigStringFromPoly(accountType.getName()), resourceName));
        }
        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return list;
    }

    private void initPersonalInfo() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO,
                createStringResource("PageDashboard.personalInfo")) {

            @Override
            protected Component getLazyLoadComponent(String componentId) {
                return new PersonalInfoPanel(componentId);
            }
        };
        add(personalInfo);
    }

    private void initMyWorkItems() {
        Dashboard dashboard = new Dashboard();
        dashboard.setShowMinimize(true);
        DashboardPanel workItems = new DashboardPanel(ID_WORK_ITEMS, createStringResource("PageDashboard.workItems"),
                new Model<Dashboard>(dashboard));
        add(workItems);
    }

    private void initMyAccounts() {
        AsyncDashboardPanel<Object, List<SimpleAccountDto>> accounts =
                new AsyncDashboardPanel<Object, List<SimpleAccountDto>>(ID_ACCOUNTS,
                        createStringResource("PageDashboard.accounts")) {

                    @Override
                    protected Callable<List<SimpleAccountDto>> createCallable(IModel callableParameterModel) {
                        return new Callable<List<SimpleAccountDto>>() {

                            @Override
                            public List<SimpleAccountDto> call() throws Exception {
                                return loadAccounts();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAccountsPanel(markupId, getModel());
                    }
                };
        add(accounts);
    }

    private void initAssignments() {
        AsyncDashboardPanel<Object, List<AssignmentItemDto>> assignedOrgUnits =
                new AsyncDashboardPanel<Object, List<AssignmentItemDto>>(ID_ASSIGNMENTS,
                        createStringResource("PageDashboard.assignments")) {

                    @Override
                    protected Callable<List<AssignmentItemDto>> createCallable(IModel callableParameterModel) {
                        return new Callable<List<AssignmentItemDto>>() {

                            @Override
                            public List<AssignmentItemDto> call() throws Exception {
                                return loadAssignments();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new Label(markupId, getModel());
                    }
                };
        add(assignedOrgUnits);
    }

    private List<AssignmentItemDto> loadAssignments() throws Exception {
        //todo implement
        Thread.sleep(3000);
        return new ArrayList<AssignmentItemDto>();
    }
}
