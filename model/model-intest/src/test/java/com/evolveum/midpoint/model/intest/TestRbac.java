/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
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
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbac extends AbstractInitializedModelIntegrationTest {
	
	private final String EXISTING_GOSSIP = "Black spot!"; 
	
	public TestRbac() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		
	}
	
	@Test
    public void test010SearchReuqestableRoles() throws Exception {
		final String TEST_NAME = "test010SearchReuqestableRoles";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectFilter filter = EqualsFilter.createEqual(RoleType.class, prismContext, RoleType.F_REQUESTABLE, true);
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
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Caribbean");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test102JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test102JackModifyUserLocality";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
 		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
 		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
 				EXISTING_GOSSIP);
        
        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Tortuga"));
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test110UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test110UnAssignRolePirate";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        assertAssignedNoRole(USER_JACK_OID, task, result);
        assertNoDummyAccount("jack");
	}

	@Test
    public void test120JackAssignRolePirateWhileAlreadyHasAccount() throws Exception {
		final String TEST_NAME = "test120JackAssignRolePirateWhileAlreadyHasAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
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
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "weapon", "rum");
        
        // gossip is a tolerant attribute. Make sure there there is something to tolerate
  		DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
  		jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
  				EXISTING_GOSSIP);
        
        // WHEN
  		IntegrationTestTools.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        IntegrationTestTools.displayThen(TEST_NAME);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertAccounts(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        // The account already has a value for 'weapon', it should be unchanged. But it is not.
        // The relative change does cannot see the existing value so it adds its own.
        assertDefaultDummyAccountAttribute("jack", "weapon", "rum", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	
	
	@Test
    public void test121JackAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test121JackAssignAccountImplicitIntent";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		        
        // Precondition (simplified)
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        // WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertAccounts(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	@Test
    public void test122JackAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test122JackAssignAccountExplicitIntent";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		        
        // Precondition (simplified)
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        // WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 3);
        assertAccounts(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
        
	}
	
	@Test
    public void test127UnAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test127UnAssignAccountImplicitIntent";
        displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertAccounts(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test128UnAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test128UnAssignAccountExplicitIntent";
        displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertAccounts(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
	}
	
	@Test
    public void test129UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test129UnAssignRolePirate";
        displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount("jack");
	}
	
	@Test
    public void test130JackAssignRolePirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test130JackAssignRolePirateWithSeaInAssignment";
        displayTestTile(this, TEST_NAME);

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
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute("jack", DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Caribbean, with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test132JackUnAssignRolePirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test132JackUnAssignRolePirateWithSeaInAssignment";
        displayTestTile(this, TEST_NAME);

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
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount("jack");
	}
	
	/**
	 * The value for sea is set in Adriatic Pirate role extension.
	 */
	@Test
    public void test134JackAssignRoleAdriaticPirate() throws Exception {
		final String TEST_NAME = "test134JackAssignRoleAdriaticPirate";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute("jack", DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Adriatic, immediately Adriatic, with this The Seven Seas while focused on  (in Pirate)");
	}
	
	@Test
    public void test136JackUnAssignRoleAdriaticPirate() throws Exception {
		final String TEST_NAME = "test136JackUnAssignRoleAdriaticPirate";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, null, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount("jack");
	}
	
	/**
	 * Even though we assign Adriatic Pirate role which has a sea set in its extension the
	 * sea set in user's extension should override it.
	 */
	@Test
    public void test137JackAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test137JackAssignRoleAdriaticPirateWithSeaInAssignment";
        displayTestTile(this, TEST_NAME);

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
        assertAssignedRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute("jack", DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, 
        		"jack sailed Caribbean, immediately Adriatic, with this The Seven Seas while focused on Caribbean (in Pirate)");
	}
	
	@Test
    public void test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
		final String TEST_NAME = "test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment";
        displayTestTile(this, TEST_NAME);

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
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertNoDummyAccount("jack");
	}
	
	//////////////////////
	// Following tests use POSITIVE enforcement mode
	/////////////////////
	
	@Test
    public void test501JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test501JackAssignRolePirate";
        displayTestTile(this, TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Tortuga has ever seen");
	}
	
	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test502JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test502JackModifyUserLocality";
        displayTestTile(this, TEST_NAME);

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
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen", EXISTING_GOSSIP);
	}
	
	/**
	 * Assignment policy is POSITIVE, therefore the account should remain.
	 */
	@Test
    public void test510UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test510UnAssignRolePirate";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
               
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);

        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, EXISTING_GOSSIP);
	}
	
	/**
	 * This should go fine without any policy violation error.
	 */
	@Test
    public void test511DeleteAccount() throws Exception {
		final String TEST_NAME = "test511DeleteAccount";
        displayTestTile(this, TEST_NAME);

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
        assertNoDummyAccount("jack");
	}
	
	@Test
    public void test520JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test520JackAssignRolePirate";
        displayTestTile(this, TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Isla de Muerta");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
        assertDefaultDummyAccountAttribute("jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Isla de Muerta has ever seen");
	}
	
	@Test
    public void test521JackUnassignRolePirateDeleteAccount() throws Exception {
		final String TEST_NAME = "test521JackUnassignRolePirateDeleteAccount";
        displayTestTile(this, TEST_NAME);
        
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        modifications.add(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, false));
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
        assertNoDummyAccount("jack");
	}

}
