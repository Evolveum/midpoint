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
package com.evolveum.midpoint.model;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbac extends AbstractInitializedModelIntegrationTest {
	
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
    public void test001JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test001JackAssignRolePirate";
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
	}
	
	/**
	 * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
	 * be updated as well. 
	 */
	@Test
    public void test002JackModifyUserLocality() throws Exception {
		final String TEST_NAME = "test002JackModifyUserLocality";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Tortuga"));
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Tortuga");
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
	}
	
	@Test
    public void test010UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test010UnAssignRolePirate";
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
    public void test100JackAssignRolePirateWhileAlreadyHasAccount() throws Exception {
        displayTestTile(this, "test100JackAssignRolePirateWhileAlreadyHasAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test100JackAssignRolePirateWhileAlreadyHasAccount");
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        // Make sure that the account has explicit intent
        account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
        
        // Make sure that the existing account has the same value as is set by the role
        // This causes problems if the resource does not tolerate duplicate values in deltas. But provisioning
        // should work around that.
        TestUtil.setAttribute(account, new QName(resourceDummyType.getNamespace(), "title"), DOMUtil.XSD_STRING,
        		prismContext, "Bloody Pirate");
        
		ObjectDelta<UserType> delta = ObjectDelta.createModificationAddReference(UserType.class, USER_JACK_OID, 
				UserType.F_ACCOUNT_REF, prismContext, account);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		// We need to switch off the encorcement for this opertation. Otherwise we won't be able to create the account
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		modelService.executeChanges(deltas, null, task, result);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		        
        // Precondition (simplified)
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "weapon", "rum");
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
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
        
	}
	
	
	
	@Test
    public void test101JackAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test101JackAssignAccountImplicitIntent";
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
        
	}
	
	@Test
    public void test102JackAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test102JackAssignAccountExplicitIntent";
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
        
	}
	
	@Test
    public void test107UnAssignAccountImplicitIntent() throws Exception {
		final String TEST_NAME = "test107UnAssignAccountImplicitIntent";
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
	}
	
	@Test
    public void test108UnAssignAccountExplicitIntent() throws Exception {
		final String TEST_NAME = "test108UnAssignAccountExplicitIntent";
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
	}
	
	@Test
    public void test109UnAssignRolePirate() throws Exception {
		final String TEST_NAME = "test109UnAssignRolePirate";
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
        
        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Isla de Muerta"));
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute("jack", "title", "Bloody Pirate");
        assertDefaultDummyAccountAttribute("jack", "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute("jack", "weapon", "cutlass");
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
        String accountOid = userJack.asObjectable().getAccountRef().iterator().next().getOid();
        
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, accountOid, prismContext);
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteReference(UserType.class, USER_JACK_OID, UserType.F_ACCOUNT_REF, prismContext, accountOid);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertNoDummyAccount("jack");
	}

}
