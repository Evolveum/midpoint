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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyExceptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityBasic extends AbstractSecurityTest {

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@Test
    public void test200AutzJackNoRole() throws Exception {
		final String TEST_NAME = "test200AutzJackNoRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test201AutzJackSuperuserRole() throws Exception {
		final String TEST_NAME = "test201AutzJackSuperuserRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SUPERUSER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertSuperuserAccess(NUMBER_OF_ALL_USERS);
        
        assertGlobalStateUntouched();
	}
	

	@Test
    public void test202AutzJackReadonlyRole() throws Exception {
		final String TEST_NAME = "test202AutzJackReadonlyRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertReadCertCasesAllow();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}

	/**
	 * Authorized only for request but not execution. Everything should be denied.
	 */
	@Test
    public void test202rAutzJackReadonlyReqRole() throws Exception {
		final String TEST_NAME = "test202rAutzJackReadonlyReqRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_REQ_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}
	
	/**
	 * Authorized only for execution but not request. Everything should be denied.
	 */
	@Test
    public void test202eAutzJackReadonlyExecRole() throws Exception {
		final String TEST_NAME = "test202eAutzJackReadonlyExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_EXEC_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}
	
	@Test
    public void test202reAutzJackReadonlyReqExecRole() throws Exception {
		final String TEST_NAME = "test202reAutzJackReadonlyReqExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}

	@Test
    public void test203AutzJackReadonlyDeepRole() throws Exception {
		final String TEST_NAME = "test203AutzJackReadonlyDeepRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}
	
	@Test
    public void test203eAutzJackReadonlyDeepExecRole() throws Exception {
		final String TEST_NAME = "test203eAutzJackReadonlyDeepExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test204AutzJackSelfRole() throws Exception {
		final String TEST_NAME = "test204AutzJackSelfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_OID);
		assignRole(USER_JACK_OID, ROLE_READ_JACKS_CAMPAIGNS_OID);		// we cannot specify "own campaigns" yet
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        
        assertVisibleUsers(1);
        // The search with ObjectClass is important. It is a very different case
        // than searching just for UserType
        assertSearch(ObjectType.class, null, 2);		// user + campaign

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

		assertReadCertCases(2);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test205AutzJackObjectFilterModifyCaribbeanfRole() throws Exception {
		final String TEST_NAME = "test205AutzJackObjectFilterModifyCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test207AutzJackObjectFilterCaribbeanRole() throws Exception {
		final String TEST_NAME = "test207AutzJackObjectFilterCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_CARIBBEAN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 2);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3647
	 */
	@Test
    public void test208AutzJackReadSomeRoles() throws Exception {
		final String TEST_NAME = "test208AutzJackReadSomeRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_SOME_ROLES_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertSearch(UserType.class, null, 0);
        assertSearch(RoleType.class, null, 5);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        
        assertGetDeny(RoleType.class, ROLE_SUPERUSER_OID);
        assertGetDeny(RoleType.class, ROLE_SELF_OID);
        assertGetDeny(RoleType.class, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assertGetAllow(RoleType.class, ROLE_APPLICATION_1_OID);
        assertGetAllow(RoleType.class, ROLE_APPLICATION_2_OID);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_1_OID);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_2_OID);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_3_OID);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3126
	 */
	@Test
    public void test210AutzJackPropReadAllModifySome() throws Exception {
		final String TEST_NAME = "test210AutzJackPropReadAllModifySome";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertJackEditSchemaReadAllModifySome(userJack);
		
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3126
	 */
	@Test
    public void test211AutzJackPropReadAllModifySomeUser() throws Exception {
		final String TEST_NAME = "test211AutzJackPropReadAllModifySomeUser";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
		assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
		assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
		assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
		
		assertSearch(UserType.class, null, 1);
		assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
		assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
		assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
		assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertJackEditSchemaReadAllModifySome(userJack);
                
        assertGlobalStateUntouched();
	}
	
	private void assertJackEditSchemaReadAllModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException {
		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), true, false, false);
	}
	
	@Test
    public void test215AutzJackPropReadSomeModifySome() throws Exception {
		final String TEST_NAME = "test215AutzJackPropReadSomeModifySome";
		testAutzJackPropReadSomeModifySome(TEST_NAME, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
	}

	@Test
    public void test215reAutzJackPropReadSomeModifySomeReqExec() throws Exception {
		final String TEST_NAME = "test215reAutzJackPropReadSomeModifySomeReqExec";
		testAutzJackPropReadSomeModifySome(TEST_NAME, ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_OID);
	}
	
	/**
	 * MID-3126
	 */
    @Test
    public void test216AutzJackPropReadSomeModifySomeUser() throws Exception {
		final String TEST_NAME = "test216AutzJackPropReadSomeModifySomeUser";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID);
		login(USER_JACK_USERNAME);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
				
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertUserJackReadSomeModifySome(userJack);
		assertJackEditSchemaReadSomeModifySome(userJack);
		
		PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
		display("Guybrush", userGuybrush);
		assertNull("Unexpected Guybrush", userGuybrush);
		
		assertAddDeny();
		
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
				JACK_VALID_FROM_LONG_AGO);
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
		
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
		assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));
		
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
		
		assertDeleteDeny();
		
		assertGlobalStateUntouched();
	}
    
    private void assertUserJackReadSomeModifySome(PrismObject<UserType> userJack) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
    	
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
			ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_FAMILY_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
		PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		assertAssignmentsWithTargets(userJack, 1);
    }
    
    private void assertJackEditSchemaReadSomeModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException {
    	PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);
    }

    public void testAutzJackPropReadSomeModifySome(final String TEST_NAME, String roleOid) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, roleOid);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertUserJackReadSomeModifySome(userJack);
		assertJackEditSchemaReadSomeModifySome(userJack);
		
        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
            	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 1);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
				JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    
    @Test
    public void test218AutzJackPropReadSomeModifySomeExecAll() throws Exception {
		final String TEST_NAME = "test218AutzJackPropReadSomeModifySomeExecAll";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_JACK_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userJack, 1);
        
        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        display("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);
        
        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
            	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 1);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString("Brethren of the Coast"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    @Test
    public void test220AutzJackPropDenyModifySome() throws Exception {
		final String TEST_NAME = "test220AutzJackPropDenyModifySome";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_DENY_MODIFY_SOME_OID);
		login(USER_JACK_USERNAME);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		assertReadAllow();
				
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_JACK_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_JACK_FAMILY_NAME));
		PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
			ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
		assertAssignmentsWithTargets(userJack, 1);
		
		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, true, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, true, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, true, false);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, true, false);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, true, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, true, true);
		
		PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
		display("Guybrush", userGuybrush);
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FAMILY_NAME));
		PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
		    	ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
		assertAssignmentsWithTargets(userGuybrush, 1);
		
		assertAddAllow();
		
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString("Brethren of the Coast"));
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Jackie"));
		
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Brushie"));
		assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Hectie"));
				
		assertDeleteAllow();
		
		assertGlobalStateUntouched();
	}
    
	@Test
    public void test230AutzJackMasterMinistryOfRum() throws Exception {
		final String TEST_NAME = "test230AutzJackMasterMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MASTER_MINISTRY_OF_RUM_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(3);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetAllow(UserType.class, userCobbOid);
        assertAddAllow(USER_MANCOMB_FILE);
        
        assertVisibleUsers(4);
        
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
        
        assertVisibleUsers(3);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test232AutzJackReadOrgMinistryOfRum() throws Exception {
		final String TEST_NAME = "test232AutzJackReadOrgMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertSearch(OrgType.class, null, 1);
        // The search wit ObjectClass is important. It is a very different case
        // than searching just for UserType or OrgType
        assertSearch(ObjectType.class, null, 1);
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertAddDeny(USER_MANCOMB_FILE);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test240AutzJackManagerFullControlNoOrg() throws Exception {
		final String TEST_NAME = "test240AutzJackManagerFullControlNoOrg";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetDeny(UserType.class, userCobbOid);
        assertAddDeny(USER_MANCOMB_FILE);
        
        assertVisibleUsers(0);
        
        assertGetDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 0);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assertDeleteDeny(UserType.class, USER_ESTEVAN_OID);
        
        assertGetDeny(ShadowType.class, accountOid);
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertSearch(ShadowType.class, ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext)), 0);
                
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test241AutzJackManagerFullControlMemberMinistryOfRum() throws Exception {
		final String TEST_NAME = "test241AutzJackManagerFullControlMemberMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetDeny(UserType.class, userCobbOid);
        assertAddDeny(USER_MANCOMB_FILE);
        
        assertVisibleUsers(0);
        
        assertGetDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 0);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assertDeleteDeny(UserType.class, USER_ESTEVAN_OID);
        
        assertGetDeny(ShadowType.class, accountOid);
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertSearch(ShadowType.class, ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext)), 0);
                
        assertGlobalStateUntouched();
	}

	@Test
    public void test242AutzJackManagerFullControlManagerMinistryOfRum() throws Exception {
		final String TEST_NAME = "test242AutzJackManagerFullControlManagerMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 4);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        
        assertAddDeny();
        
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetAllow(UserType.class, userCobbOid); // Cobb is in Scumm Bar, transitive descendant of Ministry of Rum
        assertAddAllow(USER_MANCOMB_FILE);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertVisibleUsers(5);
        
        assertGetAllow(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 2);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyAllow(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assignAccount(USER_ESTEVAN_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> userEstevan = getUser(USER_ESTEVAN_OID);
        String accountEstevanOid = getSingleLinkOid(userEstevan);
        assertGetAllow(ShadowType.class, accountEstevanOid);
        PrismObject<ShadowType> shadowEstevan = getObject(ShadowType.class, accountEstevanOid);
        display("Estevan shadow", shadowEstevan);

    	// MID-2822
        
    	Task task = taskManager.createTaskInstance(TestSecurityBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext));

        // When finally fixed is should be like this:
//    	assertSearch(ShadowType.class, query, 2);
        
        try {
            
            modelService.searchObjects(ShadowType.class, query, null, task, result);
                    	
        	AssertJUnit.fail("unexpected success");
			
		} catch (SchemaException e) {
			// This is expected. The authorizations will mix on-resource and off-resource search.
			display("Expected exception", e);
		}
        result.computeStatus();
		TestUtil.assertFailure(result);
        
		
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
                
        assertVisibleUsers(4);
                
        assertGlobalStateUntouched();
	}

	@Test
    public void test246AutzJackManagerFullControlManagerMinistryOfRumAndDefense() throws Exception {
		final String TEST_NAME = "test246AutzJackManagerFullControlManagerMinistryOfRumAndDefense";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 4);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        
        assertAddDeny();
        
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetAllow(UserType.class, userCobbOid); // Cobb is in Scumm Bar, transitive descendant of Ministry of Rum
        assertAddAllow(USER_MANCOMB_FILE);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertVisibleUsers(5);
        
        assertGetAllow(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 3);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyAllow(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assignAccount(USER_ESTEVAN_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> userEstevan = getUser(USER_ESTEVAN_OID);
        String accountEstevanOid = getSingleLinkOid(userEstevan);
        assertGetAllow(ShadowType.class, accountEstevanOid);
        PrismObject<ShadowType> shadowEstevan = getObject(ShadowType.class, accountEstevanOid);
        display("Estevan shadow", shadowEstevan);

    	// MID-2822
        
    	Task task = taskManager.createTaskInstance(TestSecurityBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext));

        // When finally fixed is should be like this:
//    	assertSearch(ShadowType.class, query, 2);
        
        try {
            
            modelService.searchObjects(ShadowType.class, query, null, task, result);
                    	
        	AssertJUnit.fail("unexpected success");
			
		} catch (SchemaException e) {
			// This is expected. The authorizations will mix on-resource and off-resource search.
			display("Expected exception", e);
		}
        result.computeStatus();
		TestUtil.assertFailure(result);
        
		
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
                
        assertVisibleUsers(4);
                
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test250AutzJackSelfAccountsRead() throws Exception {
		final String TEST_NAME = "test250AutzJackSelfAccountsRead";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_OID);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

		// enable after implementing MID-2789 and MID-2790
//		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
//				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
//				.and().item(ShadowType.F_OBJECT_CLASS).eq(new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"))
//				.build();
//		assertSearch(ShadowType.class, query, null, 1);
//		assertSearch(ShadowType.class, query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        
        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        // Linked to jack
        assertDeny("add jack's account to jack",   
        		(task, result) -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result));
        
        // Linked to other user
        assertDeny("add jack's account to gyubrush",   
        		(task, result) -> modifyUserAddAccount(USER_GUYBRUSH_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result));
        
        assertDeleteDeny(ShadowType.class, accountOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        PrismObjectDefinition<UserType> userEditSchema = getEditObjectDefinition(user);
        // TODO: assert items
        
        PrismObjectDefinition<ShadowType> shadowEditSchema = getEditObjectDefinition(shadow);
        // TODO: assert items
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test255AutzJackSelfAccountsReadWrite() throws Exception {
		final String TEST_NAME = "test255AutzJackSelfAccountsReadWrite";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_WRITE_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);
        
        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        // Linked to jack
        assertAllow("add jack's account to jack",   
        		(task, result) -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result));

        user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        
        // Linked to other user
        assertDeny("add gyubrush's account",   
        		(task, result) -> modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, task, result));
        
        assertDeleteAllow(ShadowType.class, accountRedOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertGlobalStateUntouched();
	}

    @Test
    public void test256AutzJackSelfAccountsPartialControl() throws Exception {
        final String TEST_NAME = "test256AutzJackSelfAccountsPartialControl";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        login(USER_JACK_USERNAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PrismTestUtil.createPolyString("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue("nbusr123");
        assertModifyDeny(UserType.class, USER_JACK_OID, PASSWORD_PATH, passwordPs);
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, PASSWORD_PATH, passwordPs);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();
		PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, task, result);
		assertItemFlags(rDef, PASSWORD_PATH, true, false, false);
        
//        // Linked to jack
//        assertAllow("add jack's account to jack", new Attempt() {
//            @Override
//            public void run(Task task, OperationResult result) throws Exception {
//                modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
//            }
//        });
//        user = getUser(USER_JACK_OID);
//        display("Jack after red account link", user);
//        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
//        assertNotNull("Strange, red account not linked to jack", accountRedOid);
//
//        // Linked to other user
//        assertDeny("add gyubrush's account", new Attempt() {
//            @Override
//            public void run(Task task, OperationResult result) throws Exception {
//                modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
//            }
//        });
//
//        assertDeleteAllow(ShadowType.class, accountRedOid);
//        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertGlobalStateUntouched();
    }
    
    @Test
    public void test258AutzJackSelfAccountsPartialControlPassword() throws Exception {
        final String TEST_NAME = "test258AutzJackSelfAccountsPartialControlPassword";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        login(USER_JACK_USERNAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PrismTestUtil.createPolyString("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue("nbusr123");
        assertModifyAllow(UserType.class, USER_JACK_OID, PASSWORD_PATH, passwordPs);
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, PASSWORD_PATH, passwordPs);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();
		PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, task, result);
		assertItemFlags(rDef, PASSWORD_PATH, true, false, false);
        
        assertGlobalStateUntouched();
    }

    @Test
    public void test260AutzJackObjectFilterLocationShadowRole() throws Exception {
		final String TEST_NAME = "test260AutzJackObjectFilterLocationShadowRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 8);
        assertSearch(OrgType.class, null, 6);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        // Linked to jack
        assertAllow("add jack's account to jack", 
    		(task, result) -> {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			});
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        assertGetAllow(ShadowType.class, accountRedOid);
        
        assertGlobalStateUntouched();
	}


    /**
     * creates user and assigns role at the same time
     * @throws Exception
     */
    @Test
    public void test261AutzAngelicaObjectFilterLocationCreateUserShadowRole() throws Exception {
		final String TEST_NAME = "test261AutzJackObjectFilterLocationCreateUserShadowRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_OID);
        login(USER_JACK_USERNAME);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		assertAllow("add user angelica",   
        		(task, result) -> addObject(USER_ANGELICA_FILE, task, result));

        // THEN
		TestUtil.displayThen(TEST_NAME);

		login(USER_ADMINISTRATOR_USERNAME);                 // user jack seemingly has no rights to search for angelika

		PrismObject<UserType> angelica = findUserByUsername(USER_ANGELICA_NAME);
		display("angelica", angelica);
		assertUser(angelica, null, USER_ANGELICA_NAME, "angelika", "angelika", "angelika");
		assertAssignedRole(angelica, ROLE_BASIC_OID);
		assertAccount(angelica, RESOURCE_DUMMY_OID);
		
		assertGlobalStateUntouched();
	}
    
	@Test
    public void test270AutzJackAssignApplicationRoles() throws Exception {
		final String TEST_NAME = "test270AutzJackAssignApplicationRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assertAllow("assign application role to jack", 
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertDeny("assign business role to jack",   
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        assertAllow("unassign application role from jack", 
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec, "application", "nonexistent");
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertAllowRequestItems(USER_JACK_OID, ROLE_APPLICATION_1_OID, null, 
        		AssignmentType.F_TARGET_REF, ActivationType.F_VALID_FROM, ActivationType.F_VALID_TO);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test272AutzJackAssignAnyRoles() throws Exception {
		final String TEST_NAME = "test272AutzJackAssignAnyRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assertAllow("assign application role to jack", 
        		(task, result) ->  assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertAllow("assign business role to jack",   
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        assertAllow("unassign application role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertAllowRequestItems(USER_JACK_OID, ROLE_APPLICATION_1_OID, AuthorizationDecisionType.ALLOW);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Check that the #assign authorization does not allow assignment that contains
	 * policyException or policyRule.
	 */
	@Test
    public void test273AutzJackRedyAssignmentExceptionRules() throws Exception {
		final String TEST_NAME = "test273AutzJackRedyAssignmentExceptionRules";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assertDeny("assign application role to jack", 
        		(task, result) ->  assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, null,
        				assignment -> {
        					PolicyExceptionType policyException = new PolicyExceptionType();
                			policyException.setRuleName("whatever");
        					assignment.getPolicyException().add(policyException);
        				},
        				task, result)
			);
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        assertDeny("assign application role to jack", 
        		(task, result) ->  assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null,
        				assignment -> {
							PolicyRuleType policyRule = new PolicyRuleType();
							policyRule.setName("whatever");
							assignment.setPolicyRule(policyRule);
        				},
        				task, result)
			);


        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test274AutzJackAssignNonApplicationRoles() throws Exception {
		final String TEST_NAME = "test274AutzJackAssignNonApplicationRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_NON_APPLICATION_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_NON_APPLICATION_ROLES_OID);
        
        assertAllow("assign business role to jack",   
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack",   
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result));

        assertAllow("unassign business role from jack",   
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test275aAutzJackAssignRequestableRoles() throws Exception {
		final String TEST_NAME = "test275aAutzJackAssignRequestableRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", 
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", 
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        assertAllow("unassign business role from jack", 
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}

	/**
	 * MID-3636 partially
	 */
	@Test(enabled=false)
	public void test275bAutzJackAssignRequestableOrgs() throws Exception {
		final String TEST_NAME = "test275bAutzJackAssignRequestableOrgs";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_END_USER_REQUESTABLE_ABSTACTROLES_OID);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		login(USER_JACK_USERNAME);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertAssignments(user, 2);
		assertAssignedRole(user, ROLE_END_USER_REQUESTABLE_ABSTACTROLES_OID);

		assertAllow("assign requestable org to jack",   
        		(task, result) -> assignOrg(USER_JACK_OID, ORG_REQUESTABLE_OID, task, result));

		user = getUser(USER_JACK_OID);
		assertAssignments(user, OrgType.class,1);

		RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
		assertRoleTypes(spec);

		ObjectQuery query = new ObjectQuery();

		query.addFilter(spec.getFilter());
		assertSearch(AbstractRoleType.class, query, 6); // set to 6 with requestable org

		assertAllow("unassign business role from jack",   
        		(task, result) -> unassignOrg(USER_JACK_OID, ORG_REQUESTABLE_OID, task, result));

		user = getUser(USER_JACK_OID);
		assertAssignments(user, OrgType.class,0);

		assertGlobalStateUntouched();
	}

	/**
	 * MID-3136
	 */
	@Test
    public void test276AutzJackAssignRequestableRolesWithOrgRef() throws Exception {
		final String TEST_NAME = "test276AutzJackAssignRequestableRolesWithOrgRef";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", 
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", 
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        assertAllow("unassign business role from jack", 
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Assign a role with parameter while the user already has the same role without a parameter.
	 * It seems that in this case the deltas are processed in a slightly different way.
	 * MID-3136
	 */
	@Test
    public void test277AutzJackAssignRequestableRolesWithOrgRefSecondTime() throws Exception {
		final String TEST_NAME = "test277AutzJackAssignRequestableRolesWithOrgRefSecondTime";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack (no param)", 
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertAllow("assign business role to jack (org MoR)", 
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        display("user after (expected 4 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertAllow("assign business role to jack (org Scumm)", 
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_SCUMM_BAR_OID, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 5);
        display("user after (expected 5 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertAllow("unassign business role from jack (org Scumm)", 
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_SCUMM_BAR_OID, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        display("user after (expected 4 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertDeny("assign application role to jack", 
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        assertAllow("unassign business role from jack (no param)", 
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, null, task, result));
        
        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);
        
        assertAllow("unassign business role from jack (org MoR)", 
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3136
	 */
	@Test
    public void test278AutzJackAssignRequestableRolesWithOrgRefTweakedDelta() throws Exception {
		final String TEST_NAME = "test278AutzJackAssignRequestableRolesWithOrgRefTweakedDelta";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", 
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", 
        	(task, result) ->  {
				Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
				ContainerDelta<AssignmentType> assignmentDelta1 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
				PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>(prismContext);
				assignmentDelta1.addValueToAdd(cval);
				PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
				targetRef.getValue().setOid(ROLE_BUSINESS_2_OID);
				targetRef.getValue().setTargetType(RoleType.COMPLEX_TYPE);
				targetRef.getValue().setRelation(null);
				cval.setId(123L);
				ContainerDelta<AssignmentType> assignmentDelta = assignmentDelta1;
				modifications.add(assignmentDelta);
				ObjectDelta<UserType> userDelta1 = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
				ObjectDelta<UserType> userDelta = userDelta1;
				Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
				modelService.executeChanges(deltas, null, task, result);
			});

        assertAllow("unassign business role from jack", 
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3136
	 */
	@Test
    public void test279AutzJackAssignRequestableRolesWithTenantRef() throws Exception {
		final String TEST_NAME = "test279AutzJackAssignRequestableRolesWithTenantRef";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", 
        	(task, result) ->
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        assertAllow("unassign business role from jack",
        	(task, result) ->
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}

	
	@Test
    public void test280AutzJackEndUser() throws Exception {
		final String TEST_NAME = "test280AutzJackEndUser";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        user = getUser(USER_JACK_OID);
        
        // MID-3136
        assertAllow("assign business role to jack",   
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertDeny("assign application role to jack",   
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result));

        // End-user role has authorization to assign, but not to unassign
        assertDeny("unassign business role from jack",   
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);
       
        assertGlobalStateUntouched();
        
        assertCredentialsPolicy(user);
	}
	
	@Test
    public void test281AutzJackEndUserSecondTime() throws Exception {
		final String TEST_NAME = "test281AutzJackEndUserSecondTime";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        user = getUser(USER_JACK_OID);
        
        // MID-3136
        assertAllow("assign business role to jack (no param)", 
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, null, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        // MID-3136
        assertAllow("assign business role to jack (org governor)",  
        		(task, result) -> assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        // End-user role has authorization to assign, but not to unassign
        assertDeny("unassign business role from jack",  
        		(task, result) -> unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 4 assignments)", user);
        assertAssignments(user, 4);
       
        assertGlobalStateUntouched();
        
        assertCredentialsPolicy(user);
	}
		
	private void assertCredentialsPolicy(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertCredentialsPolicy");
		CredentialsPolicyType credentialsPolicy = modelInteractionService.getCredentialsPolicy(user, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertNotNull("No credentials policy for "+user, credentialsPolicy);
		SecurityQuestionsCredentialsPolicyType securityQuestions = credentialsPolicy.getSecurityQuestions();
		assertEquals("Unexepected number of security questions for "+user, 2, securityQuestions.getQuestion().size());
	}

	@Test
    public void test282AutzJackEndUserAndModify() throws Exception {
		final String TEST_NAME = "test282AutzJackEndUserAndModify";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        assertAllow("modify jack's familyName", 
        		(task, result) -> modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, new ItemPath(UserType.F_FAMILY_NAME), task, result, PrismTestUtil.createPolyString("changed")));
        
        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, "Jack", "changed");
       
        assertGlobalStateUntouched();
	}


	@Test
    public void test283AutzJackModifyAndEndUser() throws Exception {
		final String TEST_NAME = "test283AutzJackModifyAndEndUser";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_MODIFY_USER_OID);
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        assertAllow("modify jack's familyName",
			(task, result) -> modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, new ItemPath(UserType.F_FAMILY_NAME), task, result, PrismTestUtil.createPolyString("changed")));
        
        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, "Jack", "changed");

        assertGlobalStateUntouched();
	}
	
	@Test
    public void test290AutzJackRoleOwnerAssign() throws Exception {
		final String TEST_NAME = "test290AutzJackRoleOwnerAssign";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_ASSIGN_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ROLE_OWNER_ASSIGN_OID);
        
        assertAllow("assign application role 1 to jack", 
        		(task,result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result));
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertDeny("assign application role 2 to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_APPLICATION_2_OID, task, result);
			}
		});

        assertAllow("unassign application role 1 from jack", 
        		(task,result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        assertEquals("Wrong type filter type", RoleType.COMPLEX_TYPE, ((TypeFilter)spec.getFilter()).getType());
        ObjectFilter subfilter = ((TypeFilter)spec.getFilter()).getFilter();
        assertFilter(subfilter, RefFilter.class);
        assertEquals(1, ((RefFilter)subfilter).getValues().size());
        assertEquals("Wrong OID in ref filter", USER_JACK_OID, ((RefFilter)subfilter).getValues().get(0).getOid());
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test292AutzJackRoleOwnerFullControl() throws Exception {
		final String TEST_NAME = "test292AutzJackRoleOwnerFullControl";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_FULL_CONTROL_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
		assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
		
		assertSearch(UserType.class, null, 1);
		assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
		assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
				
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertSearch(RoleType.class, null, 2);

		// TODO
		
//        PrismObject<UserType> user = getUser(USER_JACK_OID);
//        assertAssignments(user, 2);
//        assertAssignedRole(user, ROLE_ROLE_OWNER_FULL_CONTROL_OID);
//        
//        assertAllow("assign application role 1 to jack", new Attempt() {
//			@Override
//			public void run(Task task, OperationResult result) throws Exception {
//				assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
//			}
//		});
//        
//        user = getUser(USER_JACK_OID);
//        assertAssignments(user, 3);
//        assertAssignedRole(user, ROLE_APPLICATION_1_OID);
//
//        assertDeny("assign application role 2 to jack", new Attempt() {
//			@Override
//			public void run(Task task, OperationResult result) throws Exception {
//				assignRole(USER_JACK_OID, ROLE_APPLICATION_2_OID, task, result);
//			}
//		});
//
//        assertAllow("unassign application role 1 from jack", new Attempt() {
//			@Override
//			public void run(Task task, OperationResult result) throws Exception {
//				unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
//			}
//		});
//
//        user = getUser(USER_JACK_OID);
//        assertAssignments(user, 2);
//        
//        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
//        assertRoleTypes(spec);
//        assertFilter(spec.getFilter(), TypeFilter.class);
//        assertEquals("Wrong type filter type", RoleType.COMPLEX_TYPE, ((TypeFilter)spec.getFilter()).getType());
//        ObjectFilter subfilter = ((TypeFilter)spec.getFilter()).getFilter();
//        assertFilter(subfilter, RefFilter.class);
//        assertEquals(1, ((RefFilter)subfilter).getValues().size());
//        assertEquals("Wrong OID in ref filter", USER_JACK_OID, ((RefFilter)subfilter).getValues().get(0).getOid());
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test295AutzJackAssignOrgRelation() throws Exception {
		final String TEST_NAME = "test295AutzJackAssignOrgRelation";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ORGRELATION_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        RoleSelectionSpecification specJack = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        display("Spec (jack)", specJack);
        assertRoleTypes(specJack);
        
        Task task = taskManager.createTaskInstance();
        SearchResultList<PrismObject<AbstractRoleType>> assignableRolesJack = 
        		modelService.searchObjects(AbstractRoleType.class, ObjectQuery.createObjectQuery(specJack.getFilter()), null, task, task.getResult());
        display("Assignable roles", assignableRolesJack);
        assertObjectOids("Wrong assignable roles (jack)", assignableRolesJack, ROLE_BUSINESS_3_OID);
        
        RoleSelectionSpecification specRum = getAssignableRoleSpecification(getUser(userRumRogersOid));
        display("Spec (rum)", specRum);
        assertRoleTypes(specRum);
        
        SearchResultList<PrismObject<AbstractRoleType>> assignableRolesRum = 
        		modelService.searchObjects(AbstractRoleType.class, ObjectQuery.createObjectQuery(specRum.getFilter()), null, task, task.getResult());
        display("Assignable roles", assignableRolesRum);
        assertObjectOids("Wrong assignable roles (rum)", assignableRolesRum, ROLE_BUSINESS_3_OID);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test300AutzAnonymous() throws Exception {
		final String TEST_NAME = "test300AutzAnonymous";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();
        
        // WHEN 
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test310AutzJackNoRolePrivileged() throws Exception {
		final String TEST_NAME = "test310AutzJackNoRolePrivileged";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        login(USER_JACK_USERNAME);
        
        // precondition
        assertNoAccess(userJack);
        
        // WHEN (security context elevated)
        securityEnforcer.runPrivileged(() -> {
				try {
					
					assertSuperuserAccess(NUMBER_OF_ALL_USERS + 1);
			        
				} catch (Exception e) {
					new RuntimeException(e.getMessage(), e);
				}
				
				return null;
        	});
        
        // WHEN (security context back to normal)
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test312AutzAnonymousPrivileged() throws Exception {
		final String TEST_NAME = "test312AutzAnonymousPrivileged";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();
        
        // precondition
        assertNoAccess(userJack);
        
        // WHEN (security context elevated)
        securityEnforcer.runPrivileged(() -> {
				try {
					
					assertSuperuserAccess(NUMBER_OF_ALL_USERS + 1);
			        
				} catch (Exception e) {
					new RuntimeException(e.getMessage(), e);
				}
				
				return null;
			});
        
        // WHEN (security context back to normal)
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test313AutzAnonymousPrivilegedRestore() throws Exception {
		final String TEST_NAME = "test313AutzAnonymousPrivilegedRestore";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();
        
        // WHEN (security context elevated)
        securityEnforcer.runPrivileged(() -> {
				
				// do nothing.
			        				
				return null;
			});
        
        // WHEN (security context back to normal)
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test360AutzJackAuditorRole() throws Exception {
		final String TEST_NAME = "test360AutzJackAuditorRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_AUDITOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertReadCertCasesAllow();

        assertGlobalStateUntouched();

        assertAuditReadAllow();
	}
	
	/**
	 * MID-3826
	 */
    @Test
    public void test370AutzJackLimitedUserAdmin() throws Exception {
		final String TEST_NAME = "test370AutzJackLimitedUserAdmin";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_USER_ADMIN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS + 1);
        assertSearch(ObjectType.class, null, NUMBER_OF_ALL_USERS + 1);
        assertSearch(OrgType.class, null, 0);

        assertAddAllow(USER_HERMAN_FILE);
        
        assertModifyDeny();
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
    
    @Test
    public void test380AutzJackSelfTaskOwner() throws Exception {
		final String TEST_NAME = "test380AutzJackSelfTaskOwner";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_TASK_OWNER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        
        assertGetDeny(TaskType.class, TASK_USELESS_ADMINISTRATOR_OID);
        assertGetAllow(TaskType.class, TASK_USELESS_JACK_OID);
        
        assertSearch(UserType.class, null, 0);
        assertSearch(ObjectType.class, null, 0);
        assertSearch(OrgType.class, null, 0);
        assertSearch(TaskType.class, null, 1);
        
        assertTaskAddAllow(TASK_T1_OID, "t1", USER_JACK_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T2_OID, "t2", USER_JACK_OID, "nonsense");
        assertTaskAddDeny(TASK_T3_OID, "t3", USER_ADMINISTRATOR_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T4_OID, "t4", USER_LECHUCK_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T5_OID, "t5", null, TASK_USELESS_HANDLER_URI);

        assertAddDeny();
        
        assertModifyDeny();
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    private void assertTaskAddAllow(String oid, String name, String ownerOid, String handlerUri) throws Exception {
    	assertAllow("add task "+name, 
            	(task, result) -> {
            		addTask(oid, name, ownerOid, handlerUri, task, result);
    			});
    }
    
    private void assertTaskAddDeny(String oid, String name, String ownerOid, String handlerUri) throws Exception {
    	assertDeny("add task "+name, 
            	(task, result) -> {
            		addTask(oid, name, ownerOid, handlerUri, task, result);
    			});
    }
    
    private void addTask(String oid, String name, String ownerOid, String handlerUri, Task execTask, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<TaskType> task = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class).instantiate();
		task.setOid(oid);
		TaskType taskType = task.asObjectable();
		taskType.setName(createPolyStringType(name));
		if (ownerOid != null) {
			ObjectReferenceType ownerRef = new ObjectReferenceType();
			ownerRef.setOid(ownerOid);
			taskType.setOwnerRef(ownerRef);
		}
		taskType.setHandlerUri(handlerUri);
		modelService.executeChanges(MiscSchemaUtil.createCollection(task.createAddDelta()), null, execTask, result);
	}

}
