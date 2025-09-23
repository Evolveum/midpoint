/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.management.relation.Role;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ActivityCustomization;

import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.query.OrgFilter;

import com.evolveum.midpoint.schema.constants.RelationTypes;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityAdvanced extends AbstractInitializedSecurityTest {

    private static final String AUTHORIZATION_ACTION_WORKITEMS = "http://midpoint.evolveum.com/xml/ns/public/security/authorization-ui-3#myWorkItems";
    private static final String BIG_BADA_BOOM = "bigBadaBoom";
    private static final String HUGE_BADA_BOOM = "hugeBadaBoom";
    private static final String FIRST_RULE = "firstRule";

    private static final File RESOURCE_DUMMY_VAULT_FILE = new File(TEST_DIR, "resource-dummy-vault.xml");
    private static final String RESOURCE_DUMMY_VAULT_OID = "84a420cc-2904-11e8-862b-0fc0d7ab7174";
    private static final String RESOURCE_DUMMY_VAULT_NAME = "vault";

    private static final File ROLE_VAULT_DWELLER_FILE = new File(TEST_DIR, "role-vault-dweller.xml");
    private static final String ROLE_VAULT_DWELLER_OID = "8d8471f4-2906-11e8-9078-4f2b205aa01d";

    private static final File ROLE_READ_ROLE_MEMBERS_FILE = new File(TEST_DIR, "role-read-role-members.xml");
    private static final String ROLE_READ_ROLE_MEMBERS_OID = "40df00e8-3efc-11e7-8d18-7b955ccb96a1";

    private static final File ROLE_READ_ROLE_MEMBERS_WRONG_FILE = new File(TEST_DIR, "role-read-role-members-wrong.xml");
    private static final String ROLE_READ_ROLE_MEMBERS_WRONG_OID = "8418e248-3efc-11e7-a546-931a90cb8ee3";

    private static final File ROLE_READ_ROLE_MEMBERS_NONE_FILE = new File(TEST_DIR, "role-read-role-members-none.xml");
    private static final String ROLE_READ_ROLE_MEMBERS_NONE_OID = "9e93dfb2-3eff-11e7-b56b-1b0e35f837fc";

    private static final File ROLE_ROLE_ADMINISTRATOR_FILE = new File(TEST_DIR, "role-role-administrator.xml");
    private static final String ROLE_ROLE_ADMINISTRATOR_OID = "b63ee91e-020c-11e9-a7c2-df4b9f00f209";

    private static final File ROLE_LIMITED_ROLE_ADMINISTRATOR_FILE = new File(TEST_DIR, "role-limited-role-administrator.xml");
    private static final String ROLE_LIMITED_ROLE_ADMINISTRATOR_OID = "ce67b472-e5a6-11e7-98c3-174355334559";

    private static final File ROLE_LIMITED_READ_ROLE_ADMINISTRATOR_FILE = new File(TEST_DIR, "role-limited-read-role-administrator.xml");
    private static final String ROLE_LIMITED_READ_ROLE_ADMINISTRATOR_OID = "b9fcce10-050d-11e8-b668-eb75ab96577d";

    private static final File ROLE_EXCLUSION_PIRATE_FILE = new File(TEST_DIR, "role-exclusion-pirate.xml");
    private static final String ROLE_EXCLUSION_PIRATE_OID = "cf60ec66-e5a8-11e7-a997-ab32b7ec5fdb";

    private static final File ROLE_MAXASSIGNEES_10_FILE = new File(TEST_DIR, "role-maxassignees-10.xml");
    private static final String ROLE_MAXASSIGNEES_10_OID = "09dadf60-f6f1-11e7-8223-a72f04f867e7";

    private static final File ROLE_MODIFY_POLICY_EXCEPTION_FILE = new File(TEST_DIR, "role-modify-policy-exception.xml");
    private static final String ROLE_MODIFY_POLICY_EXCEPTION_OID = "09e9acde-f787-11e7-987c-13212be79c7d";

    private static final File ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_FILE = new File(TEST_DIR, "role-modify-policy-exception-situation.xml");
    private static final String ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_OID = "45bee61c-f79f-11e7-a2a7-27ade881c9e0";

    private static final File ROLE_MODIFY_DESCRIPTION_FILE = new File(TEST_DIR, "role-modify-description.xml");
    private static final String ROLE_MODIFY_DESCRIPTION_OID = "1a0616e4-f79a-11e7-80c9-d77b403e1a81";

    private static final File ROLE_PROP_EXCEPT_ASSIGNMENT_FILE = new File(TEST_DIR, "role-prop-except-assignment.xml");
    private static final String ROLE_PROP_EXCEPT_ASSIGNMENT_OID = "bc0f3bfe-029f-11e8-995d-273b6606fd79";

    private static final File ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_FILE = new File(TEST_DIR, "role-prop-except-administrative-status.xml");
    private static final String ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID = "cc549256-02a5-11e8-994e-43c307e2a819";

    private static final File ROLE_PROP_SUBTYPE_FILE = new File(TEST_DIR, "role-prop-subtype.xml");
    private static final String ROLE_PROP_SUBTYPE_OID = "0a841bcc-c255-11e8-bd03-d72f34cdd7f8";

    private static final File ROLE_PROP_SUBTYPE_ESCAPE_FILE = new File(TEST_DIR, "role-prop-subtype-escape.xml");
    private static final String ROLE_PROP_SUBTYPE_ESCAPE_OID = "bdf18bb2-c314-11e8-8e99-1709836f1462";

    private static final File ROLE_ASSIGN_ORG_FILE = new File(TEST_DIR, "role-assign-org.xml");
    private static final String ROLE_ASSIGN_ORG_OID = "be96f834-2dbb-11e8-b29d-7f5de07e7995";

    private static final File ROLE_READ_ORG_EXEC_FILE = new File(TEST_DIR, "role-read-org-exec.xml");
    private static final String ROLE_READ_ORG_EXEC_OID = "1ac39d34-e675-11e8-a1ec-37748272d526";

    private static final File ROLE_READ_RESOURCE_OPERATIONAL_STATE_FILE = new File(TEST_DIR, "role-read-resource-operational-state.xml");
    private static final String ROLE_READ_RESOURCE_OPERATIONAL_STATE_OID = "18f17721-63e1-42cf-abaf-8a50a04e639f";

    private static final TestObject<RoleType> ROLE_REQUESTER =
            TestObject.file(TEST_DIR, "role-requester.xml", "40000000-1000-0000-0000-000000000000");
    private static final TestObject<RoleType> ROLE_REQUESTABLE_HIGH_RISK =
            TestObject.file(TEST_DIR, "role-requestable-high-risk.xml", "20000000-1000-0000-0000-000000000003");
    private static final TestObject<ServiceType> SERVICE_REQUESTABLE_HIGH_RISK =
            TestObject.file(TEST_DIR, "service-requestable-high-risk.xml", "20000000-1000-0000-0000-000000000004");
    private static final TestObject<ServiceType> SERVICE_REQUESTABLE_LOW_RISK =
            TestObject.file(TEST_DIR, "service-requestable-low-risk.xml", "629bafd6-8b5e-4a7c-94fa-36813984c5c3");

    private static final TestObject<RoleType> ROLE_READ_TASK_STATUS = TestObject.file(TEST_DIR, "role-read-task-status.xml", "bc2d0900-ac17-40c1-acf8-eb5466995aae");
    private static final TestObject<TaskType> TASK_DUMMY = TestObject.file(TEST_DIR, "task-dummy.xml", "89bf08ec-c5b8-4641-95ca-37559c1f3896");
    private static final TestObject<RoleType> ROLE_MANY_SHADOW_OWNER_AUTZ = TestObject.file(TEST_DIR, "role-many-shadow-owner-autz.xml", "c8c99194-3e5c-439b-bf98-c71146d3e1b5");

    private static final TestObject<TaskType> TASK_TEMPLATE_DUMMY = TestObject.file(
            TEST_DIR, "task-template-dummy.xml", "cb97aba7-c581-4f1e-a099-255a63530655");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_VAULT_NAME,
                RESOURCE_DUMMY_VAULT_FILE, RESOURCE_DUMMY_VAULT_OID, initTask, initResult);

        repoAddObjectFromFile(ROLE_VAULT_DWELLER_FILE, initResult);
        repoAddObjectFromFile(ROLE_ROLE_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_LIMITED_ROLE_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_LIMITED_READ_ROLE_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_MAXASSIGNEES_10_FILE, initResult);
        repoAddObjectFromFile(ROLE_MODIFY_POLICY_EXCEPTION_FILE, initResult);
        repoAddObjectFromFile(ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_FILE, initResult);
        repoAddObjectFromFile(ROLE_MODIFY_DESCRIPTION_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_EXCEPT_ASSIGNMENT_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_SUBTYPE_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_SUBTYPE_ESCAPE_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_ORG_FILE, initResult);
        repoAdd(ROLE_END_USER_WITH_PRIVACY, initResult);
        repoAddObjectFromFile(ROLE_READ_ROLE_MEMBERS_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_ROLE_MEMBERS_WRONG_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_ROLE_MEMBERS_NONE_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_ORG_EXEC_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_RESOURCE_OPERATIONAL_STATE_FILE, initResult);
        repoAdd(ROLE_REQUESTER, initResult);
        repoAdd(ROLE_REQUESTABLE_HIGH_RISK, initResult);
        repoAdd(SERVICE_REQUESTABLE_LOW_RISK, initResult);
        repoAdd(SERVICE_REQUESTABLE_HIGH_RISK, initResult);
        repoAdd(ROLE_READ_TASK_STATUS, initResult);
        repoAdd(TASK_DUMMY, initResult);
        repoAdd(ROLE_MANY_SHADOW_OWNER_AUTZ, initResult);

        initTestObjects(initTask, initResult,
                TASK_TEMPLATE_DUMMY); // intentionally in non-raw mode
    }

    private static final int NUMBER_OF_IMPORTED_ROLES = 23;
    private static final int NUMBER_OF_IMPORTED_TASKS = 2;

    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
    }

    protected int getNumberOfTasks() {
        return super.getNumberOfTasks() + NUMBER_OF_IMPORTED_TASKS;
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
     * Simple end-user password change. But clear Jack's credentials before
     * the change. Make sure all password metadata is set correctly.
     * This also sets the stage for following persona tests.
     * <p>
     * MID-4830
     */
    @Test
    public void test080AutzJackEndUserPassword() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_END_USER.oid);

        clearUserPassword(USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User with cleared password", user);
        assertAssignments(user, 1);
        assertLiveLinks(user, 0);
        assertUserNoPassword(user);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        assertAllow("set jack's password",
                (task, result) -> modifyUserSetPassword(USER_JACK_OID, "nbusr123", task, result));

        // THEN
        then();

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        user = getUser(USER_JACK_OID);
        display("user after password change", user);
        PasswordType passwordType = assertUserPassword(user, "nbusr123");
        MetadataType metadata = passwordType.getMetadata();
        assertNotNull("No password metadata", metadata);
        assertMetadata("password metadata", metadata, true, false, startTs, endTs, USER_JACK_OID, SchemaConstants.CHANNEL_USER_URI);

        assertGlobalStateUntouched();
    }

    @Test
    public void test100AutzJackPersonaManagement() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT.oid);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK.oid);
        assertGetDeny(UserType.class, USER_CHARLES.oid);

        assertSearch(UserType.class, null, 1);
        assertSearch(ObjectType.class, null, 1);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    @Test
    public void test102AutzLechuckPersonaManagement() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_LECHUCK.oid, 1);
        assignRole(USER_LECHUCK.oid, ROLE_PERSONA_MANAGEMENT.oid);
        login(USER_LECHUCK);

        // WHEN
        when();

        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK.oid);
        assertGetAllow(UserType.class, USER_CHARLES.oid);

