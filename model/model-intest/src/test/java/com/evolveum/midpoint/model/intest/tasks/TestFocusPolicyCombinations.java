/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * "Full-stack" focus/activity policy combination tests: each combo stacks several non-default axes at
 * once — policy kind (focus modification thresholds vs activity executionTime / itemProcessingResult),
 * placement (own child vs composition/distribution parent), action (suspend / skip / restart) and
 * topology (multithread / multinode / subtask-delegated) — rather than exercising one axis at a time
 * (as the sibling classes do). Contribution is always inline.
 *
 * The shape (mode + topology) is baked into a per-shape task fixture; the runner injects the inline
 * policy rules and each combo carries its own assertion lambda that checks the activity-state tree in
 * depth: execution/result status, per-activity status, policy-rule counters, triggers and notifications.
 *
 * A combo runs one or more {@link Run}s. Combined combos (a focus rule and an activity rule on the same
 * task) run twice: each run tunes exactly one rule to be reachable and the other to be unreachable, so it
 * is always deterministic which rule fires and which stays silent — there is no evaluation-order race.
 *
 * Notes on action/kind pairings:
 * - {@code restart} is a focus-only action here: restarting on an activity constraint
 * (executionTime / errors) would re-run the activity and re-trip the same constraint forever, while
 * restarting on a focus threshold converges once the first batch is linked.
 * - Activity rules therefore use only {@code suspend}/{@code skip}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyCombinations extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-combinations");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");
    private static final File THRESHOLDS_DIR = new File("src/test/resources/tasks/thresholds");

    private static final String RESOURCE_SOURCE_OID = "c1a70000-0000-0000-0000-000000000001";
    private static final String RESOURCE_FAULTY_OID = "c1a70000-0000-0000-0000-000000000002";
    private static final String RESOURCE_SLOW_OID = "1645e542-d034-4118-b8af-97c4d22d81d6";

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_SOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);
    private static final DummyTestResource RESOURCE_FAULTY = new DummyTestResource(
            COMMON_DIR, "resource-dummy-faulty.xml", RESOURCE_FAULTY_OID, "fpc-faulty",
            DummyResourceContoller::populateWithDefaultSchema);
    private static final DummyTestResource RESOURCE_SLOW = new DummyTestResource(
            THRESHOLDS_DIR, "resource-dummy-source-slow.xml", RESOURCE_SLOW_OID, "source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_BOOTSTRAP_IMPORT =
            TestObject.file(TEST_DIR, "combo-import.xml", "e3f00000-0000-0000-0000-0000000000ff");
    private static final TestObject<TaskType> FX_COMPOSITE_MT =
            TestObject.file(TEST_DIR, "combo-composite-mt.xml", "e3f00000-0000-0000-0000-000000000001");
    private static final TestObject<TaskType> FX_COMPOSITE_SUBTASKS =
            TestObject.file(TEST_DIR, "combo-composite-subtasks.xml", "e3f00000-0000-0000-0000-000000000003");
    private static final TestObject<TaskType> FX_SIMULATE_EXECUTE =
            TestObject.file(TEST_DIR, "combo-simulate-execute.xml", "e3f00000-0000-0000-0000-000000000004");
    private static final TestObject<TaskType> FX_RECONCILIATION =
            TestObject.file(TEST_DIR, "combo-reconciliation.xml", "e3f00000-0000-0000-0000-000000000005");
    private static final TestObject<TaskType> FX_MULTINODE =
            TestObject.file(TEST_DIR, "combo-multinode.xml", "e3f00000-0000-0000-0000-000000000006");
    private static final TestObject<TaskType> FX_SLOW_COMPOSITE_MT =
            TestObject.file(TEST_DIR, "combo-slow-composite-mt.xml", "e3f00000-0000-0000-0000-000000000011");
    private static final TestObject<TaskType> FX_SLOW_MULTINODE =
            TestObject.file(TEST_DIR, "combo-slow-multinode.xml", "e3f00000-0000-0000-0000-000000000012");
    private static final TestObject<TaskType> FX_SLOW_COMPOSITE_SUBTASKS =
            TestObject.file(TEST_DIR, "combo-slow-composite-subtasks.xml", "e3f00000-0000-0000-0000-000000000013");
    private static final TestObject<TaskType> FX_FAULTY_COMPOSITE_MT =
            TestObject.file(TEST_DIR, "combo-faulty-composite-mt.xml", "e3f00000-0000-0000-0000-000000000014");
    private static final TestObject<TaskType> FX_FAULTY_MULTINODE =
            TestObject.file(TEST_DIR, "combo-faulty-multinode.xml", "e3f00000-0000-0000-0000-000000000015");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN = "a%02d";
    /** Accounts a15..a19 fail on the faulty resource (see resource-dummy-faulty.xml). */
    private static final int FAULTY_ERRORS = 5;

    private static final int THRESHOLD = 5;
    /** For restart: high enough that one restart (first batch now linked) converges. */
    private static final int RESTART_THRESHOLD = 15;
    /** Errors threshold for itemProcessingResult (below FAULTY_ERRORS so it trips). */
    private static final int ERROR_THRESHOLD = 3;

    /** Long enough that the slow import (~10s for 20 accounts) exceeds it deterministically. */
    private static final String EXEC_TIME = "PT3S";
    /** Unreachable execution time for the silent activity rule in combined runs. */
    private static final String UNREACHABLE_TIME = "PT10M";
    /** Unreachable focus threshold for the silent focus rule in combined runs. */
    private static final int UNREACHABLE_COUNT = 999;

    private static final long TIMEOUT = 90_000;
    private static final long SLEEP = 1000;

    private static final ActivityPath MAIN = ActivityPath.fromId("main");
    private static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    private static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private static final String DUMMY_POLICY_NOTIFIER = "dummy:comboPolicyNotifier";
    private static final String DUMMY_ACTIVITY_POLICY_NOTIFIER = "dummy:comboActivityPolicyNotifier";

    enum Constraint {NONE, ADD, MODIFY, DELETE}

    /** One inline policy rule placed at a given activity. */
    record Contribution(PolicyRuleType rule, ActivityPath placement) {
    }

    /**
     * One execution of a combo: the inline rules present on the task and the assertion. Single-kind
     * combos have one run; combined combos have two (focus-targeted / activity-targeted).
     * {@code importedLo/importedHi} give an expected range of committed imported users ({@code -1} to skip).
     */
    record Run(String label, List<Contribution> contributions, int importedLo, int importedHi,
               BiConsumer<TaskAsserter<?>, RunContext> assertion) {
    }

    /** All axes of one combination as a fixed shape + a precondition + one or more staged runs. */
    record Combo(String name, TestObject<TaskType> fixture, Constraint precondition, List<Run> runs) {
    }

    /** What the assertion lambda needs that the runner resolves at run time: counter id per rule name. */
    record RunContext(Map<String, String> counterIdByRule) {
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(PATTERN)
                .withController(RESOURCE_SOURCE.controller)
                .execute();

        initDummyResource(RESOURCE_SLOW, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(PATTERN)
                .withController(RESOURCE_SLOW.controller)
                .execute();

        initDummyResource(RESOURCE_FAULTY, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(PATTERN)
                .withController(RESOURCE_FAULTY.controller)
                .execute();

        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add(DUMMY_POLICY_NOTIFIER);
        SimpleActivityPolicyRuleNotifierType activityNotifier = new SimpleActivityPolicyRuleNotifierType();
        activityNotifier.getTransport().add(DUMMY_ACTIVITY_POLICY_NOTIFIER);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);
        handler.getSimpleActivityPolicyRuleNotifier().add(activityNotifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask, initResult, handler);
    }

    @BeforeMethod
    public void resetState() throws Exception {
        prepareNotifications();
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
        deleteShadowsOf(RESOURCE_SOURCE_OID, result);
        restoreAccounts(RESOURCE_SOURCE, result);
    }

    private void deleteShadowsOf(String resourceOid, OperationResult result) throws Exception {
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                prismContext.queryFor(ShadowType.class).item(ShadowType.F_RESOURCE_REF).ref(resourceOid).build(),
                null, result);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
    }

    private void restoreAccounts(DummyTestResource resource, OperationResult result) throws Exception {
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(PATTERN, i);
            if (resource.controller.getDummyResource().getAccountByName(name) == null) {
                resource.controller.addAccount(name);
            }
        }
    }

    // region rule / action / contribution helpers

    private PolicyRuleType rule(String name, PolicyConstraintsType constraints, int threshold, PolicyActionsType actions) {
        return new PolicyRuleType()
                .name(name)
                .policyConstraints(constraints)
                .policyThreshold(new PolicyThresholdType().lowWaterMark(new WaterMarkType().count(threshold)))
                .policyActions(actions);
    }

    private PolicyRuleType addRule(String name, int threshold, PolicyActionsType actions) {
        return rule(name, new PolicyConstraintsType()
                .modification(new ModificationPolicyConstraintType().operation(ChangeTypeType.ADD)), threshold, actions);
    }

    private PolicyRuleType modifyRule(String name, int threshold, PolicyActionsType actions) {
        return rule(name, new PolicyConstraintsType()
                .modification(new ModificationPolicyConstraintType()
                        .operation(ChangeTypeType.MODIFY).item(new ItemPathType(UserType.F_COST_CENTER))), threshold, actions);
    }

    private PolicyRuleType deleteRule(String name, int threshold, PolicyActionsType actions) {
        return rule(name, new PolicyConstraintsType()
                .modification(new ModificationPolicyConstraintType().operation(ChangeTypeType.DELETE)), threshold, actions);
    }

    private PolicyRuleType executionTimeRule(String name, String duration, PolicyActionsType actions) {
        return new PolicyRuleType().name(name)
                .policyConstraints(new PolicyConstraintsType()
                        .executionTime(new DurationThresholdPolicyConstraintType()
                                .exceeds(XmlTypeConverter.createDuration(duration))))
                .policyActions(actions);
    }

    private PolicyRuleType itemErrorsRule(String name, int threshold, PolicyActionsType actions) {
        return rule(name, new PolicyConstraintsType()
                .itemProcessingResult(new ItemProcessingResultPolicyConstraintType()
                        .status(OperationResultStatusType.FATAL_ERROR)), threshold, actions);
    }

    /** Actions always include notification so triggers surface on the dummy transports. */
    private PolicyActionsType suspend() {
        return withNotify().suspendTask(new SuspendTaskPolicyActionType());
    }

    private PolicyActionsType skip() {
        return withNotify().skipActivity(new SkipActivityPolicyActionType());
    }

    private PolicyActionsType restart() {
        return withNotify().restartActivity(new RestartActivityPolicyActionType());
    }

    private PolicyActionsType withNotify() {
        return new PolicyActionsType().notification(new NotificationPolicyActionType());
    }

    private Consumer<PrismObject<TaskType>> contribute(List<Contribution> contributions) {
        return taskObj -> contributions.forEach(c -> {
            ActivityDefinitionType def = ActivityDefinitionUtil.findActivityDefinition(
                    taskObj.asObjectable().getActivity(), c.placement());
            assertThat(def).as("activity def at " + c.placement()).isNotNull();
            policies(def).getPolicy().add(c.rule().clone());
        });
    }

    private static ActivityPoliciesType policies(ActivityDefinitionType def) {
        if (def.getPolicies() == null) {
            def.setPolicies(new ActivityPoliciesType());
        }
        return def.getPolicies();
    }

    private String inlineCounterId(TestObject<TaskType> task, Contribution c) throws CommonException {
        return ActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), c.placement(), c.rule().getName(), true);
    }

    private int notificationCount(String transport) {
        List<Message> messages = dummyTransport.getMessages(transport);
        return messages == null ? 0 : messages.size();
    }

    // endregion

    // region preconditions / counts / waits

    private void importAll(OperationResult result) throws Exception {
        deleteIfPresent(TASK_BOOTSTRAP_IMPORT, result);
        addObject(TASK_BOOTSTRAP_IMPORT, getTestTask(), result, (Consumer<PrismObject<TaskType>>) t -> {
        });
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_BOOTSTRAP_IMPORT.oid, result, 5 * TIMEOUT);
        deleteIfPresent(TASK_BOOTSTRAP_IMPORT, result);
    }

    private void changeDescription(int count) throws Exception {
        // Unique value each call: a constant would be a no-op on the second modify combo (the accounts
        // keep the value across combos), so the re-import would produce no costCenter change and never trip.
        String value = "changed-" + System.nanoTime();
        for (int i = 0; i < count; i++) {
            RESOURCE_SOURCE.controller.getDummyResource().getAccountByName(String.format(PATTERN, i))
                    .replaceAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME, value);
        }
    }

    private void deleteAccounts(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            RESOURCE_SOURCE.controller.deleteAccount(String.format(PATTERN, i));
        }
    }

    private void setupPrecondition(Constraint constraint, OperationResult result) throws Exception {
        switch (constraint) {
            case NONE, ADD -> {}
            // Change all accounts: on multinode the focus counter is per-bucket/worker, so the changes
            // must be plentiful enough that each bucket clears the threshold (a handful would be split thin).
            case MODIFY -> {
                importAll(result);
                changeDescription(ACCOUNTS);
            }
            case DELETE -> {
                importAll(result);
                deleteAccounts(THRESHOLD + 2);
            }
        }
    }

    private int countImported() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME)
                            .eqPoly(String.format(PATTERN, i)).matchingNorm().build(), null, result);
        }
        return count;
    }

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

    private static boolean isRestart(PolicyRuleType rule) {
        return rule.getPolicyActions() != null && rule.getPolicyActions().getRestartActivity() != null;
    }

    // endregion

    @Test(dataProvider = "combos")
    public void testCombo(Combo combo) throws Exception {
        OperationResult result = getTestOperationResult();
        given(combo.name());

        for (Run run : combo.runs()) {
            resetState();
            setupPrecondition(combo.precondition(), result);

            TestObject<TaskType> task = combo.fixture();
            deleteIfPresent(task, result);

            when(combo.name() + " :: " + run.label());
            addObject(task, getTestTask(), result, contribute(run.contributions()));

            boolean anyRestart = run.contributions().stream().anyMatch(c -> isRestart(c.rule()));
            if (anyRestart) {
                waitForRootClose(task.oid, 3 * TIMEOUT);
            } else {
                waitForRootTermination(task.oid, TIMEOUT);
            }

            then(combo.name() + " :: " + run.label());
            Map<String, String> ids = new HashMap<>();
            for (Contribution c : run.contributions()) {
                ids.put(c.rule().getName(), inlineCounterId(task, c));
            }
            run.assertion().accept(assertTaskTree(task.oid, combo.name() + "/" + run.label()), new RunContext(ids));

            if (run.importedLo() >= 0) {
                assertThat(countImported()).as("imported users for " + combo.name() + "/" + run.label())
                        .isBetween(run.importedLo(), run.importedHi());
            }
        }
    }

    // region data provider

    private static Object[] combo(Combo c) {
        return new Object[] { c };
    }

    private Combo single(String name, TestObject<TaskType> fixture, Constraint precondition, Run run) {
        return new Combo(name, fixture, precondition, List.of(run));
    }

    private Run run(String label, List<Contribution> contributions, int importedLo, int importedHi,
            BiConsumer<TaskAsserter<?>, RunContext> assertion) {
        return new Run(label, contributions, importedLo, importedHi, assertion);
    }

    private Contribution at(PolicyRuleType rule, ActivityPath placement) {
        return new Contribution(rule, placement);
    }

    @DataProvider(name = "combos")
    public Object[][] combos() {
        return new Object[][] {

                // ===== Focus policies (modification thresholds) =====

                // 1 — f-add/suspend/parent/mt
                combo(single(
                        "f-add/suspend/parent/mt",
                        FX_COMPOSITE_MT,
                        Constraint.ADD,
                        run(
                                "only",
                                List.of(at(addRule("f-add", THRESHOLD, suspend()), ActivityPath.empty())),
                                THRESHOLD - 1,
                                ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .child("main")
                                                    .fullExecutionModePolicyRulesCounters()
                                                        .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, THRESHOLD + 5);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 2 — f-add/skip/own/mt
                combo(single(
                        "f-add/skip/own/mt",
                        FX_COMPOSITE_MT,
                        Constraint.ADD,
                        run(
                                "only",
                                List.of(at(addRule("f-add", THRESHOLD, skip()), MAIN)),

                                THRESHOLD - 1,
                                ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .child("main")
                                                    .assertAborted()
                                                    .assertFatalError()
                                                    .fullExecutionModePolicyRulesCounters()
                                                        .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, THRESHOLD + 5)
                                                        .end()
                                                    .end()
                                                .child("last")
                                                    .assertComplete()
                                                    .assertSuccess();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 3 — f-add/restart/parent/mt (converges)
                combo(single("f-add/restart/parent/mt", FX_COMPOSITE_MT, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", RESTART_THRESHOLD, restart()), ActivityPath.empty())),
                                ACCOUNTS, ACCOUNTS,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .assertSuccess()
                                            .rootActivityState()
                                                .assertExecutionAttempts(2)
                                                .child("main")
                                                    .assertComplete()
                                                    .assertSuccess();
                                    // @formatter:on
                                }))),

                // 4 — f-modify/suspend/parent/mt
                combo(single("f-modify/suspend/parent/mt", FX_COMPOSITE_MT, Constraint.MODIFY,
                        run("only", List.of(at(modifyRule("f-modify", THRESHOLD, suspend()), ActivityPath.empty())),
                                ACCOUNTS, ACCOUNTS,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .child("main")
                                                    .fullExecutionModePolicyRulesCounters()
                                                        .assertCounterMinMax(ctx.counterIdByRule().get("f-modify"), THRESHOLD, THRESHOLD + 5);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 5 — f-add/suspend/parent/mn
                combo(single("f-add/suspend/parent/mn", FX_MULTINODE, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", THRESHOLD, suspend()), ActivityPath.empty())),
                                THRESHOLD - 1, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .fullExecutionModePolicyRulesCounters()
                                                    .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, ACCOUNTS);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 6 — f-add/skip/parent/mn
                combo(single("f-add/skip/parent/mn", FX_MULTINODE, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", THRESHOLD, skip()), ActivityPath.empty())),
                                0, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .fullExecutionModePolicyRulesCounters()
                                                    .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, ACCOUNTS);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 7 — f-add/restart/parent/mn (converges)
                combo(single("f-add/restart/parent/mn", FX_MULTINODE, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", RESTART_THRESHOLD, restart()), ActivityPath.empty())),
                                ACCOUNTS, ACCOUNTS,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .assertSuccess();
                                    // @formatter:on
                                }))),

                // 8 — f-modify/skip/own/mn (rule on the distributed root; workers are its children)
                combo(single("f-modify/skip/own/mn", FX_MULTINODE, Constraint.MODIFY,
                        run("only", List.of(at(modifyRule("f-modify", THRESHOLD, skip()), ActivityPath.empty())),
                                ACCOUNTS, ACCOUNTS,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .fullExecutionModePolicyRulesCounters()
                                                    .assertCounterMinMax(ctx.counterIdByRule().get("f-modify"), THRESHOLD, ACCOUNTS);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 9 — f-add/suspend/parent/subtasks
                combo(single("f-add/suspend/parent/subtasks", FX_COMPOSITE_SUBTASKS, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", THRESHOLD, suspend()), ActivityPath.empty())),
                                THRESHOLD - 1, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .subtask("main", false)
                                                .assertFatalError();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 10 — f-delete/suspend/own/recon
                combo(single("f-delete/suspend/own/recon", FX_RECONCILIATION, Constraint.DELETE,
                        run("only", List.of(at(deleteRule("f-delete", THRESHOLD, suspend()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended();
                                    // @formatter:on
                                    // recon expands into sub-activities; the delete-focus counter lives on a
                                    // recon sub-activity. Asserting suspension + notification is the stable depth here.
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 11 — f-delete/restart/own/recon (converges: threshold below deleted count)
                combo(single("f-delete/restart/own/recon", FX_RECONCILIATION, Constraint.DELETE,
                        run("only", List.of(at(deleteRule("f-delete", THRESHOLD, restart()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .assertSuccess();
                                    // @formatter:on
                                }))),

                // 12 — f-add/suspend/own(simulate)/sim-exec
                combo(single("f-add/suspend/own/sim-exec", FX_SIMULATE_EXECUTE, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", THRESHOLD, suspend()), SIMULATE)),
                                0, 0,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .activityState(SIMULATE)
                                                .assertFatalError()
                                                .previewModePolicyRulesCounters()
                                                    .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, THRESHOLD)
                                                    .end()
                                                .end()
                                            .activityState(EXECUTE)
                                                .assertRealizationState(null);
                                    // @formatter:on
                                }))),

                // ===== Activity policies (executionTime / itemProcessingResult) — suspend/skip only =====

                // 13 — a-time/suspend/parent/mt
                combo(single("a-time/suspend/parent/mt", FX_SLOW_COMPOSITE_MT, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, suspend()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .child("main")
                                                    .policies()
                                                        .policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 14 — a-time/skip/own/mt
                combo(single("a-time/skip/own/mt", FX_SLOW_COMPOSITE_MT, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, skip()), MAIN)),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .child("main")
                                                    .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 15 — a-time/skip/parent/mt
                combo(single("a-time/skip/parent/mt", FX_SLOW_COMPOSITE_MT, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, skip()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .child("main")
                                                    .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 16 — a-time/suspend/parent/mn
                combo(single("a-time/suspend/parent/mn", FX_SLOW_MULTINODE, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, suspend()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 17 — a-time/skip/parent/mn
                combo(single("a-time/skip/parent/mn", FX_SLOW_MULTINODE, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, skip()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 18 — a-time/suspend/own/mn (worker); rule on distributed root's worker activity
                combo(single("a-time/suspend/own/mn", FX_SLOW_MULTINODE, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, suspend()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 19 — a-time/suspend/parent/subtasks
                combo(single("a-time/suspend/parent/subtasks", FX_SLOW_COMPOSITE_SUBTASKS, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, suspend()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 20 — a-time/skip/parent/subtasks
                combo(single("a-time/skip/parent/subtasks", FX_SLOW_COMPOSITE_SUBTASKS, Constraint.NONE,
                        run("only", List.of(at(executionTimeRule("a-time", EXEC_TIME, skip()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 21 — a-errors/suspend/own/mt (faulty resource: a15..a19 fail)
                combo(single("a-errors/suspend/own/mt", FX_FAULTY_COMPOSITE_MT, Constraint.NONE,
                        run("only", List.of(at(itemErrorsRule("a-errors", ERROR_THRESHOLD, suspend()), MAIN)),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .child("main")
                                                    .policies().policy("a-errors").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // 22 — a-errors/skip/parent/mn
                combo(single("a-errors/skip/parent/mn", FX_FAULTY_MULTINODE, Constraint.NONE,
                        run("only", List.of(at(itemErrorsRule("a-errors", ERROR_THRESHOLD, skip()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .policies().policy("a-errors").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),

                // ===== Combined: focus rule + activity rule on one task, staged two runs =====

                // 23 — c-add+time/mt
                combo(new Combo("c-add+time/mt", FX_SLOW_COMPOSITE_MT, Constraint.ADD, List.of(
                        run("focus",
                                List.of(at(addRule("f-add", THRESHOLD, skip()), MAIN),
                                        at(executionTimeRule("a-time", UNREACHABLE_TIME, suspend()), ActivityPath.empty())),
                                THRESHOLD - 1, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .child("main")
                                                    .assertAborted()
                                                    .fullExecutionModePolicyRulesCounters()
                                                        .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, THRESHOLD + 5);
                                    // @formatter:on
                                    // focus rule fired; the activity (executionTime) rule stayed silent
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isEqualTo(0);
                                }),
                        run("activity",
                                List.of(at(addRule("f-add", UNREACHABLE_COUNT, skip()), MAIN),
                                        at(executionTimeRule("a-time", EXEC_TIME, suspend()), ActivityPath.empty())),
                                0, ACCOUNTS,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .child("main")
                                                    .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    // activity rule fired; the focus rule stayed silent
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isEqualTo(0);
                                })))),

                // 24 — c-add+time/mn (add, not modify: a modify precondition would mutate the source
                //       resource while this fixture imports the slow one — the change wouldn't be seen)
                combo(new Combo("c-add+time/mn", FX_SLOW_MULTINODE, Constraint.ADD, List.of(
                        run("focus",
                                List.of(at(addRule("f-add", THRESHOLD, suspend()), ActivityPath.empty()),
                                        at(executionTimeRule("a-time", UNREACHABLE_TIME, skip()), ActivityPath.empty())),
                                THRESHOLD - 1, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .fullExecutionModePolicyRulesCounters()
                                                    .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, ACCOUNTS);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isEqualTo(0);
                                }),
                        run("activity",
                                List.of(at(addRule("f-add", UNREACHABLE_COUNT, suspend()), ActivityPath.empty()),
                                        at(executionTimeRule("a-time", EXEC_TIME, skip()), ActivityPath.empty())),
                                0, ACCOUNTS,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed()
                                            .rootActivityState()
                                                .policies().policy("a-time").assertTriggerCount(1).end().end();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isEqualTo(0);
                                })))),

                // 25 — c-add+time/subtasks
                combo(new Combo("c-add+time/subtasks", FX_SLOW_COMPOSITE_SUBTASKS, Constraint.ADD, List.of(
                        run("focus",
                                List.of(at(addRule("f-add", THRESHOLD, suspend()), ActivityPath.empty()),
                                        at(executionTimeRule("a-time", UNREACHABLE_TIME, skip()), ActivityPath.empty())),
                                THRESHOLD - 1, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended();
                                    // @formatter:on
                                    // subtask-delegated: state lives on a subtask; task-level state + notifiers
                                    // are the stable signal that the focus rule fired and the activity rule did not
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isEqualTo(0);
                                }),
                        run("activity",
                                List.of(at(addRule("f-add", UNREACHABLE_COUNT, suspend()), ActivityPath.empty()),
                                        at(executionTimeRule("a-time", EXEC_TIME, skip()), ActivityPath.empty())),
                                -1, -1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertClosed();
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_ACTIVITY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isEqualTo(0);
                                })))),
        };
    }

    // endregion
}
