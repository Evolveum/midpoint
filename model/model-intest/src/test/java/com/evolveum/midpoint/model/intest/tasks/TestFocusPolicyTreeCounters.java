/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Checks how the counter of a policy rule placed on a *parent* activity is summed across its child activities.
 *
 * A reconciliation splits into partial activities (operationCompletion, resourceObjects, remainingShadows). They all
 * keep their counters in the parent's activity state ({@code PartialReconciliationActivityRun#useOtherActivityStateForCounters}),
 * while the total to be checked against the threshold is computed as "local + preexisting", where the preexisting part
 * is summed over the task tree by {@code PreexistingValuesComputer} (excluding the current activity's own path).
 *
 * This test drives a single rule that counts in *two* different children (an ADD in `resourceObjects`, a DELETE in
 * `remainingShadows`), so it detects whether an item counted by an earlier child is counted once - or twice - in the
 * total seen by a later child.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyTreeCounters extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-tree");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000010";

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-dummy-tree-source.xml", RESOURCE_OID, "fpt-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_IMPORT =
            TestObject.file(TEST_DIR, "task-fpt-import.xml", "e2f00000-0000-0000-0000-000000000010");
    private static final TestObject<TaskType> TASK_RECON =
            TestObject.file(TEST_DIR, "task-fpt-recon.xml", "e2f00000-0000-0000-0000-000000000011");

    private static final String ACCOUNT_NAME_PATTERN = "t%02d";

    /** Accounts imported up-front; they are linked and unchanged, so the reconciliation does not count them. */
    private static final int BASELINE = 10;
    /** Accounts appearing only after the import => counted as ADD by `resourceObjects`. */
    private static final int ADDED = 5;
    /** Accounts disappearing after the import => counted as DELETE by `remainingShadows`. */
    private static final int REMOVED = 3;

    /**
     * Above the number of really counted items ({@link #ADDED} + {@link #REMOVED} = 8), so the rule must not trip.
     * It is however below 2 * {@link #ADDED}, so it does trip if the ADDs counted by `resourceObjects` are counted
     * a second time in the total computed for `remainingShadows`.
     */
    private static final int THRESHOLD = 9;

    private static final String RULE_NAME = "fpt-add-or-delete";

    private static final long TIMEOUT = 60_000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);
        for (int i = 0; i < BASELINE; i++) {
            RESOURCE_SOURCE.controller.addAccount(accountName(i));
        }
    }

    private static String accountName(int i) {
        return String.format(ACCOUNT_NAME_PATTERN, i);
    }

    /** Counts both the ADDs (done by `resourceObjects`) and the DELETEs (done by `remainingShadows`). */
    private PolicyRuleType addOrDeleteRule() {
        return new PolicyRuleType()
                .name(RULE_NAME)
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()
                                .operation(ChangeTypeType.ADD)
                                .operation(ChangeTypeType.DELETE)))
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(THRESHOLD)))
                .policyActions(new PolicyActionsType()
                        .suspendTask(new SuspendTaskPolicyActionType()));
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
        return ActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), path, RULE_NAME, true);
    }

    /**
     * An item counted by an earlier child activity must contribute to the parent rule's total exactly once.
     *
     * `resourceObjects` adds {@link #ADDED} users and `remainingShadows` deletes {@link #REMOVED} of them, which is
     * 8 counted items in total - below the threshold of {@link #THRESHOLD}. So the task has to finish, and the
     * counter has to end up at exactly 8.
     *
     * If the ADDs were counted twice (once as the local value read from the shared parent counter, once again via the
     * preexisting values summed over the tree), the total seen by `remainingShadows` would reach the threshold and
     * the task would be suspended instead.
     */
    @Test
    public void test100ParentPolicyCountsEachItemOnce() throws Exception {
        OperationResult result = getTestOperationResult();

        given("baseline accounts are imported, so they are linked and unchanged");
        int usersBefore = getObjectCount(UserType.class);
        deleteIfPresent(TASK_IMPORT, result);
        addObject(TASK_IMPORT, getTestTask(), result);
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_IMPORT.oid, result, TIMEOUT);
        assertThat(getObjectCount(UserType.class)).as("users after the initial import")
                .isEqualTo(usersBefore + BASELINE);

        and("some accounts appear (to be ADDed) and some disappear (to be DELETEd) afterwards");
        for (int i = BASELINE; i < BASELINE + ADDED; i++) {
            RESOURCE_SOURCE.controller.addAccount(accountName(i));
        }
        for (int i = 0; i < REMOVED; i++) {
            RESOURCE_SOURCE.controller.getDummyResource().deleteAccountByName(accountName(i));
        }

        when("reconciliation runs with an add-or-delete threshold rule on the parent (root) activity");
        deleteIfPresent(TASK_RECON, result);
        addObject(TASK_RECON, getTestTask(), result,
                contributeInline(addOrDeleteRule(), ActivityPath.empty()));
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_RECON.oid, result, 2 * TIMEOUT);

        then("the rule does not trip: each counted item contributed to the total exactly once");
        String id = inlineIdentifier(TASK_RECON, ActivityPath.empty());
        // @formatter:off
        assertTaskTree(TASK_RECON.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADDED + REMOVED, ADDED + REMOVED);
        // @formatter:on
        assertThat(getObjectCount(UserType.class)).as("users after the reconciliation")
                .isEqualTo(usersBefore + BASELINE + ADDED - REMOVED);
    }
}
