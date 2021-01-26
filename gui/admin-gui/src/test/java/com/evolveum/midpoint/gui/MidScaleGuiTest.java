/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import java.io.File;

import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuPanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.tools.testng.PerformanceTestMethodMixin;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingCart;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.page.self.PageUserSelfProfile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class MidScaleGuiTest extends AbstractInitializedGuiIntegrationTest implements PerformanceTestMethodMixin {

    private static final String TEST_DIR = "./src/test/resources/midScale";

    private static final File FILE_ORG_STRUCT = new File(TEST_DIR, "org-struct.xml");
    private static final File FILE_USERS = new File(TEST_DIR, "users.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importObjectsFromFileNotRaw(FILE_ORG_STRUCT, initTask, initResult);
        initResult.computeStatusIfUnknown();
        if (!initResult.isSuccess()) {
            System.out.println("init result:\n" + initResult);
        }
        importObjectsFromFileNotRaw(FILE_USERS, initTask, initResult);

        modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_ENABLE_EXPERIMENTAL_FEATURES),
                initTask, initResult, true);

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
        runTestFor(PageSelfCredentials.class, "rcredentials", "Credentials");
    }

    @Test
    public void test040PageRequestRole() {
        displayTestTitle(getTestName());
        runTestFor(PageAssignmentShoppingCart.class, "requestRole", "Request a role");
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
        for (int i = 0; i < 1; i++) {
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
        runTestFor(PageUser.class, "newUser", "New user");

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

    @Test
    public void test510systemConfigurationAdminGuiConfig() {
        logger.info(getTestName());
        PageParameters params = new PageParameters();
        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_ADMIN_GUI);
        runTestFor(PageSystemConfiguration.class, params,"adminGuiConfig", "Admin Gui Config");
    }

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
