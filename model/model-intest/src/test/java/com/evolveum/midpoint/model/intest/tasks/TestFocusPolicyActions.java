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
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Tests the policy actions a focus policy rule can trigger — suspendTask, notification, skipActivity,
 * restartActivity — evaluated in the clockwork (focus) context of a task activity.
 *
 * This is the focus/clockwork counterpart of the repo-common {@code TestActivityPolicies}, which covers
 * the activity-constraint path (execution time, item errors) only, inline only, and without notifications.
 *
 * Axis here: policy action. Fixed: contribution form = inline, constraint = a {@code modification} threshold,
 * placement = own. Topologies (multithread/multinode/subtasks) are added in separate test.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyActions extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-actions");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_OID = "c1a70000-0000-0000-0000-000000000001";

    /** Shared dummy source resource, initialized per class (see {@code tasks/common}). */
    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> TASK_IMPORT =
            TestObject.file(TEST_DIR, "task-fa-import.xml", "e2f00000-0000-0000-0000-000000000001");
    private static final TestObject<TaskType> TASK_IMPORT_COMPOSITE =
            TestObject.file(TEST_DIR, "task-fa-import-composite.xml", "e2f00000-0000-0000-0000-000000000002");

    private static final int ACCOUNTS = 20;
    private static final String ACCOUNT_NAME_PATTERN = "a%02d";

    private static final int ADD_THRESHOLD = 5;
    /** High enough that a single restart (which consumes the first batch) then converges. */
    private static final int RESTART_THRESHOLD = 15;

    private static final String RULE_ADD = "fpa-add";

    private static final long TIMEOUT = 60_000;

    private static final String DUMMY_ACTIVITY_POLICY_NOTIFIER = "dummy:activityPolicyNotifier";
    private static final String DUMMY_POLICY_NOTIFIER = "dummy:policyNotifier";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(ACCOUNT_NAME_PATTERN)
                .withController(RESOURCE_SOURCE.controller)
                .execute();

        SimpleActivityPolicyRuleNotifierType activityNotifier = new SimpleActivityPolicyRuleNotifierType();
        activityNotifier.getTransport().add(DUMMY_ACTIVITY_POLICY_NOTIFIER);
        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add(DUMMY_POLICY_NOTIFIER);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(activityNotifier);
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);

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
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
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
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            if (RESOURCE_SOURCE.controller.getDummyResource().getAccountByName(name) == null) {
                RESOURCE_SOURCE.controller.addAccount(name);
            }
        }
    }

    // region builders / helpers

    private PolicyRuleType addRule(int threshold, PolicyActionsType actions) {
        return new PolicyRuleType()
                .name(RULE_ADD)
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()
                                .operation(ChangeTypeType.ADD)))
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(threshold)))
                .policyActions(actions);
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
        return ActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), path, RULE_ADD, true);
    }

    private int countImportedUsers() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(ACCOUNT_NAME_PATTERN, i);
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
        }
        return count;
    }

    private int notificationCount(String transport) {
        List<Message> messages = dummyTransport.getMessages(transport);
        return messages == null ? 0 : messages.size();
    }

    /** Polls until the task closes, tolerating the transient suspend/resume of restart cycles. */
    private void waitForTaskClose(String oid, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Task t = taskManager.getTaskWithResult(oid, getTestOperationResult());
            if (t.isClosed()) {
                return;
            }
            Thread.sleep(1000);
        }
        throw new AssertionError("Task " + oid + " did not close within " + timeout + " ms");
    }

    // endregion

    // region tests

    /**
     * suspendTask + notification: task halts at the threshold, and the clockwork policy notification is
     * emitted before the halt (a pure suspendTask rule, with no notification action, emits nothing —
     * the enforcer only "forces" a notification for rules that configure one).
     */
    @Test
    public void test100Suspend() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT;
        deleteIfPresent(task, result);

        when("import with an inline add-threshold rule + suspendTask + notification");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType()
                                .suspendTask(new SuspendTaskPolicyActionType())
                                .notification(new NotificationPolicyActionType())),
                        ActivityPath.empty()));
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("task suspended at the threshold; counter recorded; notification emitted before the halt");
        String id = inlineIdentifier(task, ActivityPath.empty());
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADD_THRESHOLD, ADD_THRESHOLD);
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users").isEqualTo(ADD_THRESHOLD - 1);
        assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).as("clockwork policy notifications").isGreaterThanOrEqualTo(1);
    }

    /** notification only (no halting): activity completes; trigger recorded; notifications emitted. */
    @Test
    public void test110NotificationOnly() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT;
        deleteIfPresent(task, result);

        when("import with an inline add-threshold rule with only a notification action");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType().notification(new NotificationPolicyActionType())),
                        ActivityPath.empty()));
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("activity completes (all imported); notifications were emitted");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed();
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users").isEqualTo(ACCOUNTS);
        assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).as("clockwork policy notifications").isGreaterThanOrEqualTo(1);
    }

    /** skipActivity: 'main' is aborted, 'last' still runs, task closes with fatal error. */
    @Test
    public void test200Skip() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT_COMPOSITE;
        deleteIfPresent(task, result);

        when("composite import with an inline add-threshold rule + skipActivity on 'main'");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType().skipActivity(new SkipActivityPolicyActionType())),
                        ActivityPath.fromId("main")));
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("'main' aborted, 'last' completes, task closed with fatal error");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .assertFatalError()
                .rootActivityState()
                    .child("main")
                        .assertAborted()
                        .assertFatalError()
                        .end()
                    .child("last")
                        .assertComplete()
                        .assertSuccess();
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users before skip").isEqualTo(ADD_THRESHOLD - 1);
    }

    /**
     * restartActivity: on trip the activity restarts (counter reset, execution attempt incremented);
     * because committed users become linked, each attempt consumes a batch until it converges and closes.
     */
    @Test
    public void test300Restart() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT;
        deleteIfPresent(task, result);

        when("import with an inline add-threshold rule + restartActivity");
        addObject(task, getTestTask(), result,
                contributeInline(
                        addRule(RESTART_THRESHOLD, new PolicyActionsType().restartActivity(new RestartActivityPolicyActionType())),
                        ActivityPath.empty()));
        waitForTaskClose(task.oid, 5 * TIMEOUT);

        then("task converges: closed & successful after >= 2 execution attempts; all imported; no lingering policy state");
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .assertExecutionAttempts(2)
                    .assertNoPolicies();
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users").isEqualTo(ACCOUNTS);
    }

    /**
     * suspendTask under worker threads: the shared counter may overshoot the threshold by up to
     * {@code threads - 1} (threads add in parallel before the suspend propagates).
     */
    @Test
    public void test400SuspendMultithread() throws Exception {
        OperationResult result = getTestOperationResult();
        TestObject<TaskType> task = TASK_IMPORT;
        deleteIfPresent(task, result);
        int threads = 3;

        when("import with worker threads and an inline add-threshold rule + suspendTask");
        addObject(task, getTestTask(), result, aggregateCustomizer(
                contributeInline(
                        addRule(ADD_THRESHOLD, new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType())),
                        ActivityPath.empty()),
                rootActivityWorkerThreadsCustomizer(threads)));
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, result, TIMEOUT);

        then("task suspended; shared counter within [threshold, threshold + threads - 1]");
        String id = inlineIdentifier(task, ActivityPath.empty());
        // @formatter:off
        assertTaskTree(task.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounterMinMax(id, ADD_THRESHOLD, ADD_THRESHOLD + threads - 1);
        // @formatter:on
        assertThat(countImportedUsers()).as("imported users").isGreaterThanOrEqualTo(ADD_THRESHOLD - 1);
    }

    // endregion
}
