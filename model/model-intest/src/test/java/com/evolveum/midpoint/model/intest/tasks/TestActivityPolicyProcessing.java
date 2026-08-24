/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.TREE_OVERVIEW_PREFERRED;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.test.TestActivityPolicyUtils;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.policy.PlainPolicyRuleIdentifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesProcessingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyProcessingModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 *
 * == First part (`test1xx`)
 *
 * Tests the policy processing switch ({@code activity/policies/processing}) end to end: a task suspended
 * by a policy threshold is recovered by disabling policy processing (via
 * {@link ModelInteractionService#updateActivityPoliciesProcessing}) and resuming.
 *
 * Covers all three policy sources: inline activity policies, {@code policyRef}, and {@code virtualAssignments}
 * (the latter two are exactly the cases the older per-rule "enabled" rewriting could not handle).
 *
 * == Second part (`test2xx`)
 *
 * Having three-levels activity tree (root -> { first, second -> { 1, 2, 3 }, third }).
 * The "second/2" activity has a focus policy rule attached inline, via policyRef and via virtual assignment.
 * The action is "skip". We check that the execution is as it should be.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestActivityPolicyProcessing extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activity-policy-processing");

    private static final long TIMEOUT = 60000L;

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "dummy.xml",
            "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0001",
            "resource-app-policy",
            DummyResourceContoller::populateWithDefaultSchema);

    /** The "err*" accounts fail in the name inbound with a fatal item error; see the resource definition. */
    private static final DummyTestResource RESOURCE_DUMMY_FAULTY = new DummyTestResource(
            TEST_DIR,
            "dummy-faulty.xml",
            "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0004",
            "resource-app-policy-faulty",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<PolicyType> POLICY_110 = TestObject.file(
            TEST_DIR, "policy-110.xml", "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0002");

    private static final TestObject<RoleType> ROLE_120_VA = TestObject.file(
            TEST_DIR, "role-120-va.xml", "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0003");

    private static final TestObject<TaskType> TASK_100_IMPORT_INLINE = TestObject.file(
            TEST_DIR, "task-100-import-inline.xml", "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0100");

    private static final TestObject<TaskType> TASK_110_IMPORT_POLICYREF = TestObject.file(
            TEST_DIR, "task-110-import-policyref.xml", "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0110");

    private static final TestObject<TaskType> TASK_120_IMPORT_VA = TestObject.file(
            TEST_DIR, "task-120-import-va.xml", "b7f1e230-5c3d-4d21-9a4e-0d5c1a2b0120");

    private static final TestTask TASK_200_RECOMPUTE_INLINE = new TestTask(
            TEST_DIR, "task-200-recompute-inline.xml", "64118b9f-fd06-46da-b9a9-f6f6ae1b999f");

    private static final TestTask TASK_210_RECOMPUTE_POLICYREF = new TestTask(
            TEST_DIR, "task-210-recompute-policyref.xml", "52844dc4-54e6-456a-ab41-ef39d41e2dff");
    private static final TestObject<?> POLICY_210 = TestObject.file(
            TEST_DIR, "policy-210.xml", "8894ef46-16f3-41a7-92d7-61a1afa541e7");

    private static final TestTask TASK_220_RECOMPUTE_VA = new TestTask(
            TEST_DIR, "task-220-recompute-va.xml", "acef6509-fb4d-4fe6-9baf-759a1a547f32");
    private static final TestObject<RoleType> ROLE_220_VA = TestObject.file(
            TEST_DIR, "role-220-va.xml", "c8b1c27a-bf23-4d57-8d18-efa25ecd5086");

    private DummyResourceContoller dummyResourceCtl;
    private DummyResourceContoller faultyResourceCtl;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        faultyResourceCtl = initDummyResource(RESOURCE_DUMMY_FAULTY, initTask, initResult);
        faultyResourceCtl.addAccount("good1", "good user 1");
        faultyResourceCtl.addAccount("good2", "good user 2");
        faultyResourceCtl.addAccount("err1", "broken user 1");
        faultyResourceCtl.addAccount("err2", "broken user 2");

        initTestObjects(initTask, initResult,
                POLICY_110, POLICY_210,
                ROLE_120_VA, ROLE_220_VA);
    }

    /** Suspend on inline activity policy, then disable processing and resume to completion. */
    @Test
    public void test100InlinePolicySuspendFlagResume() throws Exception {
        OperationResult result = getTestOperationResult();

        when("the import with the inline suspend-on-errors policy runs against the faulty resource");
        addObject(TASK_100_IMPORT_INLINE, getTestTask(), result);
        waitForTaskCloseOrSuspend(TASK_100_IMPORT_INLINE.oid, TIMEOUT);

        then("the task suspends on the threshold, with the counter at 2 and the rule triggered once");
        String counterId = TestActivityPolicyUtils.buildPolicyIdentifier(
                getTask(TASK_100_IMPORT_INLINE.oid), ActivityPath.empty(), "suspend-on-errors", true);
        // @formatter:off
        assertTaskTree(TASK_100_IMPORT_INLINE.oid, "after first run")
                .display()
                .assertSuspended()
                .activityState(ActivityPath.empty())
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(counterId, 2)
                    .end()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("suspend-on-errors")
                            .assertTriggerCount(1)
                        .end()
                    .end();
        // @formatter:on

        when("policy processing is disabled and the task is resumed (accounts still broken)");
        disableProcessingAndResume(TASK_100_IMPORT_INLINE.oid, processingNone(), result);

        then("the task completes despite the failing items; no new policy trigger appears");
        // @formatter:off
        assertTaskTree(TASK_100_IMPORT_INLINE.oid, "after resume with processing disabled")
                .display()
                .assertClosed()
                .activityState(ActivityPath.empty())
                    .assertComplete()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("suspend-on-errors")
                            .assertTriggerCount(1)
                        .end()
                    .end();
        // @formatter:on
    }

    /** Suspend on a policyRef-based policy — the case the old per-rule "enabled" rewriting could not handle. */
    @Test
    public void test110PolicyRefSuspendFlagResume() throws Exception {
        OperationResult result = getTestOperationResult();

        when("the import with the referenced suspend-on-errors policy runs against the faulty resource");
        addObject(TASK_110_IMPORT_POLICYREF, getTestTask(), result);
        waitForTaskCloseOrSuspend(TASK_110_IMPORT_POLICYREF.oid, TIMEOUT);

        then("the task suspends on the threshold, with the counter at 2 and the rule triggered once");
        String counterId = PlainPolicyRuleIdentifier.of(POLICY_110.oid, 1L).asString();
        // @formatter:off
        assertTaskTree(TASK_110_IMPORT_POLICYREF.oid, "after first run")
                .display()
                .assertSuspended()
                .activityState(ActivityPath.empty())
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(counterId, 2)
                    .end()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("ref-suspend-on-errors")
                            .assertTriggerCount(1)
                        .end()
                    .end();
        // @formatter:on

        when("policy processing is disabled and the task is resumed (accounts still broken)");
        disableProcessingAndResume(TASK_110_IMPORT_POLICYREF.oid, processingNone(), result);

        then("the task completes despite the failing items; no new policy trigger appears");
        // @formatter:off
        assertTaskTree(TASK_110_IMPORT_POLICYREF.oid, "after resume with processing disabled")
                .display()
                .assertClosed()
                .activityState(ActivityPath.empty())
                    .assertComplete()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("ref-suspend-on-errors")
                            .assertTriggerCount(1)
                        .end()
                    .end();
        // @formatter:on
    }

    /** Suspend on a focus policy brought in by virtualAssignments, then disable that scope and resume. */
    @Test
    public void test120VirtualAssignmentsSuspendFlagResume() throws Exception {
        OperationResult result = getTestOperationResult();

        given("four fresh accounts without owners exist on the clean resource");
        for (int i = 1; i <= 4; i++) {
            dummyResourceCtl.addAccount("va" + i, "va user " + i);
        }

        when("the import guarded by the virtually-assigned max-2-adds policy runs");
        addObject(TASK_120_IMPORT_VA, getTestTask(), result);
        waitForTaskCloseOrSuspend(TASK_120_IMPORT_VA.oid, TIMEOUT);

        then("the task suspends after two user adds");
        String counterId = PlainPolicyRuleIdentifier.of(ROLE_120_VA.oid, 1L).asString();
        // @formatter:off
        assertTaskTree(TASK_120_IMPORT_VA.oid, "after first run")
                .display()
                .assertSuspended()
                .activityState(ActivityPath.empty())
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(counterId, 2)
                    .end();
        // @formatter:on

        when("virtual assignment policy processing is disabled and the task is resumed");
        ActivityPoliciesProcessingType processing = new ActivityPoliciesProcessingType()
                .virtualAssignmentPolicies(PolicyProcessingModeType.NONE);
        disableProcessingAndResume(TASK_120_IMPORT_VA.oid, processing, result);

        then("the import completes and all four users exist");
        // @formatter:off
        assertTaskTree(TASK_120_IMPORT_VA.oid, "after resume with processing disabled")
                .display()
                .assertClosed()
                .assertSuccess();
        // @formatter:on

        for (int i = 1; i <= 4; i++) {
            assertThat(findUserByUsername("va" + i))
                    .as("user va%d after resumed import", i)
                    .isNotNull();
        }
    }

    private ActivityPoliciesProcessingType processingNone() {
        return new ActivityPoliciesProcessingType()
                .activityPolicies(PolicyProcessingModeType.NONE)
                .virtualAssignmentPolicies(PolicyProcessingModeType.NONE);
    }

    private void disableProcessingAndResume(String taskOid, ActivityPoliciesProcessingType processing, OperationResult result)
            throws Exception {
        boolean changed = modelInteractionService.updateActivityPoliciesProcessing(
                getTask(taskOid), processing, getTestTask(), result);
        assertThat(changed).as("task changed by updateActivityPoliciesProcessing").isTrue();

        taskManager.resumeTaskTree(taskOid, result);
        waitForTaskCloseOrSuspend(taskOid, TIMEOUT);
    }

    /** Checks "skip" inside a complex activity tree, with inlined rules. See the class javadoc. */
    @Test
    public void test200RecomputeInline() throws Exception {
        executeRecomputeTest(TASK_200_RECOMPUTE_INLINE);
    }

    /** Checks "skip" inside a complex activity tree, with referenced rules. See the class javadoc. */
    @Test
    public void test210RecomputePolicyRef() throws Exception {
        executeRecomputeTest(TASK_210_RECOMPUTE_POLICYREF);
    }

    /** Checks "skip" inside a complex activity tree, with virtual assignments. See the class javadoc. */
    @Test
    public void test220RecomputeVirtual() throws Exception {
        executeRecomputeTest(TASK_220_RECOMPUTE_VA);
    }

    private void executeRecomputeTest(TestTask testTask) throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("task is run");
        testTask.init(this, task, result);
        testTask.rerunTreeErrorsOk(result);

        then("the task completes, failing in second/2, skipping second, continuing with third");
        var rootTask = testTask.assertTreeAfter().getObjectable();
        var progressInfo = ActivityProgressInformation.fromRootTask(rootTask, TREE_OVERVIEW_PREFERRED);
        assertProgress(progressInfo, "after")
                .display()
                .assertChildren(3)
                .child("first").assertComplete().end()
                .child("second") // here the rule is defined
                .child("1").assertComplete().end()
                .child("2").assertInProgress().end() // failed -> "3" is not executed, and we continue on "third"
                .child("3").assertNotStarted().end()
                .end()
                .child("third").assertComplete().end(); // executed
    }
}
