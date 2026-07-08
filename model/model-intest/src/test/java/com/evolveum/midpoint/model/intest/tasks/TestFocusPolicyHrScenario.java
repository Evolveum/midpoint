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

import javax.xml.datatype.DatatypeFactory;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
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

/**
 * "HR source" reconciliation scenario. The source resource deletes the focus (user) when its account is
 * removed. A composite task reconciles it in two phases:
 *
 * - child "simulate" — reconciliation in preview mode, guarded by a max-deleted-users suspend policy that
 *   stops the task before anything is really deleted;
 * - child "execute" — reconciliation in full mode, guarded by the same policy so it can never delete more
 *   than allowed.
 *
 * Two recovery paths are contrasted once the source is fixed so fewer users would be deleted:
 * a plain resume re-suspends (the delete counter is not cleared), and only a fresh run completes; whereas a
 * {@code restartActivity} action clears the counter each cycle, so the task recovers on its own.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyHrScenario extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/hr-scenario");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000001";

    private static final DummyTestResource RESOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_HR =
            TestObject.file(TEST_DIR, "hr-reconciliation.xml", "e4f00000-0000-0000-0000-000000000001");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN = "a%02d";
    private static final int DELETE_THRESHOLD = 5;

    private static final long TIMEOUT = 90_000;
    private static final long SLEEP = 1000;

    private static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    private static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private static final String DUMMY_POLICY_NOTIFIER = "dummy:policyNotifier";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initDummyResource(RESOURCE, initTask, initResult);
        createAccounts();

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

    // region helpers

    /** Imports all accounts (no policy), linking users to shadows — the state reconciliation later prunes. */
    private void importAllAndLink(OperationResult result) throws Exception {
        reconcileAllRaw(result);
    }

    /** A plain full reconciliation of the whole resource to create/link all users. */
    private void reconcileAllRaw(OperationResult result) throws Exception {
        TestObject<TaskType> bootstrap = TestObject.file(TEST_DIR, "hr-reconciliation.xml",
                "e4f00000-0000-0000-0000-000000000001");
        // reuse the fixture but strip policies + force full mode so it just links everyone
        deleteIfPresent(bootstrap, result);
        addObject(bootstrap, getTestTask(), result, (Consumer<PrismObject<TaskType>>) taskObj -> {
            for (ActivityDefinitionType child : taskObj.asObjectable().getActivity().getComposition().getActivity()) {
                child.setPolicies(null);
                if (child.getExecution() != null) {
                    child.getExecution().setMode(ExecutionModeType.FULL);
                }
            }
        });
        waitForRootClose(bootstrap.oid, 5 * TIMEOUT);
        deleteIfPresent(bootstrap, result);
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
                            .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_OID)
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

    // endregion

    /**
     * suspendTask flow. More accounts are removed on the source than allowed, so the simulate (preview)
     * reconciliation trips the max-deleted threshold and suspends before any real deletion. We then show that:
     *
     * - a plain resume re-suspends (the delete counter is not cleared by suspend/resume), even after the
     *   source is fixed, and
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
        addObject(TASK_HR, getTestTask(), result, (Consumer<PrismObject<TaskType>>) t -> { });
        waitForRootTermination(TASK_HR.oid, TIMEOUT);

        then("the simulate activity suspends on the delete threshold; nothing is really deleted");
        String id = ActivityPolicyUtils.buildPolicyIdentifier(getTask(TASK_HR.oid), SIMULATE, "max-deleted", true);
        // @formatter:off
        assertTaskTree(TASK_HR.oid, "after run")
                .display()
                .assertSuspended()
                .activityState(SIMULATE)
                    .assertFatalError()
                    .previewModePolicyRulesCounters()
                        .assertCounterMinMax(id, DELETE_THRESHOLD, DELETE_THRESHOLD + 3)
                        .end()
                    .end()
                .activityState(EXECUTE)
                    .assertRealizationState(null);
        // @formatter:on
        assertThat(countUsers()).as("preview did not delete").isEqualTo(ACCOUNTS);
        assertThat(policyNotifications()).as("notification fired before the suspend").isGreaterThanOrEqualTo(1);

        when("the source is fixed (4 accounts restored) and the suspended task is resumed");
        addAccounts(0, 4);   // 4 of the 8 restored -> only 4 accounts still missing
        dropShadows(0, 4);   // drop stale shadows so the restored accounts re-correlate by name
        taskManager.resumeTaskTree(TASK_HR.oid, result);
        waitForRootTermination(TASK_HR.oid, TIMEOUT);

        then("resume re-suspends — the persisted delete counter trips again despite the fix");
        assertTaskTree(TASK_HR.oid, "after resume").display().assertSuspended();
        assertThat(countUsers()).as("still nothing deleted after resume").isEqualTo(ACCOUNTS);

        when("the task is run fresh (clean counters) with the fixed source");
        deleteIfPresent(TASK_HR, result);
        addObject(TASK_HR, getTestTask(), result, (Consumer<PrismObject<TaskType>>) t -> { });
        waitForRootClose(TASK_HR.oid, TIMEOUT);

        then("the fresh run completes and only the allowed (4 < 5) users are deleted");
        // @formatter:off
        assertTaskTree(TASK_HR.oid, "after fresh run")
                .display()
                .assertClosed()
                .assertSuccess();
        // @formatter:on
        assertThat(countUsers()).as("users remaining after allowed deletions").isEqualTo(ACCOUNTS - 4);
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
        addObject(TASK_HR, getTestTask(), result, setSimulateDeleteRestart("PT10S"));

        and("once the restart cycle has begun, fixing the source (4 restored)");
        waitForRestartCycle(TASK_HR.oid, TIMEOUT);
        addAccounts(0, 4);
        dropShadows(0, 4);

        then("a following auto-restart re-runs with fixed data and the task completes on its own");
        waitForRootClose(TASK_HR.oid, 4 * TIMEOUT);
        // @formatter:off
        assertTaskTree(TASK_HR.oid, "after auto-restart")
                .display()
                .assertClosed()
                .assertSuccess();
        // @formatter:on
        assertThat(countUsers()).as("only the allowed (4 < 5) users deleted").isEqualTo(ACCOUNTS - 4);
        assertThat(policyNotifications()).as("notification fired on each restart trip").isGreaterThanOrEqualTo(1);
    }
}
