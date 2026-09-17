/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestActivityPolicyUtils;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityHaltingInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * HR source whose removed accounts deactivate their users (validTo set into the past by an object template).
 * A composite task runs a preview reconciliation guarded by a "max deactivated" suspend policy, followed by
 * an unguarded full reconciliation.
 *
 * Once the preview is halted by the policy, resuming the task must not let it complete: the full reconciliation
 * may start only after the halt is acknowledged by clearing the activity policy states.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyHrDeactivation extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/hr-scenario");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final DummyTestResource RESOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source-deactivation.xml", "c1a70000-0000-0000-0000-000000000004",
            "fpc-source-deact", DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<ObjectTemplateType> TEMPLATE =
            TestObject.file(TEST_DIR, "template-deactivate.xml", "0d1a0000-0000-0000-0000-000000000001");

    private static final TestTask TASK_HR =
            TestTask.file(TEST_DIR, "hr-reconciliation-deactivation.xml", "e4f00000-0000-0000-0000-000000000011");
    private static final TestTask TASK_HR_IMPORT =
            TestTask.file(TEST_DIR, "hr-import-deactivation.xml", "e4f00000-0000-0000-0000-0000000000fe");

    private static final int ACCOUNTS = 30;
    private static final String PATTERN = "d%02d";
    private static final int THRESHOLD = 10;

    private static final long TIMEOUT = 90_000;

    private static final ActivityPath PREVIEW = ActivityPath.fromId("preview");
    private static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        TEMPLATE.init(this, initTask, initResult);
        initDummyResource(RESOURCE, initTask, initResult);
        createAccounts();
        TASK_HR_IMPORT.init(this, initTask, initResult);
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
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            for (PrismObject<UserType> user : findUsers(i, result)) {
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

    private List<PrismObject<UserType>> findUsers(int i, OperationResult result) throws Exception {
        return repositoryService.searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).eqPoly(String.format(PATTERN, i)).matchingNorm()
                        .build(),
                null, result);
    }

    private void deleteAccounts(int from, int to) throws Exception {
        for (int i = from; i < to; i++) {
            RESOURCE.controller.deleteAccount(String.format(PATTERN, i));
        }
    }

    /** Number of users (out of the scenario ones) that are effectively disabled, i.e. really deactivated. */
    private int countDeactivatedUsers() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            for (PrismObject<UserType> user : findUsers(i, result)) {
                var activation = user.asObjectable().getActivation();
                if (activation != null
                        && (activation.getValidTo() != null
                        || activation.getEffectiveStatus() == ActivationStatusType.DISABLED)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void runFresh(OperationResult result) throws Exception {
        deleteIfPresent(TASK_HR, result);
        addObject(TASK_HR, getTestTask(), result, t -> { });
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);
    }

    private String policyId() throws Exception {
        return TestActivityPolicyUtils.buildPolicyIdentifier(getTask(TASK_HR.oid), PREVIEW, "max-deactivated", true);
    }

    private ActivityHaltingInformationType previewHaltingInformation() throws Exception {
        var state = ActivityStateUtil.getActivityState(getTask(TASK_HR.oid).asObjectable(), PREVIEW);
        return state != null ? state.getHaltingInformation() : null;
    }

    private XMLGregorianCalendar lastRunStart() throws Exception {
        return getTask(TASK_HR.oid).asObjectable().getLastRunStartTimestamp();
    }

    /** The preview was halted by the policy: task suspended, execute not started, nobody deactivated. */
    private void assertHaltedInPreview(String message) throws Exception {
        // @formatter:off
        assertTaskTree(TASK_HR.oid, message)
                .display()
                .assertSuspended()
                .assertFatalError()
                .activityState(EXECUTE)
                    .assertRealizationState(null)
                .end()
                .activityState(PREVIEW)
                    .assertFatalError()
                    .previewModePolicyRulesCounters()
                        .assertCounterMinMax(policyId(), THRESHOLD, THRESHOLD);
        // @formatter:on
        ActivityHaltingInformationType halt = previewHaltingInformation();
        assertThat(halt).as("halting information in preview state").isNotNull();
        assertThat(halt.getPolicyName()).as("halting policy name").isEqualTo("max-deactivated");
        assertThat(halt.getPolicyIdentifier()).as("halting policy identifier").isEqualTo(policyId());
        assertThat(countDeactivatedUsers()).as("deactivated users").isZero();
    }

    /** Removes the given number of accounts, lets the preview trip the policy, and resumes the suspended task. */
    private void tripPreviewAndResume(int removedAccounts) throws Exception {
        OperationResult result = getTestOperationResult();

        given("all users linked, then " + removedAccounts + " accounts removed on the source");
        TASK_HR_IMPORT.rerun(result);
        assertThat(countDeactivatedUsers()).as("deactivated users before").isZero();
        deleteAccounts(0, removedAccounts);

        when("running the composite task");
        runFresh(result);

        then("preview trips the threshold and the task suspends; nothing is deactivated");
        assertHaltedInPreview("after first run");
        XMLGregorianCalendar firstRunStart = lastRunStart();

        when("the suspended task is resumed");
        taskManager.resumeTaskTree(TASK_HR.oid, result);
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("the task really ran again, and was halted at the start of the preview, with nothing processed");
        assertThat(lastRunStart()).as("last run start after resume").isNotEqualTo(firstRunStart);
        assertHaltedInPreview("after resume");
    }

    /** More accounts removed than the threshold allows: some of them are left for the resumed preview. */
    @Test
    public void test100ResumeAfterPreviewTripAboveThreshold() throws Exception {
        tripPreviewAndResume(THRESHOLD + 2);
    }

    /**
     * Exactly as many accounts removed as the threshold: nothing countable is left for the resumed preview,
     * so without the recorded halt it would complete, and the full reconciliation would deactivate the users.
     */
    @Test
    public void test110ResumeAfterPreviewTripAtThreshold() throws Exception {
        tripPreviewAndResume(THRESHOLD);
    }

    /** Clearing the activity policy states acknowledges the halt: the resumed task continues to the full reconciliation. */
    @Test
    public void test120ClearPolicyStatesAcknowledgesHalt() throws Exception {
        OperationResult result = getTestOperationResult();

        given("preview halted at the threshold, resume halted again");
        tripPreviewAndResume(THRESHOLD);

        when("the policy states are cleared and the task is resumed");
        boolean changed = modelInteractionService.clearAllActivityPolicyStates(
                getTask(TASK_HR.oid), getTestTask(), result);
        assertThat(changed).as("clearAllActivityPolicyStates made a change").isTrue();
        assertThat(previewHaltingInformation()).as("halting information after clearing").isNull();
        taskManager.resumeTaskTree(TASK_HR.oid, result);
        waitForTaskCloseOrSuspend(TASK_HR.oid, TIMEOUT);

        then("the task completes and the users are deactivated");
        // @formatter:off
        assertTaskTree(TASK_HR.oid, "after clearing and resume")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState(PREVIEW)
                    .assertSuccess()
                .end()
                .activityState(EXECUTE)
                    .assertSuccess();
        // @formatter:on
        assertThat(countDeactivatedUsers()).as("deactivated users after acknowledged halt").isEqualTo(THRESHOLD);
    }
}
