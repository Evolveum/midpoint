/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.LoggedUser;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.configuration.*;
import com.evolveum.midpoint.schrodinger.page.certification.*;
import com.evolveum.midpoint.schrodinger.page.configuration.*;
import com.evolveum.midpoint.schrodinger.page.org.NewOrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;
import com.evolveum.midpoint.schrodinger.page.report.AuditLogViewerPage;
import com.evolveum.midpoint.schrodinger.page.report.CreatedReportsPage;
import com.evolveum.midpoint.schrodinger.page.report.ImportJasperReportPage;
import com.evolveum.midpoint.schrodinger.page.report.ListReportsPage;
import com.evolveum.midpoint.schrodinger.page.resource.ImportResourceDefinitionPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListConnectorHostsPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.NewResourcePage;
import com.evolveum.midpoint.schrodinger.page.role.ListRolesPage;
import com.evolveum.midpoint.schrodinger.page.role.NewRolePage;
import com.evolveum.midpoint.schrodinger.page.self.CredentialsPage;
import com.evolveum.midpoint.schrodinger.page.self.HomePage;
import com.evolveum.midpoint.schrodinger.page.self.ProfilePage;
import com.evolveum.midpoint.schrodinger.page.self.RequestRolePage;
import com.evolveum.midpoint.schrodinger.page.service.ListServicesPage;
import com.evolveum.midpoint.schrodinger.page.service.NewServicePage;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.FormSubmittablePage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.page.cases.*;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BasicPage {

    public LoggedUser loggedUser() {
        return new LoggedUser();
    }

    public HomePage home() {
        clickSelfServiceMenu("PageAdmin.menu.selfDashboard", null);
        return new HomePage();
    }

    public ProfilePage profile() {
        clickSelfServiceMenu("PageAdmin.menu.profile", null);
        return new ProfilePage();
    }

    public CredentialsPage credentials() {
        clickSelfServiceMenu("PageAdmin.menu.credentials", null);
        return new CredentialsPage();
    }

    public RequestRolePage requestRole() {
        clickSelfServiceMenu("PageAdmin.menu.request", null);
        return new RequestRolePage();
    }

    public DashboardPage dashboard() {
        clickAdministrationMenu("PageAdmin.menu.dashboard", null);
        return new DashboardPage();
    }

    public ListUsersPage listUsers() {
        return listUsers("");
    }

    public ListUsersPage listUsers(String objectListMenuItemKey) {
        if (StringUtils.isEmpty(objectListMenuItemKey)) {
            clickAdministrationMenu("PageAdmin.menu.top.users", "PageAdmin.menu.top.users.list");
        } else {
            clickAdministrationMenu("PageAdmin.menu.top.users", objectListMenuItemKey);
        }
        return new ListUsersPage();
    }

    public UserPage newUser() {
        clickAdministrationMenu("PageAdmin.menu.top.users", "PageAdmin.menu.top.users.new");
        return new UserPage();
    }

    public OrgTreePage orgStructure() {
        clickAdministrationMenu("PageAdmin.menu.top.users.org", "PageAdmin.menu.top.users.org.tree");
        return new OrgTreePage();
    }

    public NewOrgPage newOrgUnit() {
        clickAdministrationMenu("PageAdmin.menu.top.users.org", "PageAdmin.menu.top.users.org.new");
        return new NewOrgPage();
    }

    public ListRolesPage listRoles() {
        clickAdministrationMenu("PageAdmin.menu.top.roles", "PageAdmin.menu.top.roles.list");
        return new ListRolesPage();
    }

    public NewRolePage newRole() {
        clickAdministrationMenu("PageAdmin.menu.top.roles", "PageAdmin.menu.top.roles.new");
        return new NewRolePage();
    }

    public ListServicesPage listServices() {
        clickAdministrationMenu("PageAdmin.menu.top.services", "PageAdmin.menu.top.services.list");
        return new ListServicesPage();
    }

    public NewServicePage newService() {
        clickAdministrationMenu("PageAdmin.menu.top.services", "PageAdmin.menu.top.services.new");
        return new NewServicePage();
    }

    public ListResourcesPage listResources() {
        clickAdministrationMenu("PageAdmin.menu.top.resources", "PageAdmin.menu.top.resources.list");
        return new ListResourcesPage();
    }

    public NewResourcePage newResource() {
        clickAdministrationMenu("PageAdmin.menu.top.resources", "PageAdmin.menu.top.resources.new");
        return new NewResourcePage();
    }

    public AllCasesPage listAllCases(){
        clickAdministrationMenu(ConstantsUtil.MENU_TOP_CASES, ConstantsUtil.MENU_ALL_CASES);
        return new AllCasesPage();
    }

    public MyCasesPage listMyCases(){
        clickAdministrationMenu(ConstantsUtil.MENU_TOP_CASES, ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT);
        return new MyCasesPage();
    }

    public AllManualCasesPage listAllManualCases(){
        clickAdministrationMenu(ConstantsUtil.MENU_TOP_CASES, ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT);
        return new AllManualCasesPage();
    }

    public AllRequestsPage listAllRequests() {
        clickAdministrationMenu(ConstantsUtil.MENU_TOP_CASES, ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT);
        return new AllRequestsPage();
    }

    public AllApprovalsPage listAllApprovals() {
        clickAdministrationMenu(ConstantsUtil.MENU_TOP_CASES, ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT);
        return new AllApprovalsPage();
    }

    public MyItemsPage myItems() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.list");
        return new MyItemsPage();
    }

    public ItemsClaimableByMePage itemsClaimableByMe() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listClaimable");
        return new ItemsClaimableByMePage();
    }

    public AttorneyItemsPage attorneyItems() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listAttorney");
        return new AttorneyItemsPage();
    }

    public AllItemsPage allItems() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listAll");
        return new AllItemsPage();
    }

    public MyRequestsPage myRequests() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listProcessInstancesRequestedBy");
        return new MyRequestsPage();
    }

    public RequestsAboutMePage requestsAboutMe() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listProcessInstancesRequestedFor");
        return new RequestsAboutMePage();
    }

    public CampaignDefinitionsPage campaignDefinitions() {
        clickAdministrationMenu("PageAdmin.menu.top.certification", "PageAdmin.menu.top.certification.definitions");
        return new CampaignDefinitionsPage();
    }

    public NewCampaignDefinitionPage newCampaignDefinition() {
        clickAdministrationMenu("PageAdmin.menu.top.certification", "PageAdmin.menu.top.certification.newDefinition");
        return new NewCampaignDefinitionPage();
    }

    public CampaignsPage campaigns() {
        clickAdministrationMenu("PageAdmin.menu.top.certification", "PageAdmin.menu.top.certification.campaigns");
        return new CampaignsPage();
    }

    public CampaignsSchedulingPage campaignsScheduling() {
        clickAdministrationMenu("PageAdmin.menu.top.certification", "PageAdmin.menu.top.certification.scheduling");
        return new CampaignsSchedulingPage();
    }

    public MyWorkItemsPage myWorkItems() {
        clickAdministrationMenu("PageAdmin.menu.top.certification", "PageAdmin.menu.top.certification.decisions");
        return new MyWorkItemsPage();
    }

    public ListTasksPage listTasks() {
        clickAdministrationMenu("PageAdmin.menu.top.serverTasks", "PageAdmin.menu.top.serverTasks.list");
        return new ListTasksPage();
    }

    public TaskPage newTask() {
        clickAdministrationMenu("PageAdmin.menu.top.serverTasks", "PageAdmin.menu.top.serverTasks.new");
        return new TaskPage();
    }

    public ListReportsPage listReports() {
        clickAdministrationMenu("PageAdmin.menu.top.reports", "PageAdmin.menu.top.reports.list");
        return new ListReportsPage();
    }

    public CreatedReportsPage createdReports() {
        clickAdministrationMenu("PageAdmin.menu.top.reports", "PageAdmin.menu.top.reports.created");
        return new CreatedReportsPage();
    }

    public ImportJasperReportPage importJasperReport() {
        clickAdministrationMenu("PageAdmin.menu.top.reports", "PageAdmin.menu.top.reports.new");
        return new ImportJasperReportPage();
    }

    public AuditLogViewerPage auditLogViewer() {
        clickAdministrationMenu("PageAdmin.menu.top.reports", "PageAuditLogViewer.menuName");
        return new AuditLogViewerPage();
    }

    public ImportResourceDefinitionPage importResourceDefinition() {
        clickAdministrationMenu("PageAdmin.menu.top.resources", "PageAdmin.menu.top.resources.import");
        return new ImportResourceDefinitionPage();
    }

    public ListConnectorHostsPage listConnectorHosts() {
        clickAdministrationMenu("PageAdmin.menu.top.resources", "PageAdmin.menu.top.connectorHosts.list");
        return new ListConnectorHostsPage();
    }

    public AboutPage aboutPage() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.about", null);
        return new AboutPage();
    }

    public BulkActionsPage bulkActions() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.bulkActions", null);
        return new BulkActionsPage();
    }

    public ImportObjectPage importObject() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.importObject", null);
        return new ImportObjectPage();
    }

    public ListRepositoryObjectsPage listRepositoryObjects() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.repositoryObjects", "PageAdmin.menu.top.configuration.repositoryObjectsList");
        return new ListRepositoryObjectsPage();
    }

    public SystemTab system() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.basic");
        return new SystemPage().systemTab();
    }

    public NotificationsTab notifications() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.notifications");
        return new SystemPage().notificationsTab();
    }

    public LoggingTab logging() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.logging");
        return new SystemPage().loggingTab();
    }

    public ProfilingTab profiling() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.profiling");
        return new SystemPage().profilingTab();
    }

    public AdminGuiTab adminGui() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.adminGui");
        return new SystemPage().adminGuiTab();
    }

    public InfrastructureTab infrastructure() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.infrastructure");
        return new SystemPage().infrastructureTab();
    }

    public RoleManagementTab roleManagement() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.roleManagement");
        return new SystemPage().roleManagementTab();
    }

    public InternalsConfigurationPage internalsConfiguration() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.internals", null, 1);
        return new InternalsConfigurationPage();
    }

    public QueryPlaygroundPage queryPlayground() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.repoQuery", null);
        return new QueryPlaygroundPage();
    }

    public FormSubmittablePage dynamicForm() {
        return new FormSubmittablePage();
    }

    private void clickSelfServiceMenu(String mainMenuKey, String menuItemKey) {
        clickMenuItem("PageAdmin.menu.selfService", mainMenuKey, menuItemKey);
    }

    private void clickAdministrationMenu(String mainMenuKey, String menuItemKey) {
        clickMenuItem(ConstantsUtil.ADMINISTRATION_MENU_ITEMS_SECTION_KEY, mainMenuKey, menuItemKey);
    }

    public String getAdministrationMenuItemIconClass(String mainMenuKey, String menuItemKey){
        SelenideElement menuItem = getMenuItemElement(ConstantsUtil.ADMINISTRATION_MENU_ITEMS_SECTION_KEY, mainMenuKey, menuItemKey);
        return menuItem.parent().$(By.tagName("i")).getAttribute("class");
    }

    private void clickConfigurationMenu(String mainMenuKey, String menuItemKey) {
        clickConfigurationMenu(mainMenuKey, menuItemKey, 0);
    }

    private void clickConfigurationMenu(String mainMenuKey, String menuItemKey, int index) {
        clickMenuItem("PageAdmin.menu.top.configuration", mainMenuKey, menuItemKey, index);
    }

    public FeedbackBox<? extends BasicPage> feedback() {
        SelenideElement feedback = $(By.cssSelector("div.feedbackContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M);
        return new FeedbackBox<>(this, feedback);
    }

    private void clickMenuItem(String topLevelMenuKey, String mainMenuKey, String menuItemKey) {
        clickMenuItem(topLevelMenuKey, mainMenuKey, menuItemKey, 0);
    }

    private void clickMenuItem(String topLevelMenuKey, String mainMenuKey, String menuItemKey, int index) {
        getMenuItemElement(topLevelMenuKey, mainMenuKey, menuItemKey, index).click();
    }

    public SelenideElement getMenuItemElement(String topLevelMenuKey, String mainMenuKey, String menuItemKey) {
        return getMenuItemElement(topLevelMenuKey, mainMenuKey, menuItemKey, 0);
    }

    public SelenideElement getMenuItemElement(String topLevelMenuKey, String mainMenuKey, String menuItemKey, int index){
        SelenideElement mainMenu = getMainMenuItemElement(topLevelMenuKey, mainMenuKey, index);
        if (menuItemKey == null){
            return mainMenu;
        }
        SelenideElement menuItem = mainMenu.$(Schrodinger.byDataResourceKey(menuItemKey));
        menuItem.waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S);

        return menuItem;
    }

    public SelenideElement getMenuItemElementByMenuLabelText(String topLevelMenuKey, String mainMenuKey, String menuItemLabelText){
        SelenideElement mainMenu = getMainMenuItemElement(topLevelMenuKey, mainMenuKey);
        if (StringUtils.isEmpty(menuItemLabelText)){
            return mainMenu;
        }
        SelenideElement menuItem = mainMenu.$(By.partialLinkText(menuItemLabelText));
        menuItem.shouldBe(Condition.visible);

        return menuItem;
    }

    private SelenideElement getMainMenuItemElement(String topLevelMenuKey, String mainMenuKey) {
        return getMainMenuItemElement(topLevelMenuKey, mainMenuKey, 0);
    }

    private SelenideElement getMainMenuItemElement(String topLevelMenuKey, String mainMenuKey, int index){
        SelenideElement topLevelMenu = $(Schrodinger.byDataResourceKey(topLevelMenuKey));
        topLevelMenu.shouldBe(Condition.visible);

        SelenideElement topLevelMenuChevron = topLevelMenu.parent().$(By.tagName("i"));
        if (!topLevelMenuChevron.has(Condition.cssClass("fa-chevron-down"))) {
            topLevelMenu.click();
            topLevelMenuChevron.shouldHave(Condition.cssClass("fa-chevron-down")).waitUntil(Condition.cssClass("fa-chevron-down"), MidPoint.TIMEOUT_DEFAULT_2_S);
        }

        SelenideElement mainMenu = topLevelMenu.$(Schrodinger.byDataResourceKey(mainMenuKey), index);
        mainMenu.shouldBe(Condition.visible);

        SelenideElement mainMenuLi = mainMenu.parent().parent();
        if (!mainMenuLi.has(Condition.cssClass("active"))) {
            mainMenu.click();
            mainMenuLi.waitUntil(Condition.cssClass("active"),MidPoint.TIMEOUT_MEDIUM_6_S).shouldHave(Condition.cssClass("active"));
        }
        return mainMenu;
    }

    public String getCurrentUrl() {
        String url = WebDriverRunner.url();
        url = url.split("\\?")[0];
        return url;
    }
}
