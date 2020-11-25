/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.self.*;

import org.apache.wicket.util.tester.WicketTester;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.component.list.UserListPanel;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuPanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.testing.TestQueryListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.tools.testng.PerformanceTestMixin;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class MidScaleGuiTest extends AbstractInitializedGuiIntegrationTest implements PerformanceTestMixin {

    @Autowired TestQueryListener queryListener;

    private final List<String> memInfo = new ArrayList<>();

    @BeforeMethod
    public void reportBeforeTest() {
        queryListener.clear();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        for (int i= 0; i < 20; i++) {
            UserType user = new UserType(prismContext).name("user" + i).fullName("user" + i).givenName("user" + i);
            addObject(user.asPrismObject());
        }
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

    private void runTestFor(Class pageToRender, String stopwathName, String stopwatchDescription) {
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        Stopwatch stopwatch = stopwatch(stopwathName, stopwatchDescription);
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
    public void test310orgTree() {
        logger.info(getTestName());
        runTestFor(PageOrgTree.class, "orgTree", "Organization tree");
    }

    @Test(enabled = false) // doesn't work beacuse of getPageBase usages
    public void test200sidebarMenu() {
        logger.info(getTestName());
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
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
