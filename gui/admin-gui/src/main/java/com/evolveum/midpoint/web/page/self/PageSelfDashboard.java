package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
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
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.page.self.component.LinksPanel;
import com.evolveum.midpoint.web.page.self.component.DashboardSearchPanel;
import com.evolveum.midpoint.web.page.self.component.MyRequestsPanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
    private static final String OPERATION_LOAD_REQUESTS = DOT_CLASS + "loadRequests";
    private static final int MAX_WORK_ITEMS = 1000;
    private static final int MAX_REQUESTS = 1000;
    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();
    private IModel<List<RichHyperlinkType>> linksPanelModel = null;
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String TASK_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";

    public PageSelfDashboard() {
        principalModel.setObject(loadUser());
        createLinksPanelModel();
        initLayout();
    }

    private void initLayout(){
        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL, null);
        add(dashboardSearchPanel);
        if (! WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL, AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL)) {
            dashboardSearchPanel.setVisible(false);
        }
        LinksPanel linksPanel = new LinksPanel(ID_LINKS_PANEL, linksPanelModel, linksPanelModel.getObject());
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

        AsyncDashboardPanel<Object, List<ProcessInstanceDto>> myRequestsPanel =
                new AsyncDashboardPanel<Object, List<ProcessInstanceDto>>(ID_REQUESTS_PANEL, createStringResource("PageSelfDashboard.myRequests"),
                        "fa fa-fw fa-pencil-square-o", DashboardColor.GREEN) {

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<ProcessInstanceDto>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<ProcessInstanceDto>>>(
                                getSecurityEnforcer(), auth) {

                            @Override
                            public CallableResult<List<ProcessInstanceDto>> callWithContextPrepared() throws Exception {
                                return loadMyRequests();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyRequestsPanel(markupId, new PropertyModel<List<ProcessInstanceDto>>(getModel(), CallableResult.F_VALUE));
                    }
                };

        myRequestsPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getWorkflowManager().isEnabled();
            }
        });
        add(myRequestsPanel);

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

    private CallableResult<List<ProcessInstanceDto>> loadMyRequests() {

        LOGGER.debug("Loading requests.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<ProcessInstanceDto> list = new ArrayList<ProcessInstanceDto>();
        callableResult.setValue(list);

        if (!getWorkflowManager().isEnabled()) {
            return callableResult;
        }

        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return callableResult;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_REQUESTS);
        callableResult.setResult(result);

        try {
            List<WfProcessInstanceType> processInstanceTypes = getWorkflowService().listProcessInstancesRelatedToUser(user.getOid(),
             true, false, false, 0, MAX_REQUESTS, result);
            List<WfProcessInstanceType> processInstanceTypesFinished = getWorkflowService().listProcessInstancesRelatedToUser(user.getOid(),
             true, false, true, 0, MAX_REQUESTS, result);
            if (processInstanceTypes != null && processInstanceTypesFinished != null){
                processInstanceTypes.addAll(processInstanceTypesFinished);
            }
            for (WfProcessInstanceType processInstanceType : processInstanceTypes) {
                ProcessInstanceState processInstanceState = (ProcessInstanceState) processInstanceType.getState();
                Task shadowTask = null;
                if (processInstanceState != null) {
                    String shadowTaskOid = processInstanceState.getShadowTaskOid();
                    try {
                        shadowTask = getTaskManager().getTask(shadowTaskOid, result);
                    } catch (ObjectNotFoundException e) {
                        // task is already deleted, no problem here
                        result.muteLastSubresultError();
                    }
                }

                list.add(new ProcessInstanceDto(processInstanceType, shadowTask));
            }
        } catch (Exception e) {
            result.recordFatalError("Couldn't get list of work items.", e);
        }

        result.recordSuccessIfUnknown();
        result.recomputeStatus();

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
        PrismObject<UserType> user = WebModelUtils.loadObject(UserType.class,
                principal.getOid(), PageSelfDashboard.this, task, result);
        result.computeStatus();

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return user;
    }

    private List<RichHyperlinkType> loadLinksList(){
        List<RichHyperlinkType> list = new ArrayList<RichHyperlinkType>();

        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return list;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_WORK_ITEMS);

        Task task = createSimpleTask(TASK_GET_SYSTEM_CONFIG);
        try{
            AdminGuiConfigurationType adminGuiConfig = getModelInteractionService().getAdminGuiConfiguration(task, result);
            list = adminGuiConfig.getUserDashboardLink();
            result.recordSuccess();
        } catch(Exception ex){
            LoggingUtils.logException(LOGGER, "Couldn't load system configuration", ex);
            result.recordFatalError("Couldn't load system configuration.", ex);
        }
        return list;
    }

    private void createLinksPanelModel(){
        linksPanelModel = new IModel<List<RichHyperlinkType>>() {
            @Override
            public List<RichHyperlinkType> getObject() {
                return loadLinksList();
            }

            @Override
            public void setObject(List<RichHyperlinkType> richHyperlinkTypes) {

            }

            @Override
            public void detach() {

            }
        };
    }
}
