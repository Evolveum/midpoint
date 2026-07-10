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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Multi-node reconciliation scenario driving {@code custom-policy-reconciliation.xml}: a composite task
 * distributed into subtasks (each reconciliation child runs in its own subtask, with worker threads). The
 * "simulate" (preview) child carries two policies on the deleted-users threshold:
 *
 * - {@code max-deleted} (order 20) — restarts the activity when at least {@link #DELETE_THRESHOLD} users would
 *   be deleted (the counter is cleared on each restart);
 * - {@code max-restarts} (order 10) — suspends the task once the activity has been attempted more than twice
 *   ({@code executionAttempts exceeds 2}), i.e. on the third attempt; the lower order makes suspend win over
 *   restart when both trip on that attempt.
 *
 * Two paths are asserted:
 * - policy triggered: more accounts are removed than allowed, so the activity restarts twice and then suspends
 *   on the third attempt without deleting anything (preview);
 * - policy not triggered: fewer accounts are removed than the threshold, so nothing restarts and the whole tree
 *   completes, the full-mode "execute" child deleting just the now-allowed number.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCustomPolicyReconciliationMultiNode extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/hr-scenario");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000001";

    private static final DummyTestResource RESOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_CUSTOM =
            TestObject.file(TEST_DIR, "custom-policy-reconciliation.xml", "e4f00000-0000-0000-0000-000000000002");
    private static final TestObject<TaskType> TASK_IMPORT =
            TestObject.file(TEST_DIR, "hr-import.xml", "e4f00000-0000-0000-0000-0000000000ff");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN = "a%02d";
    private static final int DELETE_THRESHOLD = 5;

    private static final long TIMEOUT = 90_000;

    private static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    private static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private static final String DUMMY_POLICY_NOTIFIER = "dummy:policyNotifier";

    private static final Consumer<PrismObject<TaskType>> NO_CUSTOMIZATION = t -> { };

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

    /** Imports all accounts (plain import, no policy), linking users to shadows — the state the scenario prunes. */
    private void importAllAndLink(OperationResult result) throws Exception {
        deleteIfPresent(TASK_IMPORT, result);
        addObject(TASK_IMPORT, getTestTask(), result, NO_CUSTOMIZATION);
        waitForTaskCloseOrSuspend(TASK_IMPORT.oid, 5 * TIMEOUT);
        assertTaskTree(TASK_IMPORT.oid, "after import").assertClosed().assertSuccess();
        deleteIfPresent(TASK_IMPORT, result);
    }

    private void deleteAccounts(int from, int to) throws Exception {
        for (int i = from; i < to; i++) {
            RESOURCE.controller.deleteAccount(String.format(PATTERN, i));
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

    private int policyNotifications() {
        List<Message> messages = dummyTransport.getMessages(DUMMY_POLICY_NOTIFIER);
        return messages == null ? 0 : messages.size();
    }

    // endregion

    /**
     * Policy triggered. More accounts are removed on the source than allowed, so the simulate (preview)
     * reconciliation keeps tripping the max-deleted threshold: it restarts on the first two attempts and, on
     * the third attempt, the max-restarts policy suspends the whole task before anything is really deleted.
     */
    @Test
    public void test100PolicyTriggeredRestartsThenSuspends() throws Exception {
        OperationResult result = getTestOperationResult();

        given("all users linked, then 8 accounts removed on the source (over the delete threshold)");
        importAllAndLink(result);
        assertThat(countUsers()).as("users before").isEqualTo(ACCOUNTS);
        deleteAccounts(0, DELETE_THRESHOLD + 3);

        when("running the custom-policy reconciliation");
        deleteIfPresent(TASK_CUSTOM, result);
        addObject(TASK_CUSTOM, getTestTask(), result, NO_CUSTOMIZATION);
        // two restarts (delay PT2S, exponential backoff) precede the suspend, so allow extra time
        waitForTaskCloseOrSuspend(TASK_CUSTOM.oid, 4 * TIMEOUT);

        then("simulate restarted twice and suspended on the third attempt; nothing was really deleted");
        // @formatter:off
        assertTaskTree(TASK_CUSTOM.oid, "after run")
                .display()
                .assertInProgress()
                .assertSuspended()
                .subtask("simulate", false)
                    .display()
                    .assertInProgress()
                    .assertSuspended()
                    .activityState(SIMULATE)
                        .assertExecutionAttempts(3)
                        .policies()
                            .policy("max-restarts", true)
                                .assertTriggerCount(1)
                                .end()
                            .end()
                        .end();
        // @formatter:on
        assertThat(countUsers()).as("preview did not delete").isEqualTo(ACCOUNTS);
        assertThat(policyNotifications()).as("two restart trips plus the suspend fired notifications")
                .isGreaterThanOrEqualTo(3);
    }

    /**
     * Policy not triggered. Fewer accounts are removed than the threshold, so the simulate (preview)
     * reconciliation completes on its first attempt without restarting, and the whole tree finishes — the
     * full-mode execute child deleting just the now-allowed number of users.
     */
    @Test
    public void test200PolicyNotTriggeredSucceeds() throws Exception {
        OperationResult result = getTestOperationResult();

        given("all users linked, then 4 accounts removed on the source (below the delete threshold)");
        importAllAndLink(result);
        assertThat(countUsers()).as("users before").isEqualTo(ACCOUNTS);
        deleteAccounts(0, DELETE_THRESHOLD - 1);

        when("running the custom-policy reconciliation");
        deleteIfPresent(TASK_CUSTOM, result);
        addObject(TASK_CUSTOM, getTestTask(), result, NO_CUSTOMIZATION);
        waitForTaskCloseOrSuspend(TASK_CUSTOM.oid, TIMEOUT);

        then("the whole tree completes on the first attempt; execute deletes the 4 allowed users");
        String deletedId = ActivityPolicyUtils.buildPolicyIdentifier(getTask(TASK_CUSTOM.oid), SIMULATE, "max-deleted", true);
        // @formatter:off
        assertTaskTree(TASK_CUSTOM.oid, "after successful run")
                .display()
                .assertClosed()
                .assertSuccess()
                .subtask("simulate", false)
                    .display()
                    .assertClosed()
                    .activityState(SIMULATE)
                        .assertExecutionAttempts(1)
                        .assertNoPolicies()
                        .previewModePolicyRulesCounters()
                            .assertCounterMinMax(deletedId, 1, DELETE_THRESHOLD - 1)
                            .end()
                        .end()
                    .end()
                .subtask("execute", false)
                    .display()
                    .assertClosed()
                    .activityState(EXECUTE)
                        .assertNoPolicies();
        // @formatter:on
        assertThat(countUsers()).as("only the allowed (4 < 5) users deleted").isEqualTo(ACCOUNTS - (DELETE_THRESHOLD - 1));
        assertThat(policyNotifications()).as("no policy fired, so no notification").isEqualTo(0);
    }
}
