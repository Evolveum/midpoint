/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class TestFocusPolicies extends TestThresholds {

    private static final Trace LOGGER = TraceManager.getTrace(TestFocusPolicies.class);

    private static final long TIMEOUT = 60000;

    private static final int WORKER_THREADS = 1;

    protected static final String DUMMY_ACTIVITY_POLICY_NOTIFIER = "dummy:activityPolicyNotifier";

    protected static final String DUMMY_POLICY_NOTIFIER = "dummy:policyNotifier";

    protected static final TestObject<RoleType> ROLE_ADD_10_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-add-10-notification.xml", "79d1d4e7-408c-4255-9386-5697892691df");

    protected static final TestObject<RoleType> ROLE_MODIFY_5_COST_CENTER_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-modify-cost-center-5-notification.xml", "719eb6bc-6a89-4baf-a125-2482b9502ad1");

    protected static final TestObject<RoleType> ROLE_MODIFY_5_FULL_NAME_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-modify-full-name-5-notification.xml", "6b76ce6f-0ca0-4e86-9209-8d734851348d");

    protected static final TestObject<RoleType> ROLE_DELETE_5_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-delete-5-notification.xml", "552d2465-6531-4742-8626-7c9559ec8ff7");

    public static final String RULE_ADD_NOTIFICATION_NAME = "add-10-notification";
    public static final String RULE_MODIFY_COST_CENTER_NOTIFICATION_NAME = "modify-cost-center-5-notification";
    public static final String RULE_MODIFY_FULL_NAME_NOTIFICATION_NAME = "modify-full-name-5-notification";
    public static final String RULE_DELETE_NOTIFICATION_NAME = "delete-5-notification";

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
        ruleAddNotificationId = determineInducedRuleId(ROLE_ADD_10_NOTIFICATION.oid, "add-10-notification", initResult);

        repoAdd(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION, initResult);
        ruleModifyCostCenterNotificationId = determineInducedRuleId(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION.oid, "modify-5-costCenter-notification", initResult);

        repoAdd(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION, initResult);
        ruleModifyFullNameNotificationId = determineInducedRuleId(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION.oid, "modify-5-fullName-notification", initResult);

        repoAdd(ROLE_DELETE_5_NOTIFICATION, initResult);
        ruleDeleteNotificationId = determineInducedRuleId(ROLE_DELETE_5_NOTIFICATION.oid, "delete-5-notification", initResult);
    }

    protected String determineInducedRuleId(String roleOid, String policyId, OperationResult result)
            throws CommonException {

        RoleType role = repositoryService.getObject(RoleType.class, roleOid, null, result).asObjectable();
        List<AssignmentType> ruleInducements = role.getInducement().stream()
                .filter(i -> i.getPolicyRule() != null)
                .filter(i -> Objects.equals(policyId, i.getPolicyRule().getName()))
                .collect(Collectors.toList());

        assertThat(ruleInducements).as("policy rule inducements in " + role).hasSize(1);
        Long id = ruleInducements.get(0).getId();
        argCheck(id != null, "Policy rule inducement in %s has no PCV ID", roleOid);
        return roleOid + ":" + id;
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

    protected void assertNotifications(String transport, String contains, int count) {
        List<Message> messages = dummyTransport.getMessages(transport);
        if (messages == null && count == 0) {
            return;
        }

        Assertions.assertThat(messages)
                .withFailMessage("Expected some notifications in transport %s, but got none", transport)
                .isNotNull();

        messages = messages.stream()
                .filter(m -> m.getBody() != null && m.getBody().contains(contains))
                .toList();

        Assertions.assertThat(messages)
                .withFailMessage("Expected %d notifications containing '%s' in transport %s, but got %d",
                        count, contains, transport, messages.size())
                .hasSize(count);
    }

    protected Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateOrExecuteTask(TestObject<RoleType> source) {
        return transplantRolePolicyForSimulateOrExecuteTask(source, policy -> true);
    }

    protected PolicyActionsType createPolicyActionsReplacement() {
        return new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType());
    }

    /**
     * Transplant for simulate OR execute task. If task have simulate-execute,
     * please use {@link #transplantRolePolicyForSimulateExecuteTask(TestObject)}.
     */
    protected Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateOrExecuteTask(
            TestObject<RoleType> source, Predicate<PolicyRuleType> policyMatcher) {

        return task ->
                transplantRolePolicy(
                        source,
                        policyMatcher,
                        createPolicyActionsReplacement(),
                        task,
                        ActivityPath.empty());
    }

    protected Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateExecuteTask(
            TestObject<RoleType> source) {
        return transplantRolePolicyForSimulateOrExecuteTask(source, p -> true);
    }

    /**
     * Transplant role policy for simulate-execute task. If task has simulate OR execute ONLY
     * please use {@link #transplantRolePolicyForSimulateOrExecuteTask(TestObject)}.
     */
    protected Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateExecuteTask(
            TestObject<RoleType> source, Predicate<PolicyRuleType> policyMatcher) {

        return task -> {
            transplantRolePolicy(
                    source,
                    policyMatcher,
                    createPolicyActionsReplacement(),
                    task,
                    ActivityPath.fromId("simulate"));

            transplantRolePolicy(
                    source,
                    policyMatcher,
                    createPolicyActionsReplacement(),
                    task,
                    ActivityPath.fromId("execute"));
        };
    }

    protected void transplantRolePolicy(
            TestObject<RoleType> source,
            PolicyActionsType actionOverrides,
            PrismObject<TaskType> target,
            ActivityPath activityPath) {

        transplantRolePolicy(source, p -> true, actionOverrides, target, activityPath);
    }

    protected void transplantRolePolicy(
            TestObject<RoleType> source,
            Predicate<PolicyRuleType> policyMatcher,
            PolicyActionsType actionOverrides,
            PrismObject<TaskType> target,
            ActivityPath activityPath) {

        int count = 0;

        List<PolicyRuleType> rules = source.getObjectable().getInducement().stream()
                .filter(i -> Objects.equals(2, i.getOrder()))
                .filter(inducement -> inducement.getPolicyRule() != null)
                .map(AssignmentType::getPolicyRule)
                .filter(policy -> policyMatcher == null || policyMatcher.test(policy))
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
