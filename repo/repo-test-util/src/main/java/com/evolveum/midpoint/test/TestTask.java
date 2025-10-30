/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        rerunErrorsOk(null, result);
    }

    /**
     * A variant of {@link #rerunErrorsOk(OperationResult)} that is suitable for running task trees, as it ignores
     * the operation result, and looks at the root task status only.
     */
    public void rerunTreeErrorsOk(OperationResult result) throws CommonException {
        rerunErrorsOk(
                checkerBuilder -> checkerBuilder.checkOnlySchedulingState(true),
                result);
    }

    public void rerunErrorsOk(TaskFinishChecker.BuilderCustomizer builderCustomizer, OperationResult result)
            throws CommonException {
        long startTime = System.currentTimeMillis();
        test.restartTask(oid, result);
        test.waitForTaskFinish(oid, startTime, defaultTimeout, true, 0, builderCustomizer);
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

    public TaskAsserter<Void> assertTree(String message) throws SchemaException, ObjectNotFoundException {
        return test.assertTaskTree(oid, message);
    }

    public TaskAsserter<Void> assertAfter() throws SchemaException, ObjectNotFoundException {
        return doAssert("after").display();
    }

    public TaskAsserter<Void> assertTreeAfter() throws SchemaException, ObjectNotFoundException {
        return assertTree("after").display();
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

    public String buildPolicyIdentifier(ActivityPath path, String policyIdentifier)
            throws CommonException {

        return buildPolicyIdentifier(path, policyIdentifier, false);
    }

    public String buildPolicyIdentifier(ActivityPath path, String policyIdentifier, boolean exact)
            throws CommonException {

        TaskType task = test.getTask(oid).asObjectable();

        ActivityDefinitionType def = findActivityDefinition(task.getActivity(), path);
        if (def == null) {
            throw new IllegalStateException("No activity definition for path " + path + " in task " + oid);
        }

        ActivityPoliciesType policies = def.getPolicies();
        if (policies == null) {
            throw new IllegalStateException("No activity policies for path " + path + " in task " + oid);
        }

        ActivityPolicyType policy = policies.getPolicy().stream()
                .filter(p -> exact ?
                        Objects.equals(policyIdentifier, p.getName())
                        : p.getName() != null && p.getName().contains(policyIdentifier))
                .findFirst()
                .orElse(null);
        if (policy == null) {
            throw new IllegalStateException("No activity policy matching '" + policyIdentifier + "' for path " + path + " in task " + oid);
        }

        return def.getIdentifier() + ":" + policy.getId();
    }

    private ActivityDefinitionType findActivityDefinition(ActivityDefinitionType def, ActivityPath path) {
        if (path.isEmpty()) {
            return def;
        }

        if (def == null) {
            return null;
        }

        String first = path.first();
        ActivityPath remainder = path.rest();

        ActivityCompositionType composition = def.getComposition();
        if (composition != null) {
            ActivityDefinitionType child = composition.getActivity().stream()
                    .filter(a -> Objects.equals(first, a.getIdentifier()))
                    .findFirst()
                    .orElse(null);

            if (child != null) {
                return findActivityDefinition(child, remainder);
            }
        }

        if (!remainder.isEmpty()) {
            return null; // no more to search
        }

        // noinspection unchecked
        Collection<Item<?, ?>> items = def.asPrismContainerValue().getItems();
        return items.stream()
                .filter(i -> i.getDefinition().getTypeClass().isAssignableFrom(ActivityDefinitionType.class))
                .map(i -> i.getRealValues())
                .flatMap(Collection::stream)
                .map(d -> (ActivityDefinitionType) d)
                .filter(d -> Objects.equals(first, d.getIdentifier()))
                .findFirst()
                .orElse(null);
    }
}
