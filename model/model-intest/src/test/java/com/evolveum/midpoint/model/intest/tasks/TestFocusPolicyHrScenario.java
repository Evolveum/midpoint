/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.datatype.DatatypeFactory;

import com.evolveum.midpoint.test.TestActivityPolicyUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * "HR source" reconciliation scenario. The source resource deletes the focus (user) when its account is
 * removed. A composite task reconciles it in two phases:
 *
 * - child "simulate" — reconciliation in preview mode, guarded by a max-deleted-users suspend policy that
 * stops the task before anything is really deleted;
 * - child "execute" — reconciliation in full mode, guarded by the same policy so it can never delete more
 * than allowed.
 *
 * Recovery paths are contrasted. Once the source is fixed so fewer users would be deleted: a plain resume
 * re-suspends at run start (the delete counter is not cleared), and only a fresh run completes; whereas a
 * {@code restartActivity} action clears the counter each cycle, so the task recovers on its own. Separately,
 * with the source left broken, clearing the activity policy states through the model interaction service resets
 * the preview delete counter, so a resume lets the same suspended task keep processing before it trips again.
 *
 * A variant of the task with the "execute" child not guarded by any policy covers the customer scenario:
 * once "simulate" is halted by the policy, resuming the task must not let "execute" start.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TestFocusPolicyHrScenario extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/hr-scenario");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final DummyTestResource RESOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", "c1a70000-0000-0000-0000-000000000001", "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestTask TASK_HR =
            TestTask.file(TEST_DIR, "hr-reconciliation.xml", "e4f00000-0000-0000-0000-000000000001");
    private static final TestTask TASK_HR_UNGUARDED_EXECUTE =
            TestTask.file(TEST_DIR, "hr-reconciliation-unguarded-execute.xml", "e4f00000-0000-0000-0000-000000000003");
    private static final TestTask TASK_HR_IMPORT =
            TestTask.file(TEST_DIR, "hr-import.xml", "e4f00000-0000-0000-0000-0000000000ff");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN = "a%02d";
    protected static final int DELETE_THRESHOLD = 5;

    private static final long TIMEOUT = 90_000;

    protected static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    protected static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private static final String DUMMY_POLICY_NOTIFIER = "dummy:policyNotifier";

    /**
     * Topology customizer applied to every run of the HR task: worker threads and/or subtask distribution.
     * The scenario and assertions are identical across flavors; only this changes.
     */
    protected abstract Consumer<PrismObject<TaskType>> topology();

    /** How many threads are there. This is the maximum tolerance for the policy rule trigger counter value. */
    protected abstract int threads();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initDummyResource(RESOURCE, initTask, initResult);
        createAccounts();
        TASK_HR_IMPORT.init(this, initTask, initResult);

        // clockwork (focus) policy-rule notifier, redirected to a dummy transport
        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add(DUMMY_POLICY_NOTIFIER);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);
        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask, initResult, handler);
    }

    private void createAccounts() throws Exception {
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(PATTERN)
                .withController(RESOURCE.controller)
                .execute();
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
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                prismContext.queryFor(ShadowType.class).item(ShadowType.F_RESOURCE_REF).ref(RESOURCE.oid).build(),
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

    // region helpers

    /** Imports all accounts (plain import, no policy), linking users to shadows — the state the scenario prunes. */
    private void importAllAndLink(OperationResult result) throws Exception {
        TASK_HR_IMPORT.rerun(result);
    }

    private void deleteAccounts(int from, int to) throws Exception {
        for (int i = from; i < to; i++) {
            RESOURCE.controller.deleteAccount(String.format(PATTERN, i));
        }
    }

    private void addAccounts(int from, int to) throws Exception {
        for (int i = from; i < to; i++) {
            RESOURCE.controller.addAccount(String.format(PATTERN, i));
        }
    }

    /**
     * Drops the (now stale) shadows of the given accounts so a following reconciliation re-correlates the
     * restored accounts to their users by name instead of treating them as removed.
     */
    private void dropShadows(int from, int to) throws Exception {
        OperationResult result = getTestOperationResult();
        for (int i = from; i < to; i++) {
            String name = String.format(PATTERN, i);
            List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                    prismContext.queryFor(ShadowType.class)
                            .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE.oid)
                            .and().item(ShadowType.F_NAME).eqPoly(name).matchingNorm()
                            .build(), null, result);
            for (PrismObject<ShadowType> shadow : shadows) {
                repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
            }
        }
    }

    private int countUsers() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME)
                            .eqPoly(String.format(PATTERN, i)).matchingNorm().build(), null, result);
        }
        return count;
    }

    /** Replaces the simulate activity's max-deleted policy action with notification + a delayed restartActivity. */
    private Consumer<PrismObject<TaskType>> setSimulateDeleteRestart(String delay) {
        return taskObj -> {
            ActivityDefinitionType simulate = ActivityDefinitionUtil.findActivityDefinition(
                    taskObj.asObjectable().getActivity(), SIMULATE);
            assertThat(simulate).as("simulate activity").isNotNull();
            simulate.getPolicies().getPolicy().stream()
                    .filter(p -> "max-deleted".equals(p.getName()))
                    .findFirst().orElseThrow()
                    .setPolicyActions(new PolicyActionsType()
                            .notification(new NotificationPolicyActionType())
                            .restartActivity(new RestartActivityPolicyActionType()
                                    .delay(DatatypeFactory.newDefaultInstance().newDuration(delay))));
        };
    }

    private int policyNotifications() {
        List<Message> messages = dummyTransport.getMessages(DUMMY_POLICY_NOTIFIER);
        return messages == null ? 0 : messages.size();
    }

    /**
     * Best-effort wait until the restart cycle has begun: the activity was restarted at least once (its
     * execution attempt increased) or the task is momentarily suspended between restarts. Returns as soon
     * as either is observed; never throws (the caller then fixes the data and waits for convergence).
     */
    private void waitForRestartCycle(String oid, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Task t = taskManager.getTaskWithResult(oid, getTestOperationResult());
            if (t.isSuspended() || simulateExecutionAttempt(oid) >= 2) {
                return;
            }
            Thread.sleep(200);
        }
    }

    private int simulateExecutionAttempt(String oid) throws Exception {
        TaskType task = getTask(oid).asObjectable();
        if (task.getActivityState() == null || task.getActivityState().getActivity() == null) {
            return 0;
        }
        return task.getActivityState().getActivity().getActivity().stream()
                .filter(a -> "simulate".equals(a.getIdentifier()))
                .map(ActivityStateType::getExecutionAttempt)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(0);
    }

    /**
     * Asserts the initial suspension:
     *
     * * the simulate (preview) reconciliation tripped the max-deleted threshold and suspended,
     * * execute never started,
     * * and the (preview) counter is at the given value; plus threads-based tolerance
     * ** i.e. firing in at least in one thread (counted already in "min" value), at most in all threads
     *
     * Overridden by the subtask-distributed flavor, where the simulate activity runs in its own subtask.
     *
     * @param min Expected minimal value of the counter
     * @return the value of the preview delete counter at the time of suspension
     */
    protected int assertSimulateSuspended(String taskOid, String counterId, int min) throws Exception {
        // @formatter:off
        return assertTaskTree(taskOid, "after run")
                .display()
                .assertSuspended()
                .activityState(EXECUTE)
                    .assertRealizationState(null)
                .end()
                .activityState(SIMULATE)
                    .assertFatalError()
                    .previewModePolicyRulesCounters()
                        .assertCounterMinMax(counterId, min, min + threads() - 1)
                        .getCounterValue(counterId);
        // @formatter:on
    }

    /**
     * Asserts a clean completion of the whole task tree: it closed successfully, both the simulate (preview)
     * and execute (full) reconciliations succeeded, their delete counters stayed below the threshold, and no
     * policy tripped. Overridden by the subtask-distributed flavor, where the activities run in their own
     * subtasks.
     */
    protected void assertRunSucceeded(String taskOid) throws Exception {
        String simulateId = TestActivityPolicyUtils.buildPolicyIdentifier(getTask(taskOid), SIMULATE, "max-deleted", true);
        String executeId = TestActivityPolicyUtils.buildPolicyIdentifier(getTask(taskOid), EXECUTE, "max-deleted-execute", true);
        // @formatter:off
        assertTaskTree(taskOid, "after successful run")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState(SIMULATE)
                .assertSuccess()
                .assertNoPolicies()
                .previewModePolicyRulesCounters()
                .assertCounterMinMax(simulateId, 1, DELETE_THRESHOLD - 1)
                .end()
                .end()
                .activityState(EXECUTE)
                .assertSuccess()
                .assertNoPolicies()
                .fullExecutionModePolicyRulesCounters()
                .assertCounterMinMax(executeId, 1, DELETE_THRESHOLD - 1);
        // @formatter:on
    }

    /**
     * Cumulative number of objects processed by the reconciliation, summed over the root task and all of its
     * subtasks so it aggregates worker threads and subtask-distributed activities alike (a delegated activity
     * reports its statistics in its own subtask, not in the parent, so nothing is double-counted). Only the
     * simulate activity ever runs in this test (execute never starts), so the whole-tree total equals what
     * simulate has processed. Item-processing statistics accumulate across the activity's runs, so this only
     * grows while the activity keeps doing real work.
     */
    protected int simulateItemsProcessed(String rootOid) throws Exception {
        OperationResult result = getTestOperationResult();
        Task root = taskManager.getTaskTree(rootOid, result);
        int total = itemsProcessed(root);
        for (Task sub : root.listSubtasksDeeply(true, result)) {
            total += itemsProcessed(sub);
        }
        return total;
    }

    private static int itemsProcessed(Task task) {
        return ActivityItemProcessingStatisticsUtil.getItemsProcessed(
                ActivityItemProcessingStatisticsUtil.getSummarizedStatistics(task.getActivitiesStateOrClone()));
    }

    /**
     * Asserts that clearing the activity policy states wiped the simulate preview delete counter (the
     * {@code max-deleted} suspend policy is counter-based and stores no separate policy-state object). Walks the
     * root task and its subtasks so it works for the distributed flavor, where the counter lives in a subtask.
     */
    protected void assertSimulateStatesCleared(String rootOid, String counterId) throws Exception {
        OperationResult result = getTestOperationResult();
        Task root = taskManager.getTaskTree(rootOid, result);
        List<Task> tasks = new ArrayList<>();
        tasks.add(root);
        tasks.addAll(root.listSubtasksDeeply(true, result));
        for (Task task : tasks) {
            ActivityStateType simulate = findActivityState(task.getActivitiesStateOrClone(), "simulate");
            if (simulate == null) {
                continue;
            }
            ActivityPoliciesStateType policies = simulate.getPolicies();
            assertThat(policies == null || policies.getPolicy().isEmpty())
                    .as("simulate policy states cleared in " + task.getName()).isTrue();
            ActivityCounterGroupsType counters = simulate.getCounters();
            if (counters != null && counters.getPreviewModePolicyRules() != null) {
                boolean present = counters.getPreviewModePolicyRules().getCounter().stream()
                        .anyMatch(c -> counterId.equals(c.getIdentifier()));
                assertThat(present).as("simulate preview counter cleared in " + task.getName()).isFalse();
            }
        }
    }

    private static ActivityStateType findActivityState(TaskActivityStateType taskState, String identifier) {
        return taskState == null ? null : findActivityState(taskState.getActivity(), identifier);
    }

    private static ActivityStateType findActivityState(ActivityStateType state, String identifier) {
        if (state == null) {
            return null;
        }
        if (identifier.equals(state.getIdentifier())) {
            return state;
        }
        for (ActivityStateType child : state.getActivity()) {
            ActivityStateType found = findActivityState(child, identifier);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** All tasks of the tree: the root and its subtasks (where the distributed flavor keeps the activity states). */
    private List<Task> tasksOfTree(String rootOid) throws Exception {
        OperationResult result = getTestOperationResult();
        Task root = taskManager.getTaskTree(rootOid, result);
        List<Task> tasks = new ArrayList<>();
        tasks.add(root);
        tasks.addAll(root.listSubtasksDeeply(true, result));
        return tasks;
    }

    /** The most recent run start in the task tree; it moves only if some task of the tree really ran again. */
    private long latestRunStart(String rootOid) throws Exception {
        long latest = 0;
        for (Task task : tasksOfTree(rootOid)) {
            Long start = task.getLastRunStartTimestamp();
            if (start != null) {
                latest = Math.max(latest, start);
            }
        }
        return latest;
    }

    /** Halts recorded by policy actions anywhere in the task tree. */
    private List<ActivityHaltingInformationType> haltingInformation(String rootOid) throws Exception {
        List<ActivityHaltingInformationType> found = new ArrayList<>();
        for (Task task : tasksOfTree(rootOid)) {
            TaskActivityStateType taskState = task.getActivitiesStateOrClone();
            if (taskState != null) {
                collectHaltingInformation(taskState.getActivity(), found);
            }
        }
        return found;
    }

    private static void collectHaltingInformation(ActivityStateType state, List<ActivityHaltingInformationType> found) {
        if (state == null) {
            return;
        }
        if (state.getHaltingInformation() != null) {
            found.add(state.getHaltingInformation());
        }
        for (ActivityStateType child : state.getActivity()) {
            collectHaltingInformation(child, found);
        }
    }

    /** The simulate activity is halted by the max-deleted policy, the halt is recorded, and nobody was deleted. */
    private int assertSimulateHalted(String counterId, int min) throws Exception {
        int counter = assertSimulateSuspended(TASK_HR_UNGUARDED_EXECUTE.oid, counterId, min);
        assertThat(haltingInformation(TASK_HR_UNGUARDED_EXECUTE.oid))
                .as("recorded halts")
                .isNotEmpty()
                .allSatisfy(halt -> {
                    assertThat(halt.getPolicyName()).as("halting policy name").isEqualTo("max-deleted");
                    assertThat(halt.getPolicyIdentifier()).as("halting policy identifier").isEqualTo(counterId);
                });
        assertThat(countUsers()).as("users (nothing really deleted)").isEqualTo(ACCOUNTS);
        return counter;
    }

    /**
     * Removes the given number of accounts, lets the simulate activity of the "unguarded execute" task trip the policy,
     * and resumes the suspended task. The resumed task must be halted again, without doing any work.
     */
    private void tripSimulateAndResume(int removedAccounts) throws Exception {
        OperationResult result = getTestOperationResult();
        TestTask task = TASK_HR_UNGUARDED_EXECUTE;

        given("all users linked, then " + removedAccounts + " accounts removed on the source");
        importAllAndLink(result);
        assertThat(countUsers()).as("users before").isEqualTo(ACCOUNTS);
        deleteAccounts(0, removedAccounts);

        when("running the HR reconciliation with the unguarded execute activity");
        deleteIfPresent(task, result);
        addObject(task, getTestTask(), result, topology());
        waitForTaskCloseOrSuspend(task.oid, TIMEOUT);

        then("the simulate activity suspends on the delete threshold; nothing is really deleted");
        String id = TestActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), SIMULATE, "max-deleted", true);
        int counterAfterFirst = assertSimulateHalted(id, DELETE_THRESHOLD);
        int processedAtFirstSuspend = simulateItemsProcessed(task.oid);
        long runStartAtFirstSuspend = latestRunStart(task.oid);

        when("the suspended task is resumed");
        taskManager.resumeTaskTree(task.oid, result);
        waitForTaskCloseOrSuspend(task.oid, TIMEOUT);

        then("the tree really ran again, and was halted at the start of simulate with no work done");
        assertThat(latestRunStart(task.oid)).as("latest run start after resume").isGreaterThan(runStartAtFirstSuspend);
        assertSimulateHalted(id, counterAfterFirst);
        assertThat(simulateItemsProcessed(task.oid)).as("items processed after resume").isEqualTo(processedAtFirstSuspend);
    }

    // endregion

    /**
     * suspendTask flow. More accounts are removed on the source than allowed, so the simulate (preview)
     * reconciliation trips the max-deleted threshold and suspends before any real deletion. We then show that:
     *
     * - a plain resume re-suspends (the delete counter is not cleared by suspend/resume), even after the
     * source is fixed, and
     * - only a fresh run (with a clean counter) completes, deleting just the now-allowed number.
     */
    @Test
    public void test100SuspendResumeThenFreshRun() throws Exception {
        OperationResult result = getTestOperationResult();

        given("all users linked, then 8 accounts removed on the source");
        importAllAndLink(result);
        assertThat(countUsers()).as("users before").isEqualTo(ACCOUNTS);
        deleteAccounts(0, DELETE_THRESHOLD + 3);

        when("running the HR reconciliation");
        deleteIfPresent(TASK_HR, result);
        addObject(TASK_HR, getTestTask(), result, topology());
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("the simulate activity suspends on the delete threshold; nothing is really deleted");
        String id = TestActivityPolicyUtils.buildPolicyIdentifier(getTask(TASK_HR.oid), SIMULATE, "max-deleted", true);
        assertSimulateSuspended(TASK_HR.oid, id, DELETE_THRESHOLD);
        assertThat(countUsers()).as("preview did not delete").isEqualTo(ACCOUNTS);
        assertThat(policyNotifications()).as("notification fired before the suspend").isGreaterThanOrEqualTo(1);

        when("the suspended task is resumed with the source still broken");
        taskManager.resumeTaskTree(TASK_HR.oid, result);
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("resume re-suspends — the persisted counter is already at the threshold, so re-processing "
                + "any of the still-broken accounts (fewer than the threshold remain, so a cleared counter "
                + "could not trip) re-fires the policy");
        assertTaskTree(TASK_HR.oid, "after resume").display().assertSuspended();
        assertThat(countUsers()).as("still nothing deleted after resume").isEqualTo(ACCOUNTS);

        when("the source is fixed (4 accounts restored) and the task is run fresh (clean counters)");
        addAccounts(0, 4);   // 4 of the 8 restored -> only 4 accounts still missing
        dropShadows(0, 4);   // drop stale shadows so the restored accounts re-correlate by name
        int notificationsBeforeFreshRun = policyNotifications();
        deleteIfPresent(TASK_HR, result);
        addObject(TASK_HR, getTestTask(), result, topology());
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("the fresh run completes and only the allowed (4 < 5) users are deleted");
        assertRunSucceeded(TASK_HR.oid);
        assertThat(countUsers()).as("users remaining after allowed deletions").isEqualTo(ACCOUNTS - 4);
        assertThat(policyNotifications()).as("clean fresh run fires no new policy notification")
                .isEqualTo(notificationsBeforeFreshRun);
    }

    /**
     * Clear-policy-states recovery via {@link com.evolveum.midpoint.model.api.ModelInteractionService#clearAllActivityPolicyStates}.
     * Same broken source as {@link #test100SuspendResumeThenFreshRun} (8 accounts removed, over the threshold),
     * but the source is never fixed. Instead of a fresh run, the <i>same</i> task is recovered by clearing its
     * activity policy states:
     *
     * - the simulate (preview) reconciliation trips the max-deleted threshold and suspends;
     * - a plain resume re-suspends at run start (the persisted counter is untouched), doing no new work;
     * - clearing the activity policy states removes the simulate trigger and its preview delete counter;
     * - the next resume lets the reconciliation process objects again before it trips once more, so the number
     * of processed objects strictly increases even though the source stays broken.
     */
    @Test
    public void test150ClearPolicyStatesResumesProgress() throws Exception {
        OperationResult result = getTestOperationResult();

        given("all users linked, then most accounts removed on the source (far over the threshold)");
        importAllAndLink(result);
        assertThat(countUsers()).as("users before").isEqualTo(ACCOUNTS);
        deleteAccounts(0, ACCOUNTS - 2);

        when("running the HR reconciliation");
        deleteIfPresent(TASK_HR, result);
        addObject(TASK_HR, getTestTask(), result, topology());
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("the simulate activity suspends on the delete threshold, having processed some objects");
        String id = TestActivityPolicyUtils.buildPolicyIdentifier(getTask(TASK_HR.oid), SIMULATE, "max-deleted", true);
        int counterAfterFirst = assertSimulateSuspended(TASK_HR.oid, id, DELETE_THRESHOLD);
        assertThat(policyNotifications()).as("notification fired before the suspend").isGreaterThanOrEqualTo(1);
        int processedAtFirstSuspend = simulateItemsProcessed(TASK_HR.oid);
        assertThat(processedAtFirstSuspend).as("some objects processed before first trip").isGreaterThan(0);
        int notificationsAtFirstSuspend = policyNotifications();

        when("the suspended task is resumed without clearing the policy state");
        taskManager.resumeTaskTree(TASK_HR.oid, result);
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("it re-suspends in simulate right at run start - the persisted counter is still over the threshold, "
                + "so no new work is done and the counter does not move");
        assertSimulateSuspended(TASK_HR.oid, id, counterAfterFirst);
        int processedAfterPlainResume = simulateItemsProcessed(TASK_HR.oid);
        assertThat(processedAfterPlainResume)
                .as("plain resume re-trips before processing anything")
                .isEqualTo(processedAtFirstSuspend);

        when("the activity policy states (the preview delete counter) are cleared via model interaction service");
        boolean changed = modelInteractionService.clearAllActivityPolicyStates(
                getTask(TASK_HR.oid), getTestTask(), result);

        then("the clear reports a change and the simulate preview counter is gone");
        assertThat(changed).as("clearAllActivityPolicyStates made a change").isTrue();
        assertSimulateStatesCleared(TASK_HR.oid, id);

        when("the task is resumed again with the cleared counter");
        taskManager.resumeTaskTree(TASK_HR.oid, result);
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("the reconciliation processes further objects before tripping again — processed count incremented");
        assertSimulateSuspended(TASK_HR.oid, id, DELETE_THRESHOLD);
        int processedAfterClear = simulateItemsProcessed(TASK_HR.oid);
        assertThat(processedAfterClear)
                .as("clearing the counter let the reconciliation process more objects before re-tripping")
                .isGreaterThan(processedAfterPlainResume);
        assertThat(processedAfterClear - processedAfterPlainResume)
                .as("the cleared resume makes more forward progress than the plain resume did")
                .isGreaterThan(processedAfterPlainResume - processedAtFirstSuspend);
        assertThat(policyNotifications())
                .as("the fresh trip fired at least one more policy notification")
                .isGreaterThan(notificationsAtFirstSuspend);
        assertThat(countUsers()).as("still preview only — no user was really deleted").isEqualTo(ACCOUNTS);
    }

    /**
     * restartActivity flow. Same starting point, but the max-deleted policy uses {@code restartActivity} with
     * a delay. When it trips it schedules a delayed auto-restart that clears the counter. Once the source is
     * fixed during a restart window, the task recovers on its own — no manual resume or fresh run.
     */
    @Test
    public void test200RestartRecoversByItself() throws Exception {
        OperationResult result = getTestOperationResult();

        given("all users linked, then 8 accounts removed on the source");
        importAllAndLink(result);
        deleteAccounts(0, DELETE_THRESHOLD + 3);

        when("running with a restartActivity(delay) action on the max-deleted policy");
        deleteIfPresent(TASK_HR, result);
        addObject(TASK_HR, getTestTask(), result, aggregateCustomizer(topology(), setSimulateDeleteRestart("PT10S")));

        and("once the restart cycle has begun, fixing the source (4 restored)");
        waitForRestartCycle(TASK_HR.oid, TIMEOUT);
        addAccounts(0, 4);
        dropShadows(0, 4);

        then("a following auto-restart re-runs with fixed data and the task completes on its own");
        waitForTaskCloseOrSuspend(TASK_HR.oid, 4 * TIMEOUT);
        assertRunSucceeded(TASK_HR.oid);
        assertThat(countUsers()).as("only the allowed (4 < 5) users deleted").isEqualTo(ACCOUNTS - 4);
        assertThat(policyNotifications()).as("notification fired on each restart trip").isGreaterThanOrEqualTo(1);
    }

    //region Customer scenario: simulate guarded by a suspend policy, execute not guarded at all

    /** More accounts removed than allowed: some of them are left for the resumed simulate activity to count. */
    @Test
    public void test300UnguardedExecuteResumeAboveThreshold() throws Exception {
        tripSimulateAndResume(DELETE_THRESHOLD + 3);
    }

    /**
     * Exactly as many accounts removed as the threshold: nothing countable is left for the resumed simulate activity,
     * so without the recorded halt it would complete, and the unguarded execute activity would delete the users.
     */
    @Test
    public void test310UnguardedExecuteResumeAtThreshold() throws Exception {
        tripSimulateAndResume(DELETE_THRESHOLD);
    }

    /** Clearing the activity policy states acknowledges the halt: the resumed task continues to the execute activity. */
    @Test
    public void test320UnguardedExecuteClearPolicyStatesAcknowledgesHalt() throws Exception {
        OperationResult result = getTestOperationResult();
        TestTask task = TASK_HR_UNGUARDED_EXECUTE;

        given("simulate halted at the threshold, resume halted again");
        tripSimulateAndResume(DELETE_THRESHOLD);

        when("the policy states are cleared and the task is resumed");
        boolean changed = modelInteractionService.clearAllActivityPolicyStates(getTask(task.oid), getTestTask(), result);
        assertThat(changed).as("clearAllActivityPolicyStates made a change").isTrue();
        assertThat(haltingInformation(task.oid)).as("recorded halts after clearing").isEmpty();
        taskManager.resumeTaskTree(task.oid, result);
        waitForTaskCloseOrSuspend(task.oid, TIMEOUT);

        then("the task completes and the execute activity deletes the users");
        assertTaskTree(task.oid, "after clearing and resume")
                .display()
                .assertClosed()
                .assertSuccess();
        assertThat(countUsers()).as("users after the acknowledged halt").isEqualTo(ACCOUNTS - DELETE_THRESHOLD);
    }

    //endregion
}
