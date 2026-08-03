/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityTree;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRule;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRulesCollector;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests {@link ActivityPolicyRulesCollector} at the level of a single activity tree.
 *
 * These tests are "static": no task is run here. The activity tree is built from the task object and the collector
 * is called directly, which is enough to observe what the collector reports for each activity.
 *
 * The point of interest is an activity with *embedded* children (here: the composite mock, structurally the same as
 * reconciliation). Such children get their definition by cloning the parent's one. Should that clone keep the parent's
 * policies, the collector would report each parent rule twice for a child: once from walking up to the parent, and
 * once from the child's own (inherited) copy. See {@code ActivityHandlerUtils.cloneWithoutIdForChildActivity}.
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
public class TestActivityPolicyRulesCollector extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activities/collector");

    /** Composite mock activity with a single policy declared on the root activity. */
    private static final TestObject<TaskType> TASK_500_MOCK_COMPOSITE_WITH_POLICIES = TestObject.file(
            TEST_DIR,
            "task-500-mock-composite-with-policies.xml",
            "5e6a0f9c-4b1d-4a3e-9c2b-7f0d1a2b3c40");

    private static final String POLICY_NAME = "Execution time";

    /** The embedded children created by {@code CompositeMockActivityHandler}. */
    private static final List<String> CHILD_IDENTIFIERS = List.of("opening", "closing");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        taskAdd(TASK_500_MOCK_COMPOSITE_WITH_POLICIES, initResult);
    }

    /**
     * The embedded children must not inherit the policies declared on the parent into their own definitions.
     * This is the root cause of rule doubling, so it is asserted separately from the collector output below.
     */
    @Test
    public void test100EmbeddedChildrenDoNotInheritPolicies() throws Exception {
        OperationResult result = createOperationResult();

        given("a composite mock task with a policy on the root activity");
        Task task = getTaskPlain(result);

        when("the activity tree is built (without running the task)");
        List<Activity<?, ?>> children = getChildren(task);

        then("the root activity declares the policy, while the embedded children declare nothing");
        assertThat(getPolicyNames(getRootActivity(task)))
                .as("policies declared by the root activity")
                .containsExactly(POLICY_NAME);

        assertThat(children)
                .as("embedded children of the composite mock activity")
                .hasSize(CHILD_IDENTIFIERS.size());

        for (Activity<?, ?> child : children) {
            var policies = child.getDefinition().getPoliciesDefinition().getPolicies();
            assertThat(policies.getPolicy())
                    .as("policies inherited into definition of child '%s'", child.getIdentifier())
                    .isEmpty();
            assertThat(policies.getPolicyRef())
                    .as("policyRefs inherited into definition of child '%s'", child.getIdentifier())
                    .isEmpty();
        }
    }

    /**
     * The collector must report the parent's rule exactly once for each embedded child: coming from the parent,
     * under the parent's activity path. Two occurrences here would mean the rule is doubled.
     */
    @Test
    public void test110RulesAreNotDoubledForEmbeddedChildren() throws Exception {
        OperationResult result = createOperationResult();

        given("a composite mock task with a policy on the root activity");
        Task task = getTaskPlain(result);

        when("rules are collected for the root activity");
        List<ActivityPolicyRule> rootRules = collectRules(getRootActivity(task), task, result);

        then("the root's own rule is reported once");
        assertThat(rootRules)
                .as("rules collected for the root activity")
                .hasSize(1);
        assertThat(rootRules.get(0).getName()).isEqualTo(POLICY_NAME);
        assertThat(rootRules.get(0).getPath()).isEqualTo(ActivityPath.empty());

        when("rules are collected for each embedded child");
        then("the parent's rule is reported once per child, under the parent's path");
        for (Activity<?, ?> child : getChildren(task)) {
            List<ActivityPolicyRule> childRules = collectRules(child, task, result);

            // Were the policies inherited into the child's definition, there would be two rules here: the parent's one
            // and the child's inherited copy (the latter under the child's own path, hence a distinct identifier).
            assertThat(childRules)
                    .as("rules collected for child '%s'", child.getIdentifier())
                    .hasSize(1);

            ActivityPolicyRule rule = childRules.get(0);
            assertThat(rule.getName())
                    .as("name of the rule collected for child '%s'", child.getIdentifier())
                    .isEqualTo(POLICY_NAME);
            assertThat(rule.getPath())
                    .as("path of the rule collected for child '%s' (must be the declaring parent's)",
                            child.getIdentifier())
                    .isEqualTo(ActivityPath.empty());
        }
    }

    private Task getTaskPlain(OperationResult result) throws Exception {
        return taskManager.getTaskPlain(TASK_500_MOCK_COMPOSITE_WITH_POLICIES.oid, result);
    }

    private Activity<?, ?> getRootActivity(Task task) throws Exception {
        return ActivityTree.create(task).getRootActivity();
    }

    private List<Activity<?, ?>> getChildren(Task task) throws Exception {
        Activity<?, ?> root = getRootActivity(task);
        root.initializeChildrenMapIfNeeded();
        return root.getChildrenCopy();
    }

    private List<String> getPolicyNames(Activity<?, ?> activity) {
        return activity.getDefinition().getPoliciesDefinition().getPolicies().getPolicy().stream()
                .map(p -> p.getName())
                .toList();
    }

    private static List<ActivityPolicyRule> collectRules(Activity<?, ?> activity, Task task, OperationResult result)
            throws ConfigurationException {
        return ActivityPolicyRulesCollector.collectRules(
                activity, task, CommonTaskBeans.get().objectResolver, result);
    }
}
