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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.intest.password.AbstractPasswordTest;
import com.evolveum.midpoint.model.intest.rbac.TestRbac;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiResource extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/multi");
	
	// LAVENDER dummy resource has a STRICT dependency on default dummy resource
	protected static final File RESOURCE_DUMMY_LAVENDER_FILE = new File(TEST_DIR, "resource-dummy-lavender.xml");
	protected static final String RESOURCE_DUMMY_LAVENDER_OID = "10000000-0000-0000-0000-000000000504";
	protected static final String RESOURCE_DUMMY_LAVENDER_NAME = "lavender";
	protected static final String RESOURCE_DUMMY_LAVENDER_NAMESPACE = MidPointConstants.NS_RI;
	
	// IVORY dummy resource has a LAX dependency on default dummy resource
	protected static final File RESOURCE_DUMMY_IVORY_FILE = new File(TEST_DIR, "resource-dummy-ivory.xml");
	protected static final String RESOURCE_DUMMY_IVORY_OID = "10000000-0000-0000-0000-000000011504";
	protected static final String RESOURCE_DUMMY_IVORY_NAME = "ivory";
	protected static final String RESOURCE_DUMMY_IVORY_NAMESPACE = MidPointConstants.NS_RI;
	
	// IVORY dummy resource has a RELAXED dependency on default dummy resource
	protected static final File RESOURCE_DUMMY_BEIGE_FILE = new File(TEST_DIR, "resource-dummy-beige.xml");
	protected static final String RESOURCE_DUMMY_BEIGE_OID = "10000000-0000-0000-0000-00000001b504";
	protected static final String RESOURCE_DUMMY_BEIGE_NAME = "beige";
	protected static final String RESOURCE_DUMMY_BEIGE_NAMESPACE = MidPointConstants.NS_RI;
	
	// PERU dummy resource has a RELAXED dependency on YELLOW dummy resource
	protected static final File RESOURCE_DUMMY_PERU_FILE = new File(TEST_DIR, "resource-dummy-peru.xml");
	protected static final String RESOURCE_DUMMY_PERU_OID = "10000000-0000-0000-0000-00000001c504";
	protected static final String RESOURCE_DUMMY_PERU_NAME = "peru";
	protected static final String RESOURCE_DUMMY_PERU_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_DAVID_FILE = new File(TEST_DIR, "resource-dummy-david.xml");
	protected static final String RESOURCE_DUMMY_DAVID_OID = "10000000-0000-0000-0000-000000300001";
	protected static final String RESOURCE_DUMMY_DAVID_NAME = "david";
	protected static final String RESOURCE_DUMMY_DAVID_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_GOLIATH_FILE = new File(TEST_DIR, "resource-dummy-goliath.xml");
	protected static final String RESOURCE_DUMMY_GOLIATH_OID = "10000000-0000-0000-0000-000000300001";
	protected static final String RESOURCE_DUMMY_GOLIATH_NAME = "goliath";
	protected static final String RESOURCE_DUMMY_GOLIATH_NAMESPACE = MidPointConstants.NS_RI;
	
	// Assigns default dummy resource and red dummy resource
	protected static final File ROLE_DUMMIES_FILE = new File(TEST_DIR, "role-dummies.xml");
	protected static final String ROLE_DUMMIES_OID = "12345678-d34d-b33f-f00d-55555555dddd";
	protected static final File ROLE_DUMMIES_IVORY_FILE = new File(TEST_DIR, "role-dummies-ivory.xml");
	protected static final String ROLE_DUMMIES_IVORY_OID = "12345678-d34d-b33f-f00d-55555511dddd";
	protected static final File ROLE_DUMMIES_BEIGE_FILE = new File(TEST_DIR, "role-dummies-beige.xml");
	protected static final String ROLE_DUMMIES_BEIGE_OID = "12345678-d34d-b33f-f00d-5555551bdddd";
	protected static final File ROLE_FIGHT_FILE = new File(TEST_DIR, "role-fight.xml");
	protected static final String ROLE_FIGHT_OID = "12345678-d34d-b33f-f00d-5555550303dd";

    protected static final String USER_WORLD_NAME = "world";
    protected static final String USER_WORLD_FULL_NAME = "The World";

	private static final String USER_FIELD_NAME = "field";
	
	private static final String USER_PASSWORD_A_CLEAR = "A"; // too short
	
	protected static DummyResource dummyResourceLavender;
	protected static DummyResourceContoller dummyResourceCtlLavender;
	protected ResourceType resourceDummyLavenderType;
	protected PrismObject<ResourceType> resourceDummyLavender;
	
	protected static DummyResource dummyResourceIvory;
	protected static DummyResourceContoller dummyResourceCtlIvory;
	protected ResourceType resourceDummyIvoryType;
	protected PrismObject<ResourceType> resourceDummyIvory;
	
	protected static DummyResource dummyResourceBeige;
	protected static DummyResourceContoller dummyResourceCtlBeige;
	protected ResourceType resourceDummyBeigeType;
	protected PrismObject<ResourceType> resourceDummyBeige;
	
	protected static DummyResource dummyResourcePeru;
	protected static DummyResourceContoller dummyResourceCtlPeru;
	protected ResourceType resourceDummyPeruType;
	protected PrismObject<ResourceType> resourceDummyPeru;
	
	protected static DummyResource dummyResourceDavid;
	protected static DummyResourceContoller dummyResourceCtlDavid;
	protected PrismObject<ResourceType> resourceDummyDavid;

	protected static DummyResource dummyResourceGoliath;
	protected static DummyResourceContoller dummyResourceCtlGoliath;
	protected PrismObject<ResourceType> resourceDummyGoliath;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlLavender = DummyResourceContoller.create(RESOURCE_DUMMY_LAVENDER_NAME, resourceDummyLavender);
		dummyResourceCtlLavender.extendSchemaPirate();
		dummyResourceLavender = dummyResourceCtlLavender.getDummyResource();
		resourceDummyLavender = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_LAVENDER_FILE, RESOURCE_DUMMY_LAVENDER_OID, initTask, initResult);
		resourceDummyLavenderType = resourceDummyLavender.asObjectable();
		dummyResourceCtlLavender.setResource(resourceDummyLavender);
		
		dummyResourceCtlIvory = DummyResourceContoller.create(RESOURCE_DUMMY_IVORY_NAME, resourceDummyIvory);
		dummyResourceCtlIvory.extendSchemaPirate();
		dummyResourceIvory = dummyResourceCtlIvory.getDummyResource();
		resourceDummyIvory = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_IVORY_FILE, RESOURCE_DUMMY_IVORY_OID, initTask, initResult);
		resourceDummyIvoryType = resourceDummyIvory.asObjectable();
		dummyResourceCtlIvory.setResource(resourceDummyIvory);
		
		dummyResourceCtlBeige = DummyResourceContoller.create(RESOURCE_DUMMY_BEIGE_NAME, resourceDummyBeige);
		dummyResourceCtlBeige.extendSchemaPirate();
		dummyResourceBeige = dummyResourceCtlBeige.getDummyResource();
		resourceDummyBeige = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_BEIGE_FILE, RESOURCE_DUMMY_BEIGE_OID, initTask, initResult);
		resourceDummyBeigeType = resourceDummyBeige.asObjectable();
		dummyResourceCtlBeige.setResource(resourceDummyBeige);
		
		dummyResourceCtlPeru = DummyResourceContoller.create(RESOURCE_DUMMY_PERU_NAME, resourceDummyPeru);
		dummyResourceCtlPeru.extendSchemaPirate();
		dummyResourcePeru = dummyResourceCtlPeru.getDummyResource();
		resourceDummyPeru = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_PERU_FILE, RESOURCE_DUMMY_PERU_OID, initTask, initResult);
		resourceDummyPeruType = resourceDummyPeru.asObjectable();
		dummyResourceCtlPeru.setResource(resourceDummyPeru);

		dummyResourceCtlDavid = DummyResourceContoller.create(RESOURCE_DUMMY_DAVID_NAME);
		dummyResourceCtlDavid.extendSchemaPirate();
		dummyResourceDavid = dummyResourceCtlDavid.getDummyResource();
		resourceDummyDavid = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_DAVID_FILE, RESOURCE_DUMMY_DAVID_OID, initTask, initResult);
		dummyResourceCtlDavid.setResource(resourceDummyDavid);

		dummyResourceCtlGoliath = DummyResourceContoller.create(RESOURCE_DUMMY_GOLIATH_NAME);
		dummyResourceCtlGoliath.extendSchemaPirate();
		dummyResourceGoliath = dummyResourceCtlGoliath.getDummyResource();
		resourceDummyGoliath = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_GOLIATH_FILE, RESOURCE_DUMMY_GOLIATH_OID, initTask, initResult);
		dummyResourceCtlGoliath.setResource(resourceDummyGoliath);

		repoAddObjectFromFile(ROLE_DUMMIES_FILE, initResult);
		repoAddObjectFromFile(ROLE_DUMMIES_IVORY_FILE, initResult);
		repoAddObjectFromFile(ROLE_DUMMIES_BEIGE_FILE, initResult);
		repoAddObjectFromFile(ROLE_FIGHT_FILE, initResult);
		
		getDummyResource().resetBreakMode();
	}

	@Test
    public void test110JackAssignRoleDummiesFull() throws Exception {
		final String TEST_NAME = "test110JackAssignRoleDummiesFull";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		jackAssignRoleDummies(TEST_NAME);
	}
	
	@Test
    public void test113JackRenameFull() throws Exception {
		final String TEST_NAME = "test113JackRenameFull";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		jackRename(TEST_NAME);
	}
	
	@Test
    public void test114JackUnAssignRoleDummiesFull() throws Exception {
		final String TEST_NAME = "test114JackUnAssignRoleDummiesFull";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		jackUnAssignRoleDummies(TEST_NAME);
	}
	
	// TODO: lavender resource with failure
	
	@Test
    public void test115JackAssignRoleDummiesFullErrorIvory() throws Exception {
		final String TEST_NAME = "test115JackAssignRoleDummiesFullErrorIvory";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		getDummyResource().setAddBreakMode(BreakMode.NETWORK);
		jackAssignRoleDummiesError(TEST_NAME, ROLE_DUMMIES_IVORY_OID, RESOURCE_DUMMY_IVORY_NAME, true);
	}

	@Test
    public void test116JackUnAssignRoleDummiesFullErrorIvory() throws Exception {
		final String TEST_NAME = "test116JackUnAssignRoleDummiesFullErrorIvory";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		getDummyResource().setAddBreakMode(BreakMode.NETWORK);
		jackUnAssignRoleDummiesError(TEST_NAME, ROLE_DUMMIES_IVORY_OID);
	}
	
	@Test
    public void test117JackAssignRoleDummiesFullErrorBeige() throws Exception {
		final String TEST_NAME = "test117JackAssignRoleDummiesFullErrorBeige";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		getDummyResource().setAddBreakMode(BreakMode.NETWORK);
		jackAssignRoleDummiesError(TEST_NAME, ROLE_DUMMIES_BEIGE_OID, RESOURCE_DUMMY_BEIGE_NAME, false);
	}

	@Test
    public void test118JackUnAssignRoleDummiesFullErrorBeige() throws Exception {
		final String TEST_NAME = "test118JackUnAssignRoleDummiesFullErrorBeige";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		getDummyResource().setAddBreakMode(BreakMode.NETWORK);
		jackUnAssignRoleDummiesError(TEST_NAME, ROLE_DUMMIES_BEIGE_OID);
	}
	
	@Test
    public void test120JackAssignRoleDummiesRelative() throws Exception {
		final String TEST_NAME = "test120JackAssignRoleDummiesRelative";
		
		getDummyResource().resetBreakMode();
		// Clean up user
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		jackAssignRoleDummies(TEST_NAME);
	}
	
	/**
	 * Try to delete Jack's default dummy account. As other provisioned accounts depends on it the
	 * operation should fail.
	 */
	@Test
    public void test121JackTryDeleteAccount() throws Exception {
		final String TEST_NAME = "test121JackTryDeleteAccount";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		getDummyResource().resetBreakMode();
		// Clean up user
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		String accountJackDummyOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
		
		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackDummyOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        
        try {
			// WHEN
	        modelService.executeChanges(deltas, null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Expected exception", e);
        }
	}
	
	@Test
    public void test123JackRenameRelative() throws Exception {
		final String TEST_NAME = "test123JackRenameRelative";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		jackRename(TEST_NAME);
	}
	
	@Test
    public void test129JackUnAssignRoleDummiesRelative() throws Exception {
		final String TEST_NAME = "test129JackUnAssignRoleDummiesRelative";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		jackUnAssignRoleDummies(TEST_NAME);
	}
	
	/**
	 * Ivory resource has a lax dependency. The provisioning should go OK.
	 */
	@Test
    public void test200JackAssignDummyIvory() throws Exception {
		final String TEST_NAME = "test200JackAssignDummyIvory";
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		// WHEN
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_IVORY_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 1);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
	}
	
	/**
	 * Ivory resource has a lax dependency. The provisioning should go OK.
	 */
	@Test
    public void test209JackUnAssignDummyIvory() throws Exception {
		final String TEST_NAME = "test209JackUnAssignDummyIvory";
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		// WHEN
		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_IVORY_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Beige resource has a relaxed dependency. The provisioning should go OK
	 * even if there is no default dummy account.
	 */
	@Test
    public void test210JackAssignDummyBeige() throws Exception {
		final String TEST_NAME = "test210JackAssignDummyBeige";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 1);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
	}
	
	/**
	 * Beige resource has a relaxed dependency. The deprovisioning should go OK
	 * even if there is not default dummy account.
	 */
	@Test
    public void test219JackUnAssignDummyBeige() throws Exception {
		final String TEST_NAME = "test219JackUnAssignDummyBeige";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Beige resource has a relaxed dependency. Try provisioning of both
	 * beige and default dummy accounts.
	 */
	@Test
    public void test220JackAssignDummyBeigeAndDefault() throws Exception {
		final String TEST_NAME = "test220JackAssignDummyBeigeAndDefault";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, true);
		userDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, true));
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		executeChanges(userDelta, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 2);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Recompute to check that recompute will not ruin anything. 
	 */
	@Test
    public void test221JackRecompute() throws Exception {
		final String TEST_NAME = "test221JackRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 2);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Delete account on default dummy resource (but keep it assigned and keep the shadow).
	 * The recompute the user. The account should be re-created.
	 * MID-2134, MID-3093
	 */
	@Test
    public void test223JackKillDefaultDummyAccounAndRecompute() throws Exception {
		final String TEST_NAME = "test223JackKillDefaultDummyAccounAndRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        display("dummy resource before", getDummyResource());
        		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("user after", userJack);
        assertLinks(userJack, 2);
        
        display("dummy resource after", getDummyResource());
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Delete account on beige dummy resource (but keep it assigned and keep the shadow).
	 * Then recompute the user. The account should be re-created.
	 * MID-2134, MID-3093
	 */
	@Test
    public void test224JackKillBeigeAccounAndRecompute() throws Exception {
		final String TEST_NAME = "test224JackKillBeigeAccounAndRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        dummyResourceBeige.deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        display("beige dummy resource before", dummyResourceBeige);
        		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, ModelExecuteOptions.createReconcile(), task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 2);
        
        display("beige dummy resource after", dummyResourceBeige);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Delete both accounts on beige and default dummy resource (but keep it assigned and keep the shadows).
	 * The recompute the user. The accounts should be re-created.
	 * MID-2134, MID-3093
	 */
	@Test
    public void test225JackKillBothAccounsAndRecompute() throws Exception {
		final String TEST_NAME = "test225JackKillBothAccounsAndRecompute";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        display("dummy resource before", getDummyResource());
        
        dummyResourceBeige.deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        display("beige dummy resource before", dummyResourceBeige);
        		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, ModelExecuteOptions.createReconcile(), task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 2);
        
        display("dummy resource after", getDummyResource());
        display("beige dummy resource after", dummyResourceBeige);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Cause schema violation on the account during a provisioning operation. This should fail
	 * the operation, but other operations should proceed and the account should definitelly NOT
	 * be unlinked.
	 * MID-2134
	 */
	@Test
    public void test227ModifyUserJackDefaultDummyBrokenSchemaViolation() throws Exception {
		final String TEST_NAME = "test227ModifyUserJackDefaultDummyBrokenSchemaViolation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiResource.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);
                        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, 
        		new PolyString("Cpt. Jack Sparrow", null));
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("Result", result);
		TestUtil.assertPartialError(result);
		
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 2);
        
        display("dummy resource after", getDummyResource());
        display("beige dummy resource after", dummyResourceBeige);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Cpt. Jack Sparrow", true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        getDummyResource().resetBreakMode();
	}
	
	/**
	 * Reset break mode, make sure that everything is back to normal.
	 * MID-2134
	 */
	@Test
    public void test228ModifyUserJackDefaultDummyNoError() throws Exception {
		final String TEST_NAME = "test228ModifyUserJackDefaultDummyNoError";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiResource.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        getDummyResource().resetBreakMode();
                        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, 
        		new PolyString(USER_JACK_FULL_NAME, null));
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("Result", result);
		TestUtil.assertSuccess(result);
		
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 2);
        
        display("dummy resource after", getDummyResource());
        display("beige dummy resource after", dummyResourceBeige);
        
        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Beige resource has a relaxed dependency. Try provisioning of both
	 * beige and default dummy accounts.
	 */
	@Test
    public void test229JackUnassignDummyBeigeAndDefault() throws Exception {
		final String TEST_NAME = "test229JackUnassignDummyBeigeAndDefault";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        getDummyResource().resetBreakMode();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, false);
		userDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, false));
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		executeChanges(userDelta, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	
	/**
	 * Lavender resource has a strict dependency. The provisioning should fail.
	 */
	@Test
    public void test250JackAssignDummyLavender() throws Exception {
		final String TEST_NAME = "test250JackAssignDummyLavender";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		try {
			// WHEN
			assignAccount(USER_JACK_OID, RESOURCE_DUMMY_LAVENDER_OID, null, task, result);
			
			AssertJUnit.fail("Unexpected success");
		} catch (PolicyViolationException e) {
			// this is expected
		}
		
		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLinks(userJack, 0);
        assertAssignments(userJack, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);       
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * The "dummies" role assigns two dummy resources that are in a dependency. The value of DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME is propagated from one
	 * resource through the user to the other resource. If dependency does not work then no value is propagated.
	 */
    public void jackAssignRoleDummies(final String TEST_NAME) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        clearJackOrganizationalUnit(task, result);
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        // THEN
        result.computeStatus();
    	TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignedRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        assertLinks(userJack, 4);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "The Great Voodoo Master");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The Lost Souls");
        
        // This is set up by "feedback" using an inbound expression. It has nothing with dependencies yet.
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));

        display("LAVENDER dummy account", getDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME));
        assertDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
        assertDummyAccountAttribute(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Jack Sparrow must be the best CAPTAIN the Caribbean has ever seen");
        
        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");

        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
	}

    public void jackRename(final String TEST_NAME) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        jackRename(TEST_NAME, "jackie", "Jackie Sparrow", task, result);
        jackRename(TEST_NAME, USER_JACK_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, task, result);
    }
    
    public void jackRename(final String TEST_NAME, String toName, String toFullName, Task task, OperationResult result) throws Exception {

    	ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_NAME, 
    			PrismTestUtil.createPolyString(toName));
    	objectDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString(toFullName));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		
        // WHEN
    	TestUtil.displayWhen(TEST_NAME);
    	modelService.executeChanges(deltas, null, task, result);
        
        // THEN
    	TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
    	TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(toName));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(toFullName));
        assertAssignedRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        assertLinks(userJack, 4);

        assertDefaultDummyAccount(toName, toFullName, true);
        assertDefaultDummyAccountAttribute(toName, "title", "The Great Voodoo Master");
        assertDefaultDummyAccountAttribute(toName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The Lost Souls");
        
        // This is set up by "feedback" using an inbound expression. It has nothing with dependencies yet.
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));
        
        assertDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, toName, toFullName, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_LAVENDER_NAME, toName,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
        assertDummyAccountAttribute(RESOURCE_DUMMY_LAVENDER_NAME, toName, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, toFullName +" must be the best CAPTAIN the Caribbean has ever seen");
        
        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, toName, toFullName, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, toName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");

        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, toName, toFullName, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, toName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
	}

    public void jackUnAssignRoleDummies(final String TEST_NAME) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignedNoRole(user);
        assertLinks(user, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));        
	}
    
    /**
	 * The "dummies" role assigns two dummy resources that are in a dependency. The value of DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME is propagated from one
	 * resource through the user to the other resource. If dependency does not work then no value is propagated.
	 */
    public void jackAssignRoleDummiesError(final String TEST_NAME, String roleOid, String dummyResourceName, 
    		boolean expectAccount) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        clearJackOrganizationalUnit(task, result);
        
        // WHEN
        assignRole(USER_JACK_OID, roleOid, task, result);
        
        // THEN
        result.computeStatus();
        display(result);
        if (expectAccount) {
        	TestUtil.assertResultStatus(result, OperationResultStatus.PARTIAL_ERROR);
        } else {
        	TestUtil.assertStatus(result, OperationResultStatus.PARTIAL_ERROR);
//        	TestUtil.assertPartialError(result);
        }
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignedRole(USER_JACK_OID, roleOid, task, result);
        // One of the accountRefs is actually ref to an uncreated shadow
        assertLinks(userJack, expectAccount ? 2 : 1);

    	assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    	assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME);

    	if (expectAccount) {
    		assertDummyAccount(dummyResourceName, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
    		// This is actually pulled from the uncreated shadow
            assertDummyAccountAttribute(dummyResourceName, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
    	} else {
    		assertNoDummyAccount(dummyResourceName, ACCOUNT_JACK_DUMMY_USERNAME);
    	}
	}
	
	private void clearJackOrganizationalUnit(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
	}

	public void jackUnAssignRoleDummiesError(final String TEST_NAME, String roleOid) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignRole(USER_JACK_OID, roleOid, task, result);
        
        // THEN
        result.computeStatus();
        display(result);
		// there is a failure while reading dummy account - it was not created
		// because of unavailability of the resource..but it is OK..
        OperationResultStatus status = result.getStatus();
        if (status != OperationResultStatus.SUCCESS && status != OperationResultStatus.PARTIAL_ERROR) {
        	AssertJUnit.fail("Expected result success or partial error status, but was "+status);
        }
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignedNoRole(user);
        assertLinks(user, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));        
	}
	
    @Test
    public void test300AddAndAssignRelative() throws Exception {
		final String TEST_NAME = "test300AddAndAssignRelative";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Add default dummy account to jack without assigning it.
        // In relative mode this account should shay untouched while we play with assignments and
        // unsassignements of other accounts
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after default dummy account add", userJack);
		assertUserJack(userJack);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        userJack = getUser(USER_JACK_OID);
		display("User after red dummy assignment", userJack);
		assertUserJack(userJack);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
		assertLinks(userJack, 2);
        String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_BLUE_OID);
        
        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyBlueType);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyBlueType);
        
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        
	}
    
    @Test
    public void test310AddedAccountAndUnassignRelative() throws Exception {
		final String TEST_NAME = "test310AddedAccountAndUnassignRelative";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after red dummy unassignment", userJack);
		assertUserJack(userJack);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertLinks(userJack, 1);
        String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
        
        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
        
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
	}
    
    /**
     * This is mostly a cleanup. But it also tests some cases.
     */
    @Test
    public void test319UnassignDummyRelative() throws Exception {
		final String TEST_NAME = "test319UnassignDummyRelative";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after red dummy unassignment", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 0);

		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_OID, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_OID, ACCOUNT_JACK_DUMMY_USERNAME);
        
	}
    
    /**
     * Attempt to add lavender account should fail. There is unsatisfied strict dependency on
     * default dummy resource.
     */
    @Test
    public void test350AddAccountLavender() throws Exception {
		final String TEST_NAME = "test350AddAccountLavender";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, resourceDummyLavender);
        
        // WHEN
        try {
	        TestUtil.displayWhen(TEST_NAME);
	        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
		
    }
    
    @Test
    public void test352AddAccountIvory() throws Exception {
		final String TEST_NAME = "test352AddAccountIvory";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, resourceDummyIvory);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    @Test
    public void test354AddAccountBeige() throws Exception {
		final String TEST_NAME = "test354AddAccountBeige";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, resourceDummyBeige);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    @Test
    public void test360AddAccountDummy() throws Exception {
		final String TEST_NAME = "test360AddAccountDummy";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, getDummyResourceObject());
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    /**
     * This should work now as the dependency is satisfied.
     */
    @Test
    public void test362AddAccountLavender() throws Exception {
		final String TEST_NAME = "test362AddAccountLavender";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, resourceDummyLavender);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    /**
     * The account cannot be deleted because there is strict dependency on it (from lavender resource).
     */
    @Test
    public void test370DeleteAccountDummy() throws Exception {
		final String TEST_NAME = "test370DeleteAccountDummy";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, getDummyResourceObject());
        
        // WHEN
        try {
	        TestUtil.displayWhen(TEST_NAME);
	        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAccount(user, RESOURCE_DUMMY_OID);
        
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    /**
     * The account cannot be unlinked because there is strict dependency on it (from lavender resource).
     */
    @Test
    public void test372UnlinkAccountDummy() throws Exception {
		final String TEST_NAME = "test372UnlinkAccountDummy";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserUnlinkAccount(USER_JACK_OID, getDummyResourceObject());
        
        // WHEN
        try {
	        TestUtil.displayWhen(TEST_NAME);
	        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAccount(user, RESOURCE_DUMMY_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    @Test
    public void test374DeleteAccountLavender() throws Exception {
		final String TEST_NAME = "test374DeleteAccountLavender";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, resourceDummyLavender);
        
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAccount(user, RESOURCE_DUMMY_OID);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		
    }
    
    /**
     * This should go well now as the dependency is gone.
     */
    @Test
    public void test376DeleteAccountDummy() throws Exception {
		final String TEST_NAME = "test376DeleteAccountDummy";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, getDummyResourceObject());
        
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }
    
    /**
     * Beige resource has relaxed dependency on default dummy. Even though the default dummy is no
     * longer there the delete should go smoothly.
     */
    @Test
    public void test378DeleteAccountBeige() throws Exception {
		final String TEST_NAME = "test378DeleteAccountBeige";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, resourceDummyBeige);
        
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }
    
    /**
     * Ivory resource has lax dependency on default dummy. Even though the default dummy is no
     * longer there the delete should go smoothly.
     */
    @Test
    public void test379DeleteAccountIvory() throws Exception {
		final String TEST_NAME = "test379DeleteAccountIvory";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, resourceDummyIvory);
        
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }
    
    /**
     * Resource peru depends on resource yellow, but the dependency is relaxed.
     * The account should be created even if we do not have yellow account yet.
     */
    @Test
    public void test380AddAccountPeru() throws Exception {
		final String TEST_NAME = "test380AddAccountPeru";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		getDummyResource().resetBreakMode();
		
		// precondition
		assertEncryptedUserPassword(USER_JACK_OID, USER_JACK_PASSWORD);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, resourceDummyPeru);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);
    }

    @Test
    public void test382AddAccountYellow() throws Exception {
		final String TEST_NAME = "test382AddAccountYellow";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, resourceDummyYellow);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 2);
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);
        
        assertDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);
    }
    
    /**
	 * Yellow resource has minimum password length constraint. Change password to something shorter.
	 * There is dependency yellow<-peru. Make sure that the peru is not affected (the dependency is relaxed)
	 * MID-3033, MID-2134
	 */
	@Test
    public void test385ModifyUserJackPasswordA() throws Exception {
		final String TEST_NAME = "test385ModifyUserJackPasswordA";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
		// WHEN
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_A_CLEAR, task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertPartialError(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 2);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);
        
        // Check account in dummy resource (peru)
        assertDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_A_CLEAR);
        
        assertEncryptedUserPassword(userJack, USER_PASSWORD_A_CLEAR);
	}

    
    @Test
    public void test389DeleteAccountPeru() throws Exception {
		final String TEST_NAME = "test389DeleteAccountPeru";
		TestUtil.displayTestTile(TEST_NAME);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, resourceDummyPeru);
        
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        // just to be sure
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }
    
	@Test
    public void test400DavidAndGoliathAssignRole() throws Exception {
		final String TEST_NAME = "test400DavidAndGoliathAssignRole";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = createUser(USER_WORLD_NAME, USER_WORLD_FULL_NAME, true);
        userBefore.asObjectable().getOrganizationalUnit().add(PrismTestUtil.createPolyStringType("stone"));
        addObject(userBefore);
        
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertDavidGoliath(userBefore.getOid(), "stone", USER_WORLD_NAME, true, true, true);
		
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,3);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
        
        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(2, ChangeType.MODIFY, ShadowType.class);
        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
        display("Last execution delta", executionDelta);
        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 2);
	}
    
    private void assertDavidGoliath(String userOid, String ou, String name, boolean userEnabled, boolean davidEnabled, boolean goliathEnabled) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException {
    	PrismObject<UserType> userAfter = getUser(userOid);
		display("User after", userAfter);
		
    	assertUser(userAfter, userOid, name, USER_WORLD_FULL_NAME, null, null);
		assertAccount(userAfter, RESOURCE_DUMMY_GOLIATH_OID);
		assertAccount(userAfter, RESOURCE_DUMMY_DAVID_OID);
		assertLinks(userAfter, 2);
		if (userEnabled) {
			assertAdministrativeStatusEnabled(userAfter);
		} else {
			assertAdministrativeStatusDisabled(userAfter);
		}
		
    	assertDummyAccount(RESOURCE_DUMMY_DAVID_NAME, name, USER_WORLD_FULL_NAME, davidEnabled);
        
		assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, name,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ou+ " ("+name+") take");
		
		assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, name,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "the king");
        
        assertUserProperty(userAfter, UserType.F_LOCALITY, PrismTestUtil.createPolyString(ou+" ("+name+") take throw"));
        
        assertDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, name, USER_WORLD_FULL_NAME, goliathEnabled);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_GOLIATH_NAME, name,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ou+" ("+name+") take throw ("+name+") hit");
        
        assertUserProperty(userAfter, UserType.F_TITLE, PrismTestUtil.createPolyString(ou+" ("+name+") take throw ("+name+") hit fall"));
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, name,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, ou+" ("+name+") take throw ("+name+") hit fall ("+name+") win");
        
	}

	@Test
    public void test401DavidAndGoliathModifyOu() throws Exception {
		final String TEST_NAME = "test401DavidAndGoliathModifyOu";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(userBefore.getOid(), UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("rock"));
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, true, true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
        
        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(2, ChangeType.MODIFY, ShadowType.class);
        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
        display("Last execution delta", executionDelta);
        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 2);
	}
    
    @Test
    public void test403DavidAndGoliathDisableUser() throws Exception {
		final String TEST_NAME = "test403DavidAndGoliathDisableUser";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(userBefore.getOid(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, false, false, false);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);			// last one is duplicate
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
		dummyAuditService.assertExecutionDeltas(1,1);			// user is again disabled here
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
    
    @Test
    public void test404DavidAndGoliathEnableUser() throws Exception {
		final String TEST_NAME = "test404DavidAndGoliathEnableUser";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(userBefore.getOid(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, true, true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);						// last one is duplicate
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,1);			// user is again disabled here
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
    
    @Test
    public void test405DavidAndGoliathDisableAccountDavid() throws Exception {
		final String TEST_NAME = "test405DavidAndGoliathDisableAccountDavid";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        String accountDavidOid = getLinkRefOid(userBefore, RESOURCE_DUMMY_DAVID_OID);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAccountShadowReplace(accountDavidOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, false, true);
                
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,1);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
    
    /**
     * Try to recompute to make sure it does not destroy anything. The recompute should do nothing.
     */
    @Test
    public void test406DavidAndGoliathRecompute() throws Exception {
		final String TEST_NAME = "test406DavidAndGoliathRecompute";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(userBefore.getOid(), task, result);
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, false, true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertNoRecord();
	}
    
    @Test
    public void test408DavidAndGoliathEnableAccountDavid() throws Exception {
		final String TEST_NAME = "test408DavidAndGoliathEnableAccountDavid";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        String accountDavidOid = getLinkRefOid(userBefore, RESOURCE_DUMMY_DAVID_OID);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAccountShadowReplace(accountDavidOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, true, true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,1);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
    
    @Test
    public void test410DavidAndGoliathRename() throws Exception {
		final String TEST_NAME = "test410DavidAndGoliathRename";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(userBefore.getOid(), UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_FIELD_NAME));
        
     // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_FIELD_NAME, true, true, true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
        
        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(2, ChangeType.MODIFY, ShadowType.class);
        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
        display("Last execution delta", executionDelta);
        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 2);
	}
    
    @Test
    public void test419DavidAndGoliathUnassignRole() throws Exception {
		final String TEST_NAME = "test419DavidAndGoliathUnassignRole";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result, 2);
        
        PrismObject<UserType> userAfter = getUser(userBefore.getOid());
		display("User after fight", userAfter);
		assertUser(userAfter, userBefore.getOid(), USER_FIELD_NAME, USER_WORLD_FULL_NAME, null, null);
		assertLinks(userAfter, 0);
		
		assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);
		assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,1);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();        
	}
    
    @Test
    public void test420DavidAndGoliathAssignRoleGoliathDown() throws Exception {
		final String TEST_NAME = "test420DavidAndGoliathAssignRoleGoliathDown";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, userBefore.getOid(),
        		UserType.F_LOCALITY, prismContext);
        userDelta.addModificationReplaceProperty(UserType.F_TITLE);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        userBefore = findUserByUsername(USER_FIELD_NAME);
        display("User before", userBefore);
        
        dummyResourceGoliath.setBreakMode(BreakMode.NETWORK);
        
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        // Inner errors are expected
        TestUtil.assertPartialError(result);
        
        PrismObject<UserType> userAfter = getUser(userBefore.getOid());
		display("User after fight", userAfter);
		assertUser(userAfter, userBefore.getOid(), USER_FIELD_NAME, USER_WORLD_FULL_NAME, null, null);
		assertAccount(userAfter, RESOURCE_DUMMY_DAVID_OID);
		assertAccount(userAfter, RESOURCE_DUMMY_GOLIATH_OID); // This is unfinished shadow
		assertLinks(userAfter, 2);
		
		assertDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME, USER_WORLD_FULL_NAME, true);
        
		assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "rock (field) take");
        
        assertUserProperty(userAfter, UserType.F_LOCALITY, PrismTestUtil.createPolyString("rock (field) take throw"));
        
        // Goliath is down. No account.
                        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,3);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,1);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);        
	}
    
    // MID-1566
    @Test(enabled=false)
    public void test422DavidAndGoliathAssignRoleGoliathUpRecompute() throws Exception {
		final String TEST_NAME = "test422DavidAndGoliathAssignRoleGoliathUpRecompute";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        
        dummyResourceGoliath.setBreakMode(BreakMode.NONE);
        
        // WHEN
        recomputeUser(userBefore.getOid(), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "rock", USER_FIELD_NAME, true, true, true);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
        
    }
    
    @Test
    public void test429DavidAndGoliathUnassignRole() throws Exception {
		final String TEST_NAME = "test429DavidAndGoliathUnassignRole";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
//        TestUtil.assertSuccess(result, 2);
        
        dummyResourceGoliath.setBreakMode(BreakMode.NONE);
        
        PrismObject<UserType> userAfter = getUser(userBefore.getOid());
		display("User after fight", userAfter);
		assertUser(userAfter, userBefore.getOid(), USER_FIELD_NAME, USER_WORLD_FULL_NAME, null, null);
		assertLinks(userAfter, 0);
		
		assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);
		assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,1);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
	}
    
    @Test
    public void test430DavidAndGoliathAssignRoleDavidDown() throws Exception {
		final String TEST_NAME = "test430DavidAndGoliathAssignRoleDavidDown";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, userBefore.getOid(),
        		UserType.F_LOCALITY, prismContext);
        userDelta.addModificationReplaceProperty(UserType.F_TITLE);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        userBefore = findUserByUsername(USER_FIELD_NAME);
        display("User before", userBefore);
        
        dummyResourceGoliath.setBreakMode(BreakMode.NONE);
        dummyResourceDavid.setBreakMode(BreakMode.NETWORK);
        
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        // Inner errors are expected
        TestUtil.assertPartialError(result);
        
        PrismObject<UserType> userAfter = getUser(userBefore.getOid());
		display("User after fight", userAfter);
		assertUser(userAfter, userBefore.getOid(), USER_FIELD_NAME, USER_WORLD_FULL_NAME, null, null);
		assertAccount(userAfter, RESOURCE_DUMMY_DAVID_OID); // This is unfinished shadow
		// No goliath account, not even tried, the dependency stopped it
		assertLinks(userAfter, 1);
		
		// David is down. No account.
		
		// Goliath depends on David, no account.
		
		assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);
		                                
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,1);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertExecutionDeltas(2,1);
//        dummyAuditService.asserHasDelta(2,ChangeType.MODIFY, UserType.class);
        
	}
    
    @Test
    public void test440DavidAndGoliathAssignRoleAndCreateUserInOneStep() throws Exception {
		final String TEST_NAME = "test440DavidAndGoliathAssignRoleAndCreateUserInOneStep";
		TestUtil.displayTestTile(TEST_NAME);
		
		dummyResourceGoliath.setBreakMode(BreakMode.NONE);
        dummyResourceDavid.setBreakMode(BreakMode.NONE);
		try{
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		// delete user and his roles which were added before
		
        PrismObject<UserType> userWorld = findUserByUsername(USER_FIELD_NAME);
        
        AssertJUnit.assertNotNull("User must not be null.", userWorld);
        
		ObjectDelta<UserType> delta = ObjectDelta.createDeleteDelta(UserType.class, userWorld.getOid(), prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(delta);
		modelService.executeChanges(deltas, null, task, result);
		
		OperationResult deleteResult = new OperationResult("Check if user was deleted properly.");
		try {
		repositoryService.getObject(UserType.class, userWorld.getOid(), null, deleteResult);
		} catch (ObjectNotFoundException ex){
			//this is OK, we deleted user before
		}
		
		// GIVEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		
        
        PrismObject<UserType> userBefore = createUser(USER_WORLD_NAME, USER_WORLD_FULL_NAME, true);
        userBefore.asObjectable().getOrganizationalUnit().add(PrismTestUtil.createPolyStringType("stone"));
          
        PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>(prismContext);
		PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
		targetRef.getValue().setOid(ROLE_FIGHT_OID);
		targetRef.getValue().setTargetType(RoleType.COMPLEX_TYPE);
		userBefore.findOrCreateContainer(UserType.F_ASSIGNMENT).add((PrismContainerValue) cval);
		
		
		
//		userBefore.asObjectable().getAssignment().add(cval.asContainerable());
		
		// this should add user and at the sate time assign the role fight..->
		// the result of the operation have to be the same as in test 400
        addObject(userBefore);
        
        dummyAuditService.clear();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
//        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDavidGoliath(userBefore.getOid(), "stone", USER_WORLD_NAME, true, true, true);
       
        // Check audit
        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(4);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0,3);
//        dummyAuditService.asserHasDelta(0,ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(0,ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertExecutionDeltas(1,3);
//        dummyAuditService.asserHasDelta(1,ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(1,ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertExecutionDeltas(2,2);
//        dummyAuditService.asserHasDelta(2,ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(2,ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//        
//        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
//        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(2, ChangeType.MODIFY, ShadowType.class);
//        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
//        display("Last execution delta", executionDelta);
//        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 2);
		} catch (Exception ex){
			LOGGER.info("ex: {}", ex);
			throw ex;
		}
	}
}
