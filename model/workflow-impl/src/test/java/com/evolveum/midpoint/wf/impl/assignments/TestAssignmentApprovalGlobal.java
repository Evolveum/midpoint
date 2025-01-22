/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.assignments;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

public class TestAssignmentApprovalGlobal extends AbstractTestAssignmentApproval {

    private static final File SYSTEM_CONFIGURATION_GLOBAL_FILE = new File(TEST_RESOURCE_DIR, "system-configuration-global.xml");

    // Role15 has its approver but there is also a global policy rule that prevents it from being assigned.
    private static final TestObject<ObjectType> ROLE15 = TestObject.file(
            TEST_RESOURCE_DIR, "role-role15.xml", "00000001-d34d-b33f-f00d-000000000015");
    private static final TestObject<ObjectType> USER_LEAD15 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead15.xml", "00000001-d34d-b33f-f00d-a00000000015");
    private static final TestObject<ObjectType> ROLE_AUTO_ASSIGNED = TestObject.file(
            TEST_RESOURCE_DIR, "role-auto-assigned.xml", "a5a3c5d0-d1ba-45df-a34b-0e0e78212694");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(ROLE15, initResult);
        addAndRecompute(USER_LEAD15, initTask, initResult);

        initTestObjects(initTask, initResult,
                ROLE_AUTO_ASSIGNED);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_GLOBAL_FILE;
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected TestObject<RoleType> getRole(int number) {
        switch (number) {
            case 1:
                return ROLE1;
            case 2:
                return ROLE2;
            case 3:
                return ROLE3;
            case 4:
                return ROLE4;
            case 10:
                return ROLE10;
            default:
                throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }

    /**
     * MID-3836
     */
    public void test300ApprovalAndEnforce() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        task.setOwner(userAdministrator);
        OperationResult result = getTestOperationResult();

        try {
            assignRole(USER_JACK.oid, ROLE15.oid, task, result);
            fail("Unexpected success");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception: " + e);
        }
        List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), null, task, result);
        display("current work items", currentWorkItems);
        assertEquals("Wrong # of current work items", 0, currentWorkItems.size());
    }

    /**
     * Tests preview and execution of an auto-assigned role with approvals - when adding a user.
     * (Actually, there should be no approvals.)
     *
     * MID-10345.
     */
    @Test
    public void test310AutoAssignedRoleOnUserAdd() throws Exception {
        login(userAdministrator);
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();
        var orgUnit = "test";

        given("a user");
        var user = new UserType()
                .name(userName)
                .organizationalUnit(orgUnit);

        when("the user is added so that it gets the auto-assigned role (in preview mode)");
        ObjectDelta<UserType> delta = user.asPrismObject().createAddDelta();
        var modelContext = previewChanges(delta.clone(), previewOptions(), task, result);

        then("everything is OK");
        assertSuccess(result);
        assertPreviewContext(modelContext)
                .focusContext()
                .summarySecondaryDelta()
                .assertModify()
                .assertModificationPathsNonExclusive(UserType.F_ASSIGNMENT, UserType.F_ROLE_MEMBERSHIP_REF);

        when("the user is added so that it gets the auto-assigned role (in real execution mode)");
        executeChanges(delta.clone(), null, task, result);

        then("everything is OK, user has the assignment");
        assertSuccessRepeatedly(result);
        assertUserAfterByUsername(userName)
                .assertOrganizationalUnits(orgUnit)
                .assignments()
                .single()
                .assertTargetOid(ROLE_AUTO_ASSIGNED.oid);
    }

    /*
     * Tests preview and execution of an auto-assigned role with approvals - when modifying a user.
     * (Actually, there should be no approvals.)
     *
     * MID-10345.
     */
    @Test
    public void test320AutoAssignedRoleOnUserModify() throws Exception {
        login(userAdministrator);
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();
        var orgUnit = "test";

        given("a user");
        var user = new UserType()
                .name(userName);
        var oid = addObject(user, task, result);

        when("the user is modified so that it gets the auto-assigned role (in preview mode)");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ORGANIZATIONAL_UNIT)
                .replace(PolyString.fromOrig(orgUnit))
                .asObjectDelta(oid);
        var modelContext = previewChanges(delta.clone(), previewOptions(), task, result);

        then("everything is OK");
        assertSuccess(result);
        assertPreviewContext(modelContext)
                .focusContext()
                .summarySecondaryDelta()
                .assertModify()
                .assertModificationPathsNonExclusive(UserType.F_ASSIGNMENT, UserType.F_ROLE_MEMBERSHIP_REF);

        when("the user is modified so that it gets the auto-assigned role (in real execution mode)");
        executeChanges(delta.clone(), null, task, result);

        then("everything is OK, user has the assignment");
        assertSuccessRepeatedly(result);
        assertUserAfter(oid)
                .assertOrganizationalUnits(orgUnit)
                .assignments()
                .single()
                .assertTargetOid(ROLE_AUTO_ASSIGNED.oid);
    }

    private ModelExecuteOptions previewOptions() {
        return ModelExecuteOptions.create()
                .partialProcessing(new PartialProcessingOptionsType()
                        .approvals(PartialProcessingTypeType.PROCESS)); // This is not enabled by default for preview.
    }
}
