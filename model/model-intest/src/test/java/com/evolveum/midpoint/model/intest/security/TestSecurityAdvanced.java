/*
 * Copyright (c) 2010-2018 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyExceptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityAdvanced extends AbstractSecurityTest {

	private static final String AUTHORIZATION_ACTION_WORKITEMS = "http://midpoint.evolveum.com/xml/ns/public/security/authorization-ui-3#myWorkItems";
	private static final String BIG_BADA_BOOM = "bigBadaBoom";
	private static final String HUGE_BADA_BOOM = "hugeBadaBoom";
	private static final String FIRST_RULE = "firstRule";
	
	protected static final File ROLE_LIMITED_ROLE_ADMINISTRATOR_FILE = new File(TEST_DIR, "role-limited-role-administrator.xml");
	protected static final String ROLE_LIMITED_ROLE_ADMINISTRATOR_OID = "ce67b472-e5a6-11e7-98c3-174355334559";
	
	protected static final File ROLE_EXCLUSION_PIRATE_FILE = new File(TEST_DIR, "role-exclusion-pirate.xml");
	protected static final String ROLE_EXCLUSION_PIRATE_OID = "cf60ec66-e5a8-11e7-a997-ab32b7ec5fdb";
	
	protected static final File ROLE_MAXASSIGNEES_10_FILE = new File(TEST_DIR, "role-maxassignees-10.xml");
	protected static final String ROLE_MAXASSIGNEES_10_OID = "09dadf60-f6f1-11e7-8223-a72f04f867e7";
	
	protected static final File ROLE_MODIFY_POLICY_EXCEPTION_FILE = new File(TEST_DIR, "role-modify-policy-exception.xml");
	protected static final String ROLE_MODIFY_POLICY_EXCEPTION_OID = "09e9acde-f787-11e7-987c-13212be79c7d";
	
	protected static final File ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_FILE = new File(TEST_DIR, "role-modify-policy-exception-situation.xml");
	protected static final String ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_OID = "45bee61c-f79f-11e7-a2a7-27ade881c9e0";
	
	protected static final File ROLE_MODIFY_DESCRIPTION_FILE = new File(TEST_DIR, "role-modify-description.xml");
	protected static final String ROLE_MODIFY_DESCRIPTION_OID = "1a0616e4-f79a-11e7-80c9-d77b403e1a81";
	
	protected static final File ROLE_PROP_EXCEPT_ASSIGNMENT_FILE = new File(TEST_DIR, "role-prop-except-assignment.xml");
	protected static final String ROLE_PROP_EXCEPT_ASSIGNMENT_OID = "bc0f3bfe-029f-11e8-995d-273b6606fd79";
	
	protected static final File ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_FILE = new File(TEST_DIR, "role-prop-except-administrative-status.xml");
	protected static final String ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID = "cc549256-02a5-11e8-994e-43c307e2a819";


	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_LIMITED_ROLE_ADMINISTRATOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_MAXASSIGNEES_10_FILE, initResult);
		repoAddObjectFromFile(ROLE_MODIFY_POLICY_EXCEPTION_FILE, initResult);
		repoAddObjectFromFile(ROLE_MODIFY_POLICY_EXCEPTION_SITUATION_FILE, initResult);
		repoAddObjectFromFile(ROLE_MODIFY_DESCRIPTION_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_EXCEPT_ASSIGNMENT_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_FILE, initResult);

		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_SECURITY_OID, initResult);
	}
	
	protected static final int NUMBER_OF_IMPORTED_ROLES = 7;
	
	protected int getNumberOfRoles() {
		return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
	}


	@Test
    public void test100AutzJackPersonaManagement() throws Exception {
		final String TEST_NAME = "test100AutzJackPersonaManagement";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_LECHUCK_OID, 1);
        assignRole(USER_LECHUCK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_LECHUCK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_DELEGATOR_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        
        assertDeputySearchDelegatorRef(USER_JACK_OID, USER_BARBOSSA_OID);
        assertDeputySearchAssignmentTarget(USER_JACK_OID, USER_BARBOSSA_OID);

        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
        display("Logged in as Barbossa");

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        login(USER_JACK_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
        displayWhen(TEST_NAME);
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
	
	/**
	 * Assign a deputy, but this time with validFrom and validTo set to the future.
	 * The delegator role does NOT allow access to inactive delegations.
	 * MID-4172
	 */
	@Test
    public void test122AutzJackDelagatorValidity() throws Exception {
		final String TEST_NAME = "test122AutzJackDelagatorValidity";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_DELEGATOR_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        assertAssignedRole(userJack, ROLE_DELEGATOR_OID);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(XmlTypeConverter.addDuration(startTs, "PT2H"));
        activationType.setValidTo(XmlTypeConverter.addDuration(startTs, "P1D"));
        
        // Good direction
        assertAllow("delegate to Barbossa",
        	(task, result) -> {
        		assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, 
        				assignment -> assignment.setActivation(activationType), task, result);
			});

        userJack = getUser(USER_JACK_OID);
        display("Jack delegator", userJack);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        // Delegation is not active yet. Therefore jack cannot see it.
        assertAssignments(userBarbossa, 0);
        
        assertDeputySearchDelegatorRef(USER_JACK_OID /* nothing */);
        assertDeputySearchAssignmentTarget(USER_JACK_OID, USER_BARBOSSA_OID); // WRONG!!!
