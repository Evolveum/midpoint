/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityMedium extends AbstractInitializedSecurityTest {

    private static final File USER_EMPLOYEE_FRED_FILE = new File(TEST_DIR, "user-employee-fred.xml");

    private static final File ROLE_EMPLOYEE_MANAGER_FILE = new File(TEST_DIR, "role-employee-manager.xml");
    private static final String ROLE_EMPLOYEE_MANAGER_OID = "5549cb8e-d573-11e9-a61e-7f2eff22715a";
    private static final File ROLE_ARCHETYPE_REF_REVIEWER_FILE = new File(TEST_DIR, "role-archetype-ref-reviewer.xml");
    private static final String ROLE_ARCHETYPE_REF_REVIEWER_OID = "5549cb8e-d573-11e9-a61e-7f2e1232715a";

    private static final TestObject<RoleType> ROLE_RESOURCE_NO_SUPER =
            TestObject.file(TEST_DIR, "role-resource-no-super.xml", "127e6393-371d-4a15-952f-e454748bfc09");
    private static final TestObject<ResourceType> RESOURCE_NO_SUPER =
            TestObject.file(TEST_DIR, "resource-no-super.xml", "801c9610-5cb7-411f-af3f-a14b303154ca");
    private static final TestObject<ResourceType> RESOURCE_WITH_SUPER =
            TestObject.file(TEST_DIR, "resource-with-super.xml", "9e785491-7207-4288-8d1e-7f7a21a6455f");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ARCHETYPE_EMPLOYEE_FILE, initResult);
        repoAddObjectFromFile(ROLE_EMPLOYEE_MANAGER_FILE, initResult);
        repoAddObjectFromFile(ROLE_ARCHETYPE_REF_REVIEWER_FILE, initResult);
        repoAdd(ROLE_RESOURCE_NO_SUPER, initResult);
    }

    private static final int NUMBER_OF_IMPORTED_ROLES = 3;

    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
    }

    /**
     * Stay logged in as administrator. Make sure that our assumptions about
     * the users and roles are correct.
     */
    @Test
    public void test000Sanity() throws Exception {
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
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_MANAGER_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK.oid);
        assertGetDeny(UserType.class, USER_CHARLES.oid);

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

    @Test
    public void test200AutzAddResource() throws CommonException, IOException {
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_RESOURCE_NO_SUPER.oid);
        login(USER_JACK_USERNAME);

        then("adding resource with no 'super' is allowed");
        assertAddAllow(RESOURCE_NO_SUPER, ModelExecuteOptions.create().setIsImport());

        then("adding resource with 'super' is denied");
        assertAddDeny(RESOURCE_WITH_SUPER, ModelExecuteOptions.create().setIsImport());

        then("modifying resource item other than 'super' is allowed");
        assertModifyAllow(ResourceType.class, RESOURCE_NO_SUPER.oid, ResourceType.F_DESCRIPTION, "anything");

        String randomOid = "0a4b3d14-7c39-4117-b029-babacf7b254e";

        then("modifying resource 'super' item is denied");
        assertModifyDeny(
                ResourceType.class,
                RESOURCE_NO_SUPER.oid,
                ResourceType.F_SUPER,
                new SuperResourceDeclarationType()
                        .resourceRef(randomOid, ResourceType.COMPLEX_TYPE));

        then("modifying resource 'super/resourceRef' item is denied");
        assertModifyDeny(
                ResourceType.class,
                RESOURCE_NO_SUPER.oid,
                ResourceType.F_SUPER.append(SuperResourceDeclarationType.F_RESOURCE_REF),
                new ObjectReferenceType().oid(randomOid).type(ResourceType.COMPLEX_TYPE));
    }

    // #10683; USER_EMPLOYEE_FRED_FILE should be already imported in test102AutzEmployeeManagerAddEmployee
    @Test
    public void test300ArchetypeRefAuthWithUndefinedType() throws Exception {
        // GIVEN
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ARCHETYPE_REF_REVIEWER_OID);
        login(USER_JACK_USERNAME);

        // THEN
        then();
        assertSearch(UserType.class, null, 1);
        assertSearch(AccessCertificationCampaignType.class, null, 4);
        assertSearch(ObjectType.class, null, 4);    //this should find 4 campaign objects
    }

}
