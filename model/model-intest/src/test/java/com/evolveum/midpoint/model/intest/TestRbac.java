/*
 * Copyright (c) 2010-2013 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbac extends AbstractInitializedModelIntegrationTest {
	
	protected static final File TEST_DIR = new File("src/test/resources", "rbac");
	
	protected static final File ROLE_ADRIATIC_PIRATE_FILE = new File(TEST_DIR, "role-adriatic-pirate.xml");
	protected static final String ROLE_ADRIATIC_PIRATE_OID = "12345678-d34d-b33f-f00d-5555555566aa";

	protected static final File ROLE_BLACK_SEA_PIRATE_FILE = new File(TEST_DIR, "role-black-sea-pirate.xml");
	protected static final String ROLE_BLACK_SEA_PIRATE_OID = "12345678-d34d-b33f-f00d-5555555566bb";
	
	protected static final File ROLE_INDIAN_OCEAN_PIRATE_FILE = new File(TEST_DIR, "role-indian-ocean-pirate.xml");
	protected static final String ROLE_INDIAN_OCEAN_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556610";
	
	protected static final File ROLE_HONORABILITY_FILE = new File(TEST_DIR, "role-honorability.xml");
	protected static final String ROLE_HONORABILITY_OID = "12345678-d34d-b33f-f00d-555555557701";
	
	protected static final File ROLE_CLERIC_FILE = new File(TEST_DIR, "role-cleric.xml");
	protected static final String ROLE_CLERIC_OID = "12345678-d34d-b33f-f00d-555555557702";
	
	protected static final File ROLE_WANNABE_FILE = new File(TEST_DIR, "role-wannabe.xml");
	protected static final String ROLE_WANNABE_OID = "12345678-d34d-b33f-f00d-555555557703";
	
	protected static final File ROLE_HONORABLE_WANNABE_FILE = new File(TEST_DIR, "role-honorable-wannabe.xml");
	protected static final String ROLE_HONORABLE_WANNABE_OID = "12345678-d34d-b33f-f00d-555555557704";

	private final String EXISTING_GOSSIP = "Black spot!"; 
	
	public TestRbac() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		
		repoAddObjectFromFile(ROLE_ADRIATIC_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BLACK_SEA_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_INDIAN_OCEAN_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_HONORABILITY_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_CLERIC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WANNABE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_HONORABLE_WANNABE_FILE, RoleType.class, initResult);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter filter = EqualFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, prismContext, null, true);
        ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
        
		// WHEN
		PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        
        // THEN
        display("Role pirate", rolePirate);
        IntegrationTestTools.displayXml("Role pirate", rolePirate);
        assertNotNull("No pirate", rolePirate);
        
        PrismAsserts.assertEquivalent(ROLE_PIRATE_FILE, rolePirate);
	}
	
	@Test
    public void test010SearchReuqestableRoles() throws Exception {
		final String TEST_NAME = "test010SearchReuqestableRoles";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter filter = EqualFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, prismContext, null, true);
        ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
        
		// WHEN
        List<PrismObject<RoleType>> requestableRoles = modelService.searchObjects(RoleType.class, query, null, task, result);
        
        // THEN
        display("Requestable roles", requestableRoles);
        
        assertEquals("Unexpected number of requestable roles", 1, requestableRoles.size());
	}

	@Test
    public void test101JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test101JackAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Caribbean");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test102JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test102JackModifyUserLocality";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
 		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
 		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
 				EXISTING_GOSSIP);
        
        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Tortuga"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test110UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test110UnAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@Test
    public void test120JackAssignRolePirateWhileAlreadyHasAccount() throws Exception {
		final String TEST_NAME = "test120JackAssignRolePirateWhileAlreadyHasAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
        // Make sure that the account has explicit intent
        account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
        
        // Make sure that the existing account has the same value as is set by the role
        // This causes problems if the resource does not tolerate duplicate values in deltas. But provisioning
        // should work around that.
        TestUtil.setAttribute(account, new QName(resourceDummyType.getNamespace(), "title"), DOMUtil.XSD_STRING,
        		prismContext, "Bloody Pirate");
        
		ObjectDelta<UserType> delta = ObjectDelta.createModificationAddReference(UserType.class, USER_JACK_OID, 
				UserType.F_LINK_REF, prismContext, account);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		// We need to switch off the encorcement for this opertation. Otherwise we won't be able to create the account
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		modelService.executeChanges(deltas, null, task, result);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		        
        // Precondition (simplified)
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "rum");
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
  		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
  		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
  				EXISTING_GOSSIP);
        
        // WHEN
  		TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // The account already has a value for 'weapon', it should be unchanged.
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "rum");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	
	
	@Test
    public void test121JackAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test121JackAssignAccountImplicitIntent";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		        
        // Precondition (simplified)
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        
        // WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	@Test
    public void test122JackAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test122JackAssignAccountExplicitIntent";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		        
        // Precondition (simplified)
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        
        // WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 3);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	@Test
    public void test127UnAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test127UnAssignAccountImplicitIntent";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test128UnAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test128UnAssignAccountExplicitIntent";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test129UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test129UnAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test130JackAssignRolePirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test130JackAssignRolePirateWithSeaInAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Caribbean, role , with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test132JackUnAssignRolePirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test132JackUnAssignRolePirateWithSeaInAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * The value for sea is set in Adriatic Pirate role extension.
	 */
	@Test
    public void test134JackAssignRoleAdriaticPirate() throws Exception {
		final String TEST_NAME = "test134JackAssignRoleAdriaticPirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Adriatic, immediately Adriatic, role , with this The Seven Seas while focused on  (in Pirate)");
	}
	
	@Test
    public void test136JackUnAssignRoleAdriaticPirate() throws Exception {
		final String TEST_NAME = "test136JackUnAssignRoleAdriaticPirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, null, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Even though we assign Adriatic Pirate role which has a sea set in its extension the
	 * sea set in user's extension should override it.
	 */
	@Test
    public void test137JackAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test137JackAssignRoleAdriaticPirateWithSeaInAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Adriatic, role , with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test144JackAssignRoleBlackSeaPirate() throws Exception {
		final String TEST_NAME = "test144JackAssignRoleBlackSeaPirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Marmara Sea, immediately Marmara Sea, role Black Sea, with this The Seven Seas while focused on  (in Pirate)");
	}
	
	@Test
    public void test146JackUnAssignRoleBlackSeaPirate() throws Exception {
		final String TEST_NAME = "test146JackUnAssignRoleBlackSeaPirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, null, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test147JackAssignRoleBlackSeaPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test147JackAssignRoleBlackSeaPirateWithSeaInAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Marmara Sea, role Black Sea, with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test149JackUnAssignRoleBlackSeaPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test149JackUnAssignRoleBlackSeaPirateWithSeaInAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, extension, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test154JackAssignRoleIndianOceanPirate() throws Exception {
		final String TEST_NAME = "test154JackAssignRoleIndianOceanPirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Indian Ocean, immediately , role Indian Ocean, with this The Seven Seas while focused on  (in Pirate)");
	}
	
	@Test
    public void test156JackUnAssignRoleIndianOceanPirate() throws Exception {
		final String TEST_NAME = "test156JackUnAssignRoleIndianOceanPirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, null, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	//////////////////////
	// Following tests use POSITIVE enforcement mode
	/////////////////////
	
	@Test
    public void test501JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test501JackAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
	}
	
	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test502JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test502JackModifyUserLocality";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
  		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
  		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
  				EXISTING_GOSSIP);
        
        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Isla de Muerta"));
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen", EXISTING_GOSSIP);
	}
	
	/**
	 * Assignment policy is POSITIVE, therefore the account should remain.
	 */
	@Test
    public void test510UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test510UnAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);

        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, EXISTING_GOSSIP);
	}
	
	/**
	 * This should go fine without any policy violation error.
	 */
	@Test
    public void test511DeleteAccount() throws Exception {
		final String TEST_NAME = "test511DeleteAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = userJack.asObjectable().getLinkRef().iterator().next().getOid();
        
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
        // Use modification of user to delete account. Deleting account directly is tested later.
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteReference(UserType.class, USER_JACK_OID, UserType.F_LINK_REF, prismContext, accountOid);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test520JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test520JackAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen");
	}
	
	@Test
    public void test521JackUnassignRolePirateDeleteAccount() throws Exception {
		final String TEST_NAME = "test521JackUnassignRolePirateDeleteAccount";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        modifications.add(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, false));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = userJack.asObjectable().getLinkRef().iterator().next().getOid();        
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountOid, prismContext);
        // This all goes in the same context with user, explicit unlink should not be necessary
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test530JackAssignRoleCleric() throws Exception {
		final String TEST_NAME = "test530JackAssignRoleCleric";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_CLERIC_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_CLERIC_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Holy man");
	}
	
	@Test
    public void test532JackModifyAssignmentRoleCleric() throws Exception {
		final String TEST_NAME = "test532JackModifyAssignmentRoleCleric";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);
        
        ItemPath itemPath = new ItemPath(
        		new NameItemPathSegment(UserType.F_ASSIGNMENT),
        		new IdItemPathSegment(user.asObjectable().getAssignment().get(0).getId()),
        		new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		ObjectDelta<UserType> assignmentDelta = ObjectDelta.createModificationReplaceProperty(
        		UserType.class, USER_JACK_OID, itemPath, prismContext, "soul");
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(assignmentDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_CLERIC_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Holy soul");
	}
	
	@Test
    public void test539JackUnAssignRoleCleric() throws Exception {
		final String TEST_NAME = "test539JackUnAssignRoleCleric";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);
        
        AssignmentType assignmentType = new AssignmentType();
        assignmentType.setId(user.asObjectable().getAssignment().get(0).getId());
		ObjectDelta<UserType> assignmentDelta = ObjectDelta.createModificationDeleteContainer(
        		UserType.class, USER_JACK_OID, UserType.F_ASSIGNMENT, prismContext, assignmentType);
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(assignmentDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Wannabe role is conditional. All conditions should be false now. So no provisioning should happen.
	 */
	@Test
    public void test540JackAssignRoleWannabe() throws Exception {
		final String TEST_NAME = "test540JackAssignRoleWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WANNABE_OID);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Remove honorifixSuffix. This triggers a condition in Wannabe role inducement. 
	 * But as the whole role has false condition nothing should happen.
	 */
	@Test
    public void test541JackRemoveHonorificSuffixWannabe() throws Exception {
		final String TEST_NAME = "test541JackRemoveHonorificSuffixWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Modify employeeType. This triggers a condition in Wannabe role.
	 */
	@Test
    public void test542JackModifyEmployeeTypeWannabe() throws Exception {
		final String TEST_NAME = "test542JackModifyEmployeeTypeWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, task, result, "wannabe");
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Wannabe Cpt. Where's the rum?");
	}
	
	/**
	 * Remove honorifixPrefix. This triggers a condition in Wannabe role and should remove an account.
	 */
	@Test
    public void test543JackRemoveHonorificPrefixWannabe() throws Exception {
		final String TEST_NAME = "test543JackRemoveHonorificPrefixWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Set honorifixSuffix. This triggers conditions and adds a sub-role Honorable Wannabe.
	 */
	@Test
    public void test544JackSetHonorificSuffixWannabe() throws Exception {
		final String TEST_NAME = "test544JackSetHonorificSuffixWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, task, result, 
				PrismTestUtil.createPolyString("PhD."));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!", "Whatever. -- jack");
	}
	
	/**
	 * Restore honorifixPrefix. The title should be replaced again.
	 */
	@Test
    public void test545JackRestoreHonorificPrefixWannabe() throws Exception {
		final String TEST_NAME = "test545JackRestoreHonorificPrefixWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result, 
				PrismTestUtil.createPolyString("captain"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Wannabe captain Where's the rum?");
	}
	
	/**
	 * The account should be gone - regardless of the condition state.
	 */
	@Test
    public void test549JackUnassignRoleWannabe() throws Exception {
		final String TEST_NAME = "test549JackUnassignRoleWannabe";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
		TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_WANNABE_OID);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNotAssignedRole(USER_JACK_OID, ROLE_WANNABE_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test600JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test600JackAssignRoleJudge";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
//        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
//        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	/**
	 * Judge and pirate are excluded roles. This should fail.
	 */
	@Test
    public void test602JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test602JackAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
	        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
	}
	
	@Test
    public void test605JackUnAssignRoleJudgeAssignRolePirate() throws Exception {
		final String TEST_NAME = "test605JackUnAssignRoleJudgeAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        // Even though Jack is a pirate now the mapping from the role is not applied.
        // the mapping is weak and the account already has a value for weapon from the times
        // Jack was a judge. So it is unchanged
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen");
	}
	
	@Test
    public void test609JackUnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test609JackUnAssignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test700JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test700JackModifyJudgeRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
	}
	
	@Test
    public void test701JackModifyJudgeDeleteConstructionRecompute() throws Exception {
		final String TEST_NAME = "test701JackModifyJudgeDeleteConstructionRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleDeleteInducement(ROLE_JUDGE_OID, 1111L);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test702JackModifyJudgeAddInducementHonorabilityRecompute() throws Exception {
		final String TEST_NAME = "test702JackModifyJudgeAddInducementHonorabilityRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleAddInducementTarget(ROLE_JUDGE_OID, ROLE_HONORABILITY_OID);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
        
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Honorable");
	}
	
	@Test
    public void test703JackModifyJudgeDeleteInducementHonorabilityRecompute() throws Exception {
		final String TEST_NAME = "test703JackModifyJudgeDeleteInducementHonorabilityRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleDeleteInducementTarget(ROLE_JUDGE_OID, ROLE_HONORABILITY_OID);
        PrismObject<RoleType> roleJudge = modelService.getObject(RoleType.class, ROLE_JUDGE_OID, null, task, result);
        display("Role judge", roleJudge);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Honorable");
	}
	
	@Test
    public void test709JackUnAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test709JackUnAssignRoleJudge";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test710JackAssignRoleEmpty() throws Exception {
		final String TEST_NAME = "test710JackAssignRoleEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test712JackModifyEmptyRoleAddInducementPirateRecompute() throws Exception {
		final String TEST_NAME = "test712JackModifyEmptyRoleAddInducementPirateRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleAddInducementTarget(ROLE_EMPTY_OID, ROLE_PIRATE_OID);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "cutlass");
	}
	
	@Test
    public void test714JackModifyEmptyRoleDeleteInducementPirateRecompute() throws Exception {
		final String TEST_NAME = "test714JackModifyEmptyRoleDeleteInducementPirateRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        modifyRoleDeleteInducementTarget(ROLE_EMPTY_OID, ROLE_PIRATE_OID);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test719JackUnAssignRoleEmpty() throws Exception {
		final String TEST_NAME = "test719JackUnAssignRoleEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_EMPTY_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

}
