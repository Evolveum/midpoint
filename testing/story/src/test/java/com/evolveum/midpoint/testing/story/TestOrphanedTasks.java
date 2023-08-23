/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOrphanedTasks extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "orphaned-tasks");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<TaskType> TASK_MARK_ORPHANED_TASKS = TestObject.file(TEST_DIR, "task-mark-orphaned-tasks.xml", "59cb3937-b923-4903-9afc-ee27e56be262");
    private static final TestObject<TaskType> TASK_PARENT = TestObject.file(TEST_DIR, "task-parent.xml", "c6634274-1dda-4e0f-91f3-37e9a59f4cf3");
    private static final TestObject<TaskType> TASK_PARENT_VOLATILE = TestObject.file(TEST_DIR, "task-parent-volatile.xml", "c2b0ade1-5352-4c96-b7e8-b0068db902d9");
    private static final TestObject<TaskType> TASK_CHILD = TestObject.file(TEST_DIR, "task-child.xml", "0ba77e3c-b7cc-41c5-a4bd-8874ae330fcc");
    private static final TestObject<TaskType> TASK_ORPHANED = TestObject.file(TEST_DIR, "task-orphaned.xml", "95b6bd28-f7cd-4c70-aee2-fe111bf1917d");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ARCHETYPE_TASK_RECOMPUTATION, initTask, initResult);

        addObject(TASK_MARK_ORPHANED_TASKS, initTask, initResult);
        addObject(TASK_PARENT, initTask, initResult);
        addObject(TASK_PARENT_VOLATILE, initTask, initResult);
        addObject(TASK_CHILD, initTask, initResult);
        addObject(TASK_ORPHANED, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
        assertNotOrphaned(TASK_MARK_ORPHANED_TASKS.oid);
        assertNotOrphaned(TASK_PARENT.oid);
        assertNotOrphaned(TASK_PARENT_VOLATILE.oid);
        assertNotOrphaned(TASK_CHILD.oid);
        assertNotOrphaned(TASK_ORPHANED.oid);
    }

    @Test
    public void test100DeleteParentAndMarkOrphans() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repositoryService.deleteObject(TaskType.class, TASK_PARENT_VOLATILE.oid, result);

        when();
        rerunTask(TASK_MARK_ORPHANED_TASKS.oid, result);

        then();

        stabilize();
        assertTask(TASK_MARK_ORPHANED_TASKS.oid, "After")
                .display()
                .assertSuccess()
                .assertProgress(6);

        assertNotOrphaned(TASK_MARK_ORPHANED_TASKS.oid);
        assertNotOrphaned(TASK_PARENT.oid);
        assertNotOrphaned(TASK_CHILD.oid);
        assertOrphaned(TASK_ORPHANED.oid);
    }

    private void assertNotOrphaned(String oid) throws CommonException {
        assertTask(oid, "after")
                .assertNoPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_ORPHANED);
    }

    private void assertOrphaned(String oid) throws CommonException {
        assertTask(oid, "after")
                .assertPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_ORPHANED);
    }
}
