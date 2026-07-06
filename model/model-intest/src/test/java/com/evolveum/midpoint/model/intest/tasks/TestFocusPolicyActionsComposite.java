/*
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * More complex focus-policy-action scenarios in a heterogeneous composite task:
 * child {@code alpha} imports resource A multithreaded (worker threads), child {@code beta} imports
 * resource B distributed into worker subtasks (multinode). Policies are placed at different levels
 * (a specific child, or the composition root) and use different actions (skip / suspend).
 *
 * Companion to {@link TestFocusPolicyActions} (single activity, per action) and
 * {@link TestFocusPolicyActionsMultiNode} (single distributed activity, per action).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyActionsComposite extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-actions");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_A_OID = "c1a70000-0000-0000-0000-000000000001";
    private static final String RESOURCE_B_OID = "c1a70000-0000-0000-0000-000000000002";

    private static final DummyTestResource RESOURCE_A = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_A_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);
    private static final DummyTestResource RESOURCE_B = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source-2.xml", RESOURCE_B_OID, "fpc-source-2",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_COMPOSITE =
            TestObject.file(TEST_DIR, "task-fx-composite.xml", "e2f00000-0000-0000-0000-000000000005");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN_A = "a%02d";
    private static final String PATTERN_B = "b%02d";
    private static final int ADD_THRESHOLD = 5;
    /** High enough that one restart (which consumes the first batch) then converges. */
    private static final int RESTART_THRESHOLD = 15;
    private static final String RULE_ADD = "fpa-add";

    private static final long TIMEOUT = 90_000;
    private static final long SLEEP = 1000;

    private static final ActivityPath ALPHA = ActivityPath.fromId("alpha");
    private static final ActivityPath BETA = ActivityPath.fromId("beta");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initDummyResource(RESOURCE_A, initTask, initResult);
        initDummyResource(RESOURCE_B, initTask, initResult);
        createAccounts(RESOURCE_A, PATTERN_A);
        createAccounts(RESOURCE_B, PATTERN_B);
    }

    private void createAccounts(DummyTestResource resource, String pattern) throws Exception {
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(pattern)
                .withController(resource.controller)
                .execute();
    }

    @BeforeMethod
    public void resetState() throws Exception {
        OperationResult result = getTestOperationResult();
        cleanUsers(PATTERN_A, result);
        cleanUsers(PATTERN_B, result);
        cleanShadows(RESOURCE_A_OID, result);
        cleanShadows(RESOURCE_B_OID, result);
        recreateAccounts(RESOURCE_A, PATTERN_A);
        recreateAccounts(RESOURCE_B, PATTERN_B);
    }

    private void cleanUsers(String pattern, OperationResult result) throws Exception {
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(pattern, i);
            List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
            for (PrismObject<UserType> user : users) {
                repositoryService.deleteObject(UserType.class, user.getOid(), result);
            }
        }
    }

    private void cleanShadows(String resourceOid, OperationResult result) throws Exception {
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                prismContext.queryFor(ShadowType.class).item(ShadowType.F_RESOURCE_REF).ref(resourceOid).build(),
                null, result);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
    }

    private void recreateAccounts(DummyTestResource resource, String pattern) throws Exception {
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(pattern, i);
            if (resource.controller.getDummyResource().getAccountByName(name) == null) {
                resource.controller.addAccount(name);
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

    private int countImported(String pattern) throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME)
                            .eqPoly(String.format(pattern, i)).matchingNorm().build(),
                    null, result);
        }
        return count;
    }

    /** Waits until the <b>root</b> task itself reaches a terminal state (closed or suspended). */
    private void waitForRootTermination(String oid, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Task t = taskManager.getTaskWithResult(oid, getTestOperationResult());
            if (t.isClosed() || t.isSuspended()) {
                return;
            }
            Thread.sleep(SLEEP);
        }
        throw new AssertionError("Root task " + oid + " did not terminate within " + timeout + " ms");
    }

    /** Waits until the <b>root</b> task is closed, tolerating the transient suspend/resume of restart cycles. */
    private void waitForRootClose(String oid, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Task t = taskManager.getTaskWithResult(oid, getTestOperationResult());
            if (t.isClosed()) {
                return;
            }
            Thread.sleep(SLEEP);
        }
        throw new AssertionError("Root task " + oid + " did not close within " + timeout + " ms");
    }

    private PolicyActionsType skip() {
        return new PolicyActionsType().skipActivity(new SkipActivityPolicyActionType());
    }

    private PolicyActionsType suspend() {
        return new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType());
    }

    private PolicyActionsType restart() {
        return new PolicyActionsType().restartActivity(new RestartActivityPolicyActionType());
    }

    /**
     * skipActivity on the multithreaded child 'alpha': alpha is aborted at its threshold, and the
     * distributed child 'beta' still runs to completion.
     */
    @Test
    public void test100SkipInMultithreadedChild() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_COMPOSITE;
        deleteIfPresent(task, result);

        when("policy with skipActivity on multithreaded child 'alpha'");
        addObject(task, getTestTask(), result, contributeInline(addRule(ADD_THRESHOLD, skip()), ALPHA));
        waitForRootTermination(task.oid, TIMEOUT);

        then("'alpha' aborted, 'beta' completed fully");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .rootActivityState()
                    .child("alpha")
                        .assertAborted()
                        .assertFatalError()
                        .end()
                    .child("beta")
                        .assertComplete()
                        .assertSuccess();
        // @formatter:on
        assertThat(countImported(PATTERN_A)).as("A imported (alpha skipped)")
                .isGreaterThanOrEqualTo(ADD_THRESHOLD - 1).isLessThan(ACCOUNTS);
        assertThat(countImported(PATTERN_B)).as("B imported (beta full)").isEqualTo(ACCOUNTS);
    }

    /**
     * suspendTask on the distributed child 'beta': 'alpha' completes first (no policy), then 'beta'
     * trips its threshold and suspends the whole tree.
     */
    @Test
    public void test200SuspendInWorkerChild() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_COMPOSITE;
        deleteIfPresent(task, result);

        when("policy with suspendTask on distributed child 'beta'");
        addObject(task, getTestTask(), result, contributeInline(addRule(ADD_THRESHOLD, suspend()), BETA));
        waitForRootTermination(task.oid, TIMEOUT);

        then("'alpha' completed fully; tree suspended in 'beta'");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .rootActivityState()
                    .child("alpha")
                        .assertComplete()
                        .assertSuccess();
        // @formatter:on
        assertThat(countImported(PATTERN_A)).as("A imported (alpha full)").isEqualTo(ACCOUNTS);
        assertThat(countImported(PATTERN_B)).as("B imported (beta tripped)")
                .isGreaterThanOrEqualTo(ADD_THRESHOLD - 1).isLessThan(ACCOUNTS);
    }

    /**
     * A skipActivity policy on the composition <b>root</b> aborts the whole composition when it trips
     * (during 'alpha'), so the subsequent child 'beta' never starts. This contrasts with
     * {@link #test100SkipInMultithreadedChild()}, where a skip on the child only skips that child and
     * lets 'beta' continue — i.e. the same action behaves differently depending on placement.
     */
    @Test
    public void test300RootPolicyAbortsComposition() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_COMPOSITE;
        deleteIfPresent(task, result);

        when("policy with skipActivity on the composition root");
        addObject(task, getTestTask(), result, contributeInline(addRule(ADD_THRESHOLD, skip()), ActivityPath.empty()));
        waitForRootTermination(task.oid, TIMEOUT);

        then("the composition (root) is aborted and 'beta' never starts");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .rootActivityState()
                    .assertAborted()
                    .assertFatalError()
                    .child("beta")
                        .assertNotStarted();
        // @formatter:on
        assertThat(countImported(PATTERN_A)).as("A imported before the root abort")
                .isGreaterThanOrEqualTo(ADD_THRESHOLD - 1).isLessThan(ACCOUNTS);
        assertThat(countImported(PATTERN_B)).as("B imported (beta never ran)").isEqualTo(0);
    }

    /**
     * restartActivity on the multithreaded child 'alpha': 'alpha' restarts once (counter reset, attempt
     * incremented), the committed batch is now linked so the retry consumes the remainder and converges;
     * 'beta' then runs normally. The restart is scoped to 'alpha' (not the whole composition).
     */
    @Test
    public void test400RestartInMultithreadedChild() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_COMPOSITE;
        deleteIfPresent(task, result);

        when("policy with restartActivity on multithreaded child 'alpha'");
        addObject(task, getTestTask(), result, contributeInline(addRule(RESTART_THRESHOLD, restart()), ALPHA));
        waitForRootClose(task.oid, 3 * TIMEOUT);

        then("'alpha' converges after one restart, 'beta' completes; all accounts imported");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .child("alpha")
                        .assertComplete()
                        .assertSuccess()
                        .assertExecutionAttempts(2)
                        .end()
                    .child("beta")
                        .assertComplete()
                        .assertSuccess();
        // @formatter:on
        assertThat(countImported(PATTERN_A)).as("A imported").isEqualTo(ACCOUNTS);
        assertThat(countImported(PATTERN_B)).as("B imported").isEqualTo(ACCOUNTS);
    }

    /**
     * restartActivity on the distributed child 'beta': 'alpha' completes first (no policy); 'beta' trips,
     * restarts its distributed run (worker subtasks re-created, coordinator counter reset), then converges.
     */
    @Test
    public void test500RestartInWorkerChild() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_COMPOSITE;
        deleteIfPresent(task, result);

        when("policy with restartActivity on distributed child 'beta'");
        addObject(task, getTestTask(), result, contributeInline(addRule(RESTART_THRESHOLD, restart()), BETA));
        waitForRootClose(task.oid, 3 * TIMEOUT);

        then("the tree converges: both children complete, all accounts imported");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .child("alpha")
                        .assertComplete()
                        .assertSuccess()
                        .end()
                    .child("beta")
                        .assertComplete()
                        .assertSuccess();
        // @formatter:on
        assertThat(countImported(PATTERN_A)).as("A imported").isEqualTo(ACCOUNTS);
        assertThat(countImported(PATTERN_B)).as("B imported").isEqualTo(ACCOUNTS);
    }
}