//        TODO: MID-3899
//        assertSearch(UserType.class, null, 2);
//        assertSearch(ObjectType.class, null, 2);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    /**
     * MID-4830
     */
    @Test
    public void test110AutzJackPersonaAdmin() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT.oid);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertAllow("assign application role 1 to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN.oid, task, result));

        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        display("User jack after persona assign", userJack);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK.oid);
        assertGetDeny(UserType.class, USER_CHARLES.oid);

        assertPersonaLinks(userJack, 1);
        String personaJackOid = userJack.asObjectable().getPersonaRef().get(0).getOid();

        PrismObject<UserType> personaJack = assertGetAllow(UserType.class, personaJackOid);
        display("Persona jack", personaJack);
        assertEquals("Wrong jack persona givenName before change", USER_JACK_GIVEN_NAME, personaJack.asObjectable().getGivenName().getOrig());

//      TODO: MID-3899
//      assertSearch(UserType.class, null, 2);
//      assertSearch(ObjectType.class, null, 2);
        assertSearch(OrgType.class, null, 0);

        assertAllow("modify jack givenName",
                (task, result) -> modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result,
                        PolyString.fromOrig(USER_JACK_GIVEN_NAME_NEW)));

        userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertEquals("Wrong jack givenName after change", USER_JACK_GIVEN_NAME_NEW, userJack.asObjectable().getGivenName().getOrig());

        personaJack = assertGetAllow(UserType.class, personaJackOid);
        assertEquals("Wrong jack persona givenName after change", USER_JACK_GIVEN_NAME_NEW, personaJack.asObjectable().getGivenName().getOrig());

        assertAllow("unassign application role 1 to jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN.oid, task, result));

        userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertPersonaLinks(userJack, 0);

        assertNoObject(UserType.class, personaJackOid);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    @Test
    public void test120AutzJackDelegator() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_DELEGATOR.oid);

        and("barbossa has three unrelated assignments, invisible to jack");
        assignRole(USER_BARBOSSA_OID, ROLE_ORDINARY.oid);
        assignDeputy(USER_BARBOSSA_OID, USER_DRAKE_OID, task, result);
        assignDeputy(USER_BARBOSSA_OID, USER_ADMINISTRATOR_OID, TestSecurityAdvanced::disableAssignment, task, result);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        assertAssignedRole(userJack, ROLE_DELEGATOR.oid);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa); // there are other assignments but jack does not see them

        assertDeny("assign business role to jack",
                (lTask, lResult) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, lTask, lResult));

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        // Wrong direction. It should NOT work.
        assertDeny("delegate from Barbossa to Jack",
                (lTask, lResult) -> assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, lTask, lResult));

        // Good direction
        assertAllow("delegate to Barbossa",
                (lTask, lResult) -> assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, lTask, lResult));

        userJack = getUser(USER_JACK_OID);
        display("Jack delegator", userJack);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1); // jack sees only a single assignment
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);
        assertDelegatedRef(userBarbossa, USER_JACK_OID);

        assertDeputySearchViaDelegatedRef(USER_JACK_OID, USER_BARBOSSA_OID);
        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID, USER_BARBOSSA_OID);

        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        login(USER_JACK_USERNAME);
        // WHEN
        when();
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa",
                (lTask, lResult) -> unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, lTask, lResult));

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        assertGlobalStateUntouched();

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertDeny("delegate to Jack",
                (lTask, lResult) -> assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, lTask, lResult));

        assertDeny("delegate from Jack to Barbossa",
                (lTask, lResult) -> assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, lTask, lResult));

        assertGlobalStateUntouched();

        // cleanup
        login(USER_ADMINISTRATOR_USERNAME);
        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertAssignments(userBarbossa, 3);
        unassignRole(USER_BARBOSSA_OID, ROLE_ORDINARY.oid);
        unassignDeputy(USER_BARBOSSA_OID, USER_DRAKE_OID, task, result);
        unassignDeputy(USER_BARBOSSA_OID, USER_ADMINISTRATOR_OID, TestSecurityAdvanced::disableAssignment, task, result);
    }

    private static void disableAssignment(AssignmentType a) {
        a.setActivation(new ActivationType().administrativeStatus(ActivationStatusType.DISABLED));
    }

    /**
     * Assign a deputy, but this time with validFrom and validTo set to the future.
     * The delegator role does NOT allow access to inactive delegations.
     * MID-4172
     */
    @Test
    public void test122AutzJackDelegatorValidity() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_DELEGATOR.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        assertAssignedRole(userJack, ROLE_DELEGATOR.oid);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(XmlTypeConverter.addDuration(startTs, "PT2H"));
        activationType.setValidTo(XmlTypeConverter.addDuration(startTs, "P1D"));

        // Good direction
        assertAllow("delegate to Barbossa",
                (task, result) -> assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID,
                        assignment -> assignment.setActivation(activationType), task, result));

        userJack = getUser(USER_JACK_OID);
        display("Jack delegator", userJack);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        // Delegation is not active yet. Therefore jack cannot see it.
        assertAssignments(userBarbossa, 0);

        assertDeputySearchViaDelegatedRef(USER_JACK_OID /* nothing */);
        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID, USER_BARBOSSA_OID); // WRONG!!!
