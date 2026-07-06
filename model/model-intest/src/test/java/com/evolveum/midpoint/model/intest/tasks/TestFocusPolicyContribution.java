/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
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
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Tests that a focus policy rule behaves identically whether it is contributed to an activity
 * inline, via {@code policyRef}, or via {@code virtualAssignments} (the "contribution form" axis),
 * cross-cut by where in the activity tree it is placed and evaluated (the "placement" axis).
 *
 * See {@code docs/tasks/focus-policies-in-activities.adoc}.
 *
 * Fixed context (not axes here): single-node, single-thread, FULL execution mode.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyContribution extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-contribution");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000001";

    /** Shared dummy source resource, initialized per class (see {@code tasks/common}). */
    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_FPC_IMPORT =
            TestObject.file(TEST_DIR, "task-fpc-import.xml", "e1f00000-0000-0000-0000-000000000001");
    private static final TestObject<TaskType> TASK_FPC_IMPORT_COMPOSITE =
            TestObject.file(TEST_DIR, "task-fpc-import-composite.xml", "e1f00000-0000-0000-0000-000000000002");
    private static final TestObject<TaskType> TASK_FPC_RECONCILIATION =
            TestObject.file(TEST_DIR, "task-fpc-reconciliation.xml", "e1f00000-0000-0000-0000-000000000003");
    private static final TestObject<TaskType> TASK_FPC_IMPORT_SIMULATE =
            TestObject.file(TEST_DIR, "task-fpc-import-simulate.xml", "e1f00000-0000-0000-0000-000000000004");
    private static final TestObject<TaskType> TASK_FPC_IMPORT_SIMULATE_EXECUTE =
            TestObject.file(TEST_DIR, "task-fpc-import-simulate-execute.xml", "e1f00000-0000-0000-0000-000000000005");

    private static final int ACCOUNTS = 20;
    private static final String ACCOUNT_NAME_PATTERN = "a%02d";

    private static final int ADD_THRESHOLD = 5;
    private static final int DELETE_THRESHOLD = 5;
    private static final int MODIFY_THRESHOLD = 5;

    private static final String RULE_ADD = "fpc-add";
    private static final String RULE_DELETE = "fpc-delete";
    private static final String RULE_MODIFY = "fpc-modify";

    private static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    private static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private static final long TIMEOUT = 60_000;

    /** Where the policy rule is delivered to the activity. */
    enum ContributionForm {
        INLINE {
            void applyTo(ActivityDefinitionType def, PolicyRuleType rule, String policyOid) {
                orNew(def).getPolicy().add(rule.clone());
            }
        },
        POLICY_REF {
            void applyTo(ActivityDefinitionType def, PolicyRuleType rule, String policyOid) {
                orNew(def).getPolicyRef().add(
                        new ObjectReferenceType().oid(policyOid).type(PolicyType.COMPLEX_TYPE));
            }
        },
        VIRTUAL_ASSIGNMENT {
            void applyTo(ActivityDefinitionType def, PolicyRuleType rule, String policyOid) {
                orNewVa(def).getAssignment().add(
                        new AssignmentType().targetRef(policyOid, PolicyType.COMPLEX_TYPE));
            }
        };

        boolean needsPolicyObject() {
            return this != INLINE;
        }

        abstract void applyTo(ActivityDefinitionType def, PolicyRuleType rule, String policyOid);

        private static ActivityPoliciesType orNew(ActivityDefinitionType def) {
            if (def.getPolicies() == null) {
                def.setPolicies(new ActivityPoliciesType());
            }
            return def.getPolicies();
        }

        private static VirtualAssignmentsType orNewVa(ActivityDefinitionType def) {
            if (def.getVirtualAssignments() == null) {
                def.setVirtualAssignments(new VirtualAssignmentsType());
            }
            return def.getVirtualAssignments();
        }
    }

    /** A materialized contribution: the created reusable policy object (for ref/va) and the task customizer. */
    record Contribution(ContributionForm form, String policyOid, String ruleName,
                        Consumer<PrismObject<TaskType>> customizer) {
    }

    @DataProvider(name = "forms")
    public Object[][] forms() {
        return new Object[][] {
                { ContributionForm.INLINE },
                { ContributionForm.POLICY_REF },
                { ContributionForm.VIRTUAL_ASSIGNMENT },
        };
    }

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

    /**
     * Ensures every account is "unmatched" again before an import-based test: removes the users and
     * shadows created by previous runs, and recreates any accounts that a delete test removed.
     */
    @BeforeMethod
    public void resetState() throws Exception {
        OperationResult result = getTestOperationResult();

        // delete imported users
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
            for (PrismObject<UserType> user : users) {
                repositoryService.deleteObject(UserType.class, user.getOid(), result);
            }
        }

        // delete shadows for the resource (so re-import creates fresh, unmatched accounts)
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                prismContext.queryFor(ShadowType.class).item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_OID).build(),
                null, result);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }

        // recreate accounts removed by a delete test
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            if (RESOURCE_SOURCE.controller.getDummyResource().getAccountByName(name) == null) {
                RESOURCE_SOURCE.controller.addAccount(name);
            }
        }
    }

    // region rule + policy-object builders

    private PolicyRuleType buildAddRule() {
        return new PolicyRuleType()
                .name(RULE_ADD)
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()
                                .operation(ChangeTypeType.ADD)))
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(ADD_THRESHOLD)))
                .policyActions(new PolicyActionsType()
                        .suspendTask(new SuspendTaskPolicyActionType()));
    }

    private PolicyRuleType buildDeleteRule() {
        return new PolicyRuleType()
                .name(RULE_DELETE)
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()
                                .operation(ChangeTypeType.DELETE)))
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(DELETE_THRESHOLD)))
                .policyActions(new PolicyActionsType()
                        .suspendTask(new SuspendTaskPolicyActionType()));
    }

    private PolicyRuleType buildModifyRule() {
        return new PolicyRuleType()
                .name(RULE_MODIFY)
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()
                                .operation(ChangeTypeType.MODIFY)
                                .item(new ItemPathType(UserType.F_COST_CENTER))))
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(MODIFY_THRESHOLD)))
                .policyActions(new PolicyActionsType()
                        .suspendTask(new SuspendTaskPolicyActionType()));
    }

    private String createPolicyObject(PolicyRuleType rule, OperationResult result) throws CommonException {
        String oid = UUID.randomUUID().toString();
        PolicyType policy = new PolicyType()
                .oid(oid)
                .name("fpc-policy-" + rule.getName() + "-" + oid);
        policy.beginInducement().policyRule(rule.clone());
        repositoryService.addObject(policy.asPrismObject(), null, result);
        return oid;
    }

    /** As {@link #createPolicyObject} but the inducement (illegally, for policyRef use) defines an order. */
    private String createPolicyObjectWithOrder(PolicyRuleType rule, OperationResult result) throws CommonException {
        String oid = UUID.randomUUID().toString();
        PolicyType policy = new PolicyType()
                .oid(oid)
                .name("fpc-policy-bad-order-" + oid);
        policy.beginInducement().order(1).policyRule(rule.clone());
        repositoryService.addObject(policy.asPrismObject(), null, result);
        return oid;
    }

    // endregion

    // region contribution + identifier resolution

    private Contribution contribute(ContributionForm form, PolicyRuleType rule, ActivityPath path,
            OperationResult result) throws CommonException {
        String policyOid = form.needsPolicyObject() ? createPolicyObject(rule, result) : null;
        Consumer<PrismObject<TaskType>> customizer = taskObj -> {
            ActivityDefinitionType root = Objects.requireNonNull(
                    taskObj.asObjectable().getActivity(), "no activity definition");
            ActivityDefinitionType target = ActivityDefinitionUtil.findActivityDefinition(root, path);
            assertThat(target).as("activity def at " + path).isNotNull();
            form.applyTo(target, rule, policyOid);
        };
        return new Contribution(form, policyOid, rule.getName(), customizer);
    }

    /** Resolves the counter key, which differs per contribution form. */
    private String counterIdentifier(Contribution c, TestObject<TaskType> task, ActivityPath defPath,
            OperationResult result) throws CommonException {
        return switch (c.form()) {
            case INLINE -> ActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), defPath, c.ruleName(), true);
            case POLICY_REF, VIRTUAL_ASSIGNMENT -> determineInducedRuleId(c.policyOid(), c.ruleName(), result);
        };
    }

    private String determineInducedRuleId(String oid, String ruleName, OperationResult result) throws CommonException {
        AbstractRoleType role = repositoryService.getObject(AbstractRoleType.class, oid, null, result).asObjectable();
        List<AssignmentType> inducements = role.getInducement().stream()
                .filter(i -> i.getPolicyRule() != null)
                .filter(i -> Objects.equals(ruleName, i.getPolicyRule().getName()))
                .toList();
        assertThat(inducements).as("policy rule inducements in " + oid).hasSize(1);
        Long id = inducements.get(0).getId();
        assertThat(id).as("inducement id").isNotNull();
        return oid + ":" + id;
    }

    // endregion

    // region assertions

    private void assertPolicyOutcome(TestObject<TaskType> task, ActivityPath counterPath, String counterId,
            int allowed, int threads) throws Exception {
        // @formatter:off
        var tree = assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError();
        var state = counterPath.isEmpty() ? tree.rootActivityState() : tree.activityState(counterPath);
        state.display()
                .fullExecutionModePolicyRulesCounters()
                    .assertCounterMinMax(counterId, allowed + 1, allowed + threads);
        // @formatter:on
    }

    private void assertImportedUserCount(int expected) throws Exception {
        assertThat(countImportedUsers()).as("imported users").isEqualTo(expected);
    }

    private int countImportedUsers() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            int found = repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
            count += found;
        }
        return count;
    }

    private void assertTaskResultContains(String taskOid, String text) throws Exception {
        OperationResultType r = getTask(taskOid).asObjectable().getResult();
        assertThat(resultContains(r, text))
                .as("task result mentioning '%s'", text)
                .isTrue();
    }

    private boolean resultContains(OperationResultType r, String text) {
        if (r == null) {
            return false;
        }
        if (r.getMessage() != null && r.getMessage().contains(text)) {
            return true;
        }
        return r.getPartialResults().stream().anyMatch(sub -> resultContains(sub, text));
    }

    // endregion

    /** Import all accounts with no policy, to link users to shadows (bootstrap for the delete test). */
    private void importAllAccounts(OperationResult result) throws Exception {
        deleteIfPresent(TASK_FPC_IMPORT, result);
        addObject(TASK_FPC_IMPORT, getTestTask(), result, (Consumer<PrismObject<TaskType>>) t -> { });
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_FPC_IMPORT.oid, result, 5 * TIMEOUT);
        deleteIfPresent(TASK_FPC_IMPORT, result);
    }

    private void deleteAccountsOnResource(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            RESOURCE_SOURCE.controller.deleteAccount(String.format(ACCOUNT_NAME_PATTERN, i));
        }
    }

    /** Changes the description attribute (inbound-mapped to costCenter) on the first {@code count} accounts. */
    private void changeDescriptionOnResource(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            RESOURCE_SOURCE.controller.getDummyResource()
                    .getAccountByName(String.format(ACCOUNT_NAME_PATTERN, i))
                    .replaceAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME, "changed");
        }
    }

    private int countModifiedCostCenter() throws Exception {
        return repositoryService.countObjects(UserType.class,
                prismContext.queryFor(UserType.class).item(UserType.F_COST_CENTER).eq("changed").build(),
                null, getTestOperationResult());
    }

    // region tests

    /** OWN placement: rule on the single import activity; counter lands on the root activity state. */
    @Test(dataProvider = "forms")
    public void test100OwnPlacement(ContributionForm form) throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_FPC_IMPORT;
        deleteIfPresent(task, result);

        ActivityPath defPath = ActivityPath.empty();
        ActivityPath counterPath = ActivityPath.empty();

        when("importing with an " + form + " add policy on the (own) import activity");
        Contribution c = contribute(form, buildAddRule(), defPath, result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("task is suspended at the threshold and the counter is on the root activity");
        String id = counterIdentifier(c, task, defPath, result);
        assertPolicyOutcome(task, counterPath, id, ADD_THRESHOLD - 1, 1);
        assertImportedUserCount(ADD_THRESHOLD - 1);
    }

    /** PARENT placement, single evaluator: rule on the composition root, evaluated in child "main". */
    @Test(dataProvider = "forms")
    public void test200ParentSingle(ContributionForm form) throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_FPC_IMPORT_COMPOSITE;
        deleteIfPresent(task, result);

        ActivityPath defPath = ActivityPath.empty();            // attach at composition root
        ActivityPath counterPath = ActivityPath.fromId("main"); // lands in the evaluating child

        when("importing (composite) with an " + form + " add policy on the parent activity");
        Contribution c = contribute(form, buildAddRule(), defPath, result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("task is suspended and the counter lands on child 'main'");
        String id = counterIdentifier(c, task, defPath, result);
        assertPolicyOutcome(task, counterPath, id, ADD_THRESHOLD - 1, 1);
        assertImportedUserCount(ADD_THRESHOLD - 1);
    }

    /**
     * PARENT placement, shared state: reconciliation, rule on the root, evaluated across sub-activities.
     * Counter accumulates in the shared parent (root) state, proving cross-child threshold accumulation.
     */
    @Test(dataProvider = "forms")
    public void test300ParentShared(ContributionForm form) throws Exception {
        OperationResult result = getTestOperationResult();

        given("all accounts imported (users linked), then some accounts removed on the resource");
        importAllAccounts(result);
        assertImportedUserCount(ACCOUNTS);
        deleteAccountsOnResource(DELETE_THRESHOLD + 2);

        TestObject<TaskType> task = TASK_FPC_RECONCILIATION;
        deleteIfPresent(task, result);

        ActivityPath defPath = ActivityPath.empty();
        ActivityPath counterPath = ActivityPath.empty(); // shared parent (root) state

        when("reconciling with an " + form + " delete policy on the reconciliation root");
        Contribution c = contribute(form, buildDeleteRule(), defPath, result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, 5 * TIMEOUT);

        then("task is suspended when the shared delete counter reaches the threshold");
        String id = counterIdentifier(c, task, defPath, result);
        assertPolicyOutcome(task, counterPath, id, DELETE_THRESHOLD - 1, 1);
        assertImportedUserCount(ACCOUNTS - (DELETE_THRESHOLD - 1));
    }

    /**
     * N3 — sibling isolation. A rule attached directly to child "main" must be evaluated and counted
     * only there; the sibling activities ("first", "last") must not carry its counter.
     */
    @Test
    public void test400SiblingIsolation() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_FPC_IMPORT_COMPOSITE;
        deleteIfPresent(task, result);

        ActivityPath mainPath = ActivityPath.fromId("main");

        when("an inline add policy is attached directly to child 'main'");
        Contribution c = contribute(ContributionForm.INLINE, buildAddRule(), mainPath, result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("the counter is on 'main' only; 'first' has none and 'last' never starts");
        String id = counterIdentifier(c, task, mainPath, result);
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .child("first")
                        .assertComplete()
                        .assertSuccess()
                        .assertNoCounters()
                        .end()
                    .child("main")
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounterMinMax(id, ADD_THRESHOLD, ADD_THRESHOLD)
                            .end()
                        .end()
                    .child("last")
                        .assertNotStarted();
        // @formatter:on
        assertImportedUserCount(ADD_THRESHOLD - 1);
    }

    /**
     * N1 — policyRef validation. An inducement used via {@code policyRef} must not define {@code order};
     * activity initialization must fail with a configuration error and process nothing.
     */
    @Test
    public void test410PolicyRefWithOrderFails() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_FPC_IMPORT;
        deleteIfPresent(task, result);

        String policyOid = createPolicyObjectWithOrder(buildAddRule(), result);

        when("importing with a policyRef to an inducement that (illegally) defines order");
        addObject(task, getTestTask(), result, (Consumer<PrismObject<TaskType>>) taskObj -> {
            ActivityDefinitionType def = Objects.requireNonNull(taskObj.asObjectable().getActivity());
            if (def.getPolicies() == null) {
                def.setPolicies(new ActivityPoliciesType());
            }
            def.getPolicies().getPolicyRef().add(
                    new ObjectReferenceType().oid(policyOid).type(PolicyType.COMPLEX_TYPE));
        });
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("the task fails with a configuration error and imports nothing");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertFatalError();
        // @formatter:on
        assertImportedUserCount(0);
        assertTaskResultContains(task.oid, "do not support order");
    }

    /**
     * An add policy in `preview (simulate)` mode increments the preview-mode
     * counters and suspends the simulate run without committing anything. Also covers repeated execution:
     * on resume the persistent preview counter re-trips immediately.
     */
    @Test(dataProvider = "forms")
    public void test500SimulateAdd(ContributionForm form) throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_FPC_IMPORT_SIMULATE;
        deleteIfPresent(task, result);

        when("importing in preview mode with an " + form + " add policy");
        Contribution c = contribute(form, buildAddRule(), ActivityPath.empty(), result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("suspended with preview-mode counter at the threshold; nothing committed");
        String id = counterIdentifier(c, task, ActivityPath.empty(), result);
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .previewModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADD_THRESHOLD, ADD_THRESHOLD);
        // @formatter:on
        assertImportedUserCount(0);

        when("resuming the suspended task (repeated execution)");
        taskManager.resumeTaskTree(task.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("persistent preview counter re-trips immediately; still nothing committed");
        // @formatter:off
        assertTaskTree(task.oid, "after repeated execution")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .previewModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADD_THRESHOLD + 1, ADD_THRESHOLD + 1);
        // @formatter:on
        assertImportedUserCount(0);
    }

    /**
     * In `simulate-then-execute` mode the policy trips in the simulate
     * activity, so the execute activity never starts and nothing is committed.
     */
    @Test(dataProvider = "forms")
    public void test510SimulateExecuteAdd(ContributionForm form) throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_FPC_IMPORT_SIMULATE_EXECUTE;
        deleteIfPresent(task, result);

        when("importing simulate-then-execute with an " + form + " add policy on the simulate activity");
        Contribution c = contribute(form, buildAddRule(), SIMULATE, result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("simulate activity suspended, execute activity never started; nothing committed");
        String id = counterIdentifier(c, task, SIMULATE, result);
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .activityState(SIMULATE)
                    .assertFatalError()
                    .previewModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADD_THRESHOLD, ADD_THRESHOLD)
                        .end()
                    .end()
                .activityState(EXECUTE)
                    .assertRealizationState(null);
        // @formatter:on
        assertImportedUserCount(0);
    }

    /**
     * A {@code modification/costCenter} policy triggers on `modify` (not add).
     * Accounts are imported, their description (inbound-mapped to costCenter) is changed, and
     * a re-import trips the modify threshold.
     */
    @Test
    public void test600ModifyCostCenter() throws Exception {
        OperationResult result = getTestOperationResult();

        given("all accounts imported, then costCenter changed on more than the threshold");
        importAllAccounts(result);
        assertImportedUserCount(ACCOUNTS);
        changeDescriptionOnResource(MODIFY_THRESHOLD + 2);

        TestObject<TaskType> task = TASK_FPC_IMPORT;
        deleteIfPresent(task, result);

        when("re-importing with an inline modify-costCenter policy");
        Contribution c = contribute(ContributionForm.INLINE, buildModifyRule(), ActivityPath.empty(), result);
        addObject(task, getTestTask(), result, c.customizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("suspended at the modify threshold; only the pre-threshold users were modified");
        String id = counterIdentifier(c, task, ActivityPath.empty(), result);
        assertPolicyOutcome(task, ActivityPath.empty(), id, MODIFY_THRESHOLD - 1, 1);
        assertThat(countModifiedCostCenter()).as("users with changed costCenter").isEqualTo(MODIFY_THRESHOLD - 1);
    }

    // endregion
}
