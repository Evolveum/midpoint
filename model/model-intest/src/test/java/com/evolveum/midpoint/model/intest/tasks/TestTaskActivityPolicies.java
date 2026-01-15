/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
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
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskActivityPolicies extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    private static final TestTask TASK_RECONCILIATION =
            TestTask.file(TEST_DIR, "task-reconciliation.xml", "ccd6df6c-123a-4d9d-b48e-e4de9bf3f2e2");

    private static final TestObject<RoleType> ROLE_DUMMY =
            TestObject.file(TEST_DIR, "role-dummy.xml", "1782ba8d-9cd5-4779-af8e-96d11346cdb4");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "dummy.xml",
            "8f82e457-6c6e-42d7-a433-1a346b1899ee",
            "resource-dummy",
            TestTaskActivityPolicies::populateWithSchema);

    private DummyResourceContoller dummyResourceCtl;

    private static final String DUMMY_NOTIFICATION_TRANSPORT = "dummy:activityPolicyRuleNotifier";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        dummyResourceCtl.addAccount("jdoe", "john doe");
        dummyResourceCtl.addAccount("jsmith", "jim smith");
        dummyResourceCtl.addAccount("jblack", "jack black");
        dummyResourceCtl.addAccount("wwhite", "william white");

        addObject(ROLE_DUMMY, initTask, initResult);

        addObject(createDummyUser("jdoe", "John Doe"), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createDummyUser("jsmith", "Jim Smith"), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createDummyUser("jblack", "Jack Black"), ModelExecuteOptions.create().raw(), initTask, initResult);
        addObject(createDummyUser("wwhite", "William White"), ModelExecuteOptions.create().raw(), initTask, initResult);

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

    private PrismObject<UserType> createDummyUser(String name, String fullName) {
        UserType user = new UserType();
        user.setName(PolyStringType.fromOrig(name));
        user.setFullName(PolyStringType.fromOrig(fullName));
        user.beginAssignment()
                .targetRef(ROLE_DUMMY.oid, RoleType.COMPLEX_TYPE);

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
        testItemProcessingError(BreakMode.NETWORK);
    }

    @Test(enabled = false)
    public void test210TestItemProcessingGenericError() throws Exception {
        testItemProcessingError(BreakMode.GENERIC);
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

    private void testItemProcessingError(BreakMode mode) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyResourceCtl.getDummyResource().setBreakMode(mode);

        TestTask testTask = TASK_RECONCILIATION;
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
                .assertFatalError()
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
                .hasSize(2);
    }
}
