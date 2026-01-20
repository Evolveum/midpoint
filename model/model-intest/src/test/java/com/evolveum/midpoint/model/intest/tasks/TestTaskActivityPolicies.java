/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.test.*;

import org.assertj.core.api.Assertions;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.ConnectorOperationHook;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskActivityPolicies extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    private static final TestTask TASK_RECONCILIATION_DUMMY =
            TestTask.file(TEST_DIR, "task-reconciliation.xml", "ccd6df6c-123a-4d9d-b48e-e4de9bf3f2e2");

    private static final TestTask TASK_RECONCILIATION_CSV =
            TestTask.file(TEST_DIR, "task-reconciliation-csv.xml", "ccd6df6c-123a-4d9d-b48e-e4de9bf3f2e2");

    private static final TestObject<RoleType> ROLE_DUMMY =
            TestObject.file(TEST_DIR, "role-dummy.xml", "1782ba8d-9cd5-4779-af8e-96d11346cdb4");

    private static final TestObject<RoleType> ROLE_CSV =
            TestObject.file(TEST_DIR, "role-csv.xml", "badaae5b-1608-439f-beae-607ca7e60004");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "dummy.xml",
            "8f82e457-6c6e-42d7-a433-1a346b1899ee",
            "resource-dummy",
            TestTaskActivityPolicies::populateWithSchema);

    private static final TestResource RESOURCE_CSV =
            TestResource.file(TEST_DIR, "resource-csv.xml","764d58e1-cb44-4ad3-a4e0-465ff952c43e");

    private DummyResourceContoller dummyResourceCtl;

    private static final String DUMMY_NOTIFICATION_TRANSPORT = "dummy:activityPolicyRuleNotifier";

    private static final String ACCOUNT_JSMITH = "jsmith";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // dummy related setup

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        dummyResourceCtl.addAccount("jdoe", "john doe");
        dummyResourceCtl.addAccount(ACCOUNT_JSMITH, "jim smith");
        dummyResourceCtl.addAccount("jblack", "jack black");
        dummyResourceCtl.addAccount("wwhite", "william white");

        addObject(ROLE_DUMMY, initTask, initResult);

        addObject(createUser("jdoe", "John Doe", ROLE_DUMMY.oid), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createUser(ACCOUNT_JSMITH, "Jim Smith", ROLE_DUMMY.oid), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createUser("jblack", "Jack Black", ROLE_DUMMY.oid), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createUser("wwhite", "William White", ROLE_DUMMY.oid), ModelExecuteOptions.create().raw(), initTask, initResult);

        // csv related setup

        RESOURCE_CSV.initAndTest(this, initTask, initResult);

        addObject(ROLE_CSV, initTask, initResult);

        addObject(createUser("abrown", "Alice Brown", ROLE_CSV.oid), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createUser("cgreen", "Charlie Green", ROLE_CSV.oid), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createUser("dgray", "Diana Gray", ROLE_CSV.oid), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createUser("eblue", "Eve Blue", ROLE_CSV.oid), ModelExecuteOptions.create().raw(), initTask, initResult);

        // notifications setup

        SimpleActivityPolicyRuleNotifierType notifier = new SimpleActivityPolicyRuleNotifierType();
        notifier.getTransport().add(DUMMY_NOTIFICATION_TRANSPORT);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(notifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask,
                initResult,
                handler);
    }

    private PrismObject<UserType> createUser(String name, String fullName, String roleOid) {
        UserType user = new UserType();
        user.setName(PolyStringType.fromOrig(name));
        user.setFullName(PolyStringType.fromOrig(fullName));
        user.beginAssignment()
                .targetRef(roleOid, RoleType.COMPLEX_TYPE);

        return user.asPrismObject();
    }

    private static void populateWithSchema(DummyResourceContoller controller) throws Exception {
        controller.populateWithDefaultSchema();
    }

    @BeforeMethod
    public void beforeMethod() {
        prepareNotifications();
    }

    @Test(enabled = false)
    public void test200TestItemProcessingNetworkError() throws Exception {
        testItemProcessingError(BreakMode.NETWORK, new ConnectorIOException("Dummy network connector exception"));
    }

    @Test(enabled = false)
    public void test210TestItemProcessingGenericError() throws Exception {
        testItemProcessingError(BreakMode.GENERIC, new ConnectorException("Dummy generic connector exception"));
    }

    @Test(enabled = false)
    public void test300TestItemProcessingGenericErrorCSV() throws Exception {
        OperationResult result = getTestOperationResult();

        dummyTransport.clearMessages();

        TestTask testTask = TASK_RECONCILIATION_CSV;
        testTask.initWithOverwrite(this, getTestTask(), result);

        testTask.rerunErrorsOk(result);

        final String notifyRuleName = "policy rule with notify";
        var notifyCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        ActivityPath.fromId("main reconciliation"),
                        notifyRuleName);

        // @formatter:off
        testTask.assertTreeAfter()
                .assertClosed()
                .assertSuccess()
                .activityState(ActivityPath.fromId("main reconciliation"))
                    .fullExecutionModePolicyRulesCounters()
                    .assertCounter(notifyCounterIdentifier, 3)
                    .end()
                .policies()
                    .assertPolicyCount(2)
                    .policy(notifyRuleName)
                        .assertTriggerCount(1)
                        .end()
                    .end()
                .end()
                .activityState(ActivityPath.fromId("noop activity"))
                    .assertSuccess()
                    .assertComplete();
        // @formatter:on

        List<Message> messages = dummyTransport.getMessages(DUMMY_NOTIFICATION_TRANSPORT);
        Assertions.assertThat(messages)
                .isNotNull()
                .hasSize(2);
    }

    private void updateTaskPolicyErrorCategory(String taskOid, BreakMode mode) throws Exception {
        Task task = taskManager.getTaskWithResult(taskOid, getTestOperationResult());
        TaskType taskType = task.getRawTaskObjectClonedIfNecessary().asObjectable();

        ActivityPoliciesType policies;
        if (taskType.getActivity().getComposition() != null) {
            policies = taskType.getActivity().getComposition().getActivity().stream()
                    .filter(a -> "main reconciliation".equals(a.getIdentifier()))
                    .findFirst()
                    .orElseThrow()
                    .getPolicies();
        } else {
            policies = taskType.getActivity().getPolicies();
        }

        List<ItemPath> pathsToModify = policies.getPolicy().stream()
                .map(p -> p.getPolicyConstraints().getItemProcessingResult().asPrismContainerValue().getPath())
                .toList();

        ObjectDelta<TaskType> delta = PrismTestUtil.getPrismContext().deltaFor(TaskType.class)
                .asObjectDelta(taskOid);

        for (ItemPath path : pathsToModify) {
            ItemProcessingResultPolicyConstraintType constraint = new ItemProcessingResultPolicyConstraintType();
            switch (mode) {
                case NETWORK -> constraint.errorCategory(ErrorCategoryType.NETWORK);
                case GENERIC -> constraint.errorCategory(ErrorCategoryType.GENERIC);
                default -> throw new IllegalStateException("Unexpected value: " + mode);
            }

            delta.addModificationReplaceContainer(path, constraint.asPrismContainerValue());
        }

        executeChanges(delta, ModelExecuteOptions.create(), getTestTask(), getTestOperationResult());
    }

    private void testItemProcessingError(BreakMode mode, ConnectorException ex) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        dummyResourceCtl.getDummyResource().resetHooks();
        dummyResourceCtl.getDummyResource().registerHook(new ConnectorOperationHook() {

            @Override
            public void beforeHandleResultObject(DummyObject object) {
                // only one specific account is "broken" and can't be fetched
                if (Objects.equals(object.getName(), "jsmith")) {
                    throw ex;
                }
            }
        });

        TestTask testTask = TASK_RECONCILIATION_DUMMY;
        testTask.initWithOverwrite(this, task, result);

        updateTaskPolicyErrorCategory(testTask.oid, mode);

        testTask.rerunErrorsOk(result);

        final String notifyRuleName = "policy rule with notify";
        var notifyCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        ActivityPath.fromId("main reconciliation"),
                        notifyRuleName);

        // @formatter:off
        testTask.assertTreeAfter()
                .assertClosed()
                .assertSuccess()
                .activityState(ActivityPath.fromId("main reconciliation"))
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(notifyCounterIdentifier, 3)
                    .end()
                    .policies()
                        .assertPolicyCount(2)
                        .policy(notifyRuleName)
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .end()
                .activityState(ActivityPath.fromId("noop activity"))
                    .assertSuccess()
                    .assertComplete();
        // @formatter:on

        List<Message> messages = dummyTransport.getMessages(DUMMY_NOTIFICATION_TRANSPORT);
        Assertions.assertThat(messages)
                .isNotNull()
                .hasSize(2);
    }
}
