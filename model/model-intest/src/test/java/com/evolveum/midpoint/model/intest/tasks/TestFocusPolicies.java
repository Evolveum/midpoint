/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class TestFocusPolicies extends TestThresholds {

    private static final Trace LOGGER = TraceManager.getTrace(TestFocusPolicies.class);

    private static final long TIMEOUT = 60000;

    private static final int WORKER_THREADS = 1;

    private static final String DUMMY_ACTIVITY_POLICY_NOTIFIER = "dummy:activityPolicyNotifier";

    private static final String DUMMY_POLICY_NOTIFIER = "dummy:policyNotifier";

    protected static final TestObject<RoleType> ROLE_ADD_10_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-add-10-notification.xml", "79d1d4e7-408c-4255-9386-5697892691df");

    protected static final TestObject<RoleType> ROLE_MODIFY_5_COST_CENTER_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-modify-cost-center-5-notification.xml", "719eb6bc-6a89-4baf-a125-2482b9502ad1");

    protected static final TestObject<RoleType> ROLE_MODIFY_5_FULL_NAME_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-modify-full-name-5-notification.xml", "6b76ce6f-0ca0-4e86-9209-8d734851348d");

    protected static final TestObject<RoleType> ROLE_DELETE_5_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-delete-5-notification.xml", "552d2465-6531-4742-8626-7c9559ec8ff7");

    protected String ruleAddNotificationId;

    protected String ruleModifyCostCenterNotificationId;

    protected String ruleModifyFullNameNotificationId;

    protected String ruleDeleteNotificationId;

    @BeforeMethod
    public void beforeMethod() {
        prepareNotifications();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        SimpleActivityPolicyRuleNotifierType activityPolicyNotifier = new SimpleActivityPolicyRuleNotifierType();
        activityPolicyNotifier.getTransport().add(DUMMY_ACTIVITY_POLICY_NOTIFIER);

        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add(DUMMY_POLICY_NOTIFIER);

        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(activityPolicyNotifier);
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask,
                initResult,
                handler);

        repoAdd(ROLE_ADD_10_NOTIFICATION, initResult);
        ruleAddNotificationId = determineSingleInducedRuleId(ROLE_ADD_10_NOTIFICATION.oid, initResult);

        repoAdd(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION, initResult);
        ruleModifyCostCenterNotificationId = determineSingleInducedRuleId(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION.oid, initResult);

        repoAdd(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION, initResult);
        ruleModifyFullNameNotificationId = determineSingleInducedRuleId(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION.oid, initResult);

        repoAdd(ROLE_DELETE_5_NOTIFICATION, initResult);
        ruleDeleteNotificationId = determineSingleInducedRuleId(ROLE_DELETE_5_NOTIFICATION.oid, initResult);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentImportAdd() {
        return roleAssignmentCustomizer(ROLE_ADD_10_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentModifyCostCenter() {
        return roleAssignmentCustomizer(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentModifyFullName() {
        return roleAssignmentCustomizer(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentDelete() {
        return roleAssignmentCustomizer(ROLE_DELETE_5_NOTIFICATION.oid);
    }

    protected void transplantRolePolicy(
            TestObject<RoleType> source,
            PolicyActionsType actionOverrides,
            PrismObject<TaskType> target,
            ActivityPath activityPath) {

        int count = 0;

        List<PolicyRuleType> rules = source.getObjectable().getInducement().stream()
                .filter(i -> Objects.equals(2, i.getOrder()))
                .filter(inducement -> inducement.getPolicyRule() != null)
                .map(AssignmentType::getPolicyRule)
                .toList();

        ActivityDefinitionType def = target.asObjectable().getActivity();
        ActivityDefinitionType myDef = ActivityDefinitionUtil.findActivityDefinition(def, activityPath);
        Assertions.assertThat(myDef)
                .withFailMessage("No activity definition found for path %s in task %s", activityPath, target.getOid())
                .isNotNull();

        for (PolicyRuleType rule : rules) {
            PolicyRuleType newRule = rule.clone();
            if (actionOverrides != null) {
                newRule.setPolicyActions(actionOverrides);
            }

            ActivityPoliciesType policies = myDef.getPolicies();
            if (policies == null) {
                policies = new ActivityPoliciesType();
                myDef.setPolicies(policies);
            }

            policies.getPolicy().add(newRule);

            count++;
        }

        LOGGER.info("Transplanted {} policy rules from {} to {}, {}", count, source, target, activityPath);
    }

    @Override
    int getWorkerThreads() {
        return WORKER_THREADS;
    }

    @Override
    long getTimeout() {
        return TIMEOUT;
    }
}
