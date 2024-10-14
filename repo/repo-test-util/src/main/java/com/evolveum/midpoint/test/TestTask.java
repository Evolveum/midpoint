/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Task that is to be used in tests.
 *
 * Currently supporting only plain tasks, i.e. not task trees.
 */
@Experimental
public class TestTask extends TestObject<TaskType> {

    private static final long DEFAULT_TIMEOUT = 250_000;

    /** Default timeout when waiting for this task completion. */
    private final long defaultTimeout;

    /** Temporary: this is how we access the necessary functionality. */
    private AbstractIntegrationTest test;

    private TestTask(TestObjectSource source, String oid, long defaultTimeout) {
        super(source, oid);
        this.defaultTimeout = defaultTimeout;
    }

    /**
     * TODO change to static factory method
     */
    public TestTask(@NotNull File dir, @NotNull String fileName, @NotNull String oid, long defaultTimeout) {
        this(new FileBasedTestObjectSource(dir, fileName), oid, defaultTimeout);
    }

    /**
     * TODO change to static factory method
     */
    public TestTask(@NotNull File dir, @NotNull String fileName, @NotNull String oid) {
        this(dir, fileName, oid, DEFAULT_TIMEOUT);
    }

    public static TestTask of(@NotNull TaskType task, long defaultTimeout) {
        return new TestTask(new InMemoryTestObjectSource(task), task.getOid(), defaultTimeout);
    }

    public static TestTask of(@NotNull TaskType task) {
        return of(task, DEFAULT_TIMEOUT);
    }

    public static TestTask file(@NotNull File dir, @NotNull String fileName, String oid) {
        return new TestTask(dir, fileName, oid);
    }

    /**
     * Initializes the task - i.e. imports it into repository (via model).
     * This may or may not start the task, depending on the execution state in the file.
     *
     * @param test To provide access to necessary functionality. Temporary!
     */
    public void init(AbstractIntegrationTest test, Task task, OperationResult result) throws CommonException {
        commonInit(test, task, result);
        this.test = test;
    }

    /**
     * Starts the task and waits for the completion.
     *
     * TODO better name
     */
    public void rerun(OperationResult result) throws CommonException {
        long startTime = System.currentTimeMillis();
        test.restartTask(oid, result);
        test.waitForTaskFinish(oid, startTime, defaultTimeout, false);
    }

    public void rerunErrorsOk(OperationResult result) throws CommonException {
        long startTime = System.currentTimeMillis();
        test.restartTask(oid, result);
        test.waitForTaskFinish(oid, startTime, defaultTimeout, true);
    }

    public void restart(OperationResult result) throws CommonException {
        test.restartTask(oid, result);
    }

    public void runFor(long howLong, OperationResult result) throws CommonException {
        restart(result);
        MiscUtil.sleepCatchingInterruptedException(howLong);
        suspend();
    }

    public TaskAsserter<Void> doAssert(String message) throws SchemaException, ObjectNotFoundException {
        return test.assertTask(oid, message);
    }

    public TaskAsserter<Void> assertAfter() throws SchemaException, ObjectNotFoundException {
        return doAssert("after").display();
    }

    public void suspend() throws CommonException {
        test.suspendTask(oid);
    }

    public void resume(OperationResult result) throws CommonException {
        test.taskManager.resumeTask(
                test.taskManager.getTaskPlain(oid, result),
                result);
    }

    // FIXME very primitive implementation
    public void resumeAndWaitForFinish(OperationResult result) throws CommonException {
        long startTime = System.currentTimeMillis();
        resume(result);
        test.waitForTaskFinish(oid, startTime, defaultTimeout, true);
    }
}
