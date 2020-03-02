/*
 * Copyright (c) 2019 michael.gruber@wwk.de, Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


/**
 * testing inducements, no resources, no accounts in use.
 * role "processor" is assigned to user, it contains inducements for role1, role2, role3 having following conditions
 *
 * role1: no condition
 * role2: should not be induced when description of user equals "NO"
 * role3: should not be induced when user is member of role named "lock" (directly or indirectly, therefore condition runs against roleMembershipRef)
 */

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInducement extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "inducement");

    public static final File ROLE_ROLE1_FILE = new File(TEST_DIR, "role-role1.xml");
    public static final String ROLE_ROLE1_OID = "10000000-0000-0000-0000-100000000001";

    public static final File ROLE_ROLE2_FILE = new File(TEST_DIR, "role-role2.xml");
    public static final String ROLE_ROLE2_OID = "10000000-0000-0000-0000-100000000002";

    public static final File ROLE_ROLE3_FILE = new File(TEST_DIR, "role-role3.xml");
    public static final String ROLE_ROLE3_OID = "10000000-0000-0000-0000-100000000003";

    public static final File ROLE_LOCK_FILE = new File(TEST_DIR, "role-lock.xml");
    public static final String ROLE_LOCK_OID = "10000000-0000-0000-0000-10000000lock";

    public static final File ROLE_PROCESSOR_FILE = new File(TEST_DIR, "role-processor.xml");
    public static final String ROLE_PROCESSOR_OID = "10000000-0000-0000-0000-100processor";

    public static final File USER_SIMPLE_FILE = new File(TEST_DIR, "user-simple.xml");
    public static final String USER_SIMPLE_OID = "10000000-0000-0000-0001-100000simple";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Roles
        importObjectFromFile(ROLE_ROLE1_FILE, initResult);
        importObjectFromFile(ROLE_ROLE2_FILE, initResult);
        importObjectFromFile(ROLE_ROLE3_FILE, initResult);
        importObjectFromFile(ROLE_LOCK_FILE, initResult);
        importObjectFromFile(ROLE_PROCESSOR_FILE, initResult);

        //User
        importObjectFromFile(USER_SIMPLE_FILE, initResult);

    }

    @Test
    public void test000Sanity() throws Exception {
        final String TEST_NAME = "test000Sanity";
        //no resource, no extension definition
        //anything to check?

    }

    /**
     * assign role "processor".
     * role "processor" contains inducements for role1, role2, role3
     */
    @Test
    public void test010InducementConditionsTrue() throws Exception {
        final String TEST_NAME = "test010InducementConditionsTrue";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        assignRole(USER_SIMPLE_OID, ROLE_PROCESSOR_OID, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SIMPLE_OID);
        display("User simple after role assignment", user);

        assertAssignedRole(user, ROLE_PROCESSOR_OID);
        assertNotAssignedRole(user, ROLE_LOCK_OID);
        assertNotAssignedRole(user, ROLE_ROLE1_OID);
        assertNotAssignedRole(user, ROLE_ROLE2_OID);
        assertNotAssignedRole(user, ROLE_ROLE3_OID);
        assertRoleMembershipRef(user, ROLE_PROCESSOR_OID, ROLE_ROLE1_OID, ROLE_ROLE2_OID, ROLE_ROLE3_OID);
    }

    /**
     * modify description of user
     * condition in "processor" for inducing role2 returns false if description equals "NO"
     */
    @Test
    public void test020InducementRole2ConditionFalse() throws Exception {
        final String TEST_NAME = "test020InducementRole2ConditionFalse";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        modifyUserReplace(USER_SIMPLE_OID, UserType.F_DESCRIPTION, task, result, "NO");

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SIMPLE_OID);
        display("User simple having description 'NO'", user);

        assertUserProperty(USER_SIMPLE_OID, new QName("description"), "NO");
        assertAssignedRole(user, ROLE_PROCESSOR_OID);
        assertNotAssignedRole(user, ROLE_LOCK_OID);
        assertNotAssignedRole(user, ROLE_ROLE1_OID);
        assertNotAssignedRole(user, ROLE_ROLE2_OID);
        assertNotAssignedRole(user, ROLE_ROLE3_OID);
        assertRoleMembershipRef(user, ROLE_PROCESSOR_OID, ROLE_ROLE1_OID, ROLE_ROLE3_OID);
    }

    /**
     * assign role "lock" to user
     * condition in "processor" for inducing role3 returns false if lock is contained in rolemembership
     */
    @Test
    public void test030InducementRole3ConditionFalse() throws Exception {
        final String TEST_NAME = "test030InducementRole3ConditionFalse";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        assignRole(USER_SIMPLE_OID, ROLE_LOCK_OID, task, result);


        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SIMPLE_OID);
        display("User simple having role lock assigned'", user);

        assertAssignedRole(user, ROLE_PROCESSOR_OID);
        assertAssignedRole(user, ROLE_LOCK_OID);
        assertNotAssignedRole(user, ROLE_ROLE1_OID);
        assertNotAssignedRole(user, ROLE_ROLE2_OID);
        assertNotAssignedRole(user, ROLE_ROLE3_OID);
        assertRoleMembershipRef(user, ROLE_PROCESSOR_OID, ROLE_LOCK_OID, ROLE_ROLE1_OID);
    }

    /**
     * same as Test30, just recomputed again
     */
    @Test
    public void test040Recomputed() throws Exception {
        final String TEST_NAME = "test040Recomputed";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        recomputeUser(USER_SIMPLE_OID);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SIMPLE_OID);
        display("User simple having role lock assigned'", user);

        assertAssignedRole(user, ROLE_PROCESSOR_OID);
        assertAssignedRole(user, ROLE_LOCK_OID);
        assertNotAssignedRole(user, ROLE_ROLE1_OID);
        assertNotAssignedRole(user, ROLE_ROLE2_OID);
        assertNotAssignedRole(user, ROLE_ROLE3_OID);
        assertRoleMembershipRef(user, ROLE_PROCESSOR_OID, ROLE_LOCK_OID, ROLE_ROLE1_OID);
    }

    /**
     * Unassign role "lock" from user
     */
    @Test
    public void test050InducementRole3ConditionTrue() throws Exception {
        final String TEST_NAME = "test050InducementRole3ConditionTrue";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        unassignRole(USER_SIMPLE_OID, ROLE_LOCK_OID, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SIMPLE_OID);
        display("User simple having role lock unassigned'", user);

        assertAssignedRole(user, ROLE_PROCESSOR_OID);
        assertNotAssignedRole(user, ROLE_LOCK_OID);
        assertNotAssignedRole(user, ROLE_ROLE1_OID);
        assertNotAssignedRole(user, ROLE_ROLE2_OID);
        assertNotAssignedRole(user, ROLE_ROLE3_OID);
        assertRoleMembershipRef(user, ROLE_PROCESSOR_OID, ROLE_ROLE1_OID, ROLE_ROLE3_OID);
    }

    /**
     * same as Test50, just recomputed again
     */
    @Test
    public void test060Recomputed() throws Exception {
        final String TEST_NAME = "test060Recomputed";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        recomputeUser(USER_SIMPLE_OID);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SIMPLE_OID);
        display("User simple having role lock unassigned'", user);

        assertAssignedRole(user, ROLE_PROCESSOR_OID);
        assertNotAssignedRole(user, ROLE_LOCK_OID);
        assertNotAssignedRole(user, ROLE_ROLE1_OID);
        assertNotAssignedRole(user, ROLE_ROLE2_OID);
        assertNotAssignedRole(user, ROLE_ROLE3_OID);
        assertRoleMembershipRef(user, ROLE_PROCESSOR_OID, ROLE_ROLE1_OID, ROLE_ROLE3_OID);
    }

    @Test
    public void test070DeleteUser() throws Exception {
        final String TEST_NAME = "test070DeleteUser";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        deleteObject(UserType.class, USER_SIMPLE_OID, task, result);

        // THEN
        assertSuccess(result);

        assertNoObject(UserType.class, USER_SIMPLE_OID, task, result);
    }
}
