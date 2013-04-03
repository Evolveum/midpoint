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
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPassword extends AbstractInitializedModelIntegrationTest {
		
	private static final String USER_PASSWORD_1_CLEAR = "d3adM3nT3llN0Tal3s";
	private static final String USER_PASSWORD_2_CLEAR = "bl4ckP3arl";
	private static final String USER_PASSWORD_3_CLEAR = "wh3r3sTheRum?";
	private static final String USER_PASSWORD_4_CLEAR = "sh1v3rM3T1mb3rs";
	private static final String USER_PASSWORD_5_CLEAR = "s3tSa1al";
	
	private static final ItemPath PASSWORD_VALUE_PATH = new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE); 
	
	private String accountOid;
	private String accountRedOid;

	public TestPassword() throws JAXBException {
		super();
	}
		
	@Test
    public void test010AddPasswordPolicy() throws Exception {
        displayTestTile(this, "test010AddPasswordPolicy");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test010AddPasswordPolicy");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ValuePolicyType> passwordPolicy = PrismTestUtil.parseObject(new File(PASSWORD_POLICY_GLOBAL_FILENAME));
        ObjectDelta<ValuePolicyType> passwordPolicyDelta = ObjectDelta.createAddDelta(passwordPolicy);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(passwordPolicyDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        assertEquals("Wrong OID after add", PASSWORD_POLICY_GLOBAL_OID, passwordPolicyDelta.getOid());

		// Check object
        PrismObject<ValuePolicyType> accountShadow = repositoryService.getObject(ValuePolicyType.class, PASSWORD_POLICY_GLOBAL_OID, result);

        // TODO: more asserts
	}
	
	@Test
    public void test050CheckJackPassword() throws Exception {
        displayTestTile(this, "test050CheckJackPassword");

        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added
        
        // THEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test050CheckJackPassword");
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertEncryptedPassword(userJack, "deadmentellnotales");
	}
	

	@Test
    public void test051ModifyUserJackPassword() throws Exception {
        displayTestTile(this, "test051ModifyUserJackPassword");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test051ModifyUserJackPassword");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_PASSWORD_1_CLEAR);
                        
		// WHEN
        modifyUserReplace(USER_JACK_OID, 
        		PASSWORD_VALUE_PATH,
        		task, 
        		result, 
        		userPasswordPs);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertEncryptedPassword(userJack, USER_PASSWORD_1_CLEAR);
	}
	
	@Test
    public void test100ModifyUserJackAssignAccount() throws Exception {
        displayTestTile(this, "test100ModifyUserJackAssignAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test100ModifyUserJackAssignAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ResourceObjectShadowType> accountShadow = repositoryService.getObject(ResourceObjectShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ResourceObjectShadowType> accountModel = modelService.getObject(ResourceObjectShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        assertDummyPassword("jack", USER_PASSWORD_1_CLEAR);
	}
	
	/**
	 * This time user has assigned account. Account password should be changed as well.
	 */
	@Test
    public void test110ModifyUserJackPassword() throws Exception {
        displayTestTile(this, "test110ModifyUserJackPassword");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test110ModifyUserJackPassword");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_PASSWORD_2_CLEAR);
                        
		// WHEN
        modifyUserReplace(USER_JACK_OID, 
        		PASSWORD_VALUE_PATH,
        		task, 
        		result, 
        		userPasswordPs);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertEncryptedPassword(userJack, USER_PASSWORD_2_CLEAR);
		assertDummyPassword("jack", USER_PASSWORD_2_CLEAR);
	}
	
	/**
	 * Modify account password. User's password should be unchanged
	 */
	@Test
    public void test111ModifyAccountJackPassword() throws Exception {
        displayTestTile(this, "test111ModifyAccountJackPassword");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test111ModifyAccountJackPassword");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_PASSWORD_3_CLEAR);
                        
		// WHEN
        modifyAccountShadowReplace(
        		accountOid,
        		PASSWORD_VALUE_PATH,
        		task, 
        		result, 
        		userPasswordPs);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		// User should still have old password
		assertEncryptedPassword(userJack, USER_PASSWORD_2_CLEAR);
		// Account has new password
		assertDummyPassword("jack", USER_PASSWORD_3_CLEAR);
	}
	
	/**
	 * Modify both user and account password. As password outbound mapping is weak the user should have its own password
	 * and account should have its own password.
	 */
	@Test
    public void test112ModifyJackPasswordUserAndAccount() throws Exception {
        displayTestTile(this, "test112ModifyJackPasswordUserAndAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test112ModifyJackPasswordUserAndAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs4 = new ProtectedStringType();
        userPasswordPs4.setClearValue(USER_PASSWORD_4_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs4);
        
        ProtectedStringType userPasswordPs5 = new ProtectedStringType();
        userPasswordPs5.setClearValue(USER_PASSWORD_5_CLEAR);
        ObjectDelta<ResourceObjectShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountOid, resourceDummy, 
        		PASSWORD_VALUE_PATH, userPasswordPs5);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		// User should still have old password
		assertEncryptedPassword(userJack, USER_PASSWORD_4_CLEAR);
		// Account has new password
		assertDummyPassword("jack", USER_PASSWORD_5_CLEAR);
	}
		
	/**
	 * Add red dummy resource to the mix. This would be fun.
	 */
	@Test
    public void test120ModifyUserJackAssignAccountDummyRed() throws Exception {
        displayTestTile(this, "test120ModifyUserJackAssignAccountDummyRed");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test120ModifyUserJackAssignAccountDummyRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getUserAccountRef(userJack, RESOURCE_DUMMY_RED_OID);
                
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);
        
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, "jack", USER_PASSWORD_4_CLEAR);
        
        // User and default dummy account should have unchanged passwords
        assertEncryptedPassword(userJack, USER_PASSWORD_4_CLEAR);
     	assertDummyPassword("jack", USER_PASSWORD_5_CLEAR);
	}
	
	/**
	 * Modify both user and account password. Red dummy has a strong password mapping. User change should override account
	 * change.
	 */
	@Test
    public void test121ModifyJackPasswordUserAndAccountRed() throws Exception {
        displayTestTile(this, "test121ModifyJackPasswordUserAndAccountRed");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPassword.class.getName() + ".test121ModifyJackPasswordUserAndAccountRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ProtectedStringType userPasswordPs1 = new ProtectedStringType();
        userPasswordPs1.setClearValue(USER_PASSWORD_1_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs1);
        
        ProtectedStringType userPasswordPs2 = new ProtectedStringType();
        userPasswordPs2.setClearValue(USER_PASSWORD_2_CLEAR);
        ObjectDelta<ResourceObjectShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountRedOid, resourceDummy,
        		PASSWORD_VALUE_PATH, userPasswordPs2);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		// User should still have old password
		assertEncryptedPassword(userJack, USER_PASSWORD_1_CLEAR);
		// Red account has the same account as user
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, "jack", USER_PASSWORD_1_CLEAR);
		// ... and default account has also the same password as user now. There was no other change on default dummy instance 
		// so even the weak mapping took place.
		assertDummyPassword("jack", USER_PASSWORD_1_CLEAR);
	}

	private void assertEncryptedPassword(PrismObject<UserType> user, String expectedClearPassword) throws EncryptionException {
		UserType userType = user.asObjectable();
		ProtectedStringType protectedActualPassword = userType.getCredentials().getPassword().getValue();
		String actualClearPassword = protector.decryptString(protectedActualPassword);
		assertEquals("Wrong password for "+user, expectedClearPassword, actualClearPassword);
	}

	private void assertDummyPassword(String userId, String expectedClearPassword) {
		assertDummyPassword(null, userId, expectedClearPassword);
	}
	
	private void assertDummyPassword(String instance, String userId, String expectedClearPassword) {
		DummyAccount account = getDummyAccount(instance, userId);
		assertNotNull("No dummy account "+userId, account);
		assertEquals("Wrong password in dummy '"+instance+"' account "+userId, expectedClearPassword, account.getPassword());
	}
	
}
