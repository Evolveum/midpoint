/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.test.TestActivityPolicyUtils;
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
 * "Pre-seeded counter" scenario: verifies that threshold enforcement fires against a counter that is
 * already at the threshold when the activity starts - without processing any item.
 *
 * A literal pre-seed of a fresh task is not possible: the first realization purges the whole activity
 * state on root run start. So the pre-seed is produced: the first run trips
 * a {@code suspendTask} policy exactly at the threshold (single-threaded, so the counter value is
 * deterministic), and the resume continues the same realization. The halt recorded by the policy action stops
 * the resumed activity before any item is processed: the counter stays at the threshold and nothing more is
 * imported (issue 11051).
 *
 * Companion to {@link TestFocusPolicyActionsComposite#test600ThresholdSplitAcrossSiblings}, which covers
 * the cross-child leg.
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

    /** First run trips the root-placed suspend policy exactly at the threshold; the resumed run is halted at its start. */
    @Test
    public void test100ResumeEnforcesOnPreseededCounter() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_PRESEED;
        deleteIfPresent(task, result);

        when("first run: suspend policy on the composition root trips at the threshold");
        addObject(task, getTestTask(), result, contributeAtRoot(addRule(THRESHOLD, suspend())));
        waitForTaskCloseOrSuspend(task.oid, TIMEOUT);

        then("suspended exactly at the threshold; the tripping item was not imported");
        String counterId = TestActivityPolicyUtils.buildPolicyIdentifier(
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

        when("resume: the halt recorded by the policy must stop the run at its start");
        var firstRunStart = getTask(task.oid).asObjectable().getLastRunStartTimestamp();
        taskManager.resumeTaskTree(task.oid, result);
        waitForTaskCloseOrSuspend(task.oid, TIMEOUT);
        assertThat(getTask(task.oid).asObjectable().getLastRunStartTimestamp())
                .as("last run start after resume").isNotEqualTo(firstRunStart);

        then("re-suspended before processing any item; counter unchanged, nothing more was imported");
        // @formatter:off
        assertTaskTree(task.oid, "after resume")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .child("main")
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounter(counterId, THRESHOLD);
        // @formatter:on
        assertThat(countImported()).as("imported after resume (nothing processed)")
                .isEqualTo(THRESHOLD - 1);
    }
}
