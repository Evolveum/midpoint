/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.perf;

import ch.qos.logback.classic.Level;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.mysql.cj.jdbc.Driver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests the performance of bulk import of data. See MID-5368.
 *
 * This test is not meant to be run automatically.
 * It requires externally-configured MySQL database with the data to be imported.
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml","classpath:ctx-interceptor.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImport extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "perf/import");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "template-import.xml");
    private static final String OBJECT_TEMPLATE_OID = "e84d7b5a-4634-4b75-a17c-df0b8b49b593";

    private static final File RESOURCE_SOURCE_FILE = new File(TEST_DIR, "resource-source.xml");
    private static final String RESOURCE_SOURCE_OID = "f2dd9222-6aff-4099-b5a2-04ae6b3a00b7";

    private static final File ORG_BASIC_FILE = new File(TEST_DIR, "org-basic.xml");

    private static final File TASK_IMPORT_FILE = new File(TEST_DIR, "task-import.xml");
    private static final String TASK_IMPORT_OID = "50142510-8003-4a47-993a-2434119f5028";

    private static final int IMPORT_TIMEOUT = (int) TimeUnit.HOURS.toMillis(3);
    private static final long CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    private static final long PROFILING_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    private static final long PROFILING_DURATION = TimeUnit.MINUTES.toMillis(1);

    private static final int USERS = 50000;

    private int usersBefore;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyAuditService.getInstance().setEnabled(false);

        InternalsConfig.turnOffAllChecks();

        Class.forName(Driver.class.getName());

        // Resources
        importAndGetObjectFromFile(ResourceType.class, RESOURCE_SOURCE_FILE, RESOURCE_SOURCE_OID, initTask, initResult);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_FILE, initResult);
        setDefaultUserTemplate(OBJECT_TEMPLATE_OID);

        // Org
        repoAddObjectFromFile(ORG_BASIC_FILE, OrgType.class, initResult);

        usersBefore = repositoryService.countObjects(UserType.class, null, null, initResult);
        display("users before", usersBefore);

        //InternalMonitor.setTrace(InternalOperationClasses.PRISM_OBJECT_CLONES, true);
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;           // we want logging from our system config
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // nothing here
    }

    @Test
    public void test000Sanity() throws Exception {
        final String TEST_NAME = "test000Sanity";
        Task task = taskManager.createTaskInstance(TestImport.class.getName() + "." + TEST_NAME);

        OperationResult testResultHr = modelService.testResource(RESOURCE_SOURCE_OID, task);
        TestUtil.assertSuccess(testResultHr);

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);
    }

    @Test
    public void test100RunImport() throws Exception {
        final String TEST_NAME = "test100RunImport";
        Task task = taskManager.createTaskInstance(TestImport.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_IMPORT_FILE, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        long lastProfilingStarted = 0;
        long start = System.currentTimeMillis();
        for (;;) {
            PrismObject<TaskType> importTask = getTask(TASK_IMPORT_OID);
            TaskExecutionStatusType executionStatus = importTask.asObjectable().getExecutionStatus();
            if (executionStatus != TaskExecutionStatusType.RUNNABLE) {
                System.out.println("Task is not running any more; status = " + executionStatus);
                break;
            }
            if (System.currentTimeMillis() - start > IMPORT_TIMEOUT) {
                display("Task failed to finish", importTask);
                fail("Task failed to finish in give time");
            }
            if (System.currentTimeMillis() - lastProfilingStarted > PROFILING_INTERVAL) {
                lastProfilingStarted = System.currentTimeMillis();
                System.out.println("Starting profiling at " + new Date());
                OperationExecutionLogger.setGlobalOperationInvocationLevelOverride(Level.TRACE);
                Thread.sleep(PROFILING_DURATION);
                OperationExecutionLogger.setGlobalOperationInvocationLevelOverride(null);
                System.out.println("Stopping profiling at " + new Date());
            } else {
                Thread.sleep(CHECK_INTERVAL);
            }
        }

        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<TaskType> taskAfter = repositoryService.getObject(TaskType.class, TASK_IMPORT_OID, null, result);
        String taskXml = prismContext.xmlSerializer().serialize(taskAfter);
        display("Task after", taskXml);

        int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
        display("users after", usersAfter);
        assertEquals("Wrong # of users", usersBefore + USERS, usersAfter);

    }

}
