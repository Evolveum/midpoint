/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests AbstractSearchIterativeTaskHandler and related classes. See e.g. MID-5227.
 */
@UnusedTestElement("not in suite")
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIterativeTasks extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/iterative-tasks");

    private static final File TASK_BUCKETS_MULTITHREADED_FILE = new File(TEST_DIR, "task-buckets-multithreaded.xml");
    private static final String TASK_BUCKETS_MULTITHREADED_OID = "4ccd0cde-c506-49eb-9718-f85ba3438515";

    private static final int BUCKETS = 10;              // must be <= 100   (if > 10, adapt buckets specification in task)
    private static final int USERS_PER_BUCKET = 3;      // must be <= 10

    private static final int EXPECTED_SUBTASKS = 4;

    private static TestIterativeTasks instance;         // brutal hack

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        createUsers(initResult);
        instance = this;
    }

    private void createUsers(OperationResult result) throws ObjectAlreadyExistsException, SchemaException {
        for (int i = 0; i < BUCKETS; i++) {
            for (int j = 0; j < USERS_PER_BUCKET; j++) {
                createUser(result, i * 10 + j);
            }
        }
    }

    private void createUser(OperationResult result, int number) throws ObjectAlreadyExistsException, SchemaException {
        UserType user = new UserType(prismContext)
                .name(String.format("%03d", number))
                .subtype("test");
        repositoryService.addObject(user.asPrismObject(), null, result);
    }

    /**
     * MID-5227
     */
    @Test
    public void test100RunBucketsMultithreaded() throws Exception {
        // GIVEN

        // WHEN
        when();
        addTask(TASK_BUCKETS_MULTITHREADED_FILE);

        // THEN
        then();
        waitForTaskFinish(TASK_BUCKETS_MULTITHREADED_OID, false);
    }

    @SuppressWarnings("unused") // called from Groovy code
    public static void checkLightweightSubtasks(TaskType subtask) {
        RunningTask parent = instance.taskManager.getLocallyRunningTaskByIdentifier(subtask.getParent());
        assertNotNull("no parent running task", parent);
        Collection<? extends RunningTask> subtasks = parent.getLightweightAsynchronousSubtasks();
        LoggerFactory.getLogger(TestIterativeTasks.class).info(
                "Subtask: {}, parent: {}, its subtasks: ({}): {}", subtask, parent, subtasks.size(), subtasks);
        if (subtasks.size() > EXPECTED_SUBTASKS) {
            AssertJUnit.fail("Exceeded the expected number of subtasks: have " + subtasks.size()
                    + ", expected max: " + EXPECTED_SUBTASKS + ": " + subtasks);
        }
    }
}
