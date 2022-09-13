/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import java.io.File;

import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuPanel;
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
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
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
        runTestFor(pageToRender, null, stopwatchName, stopwatchDescription);
    }

    private void runTestFor(Class pageToRender, PageParameters params, String stopwatchName, String stopwatchDescription) {
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
            tester.debugComponentTrees();
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                tester.executeAjaxEvent("detailsView:template:template:additionalButtons:0:additionalButton:compositedButton", "click");
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

//        runTestFor(PageUser.class, "newUser", "New user");

    }

    @Test
    public void test230editUser() {
        logger.info(getTestName());

        for (int i = 0; i < REPETITION_COUNT; i++) {
            tester.startPage(PageUsers.class);

            String idTable = "mainForm:table";
            tester.assertComponent(idTable, MainObjectListPanel.class);

            tester.debugComponentTrees(":rows:.*:cells:3:cell:link");

            String id = idTable + ":items:itemsTable:box:tableContainer:table:body:rows:3:cells:3:cell:link";

            Stopwatch stopwatch = stopwatch("editUser", "Edit User");
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                tester.clickLink(id);
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

            String idTable = "mainForm:table";
            tester.assertComponent(idTable, MainObjectListPanel.class);

            tester.debugComponentTrees(":rows:.*:cells:3:cell:link");

            String id = idTable + ":items:itemsTable:box:tableContainer:table:body:rows:3:cells:3:cell:link";
            tester.clickLink(id);

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

            String idTable = "mainForm:table";
            tester.assertComponent(idTable, MainObjectListPanel.class);

            tester.debugComponentTrees(":rows:.*:cells:3:cell:link");

            String id = idTable + ":items:itemsTable:box:tableContainer:table:body:rows:3:cells:3:cell:link";
            tester.clickLink(id);

            Stopwatch stopwatch = stopwatch("showAssignments", "User's assignmentTab");
            try (Split ignored = stopwatch.start()) {
                queryListener.start();
                //order 0 == All Assignments
                clickOnDetailsAssignmentMenu(0, com.evolveum.midpoint.gui.impl.page.admin.user.PageUser.class);
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
    public void test310orgTree() throws Exception {
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
