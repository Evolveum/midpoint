/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;

import com.evolveum.midpoint.gui.impl.page.self.credentials.PageSelfCredentials;

import com.evolveum.midpoint.gui.impl.page.self.dashboard.PageSelfDashboard;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.menu.DetailsNavigationPanel;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuPanel;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.AllAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.impl.page.self.PageUserSelfProfile;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.tools.testng.PerformanceTestMethodMixin;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.web.AbstractGuiIntegrationTest;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class MidScaleGuiTest extends AbstractGuiIntegrationTest implements PerformanceTestMethodMixin {

    private static final String TEST_DIR = "./src/test/resources/midScale";

    private static final File FILE_ORG_STRUCT = new File(TEST_DIR, "org-struct.xml");
    private static final File FILE_USERS = new File(TEST_DIR, "users.xml");
    private static final File FILE_ARCHETYPE_TEACHER = new File(TEST_DIR, "archetype-teacher.xml");
    private static final String ARCHETYPE_TEACHER_OID = "b27830c5-f02a-4273-ab7e-b6bd0e1026dc";
    private static final String PERSON_TEMPLATE_TITLE = "Person";
    private static final String EDITED_USER_NAME = "user10";
    private static final String ASSIGNMENTS_MENU_TITLE = "Assignments";
    private static final String ALL_ASSIGNMENTS_MENU_TITLE = "All";

    private static final int REPETITION_COUNT = 10;

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        modelService.postInit(initResult);
        userAdministrator = repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, initResult);

        int users = repositoryService.countObjects(UserType.class, null, null, initResult);
        System.out.println("Users " + users);
        if (users < 5) {
            importObjectsFromFileNotRaw(FILE_ORG_STRUCT, initTask, initResult);
            initResult.computeStatusIfUnknown();
            if (!initResult.isSuccess()) {
                System.out.println("init result:\n" + initResult);
            }
            importObjectsFromFileNotRaw(FILE_USERS, initTask, initResult);
            importObjectsFromFileNotRaw(FILE_ARCHETYPE_TEACHER, initTask, initResult);

            ObjectDelta<SystemConfigurationType> systemConfigurationDelta = prismContext.deltaFor(SystemConfigurationType.class)
                            .item(ItemPath.create(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_ENABLE_EXPERIMENTAL_FEATURES))
                                    .replace(true)
                            .item(ItemPath.create(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS, GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW))
                                    .add(createTeacherCollection()).asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value());

            modelService.executeChanges(MiscUtil.createCollection(systemConfigurationDelta), null, initTask, initResult);
