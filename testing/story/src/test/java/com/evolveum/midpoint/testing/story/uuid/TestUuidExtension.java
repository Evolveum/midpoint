package com.evolveum.midpoint.testing.story.uuid;
/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.opends.server.types.DirectoryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestUuidExtension extends AbstractUuidTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration-extension.xml");

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-extension.xml");

    public static final File ROLE_EMPLOYEE_FILE = new File(TEST_DIR, "role-employee.xml");
    public static final String ROLE_EMPLOYEE_OID = "b6b6b7a6-3aa0-11e9-b5e3-7fcaee07c7e0";
    public static final String ROLE_EMPLOYEE_NAME = "Employee";

    private static final String USER_RAPP_NAME = "rapp";
    private static final String USER_RAPP_GIVEN_NAME = "Rapp";
    private static final String USER_RAPP_FAMILY_NAME = "Scallion";

    private static final String USER_KATE_NAME = "kate";
    private static final String USER_KATE_GIVEN_NAME = "Kate";
    private static final String USER_KATE_FAMILY_NAME = "Capsize";

    protected String userRappOid;
    protected String userKateOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Role
        importObjectFromFile(ROLE_EMPLOYEE_FILE, initResult);
    }

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100AddUserRapp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_RAPP_NAME, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME, true);

        // WHEN
        addObject(user, task, result);

        // THEN
        assertSuccess(result);

        userRappOid = user.getOid();

        user = getUser(userRappOid);
        assertUser(user, USER_RAPP_NAME, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
    }

    @Test
    public void test101RappAssignRoleEmployee() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(userRappOid, ROLE_EMPLOYEE_OID, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userRappOid);
        assertUser(userAfter, USER_RAPP_NAME, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
        assertLdapEmployee(userAfter, USER_RAPP_NAME, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
    }

    // TODO: modify user, account should be modified

    @Test
    public void test107RappUnAssignRoleEmployee() throws Exception {
        // WHEN
        unassignRole(userRappOid, ROLE_EMPLOYEE_OID);

        // THEN
        PrismObject<UserType> userAfter = getUser(userRappOid);
        assertUser(userAfter, USER_RAPP_NAME, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
        assertNoLdapEmployee(userAfter);
    }

    /**
     * MID-5114
     */
    @Test
    public void test110AddKateWithRoleEmployee() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createEmployeeUser(USER_KATE_NAME, USER_KATE_GIVEN_NAME, USER_KATE_FAMILY_NAME, true);
        display("User before", user);

        // WHEN
        addObject(user, task, result);

        // THEN
        assertSuccess(result);
        userKateOid = user.getOid();

        PrismObject<UserType> userAfter = getUser(userKateOid);
        assertUser(userAfter, USER_KATE_NAME, USER_KATE_GIVEN_NAME, USER_KATE_FAMILY_NAME);
        assertLdapEmployee(userAfter, USER_KATE_NAME, USER_KATE_GIVEN_NAME, USER_KATE_FAMILY_NAME);
    }

    private void assertUser(PrismObject<UserType> user, String expectedName, String firstName, String lastName)  {
        display("User", user);
        String oid = user.getOid();
        PolyStringType name = user.asObjectable().getName();
        assertEquals("User name mismatch", expectedName, name.getOrig());

        String ship = user.getPropertyRealValue(PATH_EXTENSION_SHIP, String.class);
        assertEquals("User ship-OID mismatch", oid, ship);
        // TODO
    }

    private void assertLdapEmployee(PrismObject<UserType> user, String name, String firstName, String lastName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertLiveLinks(user, 1);
        assertAssignments(user, RoleType.class, 1);
        assertAssignedRole(user, ROLE_EMPLOYEE_OID);

        assertAccount(user, RESOURCE_OPENDJ_OID);
        PrismReferenceValue linkRef = getLiveLinkRef(user, RESOURCE_OPENDJ_OID);
        PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
        display("OpenDJ shadow linked to "+user, shadow);
        IntegrationTestTools.assertSecondaryIdentifier(shadow, "uid="+user.getOid()+",ou=people,dc=example,dc=com");
        // TODO: cn, sn, givenName

        // MID-5114
        assertAttribute(resourceOpenDj, shadow.asObjectable(), ATTR_DEPARTMENT_NUMBER, user.getOid());
    }

    private void assertNoLdapEmployee(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
        assertLiveLinks(user, 0);
        assertAssignments(user, RoleType.class, 0);
        assertNotAssignedRole(user, ROLE_EMPLOYEE_OID);

        openDJController.assertNoEntry("uid="+user.getOid()+",ou=people,dc=example,dc=com");
    }

    private PrismObject<UserType> createEmployeeUser(String name, String givenName, String familyName, boolean enabled) throws SchemaException {
        PrismObject<UserType> user = createUser(name, givenName, familyName, enabled);
        AssignmentType roleAssignment = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(ROLE_EMPLOYEE_OID);
        targetRef.setType(RoleType.COMPLEX_TYPE);
        roleAssignment.setTargetRef(targetRef);
        user.asObjectable().getAssignment().add(roleAssignment);
        return user;
    }

}
