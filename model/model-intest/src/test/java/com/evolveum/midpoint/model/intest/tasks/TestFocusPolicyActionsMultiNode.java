/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Distributed (multi-node / worker-task) topology tests for focus policy actions.
 *
 * Companion to {@link TestFocusPolicyActions} (single-node: single-thread + worker threads).
 * Here the import activity is distributed into worker subtasks via bucketing; the shared policy
 * counter lives in the coordinator (root) activity state, and when a focus policy trips, all
 * worker tasks must stop.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyActionsMultiNode extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-actions");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000001";

    /** Shared dummy source resource, initialized per class (see {@code tasks/common}). */
    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_IMPORT_MULTI =
            TestObject.file(TEST_DIR, "task-fa-import-multi.xml", "e2f00000-0000-0000-0000-000000000003");
    private static final TestObject<TaskType> TASK_IMPORT_COMPOSITE_SUBTASKS =
            TestObject.file(TEST_DIR, "task-fa-import-composite-subtasks.xml", "e2f00000-0000-0000-0000-000000000004");

    private static final int ACCOUNTS = 20;
    private static final String ACCOUNT_NAME_PATTERN = "a%02d";
    private static final int ADD_THRESHOLD = 5;
    private static final int RESTART_THRESHOLD = 15;
    private static final String RULE_ADD = "fpa-add";

    private static final long TIMEOUT = 90_000;
    private static final long SLEEP = 1000;

    /** coordinator + 2 worker tasks. */
    private static final int TASKS_IN_TREE = 3;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(ACCOUNT_NAME_PATTERN)
                .withController(RESOURCE_SOURCE.controller)
                .execute();
    }

    @BeforeMethod
    public void resetState() throws Exception {
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
            for (PrismObject<UserType> user : users) {
                repositoryService.deleteObject(UserType.class, user.getOid(), result);
            }
        }
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                prismContext.queryFor(ShadowType.class).item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_OID).build(),
                null, result);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            if (RESOURCE_SOURCE.controller.getDummyResource().getAccountByName(name) == null) {
                RESOURCE_SOURCE.controller.addAccount(name);
            }
        }
    }

    private PolicyRuleType addRule(int threshold, PolicyActionsType actions) {
        return new PolicyRuleType()
                .name(RULE_ADD)
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType().operation(ChangeTypeType.ADD)))
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(threshold)))
                .policyActions(actions);
    }

    private Consumer<PrismObject<TaskType>> contributeInline(PolicyRuleType rule, ActivityPath path) {
        return taskObj -> {
            ActivityDefinitionType def = ActivityDefinitionUtil.findActivityDefinition(
                    taskObj.asObjectable().getActivity(), path);
            assertThat(def).as("activity def at " + path).isNotNull();
            if (def.getPolicies() == null) {
                def.setPolicies(new ActivityPoliciesType());
            }
            def.getPolicies().getPolicy().add(rule.clone());
        };
    }

    private String inlineIdentifier(TestObject<TaskType> task, ActivityPath path) throws CommonException {
        return ActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), path, RULE_ADD, true);
    }

    private int countImportedUsers() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
        }
        return count;
    }

    /**
     * suspendTask on a distributed import: when the shared counter reaches the threshold, the coordinator
     * and all worker tasks stop. Counter lives on the coordinator (root) activity state.
     */
    @Test
    public void test100SuspendMultinode() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT_MULTI;
        deleteIfPresent(task, result);

        when("distributed import with an inline add-threshold rule + suspendTask");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType())),
                        ActivityPath.empty()));

        and("waiting for the coordinator and all workers to suspend");
        waitForTaskTreeCloseOrCondition(task.oid, result, TIMEOUT, SLEEP, tasksSuspendedPredicate(TASKS_IN_TREE));

        then("the whole tree is suspended; the coordinator holds the tripped counter");
        String id = inlineIdentifier(task, ActivityPath.empty());
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .rootActivityState()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADD_THRESHOLD, ACCOUNTS)
                        .end()
                    .end()
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .assertSuspended()
                    .end()
                .subtask(1)
                    .display()
                    .assertSuspended();
        // @formatter:on
    }

    /** skipActivity on a distributed import: the activity is aborted and the tree closes with fatal error. */
    @Test
    public void test200SkipMultinode() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT_MULTI;
        deleteIfPresent(task, result);

        when("distributed import with an inline add-threshold rule + skipActivity");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType().skipActivity(new SkipActivityPolicyActionType())),
                        ActivityPath.empty()));

        and("waiting for the tree to close");
        waitForTaskTreeCloseOrCondition(task.oid, result, TIMEOUT, SLEEP, tasksClosedPredicate(TASKS_IN_TREE));

        then("the tree is closed with a fatal error and the import stopped before all accounts were processed");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .assertFatalError();
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users")
                .isGreaterThanOrEqualTo(ADD_THRESHOLD - 1)
                .isLessThan(ACCOUNTS);
    }

    /**
     * restartActivity on a distributed import: probes whether the coordinator's shared counter resets
     * across a distributed restart (converge) or is kept (would loop). Expectation: converges to closed.
     */
    @Test
    public void test300RestartMultinode() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT_MULTI;
        deleteIfPresent(task, result);

        when("distributed import with an inline add-threshold rule + restartActivity");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(RESTART_THRESHOLD, new PolicyActionsType().restartActivity(new RestartActivityPolicyActionType())),
                        ActivityPath.empty()));

        and("waiting for the tree to converge and close");
        waitForTaskTreeCloseOrCondition(task.oid, result, 3 * TIMEOUT, SLEEP, tasksClosedPredicate(TASKS_IN_TREE));

        then("the tree closes successfully with all accounts imported (counter reset across restart)");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess();
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users").isEqualTo(ACCOUNTS);
    }

    /**
     * Activities delegated to subtasks: a focus policy on the 'main' import activity (which runs in its own
     * subtask) is still evaluated and enforced — the tree suspends and only the pre-threshold batch is imported.
     */
    @Test
    public void test400SuspendSubtasks() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT_COMPOSITE_SUBTASKS;
        deleteIfPresent(task, result);

        when("composite-in-subtasks import with an inline add-threshold rule + suspendTask on 'main'");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType())),
                        ActivityPath.fromId("main")));
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("the tree is suspended; the fatal error and the trip are in the 'main' subtask (not the root)");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()   // root result may stay IN_PROGRESS; the error is in the subtask
                .subtask("main", false)
                    .display()
                    .assertSuspended()
                    .assertFatalError();
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users").isEqualTo(ADD_THRESHOLD - 1);
    }
}
