/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.ElementShould;

import com.evolveum.midpoint.schrodinger.component.common.UserMenuPanel;
import com.evolveum.midpoint.schrodinger.page.objectcollection.ListObjectCollectionsPage;
import com.evolveum.midpoint.schrodinger.page.objectcollection.ObjectCollectionPage;
import com.evolveum.midpoint.schrodinger.page.service.ServicePage;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.LoggedUser;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.configuration.*;
import com.evolveum.midpoint.schrodinger.page.archetype.ListArchetypesPage;
import com.evolveum.midpoint.schrodinger.page.cases.*;
import com.evolveum.midpoint.schrodinger.page.certification.*;
import com.evolveum.midpoint.schrodinger.page.configuration.*;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
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
import com.evolveum.midpoint.schrodinger.page.role.RolePage;
import com.evolveum.midpoint.schrodinger.page.self.CredentialsPage;
import com.evolveum.midpoint.schrodinger.page.self.HomePage;
import com.evolveum.midpoint.schrodinger.page.self.ProfilePage;
import com.evolveum.midpoint.schrodinger.page.self.RequestRolePage;
import com.evolveum.midpoint.schrodinger.page.service.ListServicesPage;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.FormSubmittablePage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.testng.Assert;

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

    public OrgPage newOrgUnit() {
        clickAdministrationMenu("PageAdmin.menu.top.users.org", "PageAdmin.menu.top.users.org.new");
        return new OrgPage();
    }

    public ListRolesPage listRoles() {
        clickAdministrationMenu("PageAdmin.menu.top.roles", "PageAdmin.menu.top.roles.list");
        return new ListRolesPage();
    }

    public RolePage newRole() {
        clickAdministrationMenu("PageAdmin.menu.top.roles", "PageAdmin.menu.top.roles.new");
        return new RolePage();
    }

    public ListServicesPage listServices() {
        clickAdministrationMenu("PageAdmin.menu.top.services", "PageAdmin.menu.top.services.list");
        return new ListServicesPage();
    }

    public ServicePage newService() {
        clickAdministrationMenu("PageAdmin.menu.top.services", "PageAdmin.menu.top.services.new");
        return new ServicePage();
    }

    public ListArchetypesPage listArchetypes() {
        clickAdministrationMenu("PageAdmin.menu.top.archetypes", "PageAdmin.menu.top.archetypes.list");
        return new ListArchetypesPage();
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

    public MyWorkitemsPage myItems() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.list");
        return new MyWorkitemsPage();
    }

    public WorkitemsClaimableByMePage itemsClaimableByMe() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listClaimable");
        return new WorkitemsClaimableByMePage();
    }

    public AttorneyItemsPage attorneyItems() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listAttorney");
        return new AttorneyItemsPage();
    }

    public AllWorkitemsPage allItems() {
        clickAdministrationMenu("PageAdmin.menu.top.workItems", "PageAdmin.menu.top.workItems.listAll");
        return new AllWorkitemsPage();
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
        return listTasks("");
    }

    public ListTasksPage listTasks(String objectListMenuItemKey) {
        if (StringUtils.isEmpty(objectListMenuItemKey)) {
            clickAdministrationMenu("PageAdmin.menu.top.serverTasks", "PageAdmin.menu.top.tasks.list");
        } else {
            clickAdministrationMenu("PageAdmin.menu.top.serverTasks", objectListMenuItemKey);
        }
        return new ListTasksPage();
    }

    public TaskPage newTask() {
        clickAdministrationMenu("PageAdmin.menu.top.serverTasks", "PageAdmin.menu.top.tasks.new");
        return new TaskPage();
    }

    public ListReportsPage listReports() {
        return listReports("");
    }

    public ListReportsPage listReports(String objectListMenuItemKey) {
        if (StringUtils.isEmpty(objectListMenuItemKey)) {
            clickAdministrationMenu("PageAdmin.menu.top.reports", "PageAdmin.menu.top.reports.list");
        } else {
            clickAdministrationMenu("PageAdmin.menu.top.reports", objectListMenuItemKey);
        }
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

    public ListObjectCollectionsPage listObjectCollections() {
        clickAdministrationMenu("PageAdmin.menu.top.objectCollections", "PageAdmin.menu.top.objectCollections.list");
        return new ListObjectCollectionsPage();
    }

    public ObjectCollectionPage newObjectCollection() {
        clickAdministrationMenu("PageAdmin.menu.top.objectCollections", "PageAdmin.menu.top.objectCollections.new");
        return new ObjectCollectionPage();
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

    public ObjectPolicyTab objectPolicy() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.objectPolicy");
        return new SystemPage().objectPolicyTab();
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

    public DeploymentInformationTab deploymentInformation() {
        clickConfigurationMenu("PageAdmin.menu.top.configuration.basic", "PageAdmin.menu.top.configuration.deploymentInformation");
        return new SystemPage().deploymentInformationTab();
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
        SelenideElement feedback = $(By.cssSelector("div.feedbackContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_EXTRA_LONG_10_M);
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
        menuItem.waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

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

        SelenideElement mainMenu = topLevelMenu.$(Schrodinger.byDataResourceKey("span", mainMenuKey), index);
        mainMenu.shouldBe(Condition.visible);

        SelenideElement mainMenuLi = mainMenu.parent().parent();

        // this is not a very clean and clear code. needs review and rewrite.
        // it seems that after adminLTE upgrade, top menu items doesn't have 'active' css class but 'menu-open' css class.
        try {
            checkCssClass(mainMenuLi, mainMenu, "menu-open"); //try first for top level e.g. Users, Roles, ...
        } catch (ElementShould e) {
            checkCssClass(mainMenuLi, mainMenu, "active"); //if doesn't exists, try for subitems, e.g All users, New user,...
        }
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return mainMenu;
    }

    private void checkCssClass(SelenideElement mainMenuLi, SelenideElement mainMenu, String cssClass) {
        if (!mainMenuLi.has(Condition.cssClass(cssClass))) {
            mainMenu.click();
            mainMenuLi.waitUntil(Condition.cssClass(cssClass),MidPoint.TIMEOUT_MEDIUM_6_S).shouldHave(Condition.cssClass(cssClass));
        }
    }

    public String getCurrentUrl() {
        String url = WebDriverRunner.url();
        url = url.split("\\?")[0];
        return url;
    }

    public SelenideElement getMainHeaderPanelElement() {
        return $(Schrodinger.byDataId("header", "mainHeader"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public BasicPage assertMainHeaderPanelStyleMatch(String styleToCompare) {
        Assert.assertTrue(getMainHeaderPanelElement().getCssValue("background-color").equals(styleToCompare),
                "Main header panel background color doesn't match to " + styleToCompare);
        return this;
    }

    public SelenideElement getPageTitleElement() {
        return $(Schrodinger.byDataId("span", "pageTitle"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public BasicPage assertPageTitleStartsWith(String titleStartText) {
        Assert.assertTrue(getPageTitleElement().getText().startsWith(titleStartText), "Page title doesn't start with " + titleStartText);
        return this;
    }

    public UserMenuPanel clickUserMenu() {
        if(userMenuExists()) {
            SelenideElement userMenu = $(".dropdown.user.user-menu").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
            userMenu.$(By.cssSelector(".dropdown-toggle")).click();
            SelenideElement userMenuPanel = userMenu.$(By.cssSelector(".user-footer")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
            return new UserMenuPanel(this, userMenuPanel);
        }
        return null;
    }

    public boolean userMenuExists() {
        return $(".dropdown.user.user-menu").exists();
    }

    public boolean elementWithTextExists(String text) {
        return $(byText(text)).exists();
    }

    public BasicPage assertUserMenuExist() {
        Assert.assertTrue(userMenuExists(), "User should be logged in, user menu should be visible.");
        return this;
    }

    public BasicPage assertUserMenuDoesntExist() {
        Assert.assertTrue(userMenuExists(), "User should be logged out, user menu shouldn't be visible.");
        return this;
    }

    public BasicPage assertMenuItemActive(SelenideElement menuItemElement) {
        SelenideElement menuItemLi = menuItemElement.parent().parent();
        Assert.assertTrue(menuItemLi.has(Condition.cssClass("active")), "Menu item should be active");
        return this;
    }

    public BasicPage assertMenuItemDoesntActive(SelenideElement menuItemElement) {
        SelenideElement menuItemLi = menuItemElement.parent().parent();
        Assert.assertFalse(menuItemLi.has(Condition.cssClass("active")), "Menu item shouldn't be active");
        return this;
    }


}
