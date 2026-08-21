/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * "Pre-seeded counter" scenario: verifies that threshold enforcement fires against a counter that is
 * already at the threshold when the activity starts processing - without waiting for a fresh batch of
 * matching items.
 *
 * A literal pre-seed of a fresh task is not possible: the first realization purges the whole activity
 * state on root run start. So the pre-seed is produced: the first run trips
 * a {@code suspendTask} policy exactly at the threshold (single-threaded, so the counter value is
 * deterministic), and the resume continues the same realization with the persisted counter. On resume,
 * the already-imported users correlate and yield only link/modify operations (not matching the
 * {@code modification(ADD)} constraint), so the first and only matching item is the previously tripped
 * one - it must push the counter to {@code threshold + 1} and re-enforce within the same activity run.
 *
 * This deterministically exercises enforcement within the counting child of a root-placed rule
 * against an accumulated counter - the leg that intermittently goes silent on CI (see
 * {@code TestFocusPolicyCombinations} combo 3). Companion to
 * {@link TestFocusPolicyActionsComposite#test600ThresholdSplitAcrossSiblings}, which covers the
 * cross-child leg.
 *
 * TODO Delete after flaky tests are fixed (probably).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyPreseededCounter extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-actions");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000003";

    private static final DummyTestResource RESOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source-3.xml", RESOURCE_OID, "fpc-source-3",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_PRESEED =
            TestObject.file(TEST_DIR, "task-fp-preseed.xml", "e2f00000-0000-0000-0000-000000000007");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN = "c%02d";
    private static final int THRESHOLD = 5;
    private static final String RULE_ADD = "fpp-add";

    private static final long TIMEOUT = 90_000;
    private static final long SLEEP = 500;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initDummyResource(RESOURCE, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(PATTERN)
                .withController(RESOURCE.controller)
                .execute();
    }

    @BeforeMethod
    public void resetState() throws Exception {
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(PATTERN, i);
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
            String name = String.format(PATTERN, i);
            if (RESOURCE.controller.getDummyResource().getAccountByName(name) == null) {
                RESOURCE.controller.addAccount(name);
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

    private PolicyActionsType suspend() {
        return new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType());
    }

    private java.util.function.Consumer<PrismObject<TaskType>> contributeAtRoot(PolicyRuleType rule) {
        return taskObj -> {
            ActivityDefinitionType def = ActivityDefinitionUtil.findActivityDefinition(
                    taskObj.asObjectable().getActivity(), ActivityPath.empty());
            assertThat(def).as("root activity def").isNotNull();
            if (def.getPolicies() == null) {
                def.setPolicies(new ActivityPoliciesType());
            }
            def.getPolicies().getPolicy().add(rule.clone());
        };
    }

    private int countImported() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME)
                            .eqPoly(String.format(PATTERN, i)).matchingNorm().build(),
                    null, result);
        }
        return count;
    }

    /** Reads the given counter from the 'main' child activity state; 0 if not present. */
    private int counterValue(String counterId) throws Exception {
        TaskType task = getTask(TASK_PRESEED.oid).asObjectable();
        TaskActivityStateType taskState = task.getActivityState();
        ActivityStateType root = taskState != null ? taskState.getActivity() : null;
        if (root == null) {
            return 0;
        }
        for (ActivityStateType child : root.getActivity()) {
            if (!"main".equals(child.getIdentifier())) {
                continue;
            }
            ActivityCounterGroupsType counters = child.getCounters();
            ActivityCounterGroupType group = counters != null ? counters.getFullExecutionModePolicyRules() : null;
            if (group == null) {
                return 0;
            }
            for (ActivityCounterType counter : group.getCounter()) {
                if (counterId.equals(counter.getIdentifier())) {
                    return counter.getValue() != null ? counter.getValue() : 0;
                }
            }
        }
        return 0;
    }

    /**
     * Waits until the task is suspended AND the counter reached the expected value - i.e. until the
     * <i>second</i> suspension, not the (still lingering) first one.
     */
    private void waitForSuspendedWithCounter(String counterId, int expected, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Task t = taskManager.getTaskWithResult(TASK_PRESEED.oid, getTestOperationResult());
            if (t.isSuspended() && counterValue(counterId) == expected) {
                return;
            }
            Thread.sleep(SLEEP);
        }
        throw new AssertionError("Task " + TASK_PRESEED.oid + " did not reach suspended state with counter "
                + counterId + " = " + expected + " within " + timeout + " ms (current value: "
                + counterValue(counterId) + ")");
    }

    /**
     * First run trips the root-placed suspend policy exactly at the threshold; the resume then enforces
     * against the persisted ("pre-seeded") counter on the very first matching item.
     */
    @Test
    public void test100ResumeEnforcesOnPreseededCounter() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_PRESEED;
        deleteIfPresent(task, result);

        when("first run: suspend policy on the composition root trips at the threshold");
        addObject(task, getTestTask(), result, contributeAtRoot(addRule(THRESHOLD, suspend())));
        waitForTaskCloseOrSuspend(task.oid, TIMEOUT);

        then("suspended exactly at the threshold; the tripping item was not imported");
        String counterId = ActivityPolicyUtils.buildPolicyIdentifier(
                getTask(task.oid), ActivityPath.empty(), RULE_ADD, true);
        // @formatter:off
        assertTaskTree(task.oid, "after first run")
                .display()
                .assertSuspended()
                .rootActivityState()
                    .child("main")
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounter(counterId, THRESHOLD);
        // @formatter:on
        assertThat(countImported()).as("imported before the first trip").isEqualTo(THRESHOLD - 1);

        when("resume: the persisted counter is the pre-seed; the first matching item must re-enforce");
        taskManager.resumeTaskTree(task.oid, result);
        waitForSuspendedWithCounter(counterId, THRESHOLD + 1, TIMEOUT);

        then("re-suspended after exactly one matching item; nothing more was imported");
        // @formatter:off
        assertTaskTree(task.oid, "after resume")
                .display()
                .assertSuspended()
                .rootActivityState()
                    .child("main")
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounter(counterId, THRESHOLD + 1);
        // @formatter:on
        assertThat(countImported()).as("imported after resume (tripping item blocked again)")
                .isEqualTo(THRESHOLD - 1);
    }
}
