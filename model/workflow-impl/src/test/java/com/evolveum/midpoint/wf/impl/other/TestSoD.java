/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import java.io.File;

import com.evolveum.midpoint.util.exception.CommonException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Testing approvals of role SoD: assigning roles that are in conflict.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSoD extends AbstractWfTestPolicy {

    private static final File TEST_DIR = new File("src/test/resources/sod");

    private static final TestObject<RoleType> METAROLE_CRIMINAL_EXCLUSION = TestObject.file(
            TEST_DIR, "metarole-criminal-exclusion.xml", "34d73991-8cbc-46e5-b8c2-b8b62029e711");
    private static final TestObject<RoleType> ROLE_JUDGE = TestObject.file(
            TEST_DIR, "role-judge.xml", "528f5ebb-5182-4f30-a975-d3531112ed4a");
    private static final TestObject<RoleType> ROLE_PIRATE =
            TestObject.file(TEST_DIR, "role-pirate.xml", "d99abcdf-7b29-4176-a8f7-9775b4b4c1d3");
    private static final TestObject<RoleType> ROLE_THIEF = TestObject.file(
            TEST_DIR, "role-thief.xml", "ee6a1809-a0ed-4983-a0b4-6eef24e8a76d");
    private static final TestObject<RoleType> ROLE_RESPECTABLE = TestObject.file(
            TEST_DIR, "role-respectable.xml", "4838ce2c-5250-4d9c-b5cc-b6b946852806");

    private static final TestObject<RoleType> ROLE_WATER = TestObject.file(
            TEST_DIR, "role-water.xml", "fd3dd8cf-24d7-46e7-9b73-e28ea74fc9ae");
    private static final TestObject<RoleType> ROLE_FIRE = TestObject.file(
            TEST_DIR, "role-fire.xml", "61d1db1b-41a7-44f3-9c1a-0605b489f955");
    private static final TestObject<RoleType> ROLE_OIL = TestObject.file(
            TEST_DIR, "role-oil.xml", "01f2d6d8-2a62-47c2-bcd9-a485dfc47708");

    private static final TestObject<UserType> USER_SOD_APPROVER = TestObject.file(
            TEST_DIR, "user-sod-approver.xml", "f15b45d6-f638-413f-9572-83554c7b3b88");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                METAROLE_CRIMINAL_EXCLUSION,
                ROLE_JUDGE, ROLE_PIRATE, ROLE_THIEF, ROLE_RESPECTABLE,
                ROLE_WATER, ROLE_FIRE, ROLE_OIL,
                USER_SOD_APPROVER);
    }

    /**
     * Assign Judge to jack. This should work without approvals.
     */
    @Test
    public void test010AssignRoleJudge() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when("role Judge is requested");
        assignRole(USER_JACK.oid, ROLE_JUDGE.oid, task, result);

        then("role Judge is assigned");
        assertUserAfter(USER_JACK.oid)
                .assignments()
                .assertRole(ROLE_JUDGE.oid);
    }

    /**
     * Assign Pirate to jack. This should trigger an approval.
     */
    @Test
    public void test020AssignRolePirate() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        String originalDescription = jack.asObjectable().getDescription();

        ObjectDelta<UserType> addPirateDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(ROLE_PIRATE.assignmentTo())
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("Pirate Judge")
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil.summarize(addPirateDelta, changeDescriptionDelta);

        when("role Pirate is requested");
        executeChanges(primaryDelta, null, task, result);

        then("it must be submitted to approval");
        // @formatter:off
        var workItem = assertReferencedCase(result)
                .subcases()
                .singleWithoutApprovalSchema()
                    .display()
                    .assertDeltasToApprove(changeDescriptionDelta)
                .end()
                .singleWithApprovalSchema()
                    .display()
                    .assertObjectRef(USER_JACK.ref())
                    .assertTargetRef(ROLE_PIRATE.ref())
                    .assertOpenApproval("Role \"Pirate\" excludes role \"Judge\"")
                    .assertDeltasToApprove(addPirateDelta)
                    .workItems()
                        .single()
                            .assertAssignees(USER_SOD_APPROVER.oid)
                            .getRealValue();
        // @formatter:on

        assertUser(USER_JACK.oid, "before approval")
                .assertDescription(originalDescription)
                .assignments()
                .assertNoRole(ROLE_PIRATE.oid);

        when("work item is approved");
        approveWorkItem(workItem, task, result);
        waitForCaseClose(getReferencedCaseOidRequired(result));

        then("user is updated, policy situation is set");
        // @formatter:off
        assertUserAfter(USER_JACK.oid)
                .assertDescription("Pirate Judge")
                .assignments()
                    .forRole(ROLE_PIRATE.oid)
                        .assertExclusionViolationSituation()
                    .end()
                    .forRole(ROLE_JUDGE.oid)
                        .assertExclusionViolationSituation();
        // @formatter:on
    }

    /**
     * Assign Respectable to jack. This should trigger an approval as well (because it implies a Thief).
     */
    @Test
    public void test030AssignRoleRespectable() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given("Pirate is unassigned from Jack");
        unassignRole(USER_JACK.oid, ROLE_PIRATE.oid, task, result);
        assertNotAssignedRole(USER_JACK.oid, ROLE_PIRATE.oid, result);

        testAddingConflictingRoleAssignment(
                USER_JACK, ROLE_JUDGE, ROLE_RESPECTABLE,
                "Role \"Thief\" (Respectable -> Thief) excludes role \"Judge\"",
                task, result);
    }

    /**
     * Checks that one-sided exclusion definitions are properly submitted to approvals and also recorded.
     *
     * MID-8269.
     */
    @Test
    public void test100OneSidedExclusion() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        testOneSidedExclusion(
                "test100-1", ROLE_WATER, ROLE_FIRE,
                "Role \"fire\" excludes role \"water\"",
                task, result);
        testOneSidedExclusion(
                "test100-2", ROLE_FIRE, ROLE_WATER,
                "Role \"fire\" excludes role \"water\"",
                task, result);
        testOneSidedExclusion(
                "test100-3", ROLE_WATER, ROLE_OIL,
                "Role \"oil\" excludes role \"water\"",
                task, result);
        testOneSidedExclusion(
                "test100-4", ROLE_OIL, ROLE_WATER,
                "Role \"oil\" excludes role \"water\"",
                task, result);
    }

    private void testOneSidedExclusion(
            String username, TestObject<RoleType> existingRole, TestObject<RoleType> newRole,
            String expectedCaseName, Task task, OperationResult result)
            throws CommonException {

        given("there is a user with a role");
        UserType userBean = new UserType()
                .name(username)
                .assignment(existingRole.assignmentTo());
        addObject(userBean, task, result);
        TestObject<UserType> user = TestObject.of(userBean);

        testAddingConflictingRoleAssignment(user, existingRole, newRole, expectedCaseName, task, result);
    }

    private void testAddingConflictingRoleAssignment(
            TestObject<UserType> user, TestObject<RoleType> existingRole, TestObject<RoleType> newRole,
            String expectedCaseName, Task task, OperationResult result) throws CommonException {

        // To be able to check do this repeatably
        result.clearAsynchronousOperationReferencesDeeply();

        when("role " + newRole + " is requested");
        assignRole(user.oid, newRole.oid, task, result);

        then("it must be submitted to approval");
        // @formatter:off
        var workItem = assertReferencedCase(result)
                .subcases()
                .singleWithApprovalSchema()
                    .display()
                    .assertObjectRef(user.ref())
                    .assertTargetRef(newRole.ref())
                    .assertOpenApproval(expectedCaseName)
                    .workItems()
                        .single()
                            .assertAssignees(USER_SOD_APPROVER.oid)
                            .getRealValue();
        // @formatter:on

        assertUser(user.oid, "before approval")
                .assignments()
                .assertNoRole(newRole.oid);

        when("work item is approved");
        approveWorkItem(workItem, task, result);
        waitForCaseClose(getReferencedCaseOidRequired(result));

        then("user is updated, policy situation is set");
        // @formatter:off
        assertUserAfter(user.oid)
                .assignments()
                    .forRole(newRole.oid)
                        .assertExclusionViolationSituation()
                    .end()
                    .forRole(existingRole.oid)
                        .assertExclusionViolationSituation();
        // @formatter:on
    }
}
