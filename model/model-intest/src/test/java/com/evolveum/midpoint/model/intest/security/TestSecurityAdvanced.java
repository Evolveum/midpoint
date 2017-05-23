/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest.security;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.IOException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityAdvanced extends AbstractSecurityTest {
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}


	@Test
    public void test100AutzJackPersonaManagement() throws Exception {
		final String TEST_NAME = "test100AutzJackPersonaManagement";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        assertGetDeny(UserType.class, USER_CHARLES_OID);
        
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
		final String TEST_NAME = "test102AutzLechuckPersonaManagement";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_LECHUCK_OID);
        assignRole(USER_LECHUCK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_LECHUCK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        assertGetAllow(UserType.class, USER_CHARLES_OID);

//        TODO: MID-3899
//        assertSearch(UserType.class, null, 2);
//        assertSearch(ObjectType.class, null, 2);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}
    
    @Test
    public void test110AutzJackPersonaAdmin() throws Exception {
		final String TEST_NAME = "test110AutzJackAddPersonaAdmin";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertAllow("assign application role 1 to jack", 
        		(task,result) -> assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result));
        
        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        assertGetDeny(UserType.class, USER_CHARLES_OID);
        
        assertPersonaLinks(userJack, 1);
        String personaJackOid = userJack.asObjectable().getPersonaRef().get(0).getOid();
        
        PrismObject<UserType> personaJack = assertGetAllow(UserType.class, personaJackOid);
        assertEquals("Wrong jack persona givenName before change", USER_JACK_GIVEN_NAME, personaJack.asObjectable().getGivenName().getOrig());
        
//      TODO: MID-3899
//      assertSearch(UserType.class, null, 2);
//      assertSearch(ObjectType.class, null, 2);
        assertSearch(OrgType.class, null, 0);
        
        assertAllow("modify jack givenName", 
        		(task,result) -> modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, 
        				createPolyString(USER_JACK_GIVEN_NAME_NEW)));
        
        userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertEquals("Wrong jack givenName after change", USER_JACK_GIVEN_NAME_NEW, userJack.asObjectable().getGivenName().getOrig());

        personaJack = assertGetAllow(UserType.class, personaJackOid);
        assertEquals("Wrong jack persona givenName after change", USER_JACK_GIVEN_NAME_NEW, personaJack.asObjectable().getGivenName().getOrig());

        assertAllow("unassign application role 1 to jack", 
        		(task,result) -> unassignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result));
        
        userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertPersonaLinks(userJack, 0);
        
        assertNoObject(UserType.class, personaJackOid);
        
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}
    
	@Test
    public void test120AutzJackDelagator() throws Exception {
		final String TEST_NAME = "test120AutzJackDelagator";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_DELEGATOR_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        assertAssignedRole(userJack, ROLE_DELEGATOR_OID);
        
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);
        
        assertDeny("assign business role to jack",
        	(task, result) -> {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			});
        
        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        
        // Wrong direction. It should NOT work.
        assertDeny("delegate from Barbossa to Jack", 
            	(task, result) -> {
            		assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, task, result);
    			});

        // Good direction
        assertAllow("delegate to Barbossa", 
        	(task, result) -> {
        		assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
			});
        
        userJack = getUser(USER_JACK_OID);
        display("Jack delegator", userJack);
        assertAssignments(userJack, 1);
        
        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);
        
        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Logged in as Barbossa");
        
        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        login(USER_JACK_USERNAME);
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa", 
        	(task, result) -> {
        		unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        	});

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        
        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);
                
        assertGlobalStateUntouched();
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Logged in as Barbossa");
        
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertDeny("delegate to Jack", 
        	(task, result) -> {
        		assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, task, result);
			});
        
        assertDeny("delegate from Jack to Barbossa", 
        	(task, result) -> {
        		assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
			});
        
        assertGlobalStateUntouched();
	}
    
    @Test
    public void test150AutzJackApproverUnassignRoles() throws Exception {
		final String TEST_NAME = "test150AutzJackApproverUnassignRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);
        
        PrismObject<UserType> userCobbBefore = getUser(userCobbOid);
        IntegrationTestTools.display("User cobb before", userCobbBefore);
        assertRoleMembershipRef(userCobbBefore, ROLE_ORDINARY_OID, ROLE_UNINTERESTING_OID, ORG_SCUMM_BAR_OID);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN_OID); // no assignment
        assertGetDeny(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID); // assignment exists, but wrong relation
        
        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        display("User Rum Rogers", userRumRogersOid);
        assertRoleMembershipRef(userRum, ROLE_ORDINARY_OID, ROLE_UNINTERESTING_OID, ORG_MINISTRY_OF_RUM_OID);
        assertGetAllow(UserType.class, userCobbOid);      // member of ROLE_ORDINARY_OID
        assertGetDeny(UserType.class, USER_JACK_OID);     // assignment exists, but wrong relation
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID); // no assignment to ROLE_ORDINARY_OID
        assertGetDeny(UserType.class, USER_LECHUCK_OID);  // no assignment to ROLE_ORDINARY_OID
        
        assertSearch(OrgType.class, null, 0);
        
        // The appr-read-roles authorization is maySkipOnSearch and there is no other authorization that would
        // allow read, so no role are returned
        assertSearch(RoleType.class, null, 0);

        // The appr-read-users authorization is maySkipOnSearch and there is no other authorization that would
        // allow read, so no users are returned
        assertSearch(UserType.class, null, 0);
        
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
        
        assert15xCommon();
    }
    
	@Test
    public void test151AutzJackApproverUnassignRolesAndRead() throws Exception {
		final String TEST_NAME = "test151AutzJackApproverUnassignRolesAndRead";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assignRole(USER_JACK_OID, ROLE_READ_BASIC_ITEMS_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN_OID); // no assignment
        assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID); // assignment exists, but wrong relation
        
        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        display("User Rum Rogers", userRumRogersOid);
        assertRoleMembershipRef(userRum, ROLE_ORDINARY_OID, ROLE_UNINTERESTING_OID, ORG_MINISTRY_OF_RUM_OID);
        assertGetAllow(UserType.class, userCobbOid);      // member of ROLE_ORDINARY_OID
        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);     // assignment exists, but wrong relation
        assertNoRoleMembershipRef(userJack);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID); // no assignment to ROLE_ORDINARY_OID
        assertGetAllow(UserType.class, USER_LECHUCK_OID);  // no assignment to ROLE_ORDINARY_OID
        
        assertSearch(OrgType.class, null, NUMBER_OF_ALL_ORGS);
        
        // The appr-read-roles authorization is maySkipOnSearch and the readonly role allows read.
        assertSearch(RoleType.class, null, NUMBER_OF_ALL_ROLES);

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
		final String TEST_NAME = "test154AutzJackApproverRead";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_BASIC_ITEMS_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertNoRoleMembershipRef(roleOrdinary);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN_OID);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assertNoRoleMembershipRef(roleAppr);
        
        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid);
        assertNoRoleMembershipRef(userRum);
        assertGetAllow(UserType.class, userCobbOid);
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        
        assertSearch(OrgType.class, null, NUMBER_OF_ALL_ORGS);        
        assertSearch(RoleType.class, null, NUMBER_OF_ALL_ROLES);
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 0);
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
		
		assertCanSearchRoleMemberUsers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMembers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMemberUsers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMemberUsers(ROLE_APPROVER_UNASSIGN_ROLES_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
		
		assertDeny("unassign ordinary role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_ORDINARY_OID, task, result));
		
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 0);
		
		assertDeny("unassign uninteresting role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_UNINTERESTING_OID, task, result));
		assertDeny("unassign uninteresting role from rum", 
				(task,result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result));		
		assertDeny("unassign approver role from jack", 
				(task,result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID, task, result));
		assertDeny("unassign ordinary role from lechuck", 
				(task,result) -> unassignRole(USER_LECHUCK_OID, ROLE_ORDINARY_OID, task, result));
		        
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
		final String TEST_NAME = "test155AutzJackApproverSelf";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetDeny(RoleType.class, ROLE_ORDINARY_OID);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN_OID);
        assertGetDeny(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertGetDeny(UserType.class, userCobbOid);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        
        assertSearch(OrgType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        assertSearch(UserType.class, null, 1);
        
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 0);
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
		
		assertCanSearchRoleMemberUsers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMembers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMemberUsers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMemberUsers(ROLE_APPROVER_UNASSIGN_ROLES_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
		
		assertDeny("unassign ordinary role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_ORDINARY_OID, task, result));
		
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 0);
		
		assertDeny("unassign uninteresting role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_UNINTERESTING_OID, task, result));
		assertDeny("unassign uninteresting role from rum", 
				(task,result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result));		
		assertDeny("unassign ordinary role from lechuck", 
				(task,result) -> unassignRole(USER_LECHUCK_OID, ROLE_ORDINARY_OID, task, result));
		        
		assertAddDeny();
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		assertDeleteDeny();
		assertGlobalStateUntouched();
    }
	
	@Test
    public void test157AutzJackReadRoleMembers() throws Exception {
		final String TEST_NAME = "test157AutzJackReadRoleMembers";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_OID);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertNoRoleMembershipRef(roleOrdinary);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assertNoRoleMembershipRef(roleAppr);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN_OID);
        
        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid);
        assertRoleMembershipRef(userRum, ROLE_ORDINARY_OID, ROLE_UNINTERESTING_OID, ORG_MINISTRY_OF_RUM_OID);
        assertGetAllow(UserType.class, userCobbOid); 
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        
        assertSearch(RoleType.class, null, NUMBER_OF_ALL_ROLES);
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(OrgType.class, null, 0);
        
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 2);
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
		
		assertCanSearchRoleMemberUsers(ROLE_ORDINARY_OID, true);
		assertCanSearchRoleMembers(ROLE_ORDINARY_OID, true);
		assertCanSearchRoleMemberUsers(ROLE_UNINTERESTING_OID, true);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, true);
		assertCanSearchRoleMemberUsers(ROLE_APPROVER_UNASSIGN_ROLES_OID, true);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, true);
		
		assertDeny("unassign ordinary role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_ORDINARY_OID, task, result));		
		assertDeny("unassign uninteresting role from rum", 
				(task,result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result));
		assertDeny("unassign approver role from jack", 
				(task,result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID, task, result));
		        
		assertAddDeny();
		assertModifyDeny();
		assertDeleteDeny();
		assertGlobalStateUntouched();
    }
	
	@Test
    public void test158AutzJackReadRoleMembersWrong() throws Exception {
		final String TEST_NAME = "test158AutzJackReadRoleMembersWrong";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_WRONG_OID);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertNoRoleMembershipRef(roleOrdinary);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assertNoRoleMembershipRef(roleAppr);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN_OID);
        
        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        assertNoRoleMembershipRef(userRum);
        assertGetAllow(UserType.class, userCobbOid); 
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        
        assertSearch(RoleType.class, null, NUMBER_OF_ALL_ROLES);
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(OrgType.class, null, 0);
        
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 0);
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
		
		assertCanSearchRoleMemberUsers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMembers(ROLE_ORDINARY_OID, true); // We can read roleMembershipRef from roles that are members of those roles
		assertCanSearchRoleMemberUsers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, true);
		assertCanSearchRoleMemberUsers(ROLE_APPROVER_UNASSIGN_ROLES_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, true);
		
		assertDeny("unassign ordinary role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_ORDINARY_OID, task, result));		
		assertDeny("unassign uninteresting role from rum", 
				(task,result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result));
		assertDeny("unassign approver role from jack", 
				(task,result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID, task, result));
		        
		assertAddDeny();
		assertModifyDeny();
		assertDeleteDeny();
		assertGlobalStateUntouched();
    }
	
	@Test
    public void test159AutzJackReadRoleMembersNone() throws Exception {
		final String TEST_NAME = "test159AutzJackReadRoleMembersNone";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_NONE_OID);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<RoleType> roleOrdinary = assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertNoRoleMembershipRef(roleOrdinary);
        PrismObject<RoleType> roleAppr = assertGetAllow(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assertNoRoleMembershipRef(roleAppr);
        assertGetAllow(RoleType.class, ROLE_PERSONA_ADMIN_OID);
        
        PrismObject<UserType> userRum = assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        assertNoRoleMembershipRef(userRum);
        assertGetAllow(UserType.class, userCobbOid); 
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        
        assertSearch(RoleType.class, null, NUMBER_OF_ALL_ROLES);
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(OrgType.class, null, 0);
        
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 0);
		assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
		
		assertCanSearchRoleMemberUsers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMembers(ROLE_ORDINARY_OID, false);
		assertCanSearchRoleMemberUsers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
		assertCanSearchRoleMemberUsers(ROLE_APPROVER_UNASSIGN_ROLES_OID, false);
		assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
		
		assertDeny("unassign ordinary role from cobb", 
				(task,result) -> unassignRole(userCobbOid, ROLE_ORDINARY_OID, task, result));		
		assertDeny("unassign uninteresting role from rum", 
				(task,result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result));
		assertDeny("unassign approver role from jack", 
				(task,result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID, task, result));
		        
		assertAddDeny();
		assertModifyDeny();
		assertDeleteDeny();
		assertGlobalStateUntouched();
    }

	private void assert15xCommon()  throws Exception {
        
        // list ordinary role members, this is allowed
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 2);
        assertSearch(FocusType.class, createMembersQuery(FocusType.class, ROLE_ORDINARY_OID), 2);

        // list approver role members, this is not allowed
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
        assertSearch(FocusType.class, createMembersQuery(FocusType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID), 0);
        
        assertCanSearchRoleMemberUsers(ROLE_ORDINARY_OID, true);
        assertCanSearchRoleMembers(ROLE_ORDINARY_OID, true);
        assertCanSearchRoleMemberUsers(ROLE_UNINTERESTING_OID, false);
        assertCanSearchRoleMembers(ROLE_UNINTERESTING_OID, false);
        assertCanSearchRoleMemberUsers(ROLE_APPROVER_UNASSIGN_ROLES_OID, false);
        assertCanSearchRoleMembers(ROLE_APPROVER_UNASSIGN_ROLES_OID, false);

        assertAllow("unassign ordinary role from cobb", 
        		(task,result) -> unassignRole(userCobbOid, ROLE_ORDINARY_OID, task, result));
        
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY_OID), 1);
        
        // Jack is not approver of uninteresting role, so this should be denied
        assertDeny("unassign uninteresting role from cobb", 
        		(task,result) -> unassignRole(userCobbOid, ROLE_UNINTERESTING_OID, task, result));
        
        // Jack is not approver of uninteresting role, so this should be denied 
        // - even though Rum Rogers is a member of a role that jack is an approver of
        assertDeny("unassign uninteresting role from rum", 
        		(task,result) -> unassignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result));
        
        assertDeny("unassign approver role from jack", 
        		(task,result) -> unassignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID, task, result));
        
        // Lechuck is not a member of ordinary role 
        assertDeny("unassign ordinary role from lechuck", 
        		(task,result) -> unassignRole(USER_LECHUCK_OID, ROLE_ORDINARY_OID, task, result));
                
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}
    
    @Override
    protected void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
    	super.cleanupAutzTest(userOid);
        
        Task task = taskManager.createTaskInstance(TestSecurityAdvanced.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();
        
        assignRole(userRumRogersOid, ROLE_ORDINARY_OID, task, result);
		assignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result);
		assignRole(userCobbOid, ROLE_ORDINARY_OID, task, result);
		assignRole(userCobbOid, ROLE_UNINTERESTING_OID, task, result);
        
	}
}