//        assertDeputySearchAssignmentTarget(USER_JACK_OID /* nothing */);

        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
        
        assertDeputySearchDelegatorRef(USER_JACK_OID, USER_BARBOSSA_OID);
        assertDeputySearchAssignmentTarget(USER_JACK_OID, USER_BARBOSSA_OID);
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
        displayWhen(TEST_NAME);
        display("Logged in as Barbossa");

        // Delegation is not active any more. No access.
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();


        login(USER_JACK_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa",
        	(task, result) -> {
        		unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, 
        				assignment -> assignment.setActivation(activationType), task, result);
        	});

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        assertGlobalStateUntouched();

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
	
	/**
	 * Assign a deputy with validity. But this time there is a role that allows
	 * access to inactive delegations.
	 * MID-4172
	 */
	@Test
    public void test124AutzJackDelagatorPlusValidity() throws Exception {
		final String TEST_NAME = "test124AutzJackDelagatorPlusValidity";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_DELEGATOR_PLUS_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);
        assertAssignedRole(userJack, ROLE_DELEGATOR_PLUS_OID);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(XmlTypeConverter.addDuration(startTs, "PT2H"));
        activationType.setValidTo(XmlTypeConverter.addDuration(startTs, "P1D"));
        
        // Good direction
        assertAllow("delegate to Barbossa",
        	(task, result) -> {
        		assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, 
        				assignment -> assignment.setActivation(activationType), task, result);
			});

        userJack = getUser(USER_JACK_OID);
        display("Jack delegator", userJack);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        display("Barbossa delegate", userBarbossa);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);
        
        // delegatorRef is allowed, but returns nothing. The delegation is not yet active, it is not in the delgatorRef.
        assertDeputySearchDelegatorRef(USER_JACK_OID /* nothing */);
        assertDeputySearchAssignmentTarget(USER_JACK_OID, USER_BARBOSSA_OID);

        // Non-delegate. We should be able to read just the name. Not the assignments.
        PrismObject<UserType> userRum = getUser(userRumRogersOid);
        display("User Rum Rogers", userRum);
        assertNoAssignments(userRum);

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
        
        assertDeputySearchDelegatorRef(USER_JACK_OID, USER_BARBOSSA_OID);
        assertDeputySearchAssignmentTarget(USER_JACK_OID, USER_BARBOSSA_OID);        
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
        assertDeputySearchDelegatorRef(USER_JACK_OID /* nothing */);
        assertDeputySearchAssignmentTarget(USER_JACK_OID, USER_BARBOSSA_OID);
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
        display("Logged in as Barbossa");

        // Delegation is not active any more. No access.
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();


        login(USER_JACK_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa",
        	(task, result) -> {
        		unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, 
        				assignment -> assignment.setActivation(activationType), task, result);
        	});

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 1);

        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);

        assertGlobalStateUntouched();

        login(USER_BARBOSSA_USERNAME);
        // WHEN
        displayWhen(TEST_NAME);
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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);

        PrismObject<UserType> userCobbBefore = getUser(userCobbOid);
        IntegrationTestTools.display("User cobb before", userCobbBefore);
        assertRoleMembershipRef(userCobbBefore, ROLE_ORDINARY_OID, ROLE_UNINTERESTING_OID, ORG_SCUMM_BAR_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assignRole(USER_JACK_OID, ROLE_READ_BASIC_ITEMS_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
		final String TEST_NAME = "test154AutzJackApproverRead";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_BASIC_ITEMS_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        assertSearch(RoleType.class, null, getNumberOfRoles());
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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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

        assertSearch(RoleType.class, null, getNumberOfRoles());
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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_WRONG_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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

        assertSearch(RoleType.class, null, getNumberOfRoles());
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
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_ROLE_MEMBERS_NONE_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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

        assertSearch(RoleType.class, null, getNumberOfRoles());
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

	/**
	 * User template will assign organizations to this user. However, the user
	 * does not have request authorization for organizations. Check that this
	 * proceeds smoothly.
	 * MID-3996
	 */
	@Test
    public void test200AutzJackModifyOrgunit() throws Exception {
		final String TEST_NAME = "test200AutzJackModifyOrgunit";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_SELF_MODIFY_ORGUNIT_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        		UserType.F_ORGANIZATIONAL_UNIT, createPolyString(ORG_SCUMM_BAR_NAME));

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 2);
        assertAssignedOrg(user, ORG_SCUMM_BAR_OID);

        assertModifyAllow(UserType.class, USER_JACK_OID,
        		UserType.F_ORGANIZATIONAL_UNIT, createPolyString(ORG_MINISTRY_OF_RUM_NAME));

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
		final String TEST_NAME = "test202AutzJackModifyOrgunitAndAssignRole";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_SELF_MODIFY_ORGUNIT_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        					ROLE_BUSINESS_1_OID, RoleType.COMPLEX_TYPE, null, null, true);
        			focusDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT, createPolyString(ORG_SCUMM_BAR_NAME));
        			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(focusDelta);
        			modelService.executeChanges(deltas, null, task, result);
        		});

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 4);
        assertAssignedOrg(user, ORG_SCUMM_BAR_OID);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertModifyAllow(UserType.class, USER_JACK_OID,
        		UserType.F_ORGANIZATIONAL_UNIT, createPolyString(ORG_MINISTRY_OF_RUM_NAME));

        user = getUser(USER_JACK_OID);
        display("Jack in medias res", user);
        assertAssignments(user, 4);
        assertAssignedOrg(user, ORG_MINISTRY_OF_RUM_OID);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertModifyAllow(UserType.class, USER_JACK_OID,
        		UserType.F_ORGANIZATIONAL_UNIT);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        assertNotAssignedOrg(user, ORG_MINISTRY_OF_RUM_OID);

        assertAllow("unassign role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertNotAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertGlobalStateUntouched();
	}

	/**
	 * Role with object filter that has an expression.
	 * No costCenter in user, no access.
	 * MID-4191
	 */
	@Test
    public void test220AutzJackRoleExpressionNoConstCenter() throws Exception {
		final String TEST_NAME = "test220AutzJackRoleExpressionNoConstCenter";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EXPRESSION_READ_ROLES_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetDeny(RoleType.class, ROLE_BUSINESS_1_OID);
        assertGetDeny(RoleType.class, ROLE_BUSINESS_2_OID);
        assertGetDeny(RoleType.class, ROLE_APPLICATION_1_OID);
        assertGetDeny(RoleType.class, ROLE_EXPRESSION_READ_ROLES_OID);

        assertSearchDeny(RoleType.class, null, null);
        assertSearchDeny(RoleType.class, 
        		queryFor(RoleType.class).item(RoleType.F_ROLE_TYPE).eq("business").build(),
        		null);
        assertSearchDeny(RoleType.class, 
        		queryFor(RoleType.class).item(RoleType.F_ROLE_TYPE).eq("application").build(),
        		null);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Role with object filter that has an expression.
	 * MID-4191
	 */
	@Test
    public void test222AutzJackRoleExpressionConstCenterBusiness() throws Exception {
		final String TEST_NAME = "test222AutzJackRoleExpressionConstCenterBusiness";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_EXPRESSION_READ_ROLES_OID);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, UserType.F_COST_CENTER, task, result, "business");

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetAllow(RoleType.class, ROLE_BUSINESS_1_OID);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_2_OID);
        assertGetDeny(RoleType.class, ROLE_APPLICATION_1_OID);
        assertGetDeny(RoleType.class, ROLE_EXPRESSION_READ_ROLES_OID);

        assertSearch(RoleType.class, null, 3);
        assertSearch(RoleType.class, 
        		queryFor(RoleType.class).item(RoleType.F_ROLE_TYPE).eq("business").build(), 3);
        assertSearchDeny(RoleType.class, 
        		queryFor(RoleType.class).item(RoleType.F_ROLE_TYPE).eq("application").build(),
        		null);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Unlimited power of attorney. But only granted to Caribbean users.
	 * MID-4072, MID-4205
	 */
	@Test
    public void test230AttorneyCaribbeanUnlimited() throws Exception {
		final String TEST_NAME = "test230AttorneyCaribbeanUnlimited";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_CARIBBEAN_UNLIMITED_OID);
        
        cleanupAutzTest(USER_BARBOSSA_OID);
        // Give some roles to barbossa first to really do something when we switch identity to him
        assignRole(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        display("donorFilterAll", donorFilterAll);
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
		final String TEST_NAME = "test232ManagerAttorneyNoOrg";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS_OID);
        
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        display("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 0);
        
        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        display("donorFilterWorkitems", donorFilterWorkitems);
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
		final String TEST_NAME = "test234ManagerAttorneyRum";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        display("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 4);
        
        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        display("donorFilterWorkitems", donorFilterWorkitems);
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
		final String TEST_NAME = "test234ManagerAttorneyRum";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        
        assignRole(userRumRogersOid, ROLE_APPROVER_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        display("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 4);
        
        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        display("donorFilterWorkitems", donorFilterWorkitems);
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
		final String TEST_NAME = "test236ManagerAttorneyCaribbeanRum";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_CARIBBEAN_UNLIMITED_OID);
        assignRole(USER_JACK_OID, ROLE_ATTORNEY_MANAGER_WORKITEMS_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        
        assignRole(userRumRogersOid, ROLE_APPROVER_OID);
        assignRole(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter donorFilterAll = modelInteractionService.getDonorFilter(UserType.class, null, null, task, result);
        display("donorFilterAll", donorFilterAll);
        assertSearchFilter(UserType.class, donorFilterAll, 5);
        
        ObjectFilter donorFilterWorkitems = modelInteractionService.getDonorFilter(UserType.class, null, AUTHORIZATION_ACTION_WORKITEMS, task, result);
        display("donorFilterWorkitems", donorFilterWorkitems);
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
        cleanupUnassign(userRumRogersOid, ROLE_APPROVER_OID);
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-4204
	 */
	@Test
    public void test250AssignRequestableSelfOtherApporver() throws Exception {
		final String TEST_NAME = "test250AssignRequestableSelfOtherApporver";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_OID);

        cleanupUnassign(userRumRogersOid, ROLE_APPROVER_OID);
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
        
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_OID);

        assertAllow("assign business role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        // default relation, non-requestable role
        assertDeny("assign application role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        assertAllow("unassign business role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        // wrong relation
        assertDeny("assign business role to jack (manager)",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, SchemaConstants.ORG_MANAGER, task, result));
        
        // requestable role, but assign to a different user
        assertDeny("assign application role to barbossa",
        		(task, result) -> assignRole(USER_BARBOSSA_OID, ROLE_BUSINESS_1_OID, task, result));

        assertAllow("assign business role to barbossa (approver)",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("unassign business role to barbossa (approver)",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("assign business role to barbossa (owner)",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, SchemaConstants.ORG_OWNER, task, result));

        assertAllow("unassign business role to barbossa (owner)",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, SchemaConstants.ORG_OWNER, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertAssignments(userBarbossa, 0);

        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-4204
	 */
	@Test
    public void test252AssignRequestableSelfOtherApporverEmptyDelta() throws Exception {
		final String TEST_NAME = "test252AssignRequestableSelfOtherApporverEmptyDelta";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_OID);

        cleanupUnassign(userRumRogersOid, ROLE_APPROVER_OID);
        cleanupUnassign(USER_BARBOSSA_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
        
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        final PrismObject<UserType> user1 = getUser(USER_JACK_OID);
        assertAssignments(user1, 1);
        assertAssignedRole(user1, ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_OID);

        assertAllow("assign business role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        final PrismObject<UserType> user2 = getUser(USER_JACK_OID);
        assertAssignments(user2, 2);
        assertAssignedRole(user2, ROLE_BUSINESS_1_OID);

        // default relation, non-requestable role
        assertDeny("assign application role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        assertAllow("unassign business role from jack",
        		(task, result) -> deleteFocusAssignmentEmptyDelta(user2, ROLE_BUSINESS_1_OID, task, result));

        // wrong relation
        assertDeny("assign business role to jack (manager)",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, SchemaConstants.ORG_MANAGER, task, result));
        
        // requestable role, but assign to a different user
        assertDeny("assign application role to barbossa",
        		(task, result) -> assignRole(USER_BARBOSSA_OID, ROLE_BUSINESS_1_OID, task, result));

        assertAllow("assign business role to barbossa (approver)",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("unassign business role to barbossa (approver)",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, SchemaConstants.ORG_APPROVER, task, result));

        assertAllow("assign business role to barbossa (owner)",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, SchemaConstants.ORG_OWNER, task, result));

        final PrismObject<UserType> user3 = getUser(USER_JACK_OID);
        assertAssignments(user3, 2);
        assertAssignedRole(user3, ROLE_BUSINESS_2_OID);
        
        assertAllow("unassign business role to barbossa (owner)",
        		(task, result) -> deleteFocusAssignmentEmptyDelta(user3, ROLE_BUSINESS_2_OID, SchemaConstants.ORG_OWNER, task, result));

        final PrismObject<UserType> user4 = getUser(USER_JACK_OID);
        assertAssignments(user4, 1);
        
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertAssignments(userBarbossa, 0);

        assertGlobalStateUntouched();
	}
	
	@Test
    public void test254AssignUnassignRequestableSelf() throws Exception {
		final String TEST_NAME = "test254AssignUnassignRequestableSelf";
        displayTestTitle(TEST_NAME);
        // GIVENds
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UNASSIGN_SELF_REQUESTABLE_OID);
        assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_UNASSIGN_SELF_REQUESTABLE_OID);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertAllow("unassign business role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_UNASSIGN_SELF_REQUESTABLE_OID);
        
        assertDeny("unassign ROLE_UNASSIGN_SELF_REQUESTABLE role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_UNASSIGN_SELF_REQUESTABLE_OID, task, result));
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test256AssignUnassignRequestableSelfEmptyDelta() throws Exception {
		final String TEST_NAME = "test256AssignUnassignRequestableSelfEmptyDelta";
        displayTestTitle(TEST_NAME);
        // GIVENds
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UNASSIGN_SELF_REQUESTABLE_OID);
        assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        final PrismObject<UserType> user1 = getUser(USER_JACK_OID);
        assertAssignments(user1, 2);
        assertAssignedRole(user1, ROLE_UNASSIGN_SELF_REQUESTABLE_OID);
        assertAssignedRole(user1, ROLE_BUSINESS_1_OID);

        assertAllow("unassign business role from jack",
        		(task, result) -> deleteFocusAssignmentEmptyDelta(user1, ROLE_BUSINESS_1_OID, task, result));

        final PrismObject<UserType> user2 = getUser(USER_JACK_OID);
        assertAssignments(user2, 1);
        assertAssignedRole(user2, ROLE_UNASSIGN_SELF_REQUESTABLE_OID);
        
        assertDeny("unassign ROLE_UNASSIGN_SELF_REQUESTABLE role from jack",
        		(task, result) -> deleteFocusAssignmentEmptyDelta(user2, ROLE_UNASSIGN_SELF_REQUESTABLE_OID, task, result));
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test260AutzJackLimitedRoleAdministrator() throws Exception {
		final String TEST_NAME = "test260AutzJackLimitedRoleAdministrator";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_ROLE_ADMINISTRATOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
		display("Exclusion role edit schema", roleExclusionEditSchema);
		assertItemFlags(roleExclusionEditSchema, RoleType.F_NAME, true, true, true);
		assertItemFlags(roleExclusionEditSchema, RoleType.F_DESCRIPTION, true, true, true);
		assertItemFlags(roleExclusionEditSchema, RoleType.F_ROLE_TYPE, true, true, true);
		assertItemFlags(roleExclusionEditSchema, RoleType.F_LIFECYCLE_STATE, true, true, true);
		assertItemFlags(roleExclusionEditSchema, RoleType.F_METADATA, false, false, false);
		
		assertItemFlags(roleExclusionEditSchema, RoleType.F_ASSIGNMENT, true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_DESCRIPTION),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION),
				false, false, false);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_EVALUATION_TARGET),
				false, false, false);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES),
				false, false, false);
		
		assertItemFlags(roleExclusionEditSchema, RoleType.F_INDUCEMENT, true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_DESCRIPTION),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_EVALUATION_TARGET),
				true, true, true);
		assertItemFlags(roleExclusionEditSchema, 
				new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES),
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
		final String TEST_NAME = "test260AutzJackLimitedRoleAdministrator";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_ROLE_ADMINISTRATOR_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

		assertReadAllow();
		assertReadDenyRaw();
        assertAddDeny();
        assertDeleteDeny();
        
        // check ROLE_ASSIGN_APPLICATION_ROLES_OID authorizations
        
        assertAllow("assign application role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertDeny("assign business role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        assertAllow("unassign application role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
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
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test270AutzJackModifyPolicyException() throws Exception {
		final String TEST_NAME = "test270AutzJackModifyPolicyException";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
		final String TEST_NAME = "test272AutzJackModifyPolicyExceptionFirstRule";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_POLICY_EXCEPTION_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

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
        			ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_EMPTY_OID,
					    		new ItemPath(new NameItemPathSegment(RoleType.F_POLICY_EXCEPTION)),
					    		prismContext, idOnlyPolicyException3);
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
		final String TEST_NAME = "test274AutzJackModifyPolicyExceptionSituation";
        displayTestTitle(TEST_NAME);
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

	@Test
    public void test300AutzJackExceptAssignment() throws Exception {
		final String TEST_NAME = "test300AutzJackExceptAssignment";
		displayTestTitle(TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
		modifyJackValidTo();
		login(USER_JACK_USERNAME);

		// WHEN
		displayWhen(TEST_NAME);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, createPolyString(USER_JACK_GIVEN_NAME));
		PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO);
		assertAssignments(userJack, 0);

		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, false, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), false, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, false);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, false, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

		assertAddDeny();

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyDeny(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
		assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
		
		assertDeny("assign business role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

		assertDeleteDeny();

		assertGlobalStateUntouched();
	}
	
	@Test
    public void test302AutzJackExceptAdministrativeStatus() throws Exception {
		final String TEST_NAME = "test302AutzJackExceptAdministrativeStatus";
		displayTestTitle(TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID);
		modifyJackValidTo();
		login(USER_JACK_USERNAME);

		// WHEN
		displayWhen(TEST_NAME);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, createPolyString(USER_JACK_GIVEN_NAME));
		PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AGEAD);
		assertAssignments(userJack, 1);

		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, false);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

		assertAddDeny();

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
		assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
		
		assertAllow("assign business role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

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
		final String TEST_NAME = "test304AutzJackPropExceptAssignmentReadSomeModifySomeUser";
		displayTestTitle(TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
		modifyJackValidTo();
		login(USER_JACK_USERNAME);

		// WHEN
		displayWhen(TEST_NAME);

		PrismObject<UserType> userJack = assertAlmostFullJackRead(2);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		// read of validTo is not allowed be either role
		PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO);
		
		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, false, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

		assertAddDeny();

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));

		assertDeleteDeny();

		assertGlobalStateUntouched();
	}
	
	/**
	 * Test to properly merge two roles with exceptItem specifications.
	 */
	@Test
    public void test306AutzJackPropExceptAssignmentExceptAdministrativeStatus() throws Exception {
		final String TEST_NAME = "test306AutzJackPropExceptAssignmentExceptAdministrativeStatus";
		displayTestTitle(TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ADMINISTRATIVE_STATUS_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
		modifyJackValidTo();
		login(USER_JACK_USERNAME);

		// WHEN
		displayWhen(TEST_NAME);

		PrismObject<UserType> userJack = assertAlmostFullJackRead(2);
		// read of administrativeStatus is not allowed be either role
		PrismAsserts.assertNoItem(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AGEAD);

		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, false, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);
		
		assertAddDeny();

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));

		assertDeleteDeny();

		assertGlobalStateUntouched();
	}
	
	/**
	 * Test for combination of exceptItem(assignment) with #assign/#unassign authorizations.
	 */
	@Test
    public void test308AutzJackPropExceptAssignmentAssignApplicationRoles() throws Exception {
		final String TEST_NAME = "test308AutzJackPropExceptAssignmentAssignApplicationRoles";
		displayTestTitle(TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
		modifyJackValidTo();
		login(USER_JACK_USERNAME);

		// WHEN
		displayWhen(TEST_NAME);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, createPolyString(USER_JACK_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_VALID_TO, JACK_VALID_TO_LONG_AGEAD);
		assertAssignments(userJack, 2);

		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_FROM, true, false, false);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_VALID_TO, true, false, true);
		assertItemFlags(userJackEditSchema, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, true, false, true);

		assertAddDeny();

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyDeny(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO,
				JACK_VALID_FROM_LONG_AGO);
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
		assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));

		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
		
		assertDeny("assign business 1 role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

		assertAllow("assign application 1 role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

		userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 3);
        assertAssignedRole(userJack, ROLE_APPLICATION_1_OID);
        
        assertDeny("assign business 2 role to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        assertAllow("unassign application 1 role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec, "application", "nonexistent");
        assertFilter(spec.getFilter(), TypeFilter.class);

        assertAllowRequestAssignmentItems(USER_JACK_OID, ROLE_APPLICATION_1_OID,
        		SchemaConstants.PATH_ASSIGNMENT_TARGET_REF, 
        		SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_FROM,
        		SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_TO);
		
		assertGlobalStateUntouched();
	}
	
    private void modifyJackValidTo() throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
    	Task task = createTask("modifyJackValidTo");
    	OperationResult result = task.getResult();
		modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, task, result, JACK_VALID_TO_LONG_AGEAD);
		assertSuccess(result);
	}


	private PrismObject<UserType> assertAlmostFullJackRead(int expectedTargetAssignments) throws Exception {
    	PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, createPolyString(USER_JACK_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(userJack, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ActivationStatusType.ENABLED);
		assertAssignmentsWithTargets(userJack, expectedTargetAssignments);
		
//		assertJackEditSchemaReadSomeModifySome(userJack);
		
		return userJack;
	}

	
//	@Test
//    public void test302AutzJackPropExceptAssignmentReadSomeModifySomeUser() throws Exception {
//		final String TEST_NAME = "test216AutzJackPropReadSomeModifySomeUser";
//		displayTestTitle(TEST_NAME);
//		// GIVEN
//		cleanupAutzTest(USER_JACK_OID);
//		assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID);
//		assignRole(USER_JACK_OID, ROLE_PROP_EXCEPT_ASSIGNMENT_OID);
//		login(USER_JACK_USERNAME);
//
//		// WHEN
//		displayWhen(TEST_NAME);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("Jack", userJack);
//		assertUserJackReadSomeModifySome(userJack, 1);
//		assertJackEditSchemaReadSomeModifySome(userJack);
//
//		PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
//		display("Guybrush", userGuybrush);
//		assertNull("Unexpected Guybrush", userGuybrush);
//
//		assertAddDeny();
//
//		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
//		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
//				JACK_VALID_FROM_LONG_AGO);
//		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
//
//		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
//		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
//		assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));
//
//		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
//		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
//
//		assertDeleteDeny();
//
//		assertGlobalStateUntouched();
//	}
	

	private PolicyExceptionType assertPolicyException(PrismObject<RoleType> role, String expectedRuleName, String expectedPolicySituation) {
    	List<PolicyExceptionType> policyExceptions = role.asObjectable().getPolicyException();
        assertEquals("Wrong size of policyException container in "+role, 1, policyExceptions.size());
        PolicyExceptionType policyException = policyExceptions.get(0);
        assertEquals("Wrong rule name in "+role, expectedRuleName, policyException.getRuleName());
        assertEquals("Wrong situation in "+role, expectedPolicySituation, policyException.getPolicySituation());
        return policyException;
	}


	private AssignmentType assertExclusion(PrismObject<RoleType> roleExclusion, String excludedRoleOid) {
        PrismContainer<AssignmentType> assignmentContainer = roleExclusion.findContainer(RoleType.F_ASSIGNMENT);
        assertNotNull("No assignment container in "+roleExclusion, assignmentContainer);
        assertEquals("Wrong size of assignment container in "+roleExclusion, 1, assignmentContainer.size());
        AssignmentType exclusionAssignment = assignmentContainer.getValue().asContainerable();
        PolicyRuleType exclusionPolicyRule = exclusionAssignment.getPolicyRule();
        assertNotNull("No policy rule in "+roleExclusion, exclusionPolicyRule);
        PolicyConstraintsType exclusionPolicyConstraints = exclusionPolicyRule.getPolicyConstraints();
        assertNotNull("No policy rule constraints in "+roleExclusion, exclusionPolicyConstraints);
        List<ExclusionPolicyConstraintType> exclusionExclusionPolicyConstraints = exclusionPolicyConstraints.getExclusion();
        assertEquals("Wrong size of exclusion policy constraints in "+roleExclusion, 1, exclusionExclusionPolicyConstraints.size());
        ExclusionPolicyConstraintType exclusionPolicyConstraint = exclusionExclusionPolicyConstraints.get(0);
        assertNotNull("No exclusion policy constraint in "+roleExclusion, exclusionPolicyConstraint);
        ObjectReferenceType targetRef = exclusionPolicyConstraint.getTargetRef();
        assertNotNull("No targetRef in exclusion policy constraint in "+roleExclusion, targetRef);
        assertEquals("Wrong OID targetRef in exclusion policy constraint in "+roleExclusion, excludedRoleOid, targetRef.getOid());
        return exclusionAssignment;
	}


	@Override
    protected void cleanupAutzTest(String userOid, int expectedAssignments) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
    	super.cleanupAutzTest(userOid, expectedAssignments);

        Task task = taskManager.createTaskInstance(TestSecurityAdvanced.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();

        assignRole(userRumRogersOid, ROLE_ORDINARY_OID, task, result);
		assignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, task, result);
		assignRole(userCobbOid, ROLE_ORDINARY_OID, task, result);
		assignRole(userCobbOid, ROLE_UNINTERESTING_OID, task, result);

	}
    
	private void assertDeputySearchDelegatorRef(String delegatorOid, String... expectedDeputyOids) throws Exception {
		PrismReferenceValue rval = new PrismReferenceValue(delegatorOid, UserType.COMPLEX_TYPE);
		rval.setRelation(SchemaConstants.ORG_DEPUTY);
		ObjectQuery query = queryFor(UserType.class).item(UserType.F_DELEGATED_REF).ref(rval).build();
		assertSearch(UserType.class, query, expectedDeputyOids);
	}
	
	private void assertDeputySearchAssignmentTarget(String delegatorOid, String... expectedDeputyOids) throws Exception {
		PrismReferenceValue rval = new PrismReferenceValue(delegatorOid, UserType.COMPLEX_TYPE);
		rval.setRelation(SchemaConstants.ORG_DEPUTY);
		ObjectQuery query = queryFor(UserType.class)
				.item(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)).ref(rval).build();
		assertSearch(UserType.class, query, expectedDeputyOids);
	}


	@Override
	protected void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException, ConfigurationException,
			ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		super.cleanupAutzTest(userOid);
		
		Task task = taskManager.createTaskInstance(TestSecurityAdvanced.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();
        
		cleanupDelete(RoleType.class, ROLE_EXCLUSION_PIRATE_OID, task, result);
	}

	
	
}