//            login(userAdministrator);
        }
        login(userAdministrator);
    }

    private GuiObjectListViewType createTeacherCollection() {
        GuiObjectListViewType teacherCollection = new GuiObjectListViewType(prismContext);
        CollectionRefSpecificationType collectionRefSpecificationType = new CollectionRefSpecificationType(prismContext);
        collectionRefSpecificationType.setCollectionRef(ObjectTypeUtil.createObjectRef(ARCHETYPE_TEACHER_OID, ObjectTypes.ARCHETYPE));
        teacherCollection.setCollection(collectionRefSpecificationType);
        teacherCollection.setType(UserType.COMPLEX_TYPE);
        teacherCollection.setIdentifier("teacher-collection");
        return teacherCollection;
    }

    @BeforeMethod
    public void resetQueryListener() {
        queryListener.clear();
    }

    @Test
    public void test010PageSelfDashboard() {
        displayTestTitle(getTestName());
        runTestFor(PageSelfDashboard.class, "selfDashboard", "Home");
    }

    @Test
    public void test020PageSelfProfile() {
        displayTestTitle(getTestName());
        runTestFor(PageUserSelfProfile.class, "selfProfile", "Profile");
    }

    @Test
    public void test030PageSelfCredentials() {
        displayTestTitle(getTestName());
        runTestFor(PageSelfCredentials.class, "serlfCredentials", "Credentials");
    }

    @Test
    public void test040PageRequestRole() {
        displayTestTitle(getTestName());
        runTestFor(PageRequestAccess.class, "requestAccess", "Request access");
    }

    @Test
    public void test110PageDashboard() {
        displayTestTitle(getTestName());
        runTestFor(PageDashboardInfo.class, "dashboard", "Info Dashboard");
    }

    private void runTestFor(Class pageToRender, String stopwatchName, String stopwatchDescription) {
        Stopwatch stopwatch = stopwatch(stopwatchName, stopwatchDescription);
        for (int i = 0; i < REPETITION_COUNT; i++) {
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                tester.startPage(pageToRender);
            }
        }
        queryListener.dumpAndStop();
        tester.assertRenderedPage(pageToRender);
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(AbstractStatisticsPrinter.Format.TEXT, AbstractStatisticsPrinter.SortBy.TIME), null, null));
    }

    @Test
    public void test210listUsers() {
        logger.info(getTestName());
        runTestFor(PageUsers.class, "listUsers", "List users");
    }

    @Test
    public void test220newUser() {
        logger.info(getTestName());

        Stopwatch stopwatch = stopwatch("newUser", "New user");
        for (int i = 0; i < REPETITION_COUNT; i++) {
            tester.startPage(PageUser.class);
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                clickTemplateTile(PERSON_TEMPLATE_TITLE);
            }
        }
        queryListener.dumpAndStop();
        tester.assertRenderedPage(PageUser.class);
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(AbstractStatisticsPrinter.Format.TEXT, AbstractStatisticsPrinter.SortBy.TIME), null, null));
    }

    @Test
    public void test230editUser() {
        logger.info(getTestName());

        for (int i = 0; i < REPETITION_COUNT; i++) {
            tester.startPage(PageUsers.class);

            Stopwatch stopwatch = stopwatch("editUser", "Edit User");
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                clickUserName(EDITED_USER_NAME);
            }
        }

        queryListener.dumpAndStop();
        tester.assertRenderedPage(com.evolveum.midpoint.gui.impl.page.admin.user.PageUser.class);
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(AbstractStatisticsPrinter.Format.TEXT, AbstractStatisticsPrinter.SortBy.TIME), null, null));
    }

    @Test
    public void test231editUserTabProjections() {
        logger.info(getTestName());

        for (int i = 0; i < REPETITION_COUNT; i++) {
            tester.startPage(PageUsers.class);
            clickUserName(EDITED_USER_NAME);

            Stopwatch stopwatch = stopwatch("showProjections", "User's projection tab");
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                clickOnDetailsMenu(1, com.evolveum.midpoint.gui.impl.page.admin.user.PageUser.class);
            }
        }

        queryListener.dumpAndStop();
        tester.assertRenderedPage(com.evolveum.midpoint.gui.impl.page.admin.user.PageUser.class);
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(AbstractStatisticsPrinter.Format.TEXT, AbstractStatisticsPrinter.SortBy.TIME), null, null));
    }

    @Test
    public void test232editUserTabAssignments() {
        logger.info(getTestName());

        for (int i = 0; i < REPETITION_COUNT; i++) {
            tester.startPage(PageUsers.class);
            clickUserName(EDITED_USER_NAME);

            Stopwatch stopwatch = stopwatch("showAssignments", "User's assignmentTab");
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                clickDetailsSubmenu(ASSIGNMENTS_MENU_TITLE, ALL_ASSIGNMENTS_MENU_TITLE);
            }
            AllAssignmentsPanel<?> assignmentsPanel = findComponent(
                    tester.getLastRenderedPage(), AllAssignmentsPanel.class, panel -> true);
            assertNotNull("All Assignments panel was not rendered", assignmentsPanel);
        }

        queryListener.dumpAndStop();
        tester.assertRenderedPage(com.evolveum.midpoint.gui.impl.page.admin.user.PageUser.class);
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(AbstractStatisticsPrinter.Format.TEXT, AbstractStatisticsPrinter.SortBy.TIME), null, null));
    }

    private void clickTemplateTile(String title) {
        TilePanel<?, ?> matchingTile = findComponent(
                tester.getLastRenderedPage(), TilePanel.class,
                tile -> title.equals(tile.getModelObject().getTitle()));

        assertNotNull("No template tile with title '" + title + "' was rendered", matchingTile);
        tester.executeAjaxEvent(matchingTile.getPageRelativePath(), "click");
    }

    private void clickDetailsMenu(String title) {
        DetailsNavigationPanel<?> navigationPanel = findComponent(
                tester.getLastRenderedPage(), DetailsNavigationPanel.class,
                panel -> panel.findParent(DetailsNavigationPanel.class) == null);
        assertNotNull("No details navigation panel was rendered", navigationPanel);

        AjaxLink<?> matchingLink = findAjaxLinkByLabel(navigationPanel, title);
        assertNotNull("No details menu link with title '" + title + "' was rendered", matchingLink);
        tester.clickLink(matchingLink.getPageRelativePath());
    }

    private void clickDetailsSubmenu(String menuTitle, String submenuTitle) {
        DetailsNavigationPanel<?> navigationPanel = findComponent(
                tester.getLastRenderedPage(), DetailsNavigationPanel.class,
                panel -> panel.findParent(DetailsNavigationPanel.class) == null);
        assertNotNull("No details navigation panel was rendered", navigationPanel);

        AjaxLink<?> menuLink = findAjaxLinkByLabel(navigationPanel, menuTitle);
        assertNotNull("No details menu link with title '" + menuTitle + "' was rendered", menuLink);

        DetailsNavigationPanel<?> submenu = findComponent(menuLink.getParent(), DetailsNavigationPanel.class, panel -> true);
        assertNotNull("No submenu for details menu '" + menuTitle + "' was rendered", submenu);

        if (!submenu.isVisibleInHierarchy()) {
            clickDetailsMenu(menuTitle);
            navigationPanel = findComponent(
                    tester.getLastRenderedPage(), DetailsNavigationPanel.class,
                    panel -> panel.findParent(DetailsNavigationPanel.class) == null);
            assertNotNull("No details navigation panel was rendered", navigationPanel);
            menuLink = findAjaxLinkByLabel(navigationPanel, menuTitle);
            assertNotNull("No details menu link with title '" + menuTitle + "' was rendered", menuLink);
            submenu = findComponent(menuLink.getParent(), DetailsNavigationPanel.class, panel -> true);
            assertNotNull("No submenu for details menu '" + menuTitle + "' was rendered", submenu);
        }

        AjaxLink<?> submenuLink = findAjaxLinkByLabel(submenu, submenuTitle);
        assertNotNull("No details submenu link with title '" + submenuTitle + "' was rendered", submenuLink);
        tester.clickLink(submenuLink.getPageRelativePath());
    }

    private void clickUserName(String userName) {
        SelectableDataTable.SelectableRowItem<?> matchingRow = findComponent(
                tester.getLastRenderedPage(), SelectableDataTable.SelectableRowItem.class,
                row -> row.getModelObject() instanceof SelectableBean<?> bean
                        && bean.getValue() instanceof UserType user
                        && userName.equals(WebComponentUtil.getName(user)));

        assertNotNull("No rendered user row for '" + userName + "' was found", matchingRow);

        AjaxLink<?> nameLink = findAjaxLinkByLabel(matchingRow, userName);
        assertNotNull("No name link for user '" + userName + "' was found", nameLink);
        tester.clickLink(nameLink.getPageRelativePath());
    }

    private AjaxLink<?> findAjaxLinkByLabel(MarkupContainer parent, String value) {
        Label matchingLabel = findComponent(parent, Label.class,
                label -> value.equals(label.getDefaultModelObjectAsString())
                        && label.findParent(AjaxLink.class) != null);
        return matchingLabel != null ? matchingLabel.findParent(AjaxLink.class) : null;
    }

    @SuppressWarnings("unchecked")
    private <C extends Component> C findComponent(MarkupContainer parent, Class<? extends Component> type, Predicate<C> predicate) {
        AtomicReference<C> result = new AtomicReference<>();
        parent.visitChildren(Component.class, (component, visit) -> {
            if (type.isInstance(component)) {
                C matchingComponent = (C) component;
                if (!predicate.test(matchingComponent)) {
                    return;
                }
                result.set(matchingComponent);
                visit.stop();
            }
        });
        return result.get();
    }

    @Test
    public void test310orgTree() {
        logger.info(getTestName());
        runTestFor(PageOrgTree.class, "orgTree", "Organization tree");
    }

    @Test
    public void test410allTasks() {
        logger.info(getTestName());
        runTestFor(PageTasks.class, "tasks", "All tasks");
    }

    //TODO adapt
//    @Test
//    public void test510systemConfigurationAdminGuiConfig() {
//        logger.info(getTestName());
//        PageParameters params = new PageParameters();
//        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_ADMIN_GUI);
//        runTestFor(PageSystemConfiguration.class, params,"adminGuiConfig", "Admin Gui Config");
//    }

    @Test(enabled = false) // doesn't work because of getPageBase usages
    public void test200sidebarMenu() {
        logger.info(getTestName());
        Stopwatch stopwatch = stopwatch("sidebar", "sidebar perf");
        try (Split ignored = stopwatch.start()) {
            queryListener.start();
            tester.startComponentInPage(LeftMenuPanel.class);
        }

        queryListener.dumpAndStop();
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(AbstractStatisticsPrinter.Format.TEXT, AbstractStatisticsPrinter.SortBy.TIME), null, null));
    }

}
