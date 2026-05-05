/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.policy.PlainPolicyRuleIdentifier;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Test that virtual assignment created in activity is properly evaluated.
 *
 * Test extends original {@link TestThresholds} class, following modifications are made:
 * 1/ role with suspend policy is not assigned to task, but directly to activity via virtualAssignments.
 * 2/ roles are modified to remove order from inducement, because the role will be "virtually" assigned to processed object.
 * 3/ task has assignment to role with policy (with notification action)
 *
 */
public class TestVirtualAssignmentInActivity extends TestFocusPolicyInActivity {

    private static final Trace LOGGER = TraceManager.getTrace(TestVirtualAssignmentInActivity.class);

    /**
     * clear order=2 from inducement before adding role to repository
     */
    @Override
    protected PrismObject<RoleType> customizeRoleAdd10BeforeRepoAdd(TestObject<RoleType> object) {
        return clearInducementsOrder(object.get());
    }

    /**
     * clear order=2 from inducement before adding role to repository
     */
    @Override
    protected PrismObject<RoleType> customizeRoleModifyCostCenter5(TestObject<RoleType> object) {
        return clearInducementsOrder(object.get());
    }

    /**
     * clear order=2 from inducement before adding role to repository
     */
    @Override
    protected PrismObject<RoleType> customizeRoleDelete5(TestObject<RoleType> object) {
        return clearInducementsOrder(object.get());
    }

    /**
     * clear order=2 from inducement before adding role to repository
     */
    @Override
    protected PrismObject<RoleType> customizeRoleModifyFullName5(TestObject<RoleType> object) {
        return clearInducementsOrder(object.get());
    }

    private PrismObject<RoleType> clearInducementsOrder(PrismObject<RoleType> role) {
        role.asObjectable().getInducement().forEach(i -> i.setOrder(null));
        return role;
    }

    @Override
    protected String createSuspendPolicyIdentifier(TestObject<TaskType> object) throws CommonException {
        String oid = ROLE_ADD_10.oid;
        PrismObject<RoleType> role = getObject(RoleType.class, oid);
        List<AssignmentType> inducements = role.asObjectable().getInducement();
        Assertions.assertThat(inducements)
                .withFailMessage("Expected exactly one inducement in role %s, but found %d", oid, inducements.size())
                .hasSize(1);

        return PlainPolicyRuleIdentifier.of(oid, inducements.get(0).getId()).asString();
    }

    private void createVirtualAssignmentToRole(PrismObject<TaskType> target, TestObject<RoleType> role) {
        createVirtualAssignmentToRole(target, ActivityPath.empty(), role);
    }

    private void createVirtualAssignmentToRole(PrismObject<TaskType> target, ActivityPath activityPath, TestObject<RoleType> role) {
        ActivityDefinitionType def = target.asObjectable().getActivity();
        ActivityDefinitionType myDef = ActivityDefinitionUtil.findActivityDefinition(def, activityPath);
        Assertions.assertThat(myDef)
                .withFailMessage("No activity definition found for path %s in task %s", activityPath, target.getOid())
                .isNotNull();

        myDef.beginVirtualAssignments()
                .beginAssignment()
                .targetRef(role.ref());

        LOGGER.info("Created virtual assignment for {} to {}, {}", role, target, activityPath);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Execute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_ADD_10);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Simulate() {
        return t -> createVirtualAssignmentToRole(t, ROLE_ADD_10);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10SimulateExecute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_ADD_10);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Execute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_MODIFY_COST_CENTER_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Simulate() {
        return t -> createVirtualAssignmentToRole(t, ROLE_MODIFY_COST_CENTER_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5SimulateExecute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_MODIFY_COST_CENTER_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyFullName5SimulateExecute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_MODIFY_FULL_NAME_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconModifyFullName5SimulateExecute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_MODIFY_FULL_NAME_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconDelete5Simulate() {
        return t -> createVirtualAssignmentToRole(t, ROLE_DELETE_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconDelete5SimulateExecute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_DELETE_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconDelete5Execute() {
        return t -> createVirtualAssignmentToRole(t, ROLE_DELETE_5);
    }
}