//        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID /* nothing */);

        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        // Delegation is not active yet. No access.
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        clockForward("PT3H");

        login(USER_ADMINISTRATOR_USERNAME);
        recomputeUser(USER_BARBOSSA_OID);

        // Delegation is active now

        login(USER_JACK_USERNAME);
        // WHEN

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);

        assertDeputySearchViaDelegatedRef(USER_JACK_OID, USER_BARBOSSA_OID);
        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID, USER_BARBOSSA_OID);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        clockForward("P1D");

        login(USER_ADMINISTRATOR_USERNAME);
        recomputeUser(USER_BARBOSSA_OID);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        // Delegation is not active any more. No access.
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        login(USER_JACK_USERNAME);
        // WHEN
        when();
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa",
                (task, result) -> unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID,
                        assignment -> assignment.setActivation(activationType), task, result));

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        assertGlobalStateUntouched();

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertDeny("delegate to Jack",
                (task, result) -> assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, task, result));

        assertDeny("delegate from Jack to Barbossa",
                (task, result) -> assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result));

        assertGlobalStateUntouched();
    }

    /**
     * Assign a deputy with validity. But this time there is a role that allows
     * access to inactive delegations.
     * MID-4172
     */
    @Test
    public void test124AutzJackDelegatorPlusValidity() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_DELEGATOR_PLUS.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        assertAssignedRole(userJack, ROLE_DELEGATOR_PLUS.oid);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(XmlTypeConverter.addDuration(startTs, "PT2H"));
        activationType.setValidTo(XmlTypeConverter.addDuration(startTs, "P1D"));

        // Good direction
        assertAllow("delegate to Barbossa",
                (task, result) -> assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID,
                        assignment -> assignment.setActivation(activationType), task, result));

        userJack = getUser(USER_JACK_OID);
        display("Jack delegator", userJack);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);

        // delegatorRef is allowed, but returns nothing. The delegation is not yet active, it is not in the delgatorRef.
        assertDeputySearchViaDelegatedRef(USER_JACK_OID /* nothing */);
        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID, USER_BARBOSSA_OID);

        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        // Delegation is not active yet. No access.
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        clockForward("PT3H");

        login(USER_ADMINISTRATOR_USERNAME);
        recomputeUser(USER_BARBOSSA_OID);

        // Delegation is active now

        login(USER_JACK_USERNAME);
        // WHEN

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);

        assertDeputySearchViaDelegatedRef(USER_JACK_OID, USER_BARBOSSA_OID);
        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID, USER_BARBOSSA_OID);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        clockForward("P1D");

        login(USER_ADMINISTRATOR_USERNAME);
        recomputeUser(USER_BARBOSSA_OID);

        // Delegation no longer active

        login(USER_JACK_USERNAME);
        // WHEN

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);

        // delegatorRef is allowed, but returns nothing. The delegation is not yet active, it is not in the delgatorRef.
        assertDeputySearchViaDelegatedRef(USER_JACK_OID /* nothing */);
        assertDeputySearchViaAssignmentTargetRef(USER_JACK_OID, USER_BARBOSSA_OID);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        // Delegation is not active any more. No access.
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        login(USER_JACK_USERNAME);
        // WHEN
        when();
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa",
                (task, result) -> unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID,
                        assignment -> assignment.setActivation(activationType), task, result));

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        assertGlobalStateUntouched();

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        when();
        display("Logged in as Barbossa");

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertDeny("delegate to Jack",
                (task, result) -> assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, task, result));

        assertDeny("delegate from Jack to Barbossa",
                (task, result) -> assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result));

        assertGlobalStateUntouched();
    }

    @Test
    public void test150AutzJackApproverUnassignRoles() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid);
        assignRole(USER_JACK_OID, ROLE_ORDINARY.oid, SchemaConstants.ORG_APPROVER);

        PrismObject<UserType> userCobbBefore = getUser(userCobbOid);
        IntegrationTestTools.display("User cobb before", userCobbBefore);
        assertRoleMembershipRef(userCobbBefore, ROLE_ORDINARY.oid, ROLE_UNINTERESTING.oid, ORG_SCUMM_BAR_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(RoleType.class, ROLE_ORDINARY.oid);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid); // no assignment
        assertGetDeny(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid); // assignment exists, but wrong relation

        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        displayValue("User Rum Rogers", userRumRogersOid);
        assertRoleMembershipRef(userRum, ROLE_ORDINARY.oid, ROLE_UNINTERESTING.oid, ORG_MINISTRY_OF_RUM_OID);
        assertGetAllow(UserType.class, userCobbOid);      // member of ROLE_ORDINARY_OID
        assertGetDeny(UserType.class, USER_JACK_OID);     // assignment exists, but wrong relation
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID); // no assignment to ROLE_ORDINARY_OID
        assertGetDeny(UserType.class, USER_LECHUCK.oid);  // no assignment to ROLE_ORDINARY_OID

        assertSearch(OrgType.class, null, 0);

        // The appr-read-roles authorization is maySkipOnSearch and there is no other authorization that would
        // allow read, so no role are returned
        assertSearch(RoleType.class, null, 0);

        // The appr-read-users authorization is maySkipOnSearch and there is no other authorization that would
        // allow read, so no users are returned
        assertSearch(UserType.class, null, 0);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assert15xCommon();
    }

    @Test
    public void test151AutzJackApproverUnassignRolesAndRead() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid);
        assignRole(USER_JACK_OID, ROLE_READ_BASIC_ITEMS.oid);
        assignRole(USER_JACK_OID, ROLE_ORDINARY.oid, SchemaConstants.ORG_APPROVER);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(RoleType.class, ROLE_ORDINARY.oid);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN.oid); // no assignment
        assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid); // assignment exists, but wrong relation

        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        displayValue("User Rum Rogers", userRumRogersOid);
        assertRoleMembershipRef(userRum, ROLE_ORDINARY.oid, ROLE_UNINTERESTING.oid, ORG_MINISTRY_OF_RUM_OID);
        assertGetAllow(UserType.class, userCobbOid);      // member of ROLE_ORDINARY_OID
        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);     // assignment exists, but wrong relation
        assertNoRoleMembershipRef(userJack);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID); // no assignment to ROLE_ORDINARY_OID
        assertGetAllow(UserType.class, USER_LECHUCK.oid);  // no assignment to ROLE_ORDINARY_OID

        assertSearch(OrgType.class, null, NUMBER_OF_ALL_ORGS);

        // The appr-read-roles authorization is maySkipOnSearch and the readonly role allows read.
        assertSearch(RoleType.class, null, getNumberOfRoles());

        // The appr-read-users authorization is maySkipOnSearch and the readonly role allows read.
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);

        assert15xCommon();
    }

    /**
     * Jack is an approver of a role, but he does not have any authorization
     * except very basic object read.
     */
    @Test
    public void test154AutzJackApproverRead() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_BASIC_ITEMS.oid);
        assignRole(USER_JACK_OID, ROLE_ORDINARY.oid, SchemaConstants.ORG_APPROVER);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY.oid);
        assertNoRoleMembershipRef(roleOrdinary);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN.oid);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid);
        assertNoRoleMembershipRef(roleAppr);

        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid);
        assertNoRoleMembershipRef(userRum);
        assertGetAllow(UserType.class, userCobbOid);
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK.oid);

        assertSearch(OrgType.class, null, NUMBER_OF_ALL_ORGS);
        assertSearch(RoleType.class, null, getNumberOfRoles());
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertDeny("unassign ordinary role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_ORDINARY.oid, task, result));

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);

        assertDeny("unassign uninteresting role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign uninteresting role from rum",
                (task, result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign approver role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid, task, result));
        assertDeny("unassign ordinary role from lechuck",
                (task, result) -> unassignRole(USER_LECHUCK.oid, ROLE_ORDINARY.oid, task, result));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    /**
     * Jack is an approver of a role, but he does not have any authorization
     * except reading self.
     * Note: tests with role-self and no approver are in TestSecurityBasic.test204AutzJackSelfRole()
     */
    @Test
    public void test155AutzJackApproverSelf() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF.oid);
        assignRole(USER_JACK_OID, ROLE_ORDINARY.oid, SchemaConstants.ORG_APPROVER);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetDeny(RoleType.class, ROLE_ORDINARY.oid);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid);
        assertGetDeny(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid);

        assertGetAllow(UserType.class, USER_JACK_OID);

        assertGetDeny(UserType.class, userRumRogersOid);
        assertGetDeny(UserType.class, userCobbOid);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK.oid);

        assertSearch(OrgType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        assertSearch(UserType.class, null, 1);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertDeny("unassign ordinary role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_ORDINARY.oid, task, result));

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);

        assertDeny("unassign uninteresting role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign uninteresting role from rum",
                (task, result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign ordinary role from lechuck",
                (task, result) -> unassignRole(USER_LECHUCK.oid, ROLE_ORDINARY.oid, task, result));

        assertAddDeny();
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    @Test
    public void test157AutzJackReadRoleMembers() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY.oid);
        assertNoRoleMembershipRef(roleOrdinary);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid);
        assertNoRoleMembershipRef(roleAppr);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN.oid);

        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid);
        assertRoleMembershipRef(userRum, ROLE_ORDINARY.oid, ROLE_UNINTERESTING.oid, ORG_MINISTRY_OF_RUM_OID);
        assertGetAllow(UserType.class, userCobbOid);
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK.oid);

        assertSearch(RoleType.class, null, getNumberOfRoles());
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(OrgType.class, null, 0);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 2);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertDeny("unassign ordinary role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_ORDINARY.oid, task, result));
        assertDeny("unassign uninteresting role from rum",
                (task, result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign approver role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid, task, result));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    @Test
    public void test158AutzJackReadRoleMembersWrong() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_WRONG_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY.oid);
        assertNoRoleMembershipRef(roleOrdinary);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid);
        assertNoRoleMembershipRef(roleAppr);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN.oid);

        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        assertNoRoleMembershipRef(userRum);
        assertGetAllow(UserType.class, userCobbOid);
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK.oid);

        assertSearch(RoleType.class, null, getNumberOfRoles());
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(OrgType.class, null, 0);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertDeny("unassign ordinary role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_ORDINARY.oid, task, result));
        assertDeny("unassign uninteresting role from rum",
                (task, result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign approver role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid, task, result));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    @Test
    public void test159AutzJackReadRoleMembersNone() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_NONE_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY.oid);
        assertNoRoleMembershipRef(roleOrdinary);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid);
        assertNoRoleMembershipRef(roleAppr);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN.oid);

        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        assertNoRoleMembershipRef(userRum);
        assertGetAllow(UserType.class, userCobbOid);
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK.oid);

        assertSearch(RoleType.class, null, getNumberOfRoles());
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(OrgType.class, null, 0);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertDeny("unassign ordinary role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_ORDINARY.oid, task, result));
        assertDeny("unassign uninteresting role from rum",
                (task, result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result));
        assertDeny("unassign approver role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid, task, result));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    private void assert15xCommon() throws Exception {

        // list ordinary role members, this is allowed
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 2);
        assertSearch(FocusType.class, createMembersQuery(FocusType.class, ROLE_ORDINARY.oid), 2);

        // list approver role members, this is not allowed
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);
        assertSearch(FocusType.class, createMembersQuery(FocusType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertAllow("unassign ordinary role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_ORDINARY.oid, task, result));

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 1);

        // Jack is not approver of uninteresting role, so this should be denied
        assertDeny("unassign uninteresting role from cobb",
                (task, result) -> unassignRole(userCobbOid, ROLE_UNINTERESTING.oid, task, result));

        // Jack is not approver of uninteresting role, so this should be denied
        // - even though Rum Rogers is a member of a role that jack is an approver of
        assertDeny("unassign uninteresting role from rum",
                (task, result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result));

        assertDeny("unassign approver role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES.oid, task, result));

        // Lechuck is not a member of ordinary role
        assertDeny("unassign ordinary role from lechuck",
                (task, result) -> unassignRole(USER_LECHUCK.oid, ROLE_ORDINARY.oid, task, result));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
    }

    /**
     * User template will assign organizations to this user. However, the user
     * does not have request authorization for organizations. Check that this
     * proceeds smoothly.
     * MID-3996
     */
    @Test
    public void test200AutzJackModifyOrgunit() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_SELF_MODIFY_ORG_UNIT.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        // This is supposed to fail. Jack does not have authorization for org assignment
        assertDeny("assign org to jack",
                (task, result) -> assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result));

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        // ... but this should work. Indirect assignment in object template is OK.
        assertModifyAllow(UserType.class, USER_JACK_OID,
                UserType.F_ORGANIZATIONAL_UNIT, PolyString.fromOrig(ORG_SCUMM_BAR_NAME));

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 2);
        assertAssignedOrg(user, ORG_SCUMM_BAR_OID);

        assertModifyAllow(UserType.class, USER_JACK_OID,
                UserType.F_ORGANIZATIONAL_UNIT, PolyString.fromOrig(ORG_MINISTRY_OF_RUM_NAME));

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 2);
        assertAssignedOrg(user, ORG_MINISTRY_OF_RUM_OID);

        assertModifyAllow(UserType.class, USER_JACK_OID,
                UserType.F_ORGANIZATIONAL_UNIT);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        assertGlobalStateUntouched();
    }

    /**
     * User template will assign organizations to this user. However, the user
     * does not have request authorization for organizations. Check that this
     * proceeds smoothly.
     * Similar to the previous test, we just try to confuse midPoint by assigning
     * (requestable) role and modifying the orgunit at the same time.
     * MID-3996
     */
    @Test
    public void test202AutzJackModifyOrgunitAndAssignRole() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_SELF_MODIFY_ORG_UNIT.oid);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        // This is supposed to fail. Jack does not have authorization for org assignment
        assertDeny("assign org to jack",
                (task, result) -> assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result));

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        assertAllow("doing the thing",
                (task, result) -> {
                    ObjectDelta<UserType> focusDelta = createAssignmentFocusDelta(UserType.class, USER_JACK_OID,
                            ROLE_BUSINESS_1.oid, RoleType.COMPLEX_TYPE, null, null, true);
                    focusDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT, PolyString.fromOrig(ORG_SCUMM_BAR_NAME));
                    Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(focusDelta);
                    modelService.executeChanges(deltas, null, task, result);
                });

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 4);
        assertAssignedOrg(user, ORG_SCUMM_BAR_OID);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertModifyAllow(UserType.class, USER_JACK_OID,
                UserType.F_ORGANIZATIONAL_UNIT, PolyString.fromOrig(ORG_MINISTRY_OF_RUM_NAME));

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 4);
        assertAssignedOrg(user, ORG_MINISTRY_OF_RUM_OID);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertModifyAllow(UserType.class, USER_JACK_OID,
                UserType.F_ORGANIZATIONAL_UNIT);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);
        assertNotAssignedOrg(user, ORG_MINISTRY_OF_RUM_OID);

        assertAllow("unassign role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertNotAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertGlobalStateUntouched();
    }

    /**
     * Role with object filter that has an expression.
     * No costCenter in user, no access.
     * MID-4191
     */
    @Test
    public void test220AutzJackRoleExpressionNoConstCenter() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EXPRESSION_READ_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGetDeny(RoleType.class, ROLE_BUSINESS_1.oid);
        assertGetDeny(RoleType.class, ROLE_BUSINESS_2.oid);
        assertGetDeny(RoleType.class, ROLE_APPLICATION_1.oid);
        assertGetDeny(RoleType.class, ROLE_EXPRESSION_READ_ROLES.oid);

        assertSearchDeny(RoleType.class, null, null);
        assertSearchDeny(RoleType.class,
                queryFor(RoleType.class).item(RoleType.F_SUBTYPE).eq("business").build(),
                null);
        assertSearchDeny(RoleType.class,
                queryFor(RoleType.class).item(RoleType.F_SUBTYPE).eq("application").build(),
                null);

        assertGlobalStateUntouched();
    }

    /**
     * Role with object filter that has an expression.
     * MID-4191
     */
    @Test
    public void test222AutzJackRoleExpressionConstCenterBusiness() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EXPRESSION_READ_ROLES.oid);

        Task task = getTestTask();
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, UserType.F_COST_CENTER, task, result, "business");

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGetAllow(RoleType.class, ROLE_BUSINESS_1.oid);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_2.oid);
        assertGetDeny(RoleType.class, ROLE_APPLICATION_1.oid);
        assertGetDeny(RoleType.class, ROLE_EXPRESSION_READ_ROLES.oid);

        assertSearch(RoleType.class, null, 3);
        assertSearch(RoleType.class,
                queryFor(RoleType.class).item(RoleType.F_SUBTYPE).eq("business").build(), 3);
        assertSearchDeny(RoleType.class,
                queryFor(RoleType.class).item(RoleType.F_SUBTYPE).eq("application").build(),
                null);

        assertGlobalStateUntouched();
    }

    /**
     * Unlimited power of attorney. But only granted to Caribbean users.
     * MID-4072, MID-4205
     */
    @Test
    public void test230AttorneyCaribbeanUnlimited() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_CARIBBEAN_UNLIMITED.oid);

        cleanupAutzTest(USER_BARBOSSA_OID);
        // Give some roles to barbossa first to really do something when we switch identity to him
        assignRole(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        displayDumpable("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, USER_JACK_OID, USER_BARBOSSA_OID);

        assertAuthenticated();
        assertLoggedInUsername(USER_JACK_USERNAME);
        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        MidPointPrincipal donorPrincipal = assumePowerOfAttorneyAllow(USER_BARBOSSA_OID);
        assertPrincipalAttorneyOid(donorPrincipal, USER_JACK_OID);

        assertAuthenticated();
        assertLoggedInUserOid(USER_BARBOSSA_OID);
        assertSecurityContextPrincipalAttorneyOid(USER_JACK_OID);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.MODIFY, ModelAuthorizationAction.MODIFY);

        assertReadSomeModifySome(1);

        MidPointPrincipal attorneyPrincipal = dropPowerOfAttorneyAllow();
        assertPrincipalAttorneyOid(attorneyPrincipal, null);

        assertAuthenticated();
        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assumePowerOfAttorneyDeny(userRumRogersOid);
        assumePowerOfAttorneyDeny(USER_GUYBRUSH_OID);

        // Make sure denied operation does not change security context
        assertAuthenticated();
        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assertGlobalStateUntouched();
    }

    /**
     * Attorney for subordinate employees, but Jack has no org.
     * MID-4072, MID-4205
     */
    @Test
    public void test232ManagerAttorneyNoOrg() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS.oid);

        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        displayDumpable("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 0);

        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        displayDumpable("donorFilterWorkitems", donorFilterWorkitems);
        assertSearchFilter(UserType.class, donorFilterWorkitems, 0);

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assumePowerOfAttorneyDeny(USER_BARBOSSA_OID);
        assumePowerOfAttorneyDeny(USER_GUYBRUSH_OID);
        assumePowerOfAttorneyDeny(userRumRogersOid);

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assertGlobalStateUntouched();
    }

    /**
     * Attorney for subordinate employees, Jack is manager of Ministry of Rum.
     * MID-4072, MID-4205
     */
    @Test
    public void test234ManagerAttorneyRum() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        displayDumpable("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 4);

        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        displayDumpable("donorFilterWorkitems", donorFilterWorkitems);
        assertSearchFilter(UserType.class, donorFilterWorkitems, 4);

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assumePowerOfAttorneyDeny(USER_BARBOSSA_OID);

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assumePowerOfAttorneyAllow(userRumRogersOid);

        assertLoggedInUserOid(userRumRogersOid);
        assertSecurityContextPrincipalAttorneyOid(USER_JACK_OID);
        // No authorizations. Rum Rogers does not have any roles that would authorize anything
        assertSecurityContextNoAuthorizationActions();

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        dropPowerOfAttorneyAllow();

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assumePowerOfAttorneyDeny(USER_GUYBRUSH_OID);

        assertGlobalStateUntouched();
    }

    /**
     * Similar to previous test, but now Rum Rogers has some authorizations.
     * MID-4072, MID-4205
     */
    @Test
    public void test235ManagerAttorneyRumRogersEntitled() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);

        assignRole(userRumRogersOid, ROLE_APPROVER.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        displayDumpable("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 4);

        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        displayDumpable("donorFilterWorkitems", donorFilterWorkitems);
        assertSearchFilter(UserType.class, donorFilterWorkitems, 4);

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assumePowerOfAttorneyDeny(USER_BARBOSSA_OID);

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assumePowerOfAttorneyAllow(userRumRogersOid);

        assertLoggedInUserOid(userRumRogersOid);
        assertSecurityContextPrincipalAttorneyOid(USER_JACK_OID);
        assertSecurityContextAuthorizationActions(AUTHORIZATION_ACTION_WORKITEMS);

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        dropPowerOfAttorneyAllow();

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assumePowerOfAttorneyDeny(USER_GUYBRUSH_OID);

        assertGlobalStateUntouched();
    }

    /**
     * Attorney for subordinate employees, Jack is manager of Ministry of Rum.
     * Also unlimited Caribbean attorney.
     * MID-4072, MID-4205
     */
    @Test
    public void test236ManagerAttorneyCaribbeanRum() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_CARIBBEAN_UNLIMITED.oid);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);

        assignRole(userRumRogersOid, ROLE_APPROVER.oid);
        assignRole(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        displayDumpable("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 5);

        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        displayDumpable("donorFilterWorkitems", donorFilterWorkitems);
        assertSearchFilter(UserType.class, donorFilterWorkitems, 5);

        assumePowerOfAttorneyAllow(USER_BARBOSSA_OID);

        assertLoggedInUserOid(USER_BARBOSSA_OID);
        assertSecurityContextPrincipalAttorneyOid(USER_JACK_OID);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.MODIFY, ModelAuthorizationAction.MODIFY);

        assertReadSomeModifySome(3);

        dropPowerOfAttorneyAllow();

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);
        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY,
                ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assumePowerOfAttorneyAllow(userRumRogersOid);

        assertLoggedInUserOid(userRumRogersOid);
        assertSecurityContextPrincipalAttorneyOid(USER_JACK_OID);
        assertSecurityContextAuthorizationActions(AUTHORIZATION_ACTION_WORKITEMS);

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        dropPowerOfAttorneyAllow();

        assertLoggedInUserOid(USER_JACK_OID);
        assertSecurityContextPrincipalAttorneyOid(null);

        assertSecurityContextAuthorizationActions(ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY,
                ModelAuthorizationAction.READ, ModelAuthorizationAction.ATTORNEY);

        assumePowerOfAttorneyDeny(USER_GUYBRUSH_OID);

        login(USER_ADMINISTRATOR_USERNAME);

        // CLEANUP
        cleanupUnassign(userRumRogersOid, ROLE_APPROVER.oid);
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME.oid);

        assertGlobalStateUntouched();
    }

    /**
     * MID-4204
     */
    @Test
    public void test250AssignRequestableSelfOtherApprover() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER.oid);

        cleanupUnassign(userRumRogersOid, ROLE_APPROVER.oid);
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        // default relation, non-requestable role
        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        // wrong relation
        assertDeny("assign business role to jack (manager)",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, SchemaConstants.ORG_MANAGER, task, result));

        // requestable role, but assign to a different user
        assertDeny("assign application role to barbossa",
                (task, result) -> assignRole(USER_BARBOSSA_OID, ROLE_BUSINESS_1.oid, task, result));

        assertAllow("assign business role to barbossa (approver)",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("unassign business role to barbossa (approver)",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("assign business role to barbossa (owner)",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, SchemaConstants.ORG_OWNER, task, result));

        assertAllow("unassign business role to barbossa (owner)",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, SchemaConstants.ORG_OWNER, task, result));

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertAssignments(userBarbossa, 0);

        assertAssignableRoleSpecification(userJack)
                .assertSize(3)
                .relationDefault()
                .filter()
                .type(RoleType.COMPLEX_TYPE)
                .assertEq(RoleType.F_REQUESTABLE, true)
                .end()
                .end()
                .end()
                .relation(SchemaConstants.ORG_APPROVER)
                .filter()
                .type(RoleType.COMPLEX_TYPE)
                .assertNull()
                .end()
                .end()
                .end()
                .relation(SchemaConstants.ORG_OWNER)
                .filter()
                .type(RoleType.COMPLEX_TYPE)
                .assertNull()
                .end()
                .end();

        assertGlobalStateUntouched();
    }

    /**
     * MID-4204
     */
    @Test
    public void test252AssignRequestableSelfOtherApproverEmptyDelta() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER.oid);

        cleanupUnassign(userRumRogersOid, ROLE_APPROVER.oid);
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        final PrismObject<UserType> user1 = getUser(USER_JACK_OID);
        assertAssignments(user1, 1);
        assertAssignedRole(user1, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        final PrismObject<UserType> user2 = getUser(USER_JACK_OID);
        assertAssignments(user2, 2);
        assertAssignedRole(user2, ROLE_BUSINESS_1.oid);

        // default relation, non-requestable role
        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) -> deleteFocusAssignmentEmptyDelta(user2, ROLE_BUSINESS_1.oid, task, result));

        // wrong relation
        assertDeny("assign business role to jack (manager)",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, SchemaConstants.ORG_MANAGER, task, result));

        // requestable role, but assign to a different user
        assertDeny("assign application role to barbossa",
                (task, result) -> assignRole(USER_BARBOSSA_OID, ROLE_BUSINESS_1.oid, task, result));

        assertAllow("assign business role to barbossa (approver)",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("unassign business role to barbossa (approver)",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("assign business role to barbossa (owner)",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, SchemaConstants.ORG_OWNER, task, result));

        final PrismObject<UserType> user3 = getUser(USER_JACK_OID);
        assertAssignments(user3, 2);
        assertAssignedRole(user3, ROLE_BUSINESS_2.oid);

        assertAllow("unassign business role to barbossa (owner)",
                (task, result) -> deleteFocusAssignmentEmptyDelta(user3, ROLE_BUSINESS_2.oid, SchemaConstants.ORG_OWNER, task, result));

        final PrismObject<UserType> user4 = getUser(USER_JACK_OID);
        assertAssignments(user4, 1);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertAssignments(userBarbossa, 0);

        assertGlobalStateUntouched();
    }

    @Test
    public void test254AssignUnassignRequestableSelf() throws Exception {
        // GIVENds
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UNASSIGN_SELF_REQUESTABLE.oid);
        assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_UNASSIGN_SELF_REQUESTABLE.oid);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertAllow("unassign business role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_UNASSIGN_SELF_REQUESTABLE.oid);

        assertDeny("unassign ROLE_UNASSIGN_SELF_REQUESTABLE role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_UNASSIGN_SELF_REQUESTABLE.oid, task, result));

        assertGlobalStateUntouched();
    }

    @Test
    public void test256AssignUnassignRequestableSelfEmptyDelta() throws Exception {
        // GIVENds
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UNASSIGN_SELF_REQUESTABLE.oid);
        assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        final PrismObject<UserType> user1 = getUser(USER_JACK_OID);
        assertAssignments(user1, 2);
        assertAssignedRole(user1, ROLE_UNASSIGN_SELF_REQUESTABLE.oid);
        assertAssignedRole(user1, ROLE_BUSINESS_1.oid);

        assertAllow("unassign business role from jack",
                (task, result) -> deleteFocusAssignmentEmptyDelta(user1, ROLE_BUSINESS_1.oid, task, result));

        final PrismObject<UserType> user2 = getUser(USER_JACK_OID);
        assertAssignments(user2, 1);
        assertAssignedRole(user2, ROLE_UNASSIGN_SELF_REQUESTABLE.oid);

        assertDeny("unassign ROLE_UNASSIGN_SELF_REQUESTABLE role from jack",
                (task, result) -> deleteFocusAssignmentEmptyDelta(user2, ROLE_UNASSIGN_SELF_REQUESTABLE.oid, task, result));

        assertGlobalStateUntouched();
    }

    @Test
    public void test260AutzJackLimitedRoleAdministrator() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_ROLE_ADMINISTRATOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertReadDenyRaw();

        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();
        assertDeleteDeny();

        assertAddAllow(ROLE_EXCLUSION_PIRATE_FILE);

        PrismObject<RoleType> roleExclusion = assertGetAllow(RoleType.class, ROLE_EXCLUSION_PIRATE_OID);
        display("Exclusion role", roleExclusion);
        assertExclusion(roleExclusion, ROLE_PIRATE_OID);
