package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardColor;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.page.self.component.LinksPanel;
import com.evolveum.midpoint.web.page.self.component.SearchPanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
@PageDescriptor(url = {"/self/dashboard", "/self"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL,
                label = "PageSelfDashboard.auth.dashboard.label",
                description = "PageSelfDashboard.auth.dashboard.description")})
public class PageSelfDashboard extends PageSelf {
    private static final Trace LOGGER = TraceManager.getTrace(PageSelfDashboard.class);

    private static final String ID_LINKS_PANEL = "linksPanel";
    private static final String ID_WORK_ITEMS_PANEL = "workItemsPanel";
    private static final String ID_SEARCH_PANEL = "searchPanel";
    private static final String ID_REQUESTS_PANEL = "requestPanel";
    private static final String DOT_CLASS = PageSelfDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEMS = DOT_CLASS + "loadWorkItems";
    private static final int MAX_WORK_ITEMS = 1000;
    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    public PageSelfDashboard() {
        principalModel.setObject(loadUser());
        initLayout();
    }

    private void initLayout(){
        SearchPanel searchPanel = new SearchPanel(ID_SEARCH_PANEL, null);
        add(searchPanel);



        LinksPanel linksPanel = new LinksPanel(ID_LINKS_PANEL, null);
        add(linksPanel);

        AsyncDashboardPanel<Object, List<WorkItemDto>> workItemsPanel =
                new AsyncDashboardPanel<Object, List<WorkItemDto>>(ID_WORK_ITEMS_PANEL, createStringResource("PageSelfDashboard.workItems"),
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

        workItemsPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getWorkflowManager().isEnabled();
            }
        });
        add(workItemsPanel);

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

    private PrismObject<UserType> loadUser() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        Validate.notNull(principal, "No principal");
        if (principal.getOid() == null) {
            throw new IllegalArgumentException("No OID in principal: "+principal);
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = WebModelUtils.loadObject(UserType.class,
                principal.getOid(), result, PageSelfDashboard.this);
        result.computeStatus();

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return user;
    }

}
