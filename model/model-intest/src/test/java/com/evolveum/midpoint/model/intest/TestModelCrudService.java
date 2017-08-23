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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import static org.testng.AssertJUnit.*;

/**
 * This is testing the DEPRECATED functions of model API. It should be removed once the functions are phased out.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelCrudService extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/crud");
	public static final File TEST_CONTRACT_DIR = new File("src/test/resources/contract");

	public static final File RESOURCE_MAROON_FILE = new File(TEST_DIR, "resource-dummy-maroon.xml");
	public static final String RESOURCE_MAROON_OID = "10000000-0000-0000-0000-00000000e104";
	
	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";
	
	private static String accountOid;
	
	@Autowired(required = true)
	protected ModelCrudService modelCrudService;
			
	@Test
    public void test050AddResource() throws Exception {
		final String TEST_NAME = "test050AddResource";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ResourceType resourceType = (ResourceType) PrismTestUtil.parseObject(RESOURCE_MAROON_FILE).asObjectable();
        
        // WHEN
        PrismObject<ResourceType> object = resourceType.asPrismObject();
		prismContext.adopt(resourceType);
		modelCrudService.addObject(object, null, task, result);
        		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		// Make sure the resource has t:norm part of polystring name
		PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_MAROON_OID, null, task, result);
		assertEquals("Wrong orig in resource name", "Dummy Resource Maroon", resourceAfter.asObjectable().getName().getOrig());
		assertEquals("Wrong norm in resource name", "dummy resource maroon", resourceAfter.asObjectable().getName().getNorm());
	}
	
	@Test
    public void test100ModifyUserAddAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserAddAccount";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
	}
		
	@Test
    public void test119ModifyUserDeleteAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test119ModifyUserDeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test119ModifyUserDeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountOid);
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
        
		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        	AssertJUnit.fail("Shadow "+accountOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
	}
	
	@Test
    public void test120AddAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test120AddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test120AddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
		// WHEN
        accountOid = modelCrudService.addObject(account, null, task, result);
		
		// THEN
		// Check accountRef (should be none)
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
	}
	
	@Test
    public void test121ModifyUserAddAccountRef() throws Exception {
        TestUtil.displayTestTitle(this, "test121ModifyUserAddAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test121ModifyUserAddAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
	}


	
	@Test
    public void test128ModifyUserDeleteAccountRef() throws Exception {
        TestUtil.displayTestTitle(this, "test128ModifyUserDeleteAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test128ModifyUserDeleteAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);
		        
		// Check shadow (if it is unchanged)
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account (if it is unchanged)
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource (if it is unchanged)
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
	}
	
	@Test
    public void test129DeleteAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test129DeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test129DeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
		// WHEN
        modelCrudService.deleteObject(ShadowType.class, accountOid, null, task, result);
		
		// THEN
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
		// Check is shadow is gone
        assertNoShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
	}
	
	@Test
    public void test150AddUserBlackbeardWithAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test150AddUserBlackbeardWithAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test150AddUserBlackbeardWithAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_CONTRACT_DIR, "user-blackbeard-account-dummy.xml"));
                
		// WHEN
        modelCrudService.addObject(user , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
        
        // Check account in dummy resource
        assertDefaultDummyAccount("blackbeard", "Edward Teach", true);
	}

	
	@Test
    public void test210AddUserMorganWithAssignment() throws Exception {
        TestUtil.displayTestTitle(this, "test210AddUserMorganWithAssignment");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test210AddUserMorganWithAssignment");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_CONTRACT_DIR, "user-morgan-assignment-dummy.xml"));
                
		// WHEN
        modelCrudService.addObject(user , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "morgan");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
        
        // Check account in dummy resource
        assertDefaultDummyAccount("morgan", "Sir Henry Morgan", true);
	}

	@Test
	public void test220DeleteUserMorgan() throws Exception {
		TestUtil.displayTestTitle(this, "test220DeleteUserMorgan");

		// GIVEN
		Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test220DeleteUserMorgan");
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		assertDummyAccount(null, "morgan");

		// WHEN
		modelCrudService.deleteObject(FocusType.class, USER_MORGAN_OID, null, task, result);

		// THEN
		try {
			getUser(USER_MORGAN_OID);
			fail("User morgan exists even if he should not");
		} catch (ObjectNotFoundException e) {
			// ok
		}

		assertNoDummyAccount(null, "morgan");

		result.computeStatus();
		IntegrationTestTools.display(result);
		TestUtil.assertSuccess(result);
	}
}
