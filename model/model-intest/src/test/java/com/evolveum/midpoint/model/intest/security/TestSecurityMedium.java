/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityMedium extends AbstractSecurityTest {

    protected static final File USER_EMPLOYEE_FRED_FILE = new File(TEST_DIR, "user-employee-fred.xml");
    protected static final String USER_EMPLOYEE_FRED_OID = "4e63d9a2-d575-11e9-9c19-cb0e0207a10e";

    protected static final File ROLE_EMPLOYEE_MANAGER_FILE = new File(TEST_DIR, "role-employee-manager.xml");
    protected static final String ROLE_EMPLOYEE_MANAGER_OID = "5549cb8e-d573-11e9-a61e-7f2eff22715a";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ARCHETYPE_EMPLOYEE_FILE, initResult);
        repoAddObjectFromFile(ROLE_EMPLOYEE_MANAGER_FILE, initResult);
    }

    protected static final int NUMBER_OF_IMPORTED_ROLES = 1;

    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
    }

    /**
     * Stay logged in as administrator. Make sure that our assumptions about
     * the users and roles are correct.
     */
    @Test
    public void test000Sanity() throws Exception {
        final String TEST_NAME = "test000Sanity";
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        // WHEN
        when();
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(RoleType.class, null, getNumberOfRoles());

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertReadAllowRaw(NUMBER_OF_ALL_USERS);
        assertAddAllow();
        assertAddAllowRaw();
        assertModifyAllow();
        assertDeleteAllow();

        assertGlobalStateUntouched();
    }

    /**
     * Baseline check. There are no employees yet. Therefore make sure employee manager sees nothing.
     */
    @Test
    public void test100AutzEmployeeManager() throws Exception {
        final String TEST_NAME = "test100AutzEmployeeManager";
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_MANAGER_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        assertGetDeny(UserType.class, USER_CHARLES_OID);

        assertSearch(UserType.class, null, 0);
        assertSearch(ObjectType.class, null, 0);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    /**
     * Adding completely new employee.
     * MID-5581
     */
    @Test
    public void test102AutzEmployeeManagerAddEmployee() throws Exception {
        final String TEST_NAME = "test102AutzEmployeeManagerAddEmployee";
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_MANAGER_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertAddAllow(USER_EMPLOYEE_FRED_FILE);

        assertSearch(UserType.class, null, 1);
        assertSearch(ObjectType.class, null, 1);
        assertSearch(OrgType.class, null, 0);

        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }


}
