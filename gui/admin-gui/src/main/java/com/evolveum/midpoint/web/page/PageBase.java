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

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.common.SystemConfigurationHolder;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDefinitions;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.reports.PageNewReport;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.workflow.*;
import com.evolveum.midpoint.web.page.self.PageSelfAssignments;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.page.self.PageSelfProfile;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorImpl;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageBase extends PageTemplate {

    private static final Trace LOGGER = TraceManager.getTrace(PageBase.class);

    @SpringBean(name = "modelController")
    private ScriptingService scriptingService;
    @SpringBean(name = "modelController")
    private ModelService modelService;
    @SpringBean(name = "modelController")
    private ModelInteractionService modelInteractionService;
    @SpringBean(name = "modelController")
    private TaskService taskService;
    @SpringBean(name = "modelDiagController")
    private ModelDiagnosticService modelDiagnosticService;
    @SpringBean(name = "taskManager")
    private TaskManager taskManager;
    @SpringBean(name = "modelController")
    private WorkflowService workflowService;
    @SpringBean(name = "workflowManager")
    private WorkflowManager workflowManager;
    @SpringBean(name = "midpointConfiguration")
    private MidpointConfiguration midpointConfiguration;
    @SpringBean(name = "reportManager")
    private ReportManager reportManager;
    @SpringBean(name = "certificationManager")
    private CertificationManager certificationManager;
    @SpringBean(name = "accessDecisionManager")
    private SecurityEnforcer securityEnforcer;
    @SpringBean
    private MidpointFormValidatorRegistry formValidatorRegistry;

    public PageBase(PageParameters parameters) {
        super(parameters);

        Injector.get().inject(this);
        Validate.notNull(modelService, "Model service was not injected.");
        Validate.notNull(taskManager, "Task manager was not injected.");
        Validate.notNull(reportManager, "Report manager was not injected.");
    }

    public PageBase() {
        this(null);
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        MidPointApplication application = getMidpointApplication();
        return application.getWebApplicationConfiguration();
    }

    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    protected WorkflowService getWorkflowService() {
        return workflowService;
    }

    protected WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public CertificationManager getCertificationManager() {
        return certificationManager;
    }

    public ModelService getModelService() {
        return modelService;
    }

    public ScriptingService getScriptingService(){
        return scriptingService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }

    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    protected ModelDiagnosticService getModelDiagnosticService() {
        return modelDiagnosticService;
    }

    public MidpointFormValidatorRegistry getFormValidatorRegistry() {
        return formValidatorRegistry;
    }

    public static StringResourceModel createStringResourceStatic(Component component, String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, component, new Model<String>(), resourceKey, objects);
    }

    public static StringResourceModel createStringResourceStatic(Component component, Enum e) {
        String resourceKey = createEnumResourceKey(e);
        return createStringResourceStatic(component, resourceKey);
    }

    public static String createEnumResourceKey(Enum e) {
        return e.getDeclaringClass().getSimpleName() + "." + e.name();
    }

    public Task createSimpleTask(String operation, PrismObject<UserType> owner) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        if (owner == null) {
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                return task;
            } else {
                owner = user.getUser().asPrismObject();
            }
        }

        task.setOwner(owner);
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        return createSimpleTask(operation, user != null ? user.getUser().asPrismObject() : null);
    }

    public MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    protected <P extends Object> void validateObject(String xmlObject, final Holder<P> objectHolder,
                                                     boolean validateSchema, OperationResult result) {
        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
                                                                   OperationResult objectResult) {
                objectHolder.setValue((P) object);
                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
            }
        };
        Validator validator = new Validator(getPrismContext(), handler);
        validator.setVerbose(true);
        validator.setValidateSchema(validateSchema);
        validator.validateObject(xmlObject, result);

        result.computeStatus();
    }

    public long getItemsPerPage(UserProfileStorage.TableId tableId){
        UserProfileStorage userProfile = getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableId);
    }

    @Override
    protected List<SideBarMenuItem> createMenuItems() {
        List<SideBarMenuItem> menus = new ArrayList<>();

        SideBarMenuItem menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.selfService"));
        menus.add(menu);
        createSelfServiceMenu(menu);

        menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.mainNavigation"));
        menus.add(menu);
        List<MainMenuItem> items = menu.getItems();

        // todo fix with visible behaviour [lazyman]
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createHomeItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
                AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createUsersItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ORG_STRUCT_URL,
                AuthorizationConstants.AUTZ_UI_ORG_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createOrganizationsMenu());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createRolesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createResourcesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            if (getWorkflowManager().isEnabled()) {
                items.add(createWorkItemsItems());
            }
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CERTIFICATION_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)
                && SystemConfigurationHolder.isExperimentalCodeEnabled()) {
            items.add(createCertificationItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createServerTasksItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_REPORTS_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createReportsItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createConfigurationItems());
        }

        return menus;
    }

    private MainMenuItem createWorkItemsItems() {
        MainMenuItem item = new MainMenuItem("fa fa-inbox",
                createStringResource("PageAdmin.menu.top.workItems"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.list"),
                PageWorkItems.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listClaimable"),
                PageWorkItemsClaimable.class);
        submenu.add(menu);

        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesAll"),
                PageProcessInstancesAll.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedBy"),
                PageProcessInstancesRequestedBy.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedFor"),
                PageProcessInstancesRequestedFor.class);
        submenu.add(menu);

        return item;
    }

    private MainMenuItem createServerTasksItems() {
        MainMenuItem item = new MainMenuItem("fa fa-tasks",
                createStringResource("PageAdmin.menu.top.serverTasks"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.list"),
                PageTasks.class);
        submenu.add(list);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.new"),
                PageTaskAdd.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createResourcesItems() {
        MainMenuItem item = new MainMenuItem("fa fa-laptop",
                createStringResource("PageAdmin.menu.top.resources"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.resources.list"),
                PageResources.class);
        submenu.add(list);
        MenuItem created = new MenuItem(createStringResource("PageAdmin.menu.top.resources.new"),
                PageResourceWizard.class);
        submenu.add(created);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.resources.import"),
                PageImportObject.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createReportsItems() {
        MainMenuItem item = new MainMenuItem("fa fa-pie-chart",
                createStringResource("PageAdmin.menu.top.reports"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.reports.list"),
                PageReports.class);
        submenu.add(list);
        MenuItem created = new MenuItem(createStringResource("PageAdmin.menu.top.reports.created"),
                PageCreatedReports.class);
        submenu.add(created);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.reports.new"),
                PageNewReport.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createCertificationItems() {

        MainMenuItem item = new MainMenuItem("fa fa-certificate",
                createStringResource("PageAdmin.menu.top.certification"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.definitions"),
                PageCertDefinitions.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.newDefinition"),
                PageImportObject.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.campaigns"),
                PageCertCampaigns.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.decisions"),
                PageCertDecisions.class);
        submenu.add(menu);

        return item;
    }

    private MainMenuItem createConfigurationItems() {
        MainMenuItem item = new MainMenuItem("fa fa-cog",
                createStringResource("PageAdmin.menu.top.configuration"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.bulkActions"),
                PageBulkAction.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.importObject"),
                PageImportObject.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjects"),
                PageDebugList.class);
        submenu.add(menu);

        PageParameters params = new PageParameters();
        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_BASIC);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.basic"),
                PageSystemConfiguration.class, params, null);
        submenu.add(menu);

        params = new PageParameters();
        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_LOGGING);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.logging"),
                PageSystemConfiguration.class, params, null);
        submenu.add(menu);


        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.shadowsDetails"),
                PageAccounts.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.internals"),
                PageInternals.class);
        submenu.add(menu);


        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.about"),
                PageAbout.class);
        submenu.add(menu);

        return item;
    }

    private void createSelfServiceMenu(SideBarMenuItem menu) {
        MainMenuItem item = new MainMenuItem("fa fa-dashboard",
                createStringResource("PageAdmin.menu.dashboard"), PageSelfDashboard.class);
        menu.getItems().add(item);
        item = new MainMenuItem("fa fa-user",
                createStringResource("PageAdmin.menu.profile"), PageSelfProfile.class);
        menu.getItems().add(item);
        item = new MainMenuItem("fa fa-star",
                createStringResource("PageAdmin.menu.assignments"), PageSelfAssignments.class);
        menu.getItems().add(item);
        item = new MainMenuItem("fa fa-shield",
                createStringResource("PageAdmin.menu.credentials"), PageSelfCredentials.class);
        menu.getItems().add(item);
    }

    private MainMenuItem createHomeItems() {
        MainMenuItem item = new MainMenuItem("fa fa-dashboard",
                createStringResource("PageAdmin.menu.dashboard"), PageDashboard.class);

        return item;
    }

    private MainMenuItem createUsersItems() {
        MainMenuItem item = new MainMenuItem("fa fa-group",
                createStringResource("PageAdmin.menu.top.users"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.users.list"), PageUsers.class);
        submenu.add(list);
        MenuItem newUser = new MenuItem(createStringResource("PageAdmin.menu.top.users.new"), PageUser.class);
        submenu.add(newUser);
//        MenuItem search = new MenuItem(createStringResource("PageAdmin.menu.users.search"),
//        PageUsersSearch.class);
//        submenu.add(search);

        return item;
    }

    private MainMenuItem createOrganizationsMenu() {
        MainMenuItem item = new MainMenuItem("fa fa-building",
                createStringResource("PageAdmin.menu.top.users.org"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.users.org.tree"), PageOrgTree.class);
        submenu.add(list);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.users.org.new"), PageOrgUnit.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createRolesItems() {
        MainMenuItem item = new MainMenuItem("fa fa-bookmark",
                createStringResource("PageAdmin.menu.top.roles"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.roles.list"), PageRoles.class);
        submenu.add(list);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.roles.new"), PageRole.class);
        submenu.add(n);

        return item;
    }
}