//        display("Exclusion role def", roleExclusion.getDefinition());

        PrismObjectDefinition<RoleType> roleExclusionEditSchema = getEditObjectDefinition(roleExclusion);
        displayDumpable("Exclusion role edit schema", roleExclusionEditSchema);
        assertItemFlags(roleExclusionEditSchema, RoleType.F_NAME, true, true, true);
        assertItemFlags(roleExclusionEditSchema, RoleType.F_DESCRIPTION, true, true, true);
        assertItemFlags(roleExclusionEditSchema, RoleType.F_SUBTYPE, true, true, true);
        assertItemFlags(roleExclusionEditSchema, RoleType.F_LIFECYCLE_STATE, true, true, true);
        assertItemFlags(roleExclusionEditSchema, RoleType.F_METADATA, false, false, false);

        assertItemFlags(roleExclusionEditSchema, RoleType.F_ASSIGNMENT, true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_DESCRIPTION),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION),
                false, false, false);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_EVALUATION_TARGET),
                false, false, false);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES),
                false, false, false);

        assertItemFlags(roleExclusionEditSchema, RoleType.F_INDUCEMENT, true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_DESCRIPTION),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_EVALUATION_TARGET),
                true, true, true);
        assertItemFlags(roleExclusionEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES),
                true, true, true);

        assertAllow("add exclusion (1)",
                (task, result) -> modifyRoleAddExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        PrismObject<RoleType> roleEmptyExclusion = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with exclusion (1)", roleEmptyExclusion);
        assertExclusion(roleEmptyExclusion, ROLE_PIRATE_OID);

        assertAllow("delete exclusion (1)",
                (task, result) -> modifyRoleDeleteExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        assertAllow("add exclusion (2)",
                (task, result) -> modifyRoleAddExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        roleEmptyExclusion = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with exclusion (2)", roleEmptyExclusion);
        AssignmentType exclusionAssignment = assertExclusion(roleEmptyExclusion, ROLE_PIRATE_OID);

        assertAllow("delete exclusion (2)",
                (task, result) -> modifyRoleDeleteAssignment(ROLE_EMPTY_OID, createAssignmentIdOnly(exclusionAssignment.getId()), task, result));

        // TODO: add exclusion with metadata (should be denied)

        assertDeny("add minAssignee",
                (task, result) -> modifyRolePolicyRule(ROLE_EMPTY_OID, createMinAssigneePolicyRule(1), true, task, result));

        assertDeny("delete maxAssignee 10 (by value)",
                (task, result) -> modifyRolePolicyRule(ROLE_MAXASSIGNEES_10_OID, createMaxAssigneePolicyRule(10), false, task, result));

        assertDeny("delete maxAssignee 10 (by id)",
                (task, result) -> modifyRoleDeleteAssignment(ROLE_MAXASSIGNEES_10_OID, createAssignmentIdOnly(10L), task, result));

        assertDeny("assign role pirate to empty role",
                (task, result) -> assignRole(RoleType.class, ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        roleEmptyExclusion = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role without exclusion", roleEmptyExclusion);
        assertAssignments(roleEmptyExclusion, 0);

        assertGlobalStateUntouched();
    }

    /**
     * MID-4399
     */
    @Test
    public void test262AutzJackLimitedRoleAdministratorAndAssignApplicationRoles() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_ROLE_ADMINISTRATOR_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES.oid);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow();
        assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();

        // check ROLE_ASSIGN_APPLICATION_ROLES_OID authorizations

        assertAllow("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1.oid);

        assertDeny("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        assertAllow("unassign application role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        // check ROLE_LIMITED_ROLE_ADMINISTRATOR_OID authorizations

        assertAddAllow(ROLE_EXCLUSION_PIRATE_FILE);

        PrismObject<RoleType> roleExclusion = assertGetAllow(RoleType.class, ROLE_EXCLUSION_PIRATE_OID);
        display("Exclusion role", roleExclusion);
        assertExclusion(roleExclusion, ROLE_PIRATE_OID);
//        display("Exclusion role def", roleExclusion.getDefinition());

        assertAllow("add exclusion (1)",
                (task, result) -> modifyRoleAddExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        PrismObject<RoleType> roleEmptyExclusion = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with exclusion (1)", roleEmptyExclusion);
        assertExclusion(roleEmptyExclusion, ROLE_PIRATE_OID);

        assertAllow("delete exclusion (1)",
                (task, result) -> modifyRoleDeleteExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        assertAllow("add exclusion (2)",
                (task, result) -> modifyRoleAddExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        roleEmptyExclusion = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with exclusion (2)", roleEmptyExclusion);
        AssignmentType exclusionAssignment = assertExclusion(roleEmptyExclusion, ROLE_PIRATE_OID);

        display("TTTA1");
        assertAllow("delete exclusion (2)",
                (task, result) -> modifyRoleDeleteAssignment(ROLE_EMPTY_OID, createAssignmentIdOnly(exclusionAssignment.getId()), task, result));

        // TODO: add exclusion with metadata (should be denied)

        assertDeny("add minAssignee",
                (task, result) -> modifyRolePolicyRule(ROLE_EMPTY_OID, createMinAssigneePolicyRule(1), true, task, result));

        assertDeny("delete maxAssignee 10 (by value)",
                (task, result) -> modifyRolePolicyRule(ROLE_MAXASSIGNEES_10_OID, createMaxAssigneePolicyRule(10), false, task, result));

        display("TTTA2");
        assertDeny("delete maxAssignee 10 (by id)",
                (task, result) -> modifyRoleDeleteAssignment(ROLE_MAXASSIGNEES_10_OID, createAssignmentIdOnly(10L), task, result));

        assertDeny("assign role pirate to empty role",
                (task, result) -> assignRole(RoleType.class, ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        roleEmptyExclusion = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role without exclusion", roleEmptyExclusion);
        assertAssignments(roleEmptyExclusion, 0);

        asAdministrator(
                (task, result) -> deleteObject(RoleType.class, ROLE_EMPTY_OID));

        // MID-4443
        assertAddAllow(ROLE_EMPTY_FILE);

        asAdministrator(
                (task, result) -> deleteObject(RoleType.class, ROLE_EMPTY_OID));

        PrismObject<RoleType> roleEmpty2 = parseObject(ROLE_EMPTY_FILE);
        AssignmentType appliationRoleAssignment = new AssignmentType();
        ObjectReferenceType appliationRoleTargetRef = new ObjectReferenceType();
        appliationRoleTargetRef.setOid(ROLE_APPLICATION_1.oid);
        appliationRoleTargetRef.setType(RoleType.COMPLEX_TYPE);
        appliationRoleAssignment.setTargetRef(appliationRoleTargetRef);
        roleEmpty2.asObjectable().getAssignment().add(appliationRoleAssignment);

        // MID-4443
        assertAllow("Add empty role with application role assignment",
                (task, result) -> addObject(roleEmpty2));

        // MID-4369
        // Empty role as object. Really empty: no items there at all.
        PrismObject<RoleType> testRoleObject = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class).instantiate();
        // There are no items in this empty role. Therefore there is no item that is allowed. But also no item that
        // is denied or not allowed. This is a corner case. But this approach is often used by GUI to determine if
        // a specific class of object is allowed, e.g. if it is allowed to create (some) roles. This is used to
        // determine whether to display a particular menu item.
        assertTrue(testRoleObject.isEmpty());
        assertIsAuthorized(
                ModelAuthorizationAction.ADD.getUrl(), AuthorizationPhaseType.REQUEST,
                AuthorizationParameters.Builder.buildObject(testRoleObject));

        testRoleObject.asObjectable().setRiskLevel("hazardous");
        assertFalse(testRoleObject.isEmpty());
        assertIsNotAuthorized(
                ModelAuthorizationAction.ADD.getUrl(), AuthorizationPhaseType.REQUEST,
                AuthorizationParameters.Builder.buildObject(testRoleObject));

        assertGlobalStateUntouched();
    }

    /**
     * MID-4338
     */
    @Test
    public void test264AutzJackLimitedReadRoleAdministrator() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_READ_ROLE_ADMINISTRATOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertReadDenyRaw();

        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();
        assertDeleteDeny();

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role", roleEmpty);

        PrismObjectDefinition<RoleType> roleEmptyEditSchema = getEditObjectDefinition(roleEmpty);
        displayDumpable("Exclusion role edit schema", roleEmptyEditSchema);
        assertItemFlags(roleEmptyEditSchema, RoleType.F_NAME, true, true, true);
        assertItemFlags(roleEmptyEditSchema, RoleType.F_DESCRIPTION, true, true, true);
        assertItemFlags(roleEmptyEditSchema, RoleType.F_SUBTYPE, true, true, true);
        assertItemFlags(roleEmptyEditSchema, RoleType.F_LIFECYCLE_STATE, true, true, true);
        assertItemFlags(roleEmptyEditSchema, RoleType.F_METADATA, false, false, false);

        assertItemFlags(roleEmptyEditSchema, RoleType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE),
                true, false, false);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS),
                true, false, false);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION),
                true, false, false);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION),
                true, false, false);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_EVALUATION_TARGET),
                true, false, false);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES),
                true, false, false);

        assertItemFlags(roleEmptyEditSchema, RoleType.F_INDUCEMENT, true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION),
                true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_STRENGTH),
                true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF),
                true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT),
                false, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE),
                true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_OUTBOUND),
                true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_OUTBOUND, MappingType.F_STRENGTH),
                true, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_OUTBOUND, MappingType.F_DESCRIPTION),
                false, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_MATCHING_RULE),
                false, true, true);
        assertItemFlags(roleEmptyEditSchema,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_STRENGTH),
                true, true, true);

        assertAllow("induce role uninteresting to empty role",
                (task, result) -> induceRole(RoleType.class, ROLE_EMPTY_OID, ROLE_UNINTERESTING.oid, task, result));

        assertAllow("uninduce role uninteresting to empty role",
                (task, result) -> uninduceRole(RoleType.class, ROLE_EMPTY_OID, ROLE_UNINTERESTING.oid, task, result));

        assertGlobalStateUntouched();
    }

    /**
     * MID-5005
     */
    @Test
    public void test266AutzJackRoleAdministrator() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ROLE_ADMINISTRATOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertReadDenyRaw();

        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();
        assertDeleteDeny();

        assertAddAllow(ROLE_EXCLUSION_PIRATE_FILE);

        PrismObject<RoleType> roleExclusion = assertGetAllow(RoleType.class, ROLE_EXCLUSION_PIRATE_OID);
        display("Exclusion role", roleExclusion);
        assertExclusion(roleExclusion, ROLE_PIRATE_OID);

        assertAllow("assign role uninteresting to empty role",
                (task, result) -> assignRole(RoleType.class, ROLE_EMPTY_OID, ROLE_UNINTERESTING.oid, task, result));

        assertAllow("unassign role uninteresting to empty role",
                (task, result) -> unassignRole(RoleType.class, ROLE_EMPTY_OID, ROLE_UNINTERESTING.oid, task, result));

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty empty (1)", roleEmpty);
        assertAssignments(roleEmpty, 0);
        assertInducements(roleEmpty, 0);

        assertAllow("induce role uninteresting to empty role",
                (task, result) -> induceRole(RoleType.class, ROLE_EMPTY_OID, ROLE_UNINTERESTING.oid, task, result));

        assertAllow("uninduce role uninteresting to empty role",
                (task, result) -> uninduceRole(RoleType.class, ROLE_EMPTY_OID, ROLE_UNINTERESTING.oid, task, result));

        roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty empty (2)", roleEmpty);
        assertAssignments(roleEmpty, 0);
        assertInducements(roleEmpty, 0);

        assertGlobalStateUntouched();
    }

    @Test
    public void test270AutzJackModifyPolicyException() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role", roleEmpty);

        assertAllow("add policyException (1)",
                (task, result) -> modifyRoleAddPolicyException(ROLE_EMPTY_OID, createPolicyException(null, BIG_BADA_BOOM), task, result));

        PrismObject<RoleType> roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with policyException (1)", roleEmptyException);
        assertPolicyException(roleEmptyException, null, BIG_BADA_BOOM);

        assertAllow("delete policyException (1)",
                (task, result) -> modifyRoleDeletePolicyException(ROLE_EMPTY_OID, createPolicyException(null, BIG_BADA_BOOM), task, result));

        assertAllow("add policyException (2)",
                (task, result) -> modifyRoleAddPolicyException(ROLE_EMPTY_OID, createPolicyException(null, BIG_BADA_BOOM), task, result));

        roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with policyException (2)", roleEmptyException);
        PolicyExceptionType existingPolicyException = assertPolicyException(roleEmptyException, null, BIG_BADA_BOOM);
        PolicyExceptionType idOnlyPolicyException2 = new PolicyExceptionType();
        idOnlyPolicyException2.asPrismContainerValue().setId(existingPolicyException.asPrismContainerValue().getId());

        assertAllow("delete policyException (2)",
                (task, result) -> modifyRoleDeletePolicyException(ROLE_EMPTY_OID, idOnlyPolicyException2, task, result));

        assertDeny("add minAssignee",
                (task, result) -> modifyRolePolicyRule(ROLE_EMPTY_OID, createMinAssigneePolicyRule(1), true, task, result));

        assertDeny("assign role pirate to empty role",
                (task, result) -> assignRole(RoleType.class, ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        assertDeny("add exclusion",
                (task, result) -> modifyRoleAddExclusion(ROLE_EMPTY_OID, ROLE_PIRATE_OID, task, result));

        roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role without exclusion", roleEmptyException);
        assertAssignments(roleEmptyException, 0);

        assertGlobalStateUntouched();
    }

    @Test
    public void test272AutzJackModifyPolicyExceptionFirstRule() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role", roleEmpty);

        assertAllow("add policyException (1)",
                (task, result) -> modifyRoleAddPolicyException(ROLE_EMPTY_OID, createPolicyException(FIRST_RULE, BIG_BADA_BOOM), task, result));

        PrismObject<RoleType> roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with policyException (1)", roleEmptyException);
        PolicyExceptionType existingPolicyException = assertPolicyException(roleEmptyException, FIRST_RULE, BIG_BADA_BOOM);
        PolicyExceptionType idOnlyPolicyException1 = new PolicyExceptionType();
        idOnlyPolicyException1.asPrismContainerValue().setId(existingPolicyException.asPrismContainerValue().getId());

        login(USER_ADMINISTRATOR_USERNAME);
        unassignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_OID);
        login(USER_JACK_USERNAME);

        assertDeny("delete policyException (1)",
                (task, result) -> modifyRoleDeletePolicyException(ROLE_EMPTY_OID, idOnlyPolicyException1, task, result));

        assertDeny("delete policyException (2)",
                (task, result) -> modifyRoleDeletePolicyException(ROLE_EMPTY_OID, createPolicyException(FIRST_RULE, BIG_BADA_BOOM), task, result));

        // Try to trick the authorization to allow operation by mixing legal (allowed) delta with almost empty id-only delta.
        // There are no items in the id-only delta, therefore there is nothing that would conflict with an authorization.
        // ... and the legal delta might skew the decision towards allow.
        // But the authorization code should be smart enough to examine the id-only delta thoroughly. And it should detect
        // that we are trying to delete something that we are not allowed to.
        PolicyExceptionType idOnlyPolicyException3 = new PolicyExceptionType();
        idOnlyPolicyException3.asPrismContainerValue().setId(existingPolicyException.asPrismContainerValue().getId());
        assertDeny("delete policyException (3)",
                (task, result) -> {
                    ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                            .createModificationDeleteContainer(RoleType.class, ROLE_EMPTY_OID,
                                    RoleType.F_POLICY_EXCEPTION, idOnlyPolicyException3);
                    roleDelta.addModificationReplaceProperty(RoleType.F_DESCRIPTION, "whatever");
                    modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), null, task, result);
                });

        // Attempt to replace existing policy exceptions with a new one. The new value is allowed by the authorization.
        // But removal of old value is not allowed (there is a ruleName item which is not allowed). Therefore this replace
        // should be denied.
        assertDeny("replace policyException (1)",
                (task, result) -> modifyRoleReplacePolicyException(ROLE_EMPTY_OID, createPolicyException(null, HUGE_BADA_BOOM), task, result));

        assertGlobalStateUntouched();
    }

    @Test
    public void test274AutzJackModifyPolicyExceptionSituation() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_OID);
        login(USER_JACK_USERNAME);

        assertDeny("add policyException (1)",
                (task, result) -> modifyRoleAddPolicyException(ROLE_EMPTY_OID, createPolicyException(FIRST_RULE, BIG_BADA_BOOM), task, result));

        assertAllow("add policyException (3)",
                (task, result) -> modifyRoleAddPolicyException(ROLE_EMPTY_OID, createPolicyException(null, BIG_BADA_BOOM), task, result));

        assertAllow("replace policyException",
                (task, result) -> modifyRoleReplacePolicyException(ROLE_EMPTY_OID, createPolicyException(null, HUGE_BADA_BOOM), task, result));

        PrismObject<RoleType> roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with policyException (3)", roleEmptyException);
        PolicyExceptionType existingPolicyException = assertPolicyException(roleEmptyException, null, HUGE_BADA_BOOM);
        PolicyExceptionType idOnlyPolicyException3 = new PolicyExceptionType();
        idOnlyPolicyException3.asPrismContainerValue().setId(existingPolicyException.asPrismContainerValue().getId());

        login(USER_ADMINISTRATOR_USERNAME);
        unassignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_DESCRIPTION_OID);
        login(USER_JACK_USERNAME);

        assertDeny("delete policyException (3)",
                (task, result) -> modifyRoleDeletePolicyException(ROLE_EMPTY_OID, idOnlyPolicyException3, task, result));

        assertGlobalStateUntouched();
    }

    /**
     * MID-4517
     */
    @Test
    public void test280AutzJackModifyPolicyExceptionAndAssignOrg() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_ROLE_ADMINISTRATOR_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ORG_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role", roleEmpty);

        assertAllow("add exclusion & assign org (1)",
                (task, result) -> modifyRoleAddExclusionAndAssignOrg(ROLE_EMPTY_OID, ROLE_PIRATE_OID, ORG_MINISTRY_OF_RUM_OID, task, result));

        PrismObject<RoleType> roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role with exclusion and org", roleEmptyException);
        assertAssignments(roleEmptyException, 2);

        // TODO: delete the assignments

        assertGlobalStateUntouched();
    }

    /**
     * Partial check related to test280AutzJackModifyPolicyExceptionAndAssignOrg.
     * Make sure that the operation is denied if the user does not have all the roles.
     */
    @Test
    public void test282AutzJackModifyPolicyExceptionAndAssignOrgDeny() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_ROLE_ADMINISTRATOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role", roleEmpty);

        assertDeny("add policyException & assign org (1)",
                (task, result) -> modifyRoleAddExclusionAndAssignOrg(ROLE_EMPTY_OID, ROLE_PIRATE_OID, ORG_MINISTRY_OF_RUM_OID, task, result));

        PrismObject<RoleType> roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role ", roleEmptyException);
        assertAssignments(roleEmptyException, 0);

        assertGlobalStateUntouched();
    }

    /**
     * Partial check related to test280AutzJackModifyPolicyExceptionAndAssignOrg.
     * Make sure that org assignment is allowed with just the org assign role.
     */
    @Test
    public void test283AutzJackModifyPolicyAssignOrg() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ORG_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();

        PrismObject<RoleType> roleEmpty = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role", roleEmpty);

        assertAllow("assign org (1)",
                (task, result) -> assignOrg(RoleType.class, ROLE_EMPTY_OID, ORG_MINISTRY_OF_RUM_OID, task, result));

        PrismObject<RoleType> roleEmptyException = assertGetAllow(RoleType.class, ROLE_EMPTY_OID);
        display("Empty role ", roleEmptyException);
        assertAssignments(roleEmptyException, 1);

        assertGlobalStateUntouched();
    }

    @SuppressWarnings("SameParameterValue")
    private void modifyRoleAddExclusionAndAssignOrg(
            String roleOid, String excludedRoleOid, String orgOid, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<RoleType> roleDelta = createAssignmentAssignmentHolderDelta(
                RoleType.class, roleOid, orgOid, OrgType.COMPLEX_TYPE, null, null, null, true);
        PolicyRuleType exclusionPolicyRule = createExclusionPolicyRule(excludedRoleOid);
        AssignmentType assignment = new AssignmentType();
        assignment.setPolicyRule(exclusionPolicyRule);
        roleDelta.addModificationAddContainer(RoleType.F_ASSIGNMENT, assignment);
        executeChanges(roleDelta, null, task, result);
    }

    @Test
    public void test300AutzJackExceptAssignment() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PolyString.fromOrig(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PolyString.fromOrig(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO);
        assertAssignments(userJack, 0);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, false, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), false, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, false);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, false, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyDeny(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeny("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    @Test
    public void test302AutzJackExceptAdministrativeStatus() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PolyString.fromOrig(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PolyString.fromOrig(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AHEAD);
        assertAssignments(userJack, 1);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, false);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * ROLE_PROP_EXCEPT_ASSIGNMENT_OID allows read of everything except assignment (and few other things)
     * ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID allows read of assignment.
     * Therefore if jack has both roles he should have access to (almost) everything.
     */
    @Test
    public void test304AutzJackPropExceptAssignmentReadSomeModifySomeUser() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER.oid);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = assertAlmostFullJackRead(2);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
        // read of validTo is not allowed be either role
        PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, false, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * Simple check of `exceptItem` handling: There are read + modify authorizations for all items except
     * for `activation/administrativeStatus`. We make sure that all items (including `activation` and `activation/validTo`)
     * are properly handled.
     */
    @Test
    public void test305AutzJackPropExceptAdministrativeStatus() throws Exception {
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        when();

        PrismObject<UserType> userJack = assertAlmostFullJackRead(1);
        // Administrative status is explicitly excluded from the authorization.
        PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        // However, validTo should be readable
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AHEAD);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, false);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, JACK_VALID_FROM_LONG_AGO);
        assertModifyDeny(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.DISABLED);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * Test to properly merge two roles with exceptItem specifications.
     */
    @Test
    public void test306AutzJackPropExceptAssignmentExceptAdministrativeStatus() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = assertAlmostFullJackRead(2);
        // read of administrativeStatus is not allowed be either role
        PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AHEAD);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * Test for combination of exceptItem(assignment) with #assign/#unassign authorizations.
     */
    @Test
    public void test308AutzJackPropExceptAssignmentAssignApplicationRoles() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES.oid);
        assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PolyString.fromOrig(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PolyString.fromOrig(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AHEAD);
        assertAssignments(userJack, 2);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, false);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
        assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyDeny(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeny("assign business 1 role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        assertAllow("assign application 1 role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 3);
        assertAssignedRole(userJack, ROLE_APPLICATION_1.oid);

        assertDeny("assign business 2 role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign application 1 role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 2);

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertAllowRequestAssignmentItems(USER_JACK_OID, ROLE_APPLICATION_1.oid,
                SchemaConstants.PATH_ASSIGNMENT_TARGET_REF,
                SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_FROM,
                SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_TO);

        assertGlobalStateUntouched();
    }

    /**
     * User tries to get out of his zone of control. Allowed to modify only objects that
     * subtype=captain and tries to modify subtype to something else.
     */
    @Test
    public void test310AutzJackPropSubtypeDenyEscapingZoneOfControl() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_SUBTYPE_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        assertUserBefore(USER_JACK_OID)
                .assertName(USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertSubtype(USER_JACK_SUBTYPE);

        // WHEN
        when();

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_SUBTYPE, "escape");

        // WHEN
        then();

        assertUserAfter(USER_JACK_OID)
                .assertName(USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertSubtype(USER_JACK_SUBTYPE);

        assertGlobalStateUntouched();
    }

    /**
     * User tries to get out of his zone of control. Allowed to modify only objects that
     * subtype=captain and tries to modify subtype to something else.
     * This time authorization explicitly allows escaping zone of control.
     */
    @Test
    public void test312AutzJackPropSubtypeAllowEscapingZoneOfControl() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_SUBTYPE_ESCAPE_OID);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        assertUserBefore(USER_JACK_OID)
                .assertName(USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertSubtype(USER_JACK_SUBTYPE);

        // WHEN
        when();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_SUBTYPE, "escape");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_SUBTYPE, "escape again");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_SUBTYPE, USER_JACK_SUBTYPE);

        // WHEN
        then();

        assertUserAfter(USER_JACK_OID)
                .assertName(USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertSubtype("escape");

        assertGlobalStateUntouched();
    }

    /**
     * MID-4304
     */
    @Test
    public void test320AutzJackGuybrushValutDweller() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assertNoDummyAccount(RESOURCE_DUMMY_VAULT_NAME, USER_GUYBRUSH_USERNAME);

        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES.oid);
        modifyJackValidTo();
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userBuybrush = getUser(USER_GUYBRUSH_OID);
        display("Guybrush(1)", userBuybrush);
        assertAssignments(userBuybrush, 1);

        assertGetDeny(LookupTableType.class, LOOKUP_LANGUAGES_OID);

        assertAllow("assign vault dweller role to guybrush",
                (task, result) -> assignRole(USER_GUYBRUSH_OID, ROLE_VAULT_DWELLER_OID, task, result)
        );

        userBuybrush = getUser(USER_GUYBRUSH_OID);
        display("Guybrush(2)", userBuybrush);
        assertAssignments(userBuybrush, 2);
        assertAssignedRole(userBuybrush, ROLE_VAULT_DWELLER_OID);

        assertDummyAccount(RESOURCE_DUMMY_VAULT_NAME, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_VAULT_NAME, USER_GUYBRUSH_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "I can see lookupTable:70000000-0000-0000-1111-000000000001(Languages)");

        assertGetDeny(LookupTableType.class, LOOKUP_LANGUAGES_OID);

        assertAllow("unassign vault dweller role from guybrush",
                (task, result) -> unassignRole(USER_GUYBRUSH_OID, ROLE_VAULT_DWELLER_OID, task, result)
        );

        userBuybrush = getUser(USER_GUYBRUSH_OID);
        assertAssignments(userBuybrush, 1);

        assertNoDummyAccount(RESOURCE_DUMMY_VAULT_NAME, USER_GUYBRUSH_USERNAME);

        assertGlobalStateUntouched();
    }

    /**
     * We can get any users, but we can search only the CAPTAINs.
     * <p>
     * MID-4860, MID-4654, MID-4859
     */
    @Test
    public void test330AutzJackEndUserWithPrivacy() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assertNoDummyAccount(RESOURCE_DUMMY_VAULT_NAME, USER_GUYBRUSH_USERNAME);

        assignRole(USER_JACK_OID, ROLE_END_USER_WITH_PRIVACY.oid);
        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        display("Jack", userJack);
        // Access to employeeType is not allowed for get. Therefore is should not part of the result.
        PrismAsserts.assertNoItem(userJack, UserType.F_SUBTYPE);

        // Direct get, should be allowed even though guybrush is not a CAPTAIN
        PrismObject<UserType> userBuybrush = assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        display("Guybrush", userBuybrush);

        assertReadDenyRaw();
        assertGetDeny(LookupTableType.class, LOOKUP_LANGUAGES_OID);

        assertSearch(UserType.class, null, 1);
        assertSearchDeny(UserType.class, null, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * Superuser role should allow everything. Adding another role with any (allow)
     * authorizations should not limit superuser. Not even if those authorizations
     * are completely loony.
     * <p>
     * MID-4931
     */
    @Test
    public void test340AutzJackSuperUserAndExecRead() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_SUPERUSER_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ORG_EXEC_OID);

        // preconditions
        assertSearch(UserType.class, createOrgSubtreeQuery(ORG_MINISTRY_OF_OFFENSE_OID), USER_LECHUCK.oid, USER_GUYBRUSH_OID, userCobbOid, USER_ESTEVAN.oid);
        assertSearch(UserType.class, createOrgSubtreeAndNameQuery(ORG_MINISTRY_OF_OFFENSE_OID, USER_GUYBRUSH_USERNAME), USER_GUYBRUSH_OID);
        assertSearch(ObjectType.class, createOrgSubtreeAndNameQuery(ORG_MINISTRY_OF_OFFENSE_OID, USER_GUYBRUSH_USERNAME), USER_GUYBRUSH_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertSearch(UserType.class, createOrgSubtreeQuery(ORG_MINISTRY_OF_OFFENSE_OID), USER_LECHUCK.oid, USER_GUYBRUSH_OID, userCobbOid, USER_ESTEVAN.oid);
        assertSearch(UserType.class, createOrgSubtreeAndNameQuery(ORG_MINISTRY_OF_OFFENSE_OID, USER_GUYBRUSH_USERNAME), USER_GUYBRUSH_OID);
        assertSearch(ObjectType.class, createOrgSubtreeAndNameQuery(ORG_MINISTRY_OF_OFFENSE_OID, USER_GUYBRUSH_USERNAME), USER_GUYBRUSH_OID);

        assertSuperuserAccess(NUMBER_OF_ALL_USERS);

        assertGlobalStateUntouched();
    }

    /**
     * Checks whether resource operationalState authorization works.
     * <p>
     * MID-5168, MID-3749
     */
    @Test
    public void test350AutzJackResourceRead() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_READ_RESOURCE_OPERATIONAL_STATE_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        PrismObject<ResourceType> resource = getObject(ResourceType.class, RESOURCE_DUMMY_VAULT_OID);

        // THEN
        display("resource", resource);
        assertNull("schemaHandling is present although it should not be", resource.asObjectable().getSchemaHandling());
        assertEquals("Wrong # of items in resource read", 1, resource.getValue().size());
    }

    /**
     * Just to be sure we do not throw away empty PC/PCVs when not necessary.
     * <p>
     * MID-5168, MID-3749
     */
    @Test
    public void test360AutzAdminResourceRead() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        when();

        PrismObject<ResourceType> resource = getObject(ResourceType.class, RESOURCE_DUMMY_VAULT_OID);

        // THEN
        display("resource", resource);
        ResourceObjectTypeDefinitionType accountSchemaHandling = resource.asObjectable()
                .getSchemaHandling()
                .getObjectType().stream().filter(def -> def.getKind() == ShadowKindType.ACCOUNT).findFirst()
                .orElseThrow(() -> new AssertionError("no account definition"));
        assertNotNull(accountSchemaHandling.getActivation());
        assertNotNull(accountSchemaHandling.getActivation().getAdministrativeStatus());
        List<MappingType> outbounds = accountSchemaHandling.getActivation().getAdministrativeStatus().getOutbound();
        assertEquals("Wrong # of admin status outbounds", 1, outbounds.size());
    }

    /**
     * Checks whether task status can be read.
     *
     * MID-6721
     */
    @Test
    public void test360AutzJackTaskRead() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_READ_TASK_STATUS.oid);

        login(USER_JACK_USERNAME);

        when();

        PrismObject<TaskType> task = getObject(TaskType.class, TASK_DUMMY.oid);

        then();

        display("task", task);
        assertEquals("task-dummy", task.asObjectable().getName().getOrig());
        assertNull("result is present although it should not be", task.asObjectable().getResult());
        assertEquals("resultStatus is wrong or missing", OperationResultStatusType.SUCCESS, task.asObjectable().getResultStatus());
        assertEquals("Wrong # of items in task read", 2, task.getValue().size());
    }

    /**
     * Checks the caching of "owner search" operation during autz checking (MID-8363).
     */
    @Test
    public void test370AutzJackManyAutz() throws Exception {
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANY_SHADOW_OWNER_AUTZ.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        String accountOid =
                MiscUtil.extractSingletonRequired(
                                getUser(USER_JACK_OID).asObjectable().getLinkRef())
                        .getOid();

        login(USER_JACK_USERNAME);

        when("account is retrieved");

        PerformanceMonitor performanceMonitor = repositoryService.getPerformanceMonitor();
        performanceMonitor.clearGlobalPerformanceInformation();
        PrismObject<ShadowType> account = getObject(ShadowType.class, accountOid);

        then("only one search is executed");
        PerformanceInformation performanceInformation = performanceMonitor.getGlobalPerformanceInformation();
        displayValue("performance information",
                RepositoryPerformanceInformationUtil.format(performanceInformation.toRepositoryPerformanceInformationType()));
        String opName = isNativeRepository() ? "SqaleRepositoryService.searchObjects" : "searchObjects";
        assertThat(performanceInformation.getInvocationCount(opName))
                .as("searchObjects operation count")
                .isEqualTo(1);

        and("account is OK");
        display("account", account);
        assertThat(account.getValue().getItems()).as("items in account object").hasSize(8);
    }

    /**
     * Checks task template instantiation without the `#use` authorization. It should fail.
     */
    @Test
    public void test380AutzTaskTemplateNotAuthorized() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        cleanupAutzTest(USER_JACK_OID);

        login(USER_JACK_USERNAME);

        when("task template is instantiated");
        try {
            modelInteractionService.submitTaskFromTemplate(
                    TASK_TEMPLATE_DUMMY.oid,
                    ActivityCustomization.none(),
                    task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
            // FIXME add assertion here
        }
    }

    /**
     * Checks task template instantiation with the `#use` authorization. It should succeed.
     */
    @Test
    public void test385AutzTaskTemplateAuthorized() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_USE_TASK_TEMPLATES.oid);

        login(USER_JACK_USERNAME);

        when("task template is instantiated");
        var taskOid = modelInteractionService.submitTaskFromTemplate(
                TASK_TEMPLATE_DUMMY.oid,
                ActivityCustomization.none(),
                task, result);

        then("the task successfully finishes");
        waitForTaskFinish(taskOid); // assert success as well
    }

    /**
     * MID-10206
     */
    @Test
    public void test390AutzJackCannotFilterUnpermittedItems() throws Exception {
        given();

        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, "40000000-1000-0000-0000-000000000000");

        login(USER_JACK_USERNAME);

        when("Search for role type objects by non-permitted items (requestable, riskLevel)");

        assertSearchByNonPermittedItems(RoleType.class, "assert search for role type");
        assertSearchByNonPermittedItems(ServiceType.class, "assert search for service type", "629bafd6-8b5e-4a7c-94fa-36813984c5c3");
        // todo service probably should be there -> "629bafd6-8b5e-4a7c-94fa-36813984c5c3"
        assertSearchByNonPermittedItems(AbstractRoleType.class, "assert search for abstract role type");
    }

    private <R extends AbstractRoleType> void assertSearchByNonPermittedItems(Class<R> type, String message, String... expectedOids) throws Exception {
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID), type, 0);
        ObjectFilter filter = spec.getRelationMap().get(RelationTypes.MEMBER.getRelation());
        ObjectQuery query = queryFor(type)
                .isInScopeOf(ORG_REQUESTABLE.oid, OrgFilter.Scope.ONE_LEVEL)
                .build();

        query.addFilter(filter);

        logger.info(message);

        assertSearch(type, query, expectedOids);
    }

    @SuppressWarnings("SameParameterValue")
    private ObjectQuery createOrgSubtreeAndNameQuery(String orgOid, String name) {
        return queryFor(ObjectType.class)
                .isChildOf(orgOid)
                .and()
                .item(ObjectType.F_NAME).eqPoly(name)
                .build();
    }

    private void modifyJackValidTo()
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("modifyJackValidTo");
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, task, result, JACK_VALID_TO_LONG_AHEAD);
        assertSuccess(result);
    }

    @SuppressWarnings("SameParameterValue")
    private PrismObject<UserType> assertAlmostFullJackRead(int expectedTargetAssignments) throws Exception {
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PolyString.fromOrig(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PolyString.fromOrig(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
        assertAssignmentsWithTargets(userJack, expectedTargetAssignments);
        return userJack;
    }

    private PolicyExceptionType assertPolicyException(PrismObject<RoleType> role, String expectedRuleName, String expectedPolicySituation) {
        List<PolicyExceptionType> policyExceptions = role.asObjectable().getPolicyException();
        assertEquals("Wrong size of policyException container in " + role, 1, policyExceptions.size());
        PolicyExceptionType policyException = policyExceptions.get(0);
        assertEquals("Wrong rule name in " + role, expectedRuleName, policyException.getRuleName());
        assertEquals("Wrong situation in " + role, expectedPolicySituation, policyException.getPolicySituation());
        return policyException;
    }

    @SuppressWarnings("SameParameterValue")
    private AssignmentType assertExclusion(PrismObject<RoleType> roleExclusion, String excludedRoleOid) {
        List<AssignmentType> noTargetAssignments = roleExclusion.asObjectable().getAssignment().stream()
                .filter(a -> a.getTargetRef() == null)
                .toList();
        assertEquals("Wrong size of no-target assignments in " + roleExclusion, 1, noTargetAssignments.size());
        AssignmentType exclusionAssignment = noTargetAssignments.get(0);
        PolicyRuleType exclusionPolicyRule = exclusionAssignment.getPolicyRule();
        assertNotNull("No policy rule in " + roleExclusion, exclusionPolicyRule);
        PolicyConstraintsType exclusionPolicyConstraints = exclusionPolicyRule.getPolicyConstraints();
        assertNotNull("No policy rule constraints in " + roleExclusion, exclusionPolicyConstraints);
        List<ExclusionPolicyConstraintType> exclusionExclusionPolicyConstraints = exclusionPolicyConstraints.getExclusion();
        assertEquals("Wrong size of exclusion policy constraints in " + roleExclusion, 1, exclusionExclusionPolicyConstraints.size());
        ExclusionPolicyConstraintType exclusionPolicyConstraint = exclusionExclusionPolicyConstraints.get(0);
        assertNotNull("No exclusion policy constraint in " + roleExclusion, exclusionPolicyConstraint);
        ObjectReferenceType targetRef = exclusionPolicyConstraint.getTargetRef();
        assertNotNull("No targetRef in exclusion policy constraint in " + roleExclusion, targetRef);
        assertEquals("Wrong OID targetRef in exclusion policy constraint in " + roleExclusion, excludedRoleOid, targetRef.getOid());
        return exclusionAssignment;
    }

    @Override
    protected void cleanupAutzTest(String userOid, int expectedAssignments)
            throws CommonException, IOException {
        super.cleanupAutzTest(userOid, expectedAssignments);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assignRole(userRumRogersOid, ROLE_ORDINARY.oid, task, result);
        assignRole(userRumRogersOid, ROLE_UNINTERESTING.oid, task, result);
        assignRole(userCobbOid, ROLE_ORDINARY.oid, task, result);
        assignRole(userCobbOid, ROLE_UNINTERESTING.oid, task, result);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertDeputySearchViaDelegatedRef(String delegatorOid, String... expectedDeputyOids)
            throws Exception {
        assertSearch(
                UserType.class,
                queryFor(UserType.class)
                        .item(UserType.F_DELEGATED_REF)
                        .ref(delegatorOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY)
                        .build(),
                expectedDeputyOids);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertDeputySearchViaAssignmentTargetRef(
            String delegatorOid, String... expectedDeputyOids) throws Exception {
        assertSearch(
                UserType.class,
                queryFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(delegatorOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY)
                        .build(),
                expectedDeputyOids);
    }

    @Override
    protected void cleanupAutzTest(String userOid) throws CommonException, IOException {
        super.cleanupAutzTest(userOid);

        Task task = createPlainTask("cleanupAutzTest");
        OperationResult result = task.getResult();

        cleanupDelete(RoleType.class, ROLE_EXCLUSION_PIRATE_OID, task, result);
    }
}
