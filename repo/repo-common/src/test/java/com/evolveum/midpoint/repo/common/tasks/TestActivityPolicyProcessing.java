/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityTree;
import com.evolveum.midpoint.repo.common.activity.ActivityUtil;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRule;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRulesCollector;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesProcessingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyProcessingModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests the policy processing switch ({@code activity/policies/processing}) at the level of a single activity tree.
 *
 * These tests are "static": no task is run or even persisted. Each test parses a base task file (which carries
 * no processing flag), optionally applies the flag in memory, builds the activity tree, and calls the rule
 * collector (or the virtual assignment collector) directly. This way one base file serves both the positive
 * control and the flagged variants.
 *
 * Semantics under test: suppression declared on an activity applies to policies *declared* at that activity
 * and its whole subtree; rules declared on ancestors are not affected.
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
public class TestActivityPolicyProcessing extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activities/processing");

    /** Composition with a "root-rule" policy on the root and a "child-rule" policy on the first child. */
    private static final TestObject<TaskType> TASK_100_COMPOSITION_POLICIES = TestObject.file(
            TEST_DIR, "task-100-composition-policies.xml", "9c1de6a2-08f1-4a51-9d21-6e0b3c1a0100");

    /** Policy object carrying the "ref-rule", referenced from task 120. */
    private static final TestObject<PolicyType> POLICY_120 = TestObject.file(
            TEST_DIR, "policy-120.xml", "9c1de6a2-08f1-4a51-9d21-6e0b3c1a0121");

    /** Single activity referencing {@link #POLICY_120}. */
    private static final TestObject<TaskType> TASK_120_POLICYREF = TestObject.file(
            TEST_DIR, "task-120-policyref.xml", "9c1de6a2-08f1-4a51-9d21-6e0b3c1a0120");

    /** Composition with a virtual assignment on the root (the target role is intentionally not resolved). */
    private static final TestObject<TaskType> TASK_130_VIRTUAL_ASSIGNMENTS = TestObject.file(
            TEST_DIR, "task-130-virtual-assignments.xml", "9c1de6a2-08f1-4a51-9d21-6e0b3c1a0130");

    private static final String ROLE_130_OID = "9c1de6a2-08f1-4a51-9d21-6e0b3c1a0131";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(POLICY_120, initResult);
    }

    @Test
    public void test100NoFlagCollectsAllDeclaredRules() throws Exception {
        OperationResult result = createOperationResult();

        given("the composition task without any processing flag");
        Task task = createInMemoryTask(TASK_100_COMPOSITION_POLICIES, null, result);

        when("rules are collected for the root and for each child");
        then("the root sees its own rule; the first child additionally sees its own declaration");
        assertThat(getRuleNames(collectRules(getRootActivity(task), task, result)))
                .as("rules collected for the root activity")
                .containsExactly("root-rule");

        for (Activity<?, ?> child : getChildren(task)) {
            List<String> expected = "first".equals(child.getIdentifier())
                    ? List.of("root-rule", "child-rule")
                    : List.of("root-rule");
            assertThat(getRuleNames(collectRules(child, task, result)))
                    .as("rules collected for child '%s'", child.getIdentifier())
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    public void test110FlagOnRootSuppressesWholeTree() throws Exception {
        OperationResult result = createOperationResult();

        given("the composition task with processing disabled on the root");
        Task task = createInMemoryTask(TASK_100_COMPOSITION_POLICIES, this::disableOnRoot, result);

        when("rules are collected for the root and for each child");
        then("no rules are collected anywhere in the tree");
        assertThat(collectRules(getRootActivity(task), task, result))
                .as("rules collected for the root activity")
                .isEmpty();

        for (Activity<?, ?> child : getChildren(task)) {
            assertThat(collectRules(child, task, result))
                    .as("rules collected for child '%s'", child.getIdentifier())
                    .isEmpty();
        }
    }

    @Test
    public void test120FlagOnChildSuppressesOnlyChildRules() throws Exception {
        OperationResult result = createOperationResult();

        given("the composition task with processing disabled on the first child only");
        Task task = createInMemoryTask(
                TASK_100_COMPOSITION_POLICIES,
                def -> setActivityPoliciesProcessing(def.getComposition().getActivity().get(0), PolicyProcessingModeType.NONE),
                result);

        when("rules are collected for the root and for each child");
        then("the root keeps its rule; the flagged child loses only its own declaration");
        assertThat(getRuleNames(collectRules(getRootActivity(task), task, result)))
                .as("rules collected for the root activity")
                .containsExactly("root-rule");

        for (Activity<?, ?> child : getChildren(task)) {
            assertThat(getRuleNames(collectRules(child, task, result)))
                    .as("rules collected for child '%s'", child.getIdentifier())
                    .containsExactly("root-rule");
        }
    }

    @Test
    public void test130FlagSuppressesPolicyRefRules() throws Exception {
        OperationResult result = createOperationResult();

        given("the policyRef task, once without and once with the processing flag");
        Task control = createInMemoryTask(TASK_120_POLICYREF, null, result);
        Task flagged = createInMemoryTask(TASK_120_POLICYREF, this::disableOnRoot, result);

        when("rules are collected for both root activities");
        then("the control sees the referenced rule (proving the policyRef path works); the flagged one sees nothing");
        assertThat(getRuleNames(collectRules(getRootActivity(control), control, result)))
                .as("rules collected without the flag")
                .containsExactly("ref-rule");

        assertThat(collectRules(getRootActivity(flagged), flagged, result))
                .as("rules collected with the flag")
                .isEmpty();
    }

    @Test
    public void test140FlagSuppressesVirtualAssignments() throws Exception {
        OperationResult result = createOperationResult();

        given("the virtual assignment task, once without and once with the processing flag");
        Task control = createInMemoryTask(TASK_130_VIRTUAL_ASSIGNMENTS, null, result);
        Task flagged = createInMemoryTask(
                TASK_130_VIRTUAL_ASSIGNMENTS,
                def -> setVirtualAssignmentPoliciesProcessing(def, PolicyProcessingModeType.NONE),
                result);

        when("virtual assignments are collected for the child activities");
        then("the control's child sees the root's virtual assignment (proving the XML shape works)");
        var controlAssignments = ActivityUtil.getAllVirtualAssignments(getChildren(control).get(0));
        assertThat(controlAssignments)
                .as("virtual assignments collected without the flag")
                .hasSize(1);
        assertThat(controlAssignments.iterator().next().getLeft().getTargetRef().getOid())
                .as("target of the collected virtual assignment")
                .isEqualTo(ROLE_130_OID);

        then("the flagged task's activities see no virtual assignments");
        assertThat(ActivityUtil.getAllVirtualAssignments(getRootActivity(flagged)))
                .as("virtual assignments collected for the flagged root")
                .isEmpty();
        assertThat(ActivityUtil.getAllVirtualAssignments(getChildren(flagged).get(0)))
                .as("virtual assignments collected for the flagged child")
                .isEmpty();
    }

    /**
     * Parses the task file fresh, applies the customizer to its root activity definition (if any),
     * and wraps it in a transient (non-persisted) task instance — enough for building the activity tree.
     */
    private Task createInMemoryTask(
            TestObject<TaskType> testTask, Consumer<ActivityDefinitionType> customizer, OperationResult result)
            throws Exception {
        PrismObject<TaskType> task = testTask.getFresh();
        if (customizer != null) {
            customizer.accept(task.asObjectable().getActivity());
        }
        return taskManager.createTaskInstance(task, result);
    }

    private void disableOnRoot(ActivityDefinitionType def) {
        setActivityPoliciesProcessing(def, PolicyProcessingModeType.NONE);
    }

    private static void setActivityPoliciesProcessing(ActivityDefinitionType def, PolicyProcessingModeType mode) {
        getOrCreateProcessing(def).setActivityPolicies(mode);
    }

    private static void setVirtualAssignmentPoliciesProcessing(ActivityDefinitionType def, PolicyProcessingModeType mode) {
        getOrCreateProcessing(def).setVirtualAssignmentPolicies(mode);
    }

    private static ActivityPoliciesProcessingType getOrCreateProcessing(ActivityDefinitionType def) {
        if (def.getPolicies() == null) {
            def.beginPolicies();
        }
        if (def.getPolicies().getProcessing() == null) {
            def.getPolicies().beginProcessing();
        }
        return def.getPolicies().getProcessing();
    }

    private Activity<?, ?> getRootActivity(Task task) throws Exception {
        return ActivityTree.create(task).getRootActivity();
    }

    private List<Activity<?, ?>> getChildren(Task task) throws Exception {
        Activity<?, ?> root = getRootActivity(task);
        root.initializeChildrenMapIfNeeded();
        return root.getChildrenCopy();
    }

    private List<String> getRuleNames(List<ActivityPolicyRule> rules) {
        return rules.stream().map(ActivityPolicyRule::getName).toList();
    }

    private static List<ActivityPolicyRule> collectRules(Activity<?, ?> activity, Task task, OperationResult result)
            throws ConfigurationException {
        return ActivityPolicyRulesCollector.collectRules(
                activity, task, CommonTaskBeans.get().objectResolver, result);
    }
}
