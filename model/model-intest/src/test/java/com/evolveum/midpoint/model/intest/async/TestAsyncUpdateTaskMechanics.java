/*
 * Copyright (C) 2019-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.async;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.sources.Amqp091AsyncUpdateSource;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.amqp.EmbeddedBroker;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests working of async update task - starting, stopping, reporting, and so on.
 */
@UnusedTestElement("not in suite, fails during init with: Unknown config store type 'Memory'")
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestAsyncUpdateTaskMechanics extends AbstractConfiguredModelIntegrationTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "async/task");

    private static final TestResource<?> RESOURCE_HR =
            new TestResource<>(TEST_DIR, "resource-hr-amqp091.xml", "63693a4a-07ee-4903-a206-3f777f4495a5");
    private static final TestResource<?> TASK_ASYNC_UPDATE_HR_NO_WORKERS =
            new TestResource<>(TEST_DIR, "task-async-update-hr-no-workers.xml", "074fe1fd-3099-42f7-b6ad-1e1e5eec51d5");
    private static final TestResource<?> TASK_ASYNC_UPDATE_HR_ONE_WORKER =
            new TestResource<>(TEST_DIR, "task-async-update-hr-one-worker.xml", "e6cc59c5-8404-4a0f-9ad0-2cd5c81d9f6b");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File CHANGE_USER_ADD_FILE = new File(TEST_DIR, "change-template-user-add.xml");

    private static final String QUEUE_NAME = "testQueue";

    private final EmbeddedBroker embeddedBroker = new EmbeddedBroker();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        embeddedBroker.start();
        embeddedBroker.createQueue(QUEUE_NAME);

        importObject(RESOURCE_HR, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @AfterClass
    public void stop() {
        embeddedBroker.stop();
    }

    @Test
    public void test000Sanity() throws CommonException {
        Task task = getTestTask();
        assertSuccess(modelService.testResource(RESOURCE_HR.oid, task, task.getResult()));
    }

    @Test
    public void test100SmallTaskNoWorkers() throws IOException, TimeoutException, CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersBefore = getObjectCount(UserType.class);

        prepareMessages(CHANGE_USER_ADD_FILE, "100-", 10, true);

        when();
        importObject(TASK_ASYNC_UPDATE_HR_NO_WORKERS, task, result);

        then();
        waitForTaskFinish(TASK_ASYNC_UPDATE_HR_NO_WORKERS.oid, 30000);

        PrismObject<TaskType> taskAfter = getTask(TASK_ASYNC_UPDATE_HR_NO_WORKERS.oid);
        displayTaskWithOperationStats("Task after", taskAfter);

        int usersAdded = getObjectCount(UserType.class) - usersBefore;
        displayValue("Users added", usersAdded);

        assertEquals("Wrong # of users added", 10, usersAdded);
        assertEquals("Wrong task progress", 10, taskAfter.asObjectable().getProgress().intValue());
    }

    // Occasionally fails with: Timeout (30000) while waiting for Task(id:e6cc59c5-8404-4a0f-9ad0-2cd5c81d9f6b,
    // name:HR async update (one worker), oid:e6cc59c5-8404-4a0f-9ad0-2cd5c81d9f6b) to finish. Last result R(run IN_PROGRESS null)
    @Test
    public void test110SmallTaskOneWorker() throws IOException, TimeoutException, CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersBefore = getObjectCount(UserType.class);

        prepareMessages(CHANGE_USER_ADD_FILE, "110-", 10, true);

        when();
        importObject(TASK_ASYNC_UPDATE_HR_ONE_WORKER, task, result);

        then();
        waitForTaskFinish(TASK_ASYNC_UPDATE_HR_ONE_WORKER.oid, 30000);

        PrismObject<TaskType> taskAfter = getTask(TASK_ASYNC_UPDATE_HR_ONE_WORKER.oid);
        displayTaskWithOperationStats("Task after", taskAfter);

        int usersAdded = getObjectCount(UserType.class) - usersBefore;
        displayValue("Users added", usersAdded);

        assertEquals("Wrong # of users added", 10, usersAdded);
        assertNotNull("No task progress", taskAfter.asObjectable().getProgress());
        assertEquals("Wrong task progress", 10, taskAfter.asObjectable().getProgress().intValue());
    }

    @SuppressWarnings("SameParameterValue")
    private void prepareMessages(File templateFile, String prefix, int howMany, boolean markLast)
            throws IOException, TimeoutException {
        String template = String.join("\n", IOUtils.readLines(new FileReader(templateFile)));
        for (int i = 0; i < howMany; i++) {
            String number = String.format("%s%06d", prefix, i);
            String message = template.replaceAll("#", number);
            Map<String, Object> headers = new HashMap<>();
            if (markLast && i == howMany - 1) {
                headers.put(Amqp091AsyncUpdateSource.HEADER_LAST_MESSAGE, true);
            }
            embeddedBroker.send(QUEUE_NAME, message, headers);
        }
    }
}
