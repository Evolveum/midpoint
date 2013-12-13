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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRoleEntitlement extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static String groupOid;
	private static String userCharlesOid;
	
	@Test
    public void test050GetRolePirate() throws Exception {
		final String TEST_NAME = "test050GetRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // WHEN
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        
        // THEN
        display("Role pirate", role);
        assertRolePirate(role);
        
        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
	}
	
	protected void assertRolePirate(PrismObject<RoleType> role) {
		assertObject(role);
		assertEquals("Wrong "+role+" OID (prism)", ROLE_PIRATE_OID, role.getOid());
		RoleType roleType = role.asObjectable();
		assertEquals("Wrong "+role+" OID (jaxb)", ROLE_PIRATE_OID, roleType.getOid());
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", "Pirate", roleType.getName());		
	}
	
	@Test
    public void test100ModifyRoleAddEntitlement() throws Exception {
        TestUtil.displayTestTile(this, "test100ModifyRoleAddEntitlement");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test100ModifyUserAddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<ShadowType> group = PrismTestUtil.parseObject(GROUP_PIRATE_DUMMY_FILE);
        
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createEmptyModifyDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        PrismReferenceValue linkRefVal = new PrismReferenceValue();
		linkRefVal.setObject(group);
		ReferenceDelta groupDelta = ReferenceDelta.createModificationAdd(RoleType.F_LINK_REF, getRoleDefinition(), linkRefVal);
		roleDelta.addModification(groupDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(roleDelta);
		
		dummyAuditService.clear();
        prepareNotifications();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
		// Check accountRef
		PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        display("Role pirate after", rolePirate);
		assertRolePirate(rolePirate);
		assertLinks(rolePirate, 1);
		groupOid = getSingleLinkOid(rolePirate);
        assertFalse("No linkRef oid", StringUtils.isBlank(groupOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(accountShadow, groupOid, GROUP_PIRATE_DUMMY_NAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        assertDummyGroupShadowModel(accountModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
        
        // Check account in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, GROUP_PIRATE_DUMMY_DESCRIPTION);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

	}

//    @Test
//    public void test101GetAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test101GetAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test101GetAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        
//        // Let's do some evil things. Like changing some of the attribute on a resource and see if they will be
//        // fetched after get.
//        // Also set a value for ignored "water" attribute. The system should cope with that.
//        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
//        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "The best pirate captain ever");
//        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, "cold");
//        
//		// WHEN
//		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, groupOid, null , task, result);
//		
//		// THEN
//		display("Account", account);
//		display("Account def", account.getDefinition());
//		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
//		display("Account attributes def", accountContainer.getDefinition());
//		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
//        assertDummyAccountShadowModel(account, groupOid, "jack", "Jack Sparrow");
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("getObject result", result);
//        
//        account.checkConsistence(true, true);
//        
//        IntegrationTestTools.assertAttribute(account, getAttributeQName(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), 
//        		"The best pirate captain ever");
//        // This one should still be here, even if ignored
//        IntegrationTestTools.assertAttribute(account, getAttributeQName(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME), 
//        		"cold");
//	}
//
//	@Test
//    public void test102GetAccountNoFetch() throws Exception {
//        TestUtil.displayTestTile(this, "test102GetAccountNoFetch");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test102GetAccountNoFetch");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        
//        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
//		
//		// WHEN
//		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, groupOid, options , task, result);
//		
//		display("Account", account);
//		display("Account def", account.getDefinition());
//		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
//		display("Account attributes def", accountContainer.getDefinition());
//		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
//        assertDummyAccountShadowRepo(account, groupOid, "jack");
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("getObject result", result);
//	}
//	
//	@Test
//    public void test103GetAccountRaw() throws Exception {
//        TestUtil.displayTestTile(this, "test103GetAccountRaw");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test103GetAccountRaw");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
//		
//		// WHEN
//		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, groupOid, options , task, result);
//		
//		display("Account", account);
//		display("Account def", account.getDefinition());
//		PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
//		display("Account attributes def", accountContainer.getDefinition());
//		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
//        assertDummyAccountShadowRepo(account, groupOid, "jack");
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("getObject result", result);
//	}
//
//	@Test
//    public void test108ModifyUserAddAccountAgain() throws Exception {
//        TestUtil.displayTestTile(this, "test108ModifyUserAddAccountAgain");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test108ModifyUserAddAccountAgain");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//		
//		dummyAuditService.clear();
//
//		try {
//			
//			// WHEN
//			modelService.executeChanges(deltas, null, task, result);
//			
//			// THEN
//			assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
//		} catch (SchemaException e) {
//			// This is expected
//			e.printStackTrace();
//			// THEN
//			String message = e.getMessage();
//			assertMessageContains(message, "already contains account");
//			assertMessageContains(message, "default");
//		}
//		
//		// Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//    }
//	
//	@Test
//    public void test109ModifyUserAddAccountAgain() throws Exception {
//        TestUtil.displayTestTile(this, "test109ModifyUserAddAccountAgain");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test109ModifyUserAddAccountAgain");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        account.setOid(null);
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//		
//		purgeScriptHistory();
//		dummyAuditService.clear();
//        
//		try {
//			
//			// WHEN
//			modelService.executeChanges(deltas, null, task, result);
//			
//			// THEN
//			assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
//		} catch (SchemaException e) {
//			// This is expected
//			// THEN
//			String message = e.getMessage();
//			assertMessageContains(message, "already contains account");
//			assertMessageContains(message, "default");
//		}
//		
//		assertNoProvisioningScripts();
//		
//		// Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//		
//	}
//
//	@Test
//    public void test110GetUserResolveAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test110GetUserResolveAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test110GetUserResolveAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//
//        Collection<SelectorOptions<GetOperationOptions>> options = 
//        	SelectorOptions.createCollection(UserType.F_LINK, GetOperationOptions.createResolve());
//        
//		// WHEN
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);
//		
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
//        String groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        
//        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
//        assertEquals("OID mismatch in accountRefValue", groupOid, accountRefValue.getOid());
//        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());
//
//        assertEquals("Unexpected number of accounts", 1, userJackType.getLink().size());
//        ShadowType ResourceObjectShadowType = userJackType.getLink().get(0);
//        assertDummyAccountShadowModel(ResourceObjectShadowType.asPrismObject(), groupOid, "jack", "Jack Sparrow");
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("getObject result", result);
//	}
//
//
//	@Test
//    public void test111GetUserResolveAccountResource() throws Exception {
//        TestUtil.displayTestTile(this, "test111GetUserResolveAccountResource");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test111GetUserResolveAccountResource");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//
//        Collection<SelectorOptions<GetOperationOptions>> options = 
//            	SelectorOptions.createCollection(GetOperationOptions.createResolve(),
//        			new ItemPath(UserType.F_LINK),
//    				new ItemPath(UserType.F_LINK, ShadowType.F_RESOURCE)
//        	);
//        
//		// WHEN
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);
//		
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
//        String groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        
//        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
//        assertEquals("OID mismatch in accountRefValue", groupOid, accountRefValue.getOid());
//        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());
//
//        assertEquals("Unexpected number of accounts", 1, userJackType.getLink().size());
//        ShadowType ResourceObjectShadowType = userJackType.getLink().get(0);
//        assertDummyAccountShadowModel(ResourceObjectShadowType.asPrismObject(), groupOid, "jack", "Jack Sparrow");
//        
//        assertNotNull("Resource in account was not resolved", ResourceObjectShadowType.getResource());
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("getObject result", result);
//        
//        userJack.checkConsistence(true, true);
//	}
//
//	@Test
//    public void test112GetUserResolveAccountNoFetch() throws Exception {
//        TestUtil.displayTestTile(this, "test112GetUserResolveAccountNoFetch");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test112GetUserResolveAccountNoFetch");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//
//        GetOperationOptions getOpts = new GetOperationOptions();
//        getOpts.setResolve(true);
//        getOpts.setNoFetch(true);
//		Collection<SelectorOptions<GetOperationOptions>> options = 
//        	SelectorOptions.createCollection(UserType.F_LINK, getOpts);
//        
//		// WHEN
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options , task, result);
//		
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
//        String groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        
//        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
//        assertEquals("OID mismatch in accountRefValue", groupOid, accountRefValue.getOid());
//        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());
//
//        assertEquals("Unexpected number of accounts", 1, userJackType.getLink().size());
//        ShadowType ResourceObjectShadowType = userJackType.getLink().get(0);
//        assertDummyAccountShadowRepo(ResourceObjectShadowType.asPrismObject(), groupOid, "jack");
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("getObject result", result);
//        
//        userJack.checkConsistence(true, true);
//	}
//	
//	@Test
//    public void test119ModifyUserDeleteAccount() throws Exception {
//		final String TEST_NAME = "test119ModifyUserDeleteAccount";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        account.setOid(groupOid);
//        		
//		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//
//        prepareNotifications();
//
//        // WHEN
//		TestUtil.displayWhen(TEST_NAME);
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result, 2);
//        
//		// Check accountRef
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of linkRefs", 0, userJackType.getLinkRef().size());
//        
//		// Check is shadow is gone
//        try {
//        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        	AssertJUnit.fail("Shadow "+groupOid+" still exists");
//        } catch (ObjectNotFoundException e) {
//        	// This is OK
//        }
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//    }
//
//    @Test
//    public void test120AddAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test120AddAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test120AddAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//
//        prepareNotifications();
//
//        purgeScriptHistory();
//        
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createAddDelta(account);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//        
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//        result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//        groupOid = accountDelta.getOid();
//        assertNotNull("No account OID in resulting delta", groupOid);
//		// Check accountRef (should be none)
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        // The user is not associated with the account
//        assertDummyScriptsAdd(null, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(accountShadow.getOid());
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);          // there's no password for that account
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);               // account has no owner
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	@Test
//    public void test121ModifyUserAddAccountRef() throws Exception {
//        TestUtil.displayTestTile(this, "test121ModifyUserAddAccountRef");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test121ModifyUserAddAccountRef");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//
//        prepareNotifications();
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), groupOid);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);                // some attributes on the account are changed
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//
//
//	
//	@Test
//    public void test128ModifyUserDeleteAccountRef() throws Exception {
//        TestUtil.displayTestTile(this, "test128ModifyUserDeleteAccountRef");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test128ModifyUserDeleteAccountRef");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        prepareNotifications();
//
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), groupOid);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//		        
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        assertUserJack(userJack);
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//		        
//		// Check shadow (if it is unchanged)
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account (if it is unchanged)
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        
//        // Check account in dummy resource (if it is unchanged)
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	@Test
//    public void test129DeleteAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test129DeleteAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test129DeleteAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        prepareNotifications();
//        purgeScriptHistory();
//        
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, groupOid, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
//        
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//        result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        assertUserJack(userJack);
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//		// Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(groupOid);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);           // there's no link user->account (removed in test128)
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//    }
//
//	
//	@Test
//    public void test130PreviewModifyUserJackAssignAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test130PreviewModifyUserJackAssignAccount");
//
//        // GIVEN
//        try{
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test130PreviewModifyUserJackAssignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//                
//		// WHEN
//        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("previewChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//		// TODO: assert context
//		// TODO: assert context
//		// TODO: assert context
//        
//        assertResolvedResourceRefs(modelContext);
//        
//        // Check account in dummy resource
//        assertNoDummyAccount("jack");
//        
//        dummyAuditService.assertNoRecord();
//        }catch(Exception ex){
//    		LOGGER.info("Exception {}", ex.getMessage(), ex);
//    	}
//	}
//	
//	@Test
//    public void test131ModifyUserJackAssignAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test131ModifyUserJackAssignAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test131ModifyUserJackAssignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        prepareNotifications();
//        purgeScriptHistory();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//                  
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//    }
//	
//	/**
//	 * Modify the account. Some of the changes should be reflected back to the user by inbound mapping.
//	 */
//	@Test
//    public void test132ModifyAccountJackDummy() throws Exception {
//        TestUtil.displayTestTile(this, "test132ModifyAccountJackDummy");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test132ModifyAccountJackDummy");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		groupOid, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
//        accountDelta.addModificationReplaceProperty(
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		"Queen Anne's Revenge");
//        deltas.add(accountDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		// Fullname inbound mapping is not used because it is weak
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// ship inbound mapping is used, it is strong 
//		assertEquals("Wrong user locality (orig)", "The crew of Queen Anne's Revenge", 
//				userJack.asObjectable().getOrganizationalUnit().iterator().next().getOrig());
//		assertEquals("Wrong user locality (norm)", "the crew of queen annes revenge", 
//				userJack.asObjectable().getOrganizationalUnit().iterator().next().getNorm());
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account
//        // All the changes should be reflected to the account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Cpt. Jack Sparrow");
//        PrismAsserts.assertPropertyValue(accountModel, 
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		"Queen Anne's Revenge");
//        
//        // Check account in dummy resource
//        assertDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, 
//        		"Queen Anne's Revenge");
//        
//        assertDummyScriptsModify(userJack);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(3);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0, 1);
//        dummyAuditService.asserHasDelta(0, ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionDeltas(1, 1);
//        dummyAuditService.asserHasDelta(1, ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	@Test
//    public void test139ModifyUserJackUnassignAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test139ModifyUserJackUnassignAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test139ModifyUserJackUnassignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test. The account should be added.
//	 */
//	@Test
//    public void test141ModifyUserJackAssignAccountPositiveEnforcement() throws Exception {
//		final String TEST_NAME = "test141ModifyUserJackAssignAccountPositiveEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//        
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test. The account should remain as it is.
//	 */
//	@Test
//    public void test148ModifyUserJackUnassignAccountPositiveEnforcement() throws Exception {
//		final String TEST_NAME = "test148ModifyUserJackUnassignAccountPositiveEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() 
//        		+ "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//        
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//                
//        //change resource assigment policy to be positive..if they are not applied by projector, the test will fail
//        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//		assertLinked(userJack, groupOid);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        assertNoProvisioningScripts();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//        
//        // return resource to the previous state..delete assignment enforcement to prevent next test to fail..
//        deleteResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
//	 * Now we will explicitly delete the account.
//	 */
//	@Test
//    public void test149ModifyUserJackDeleteAccount() throws Exception {
//		final String TEST_NAME = "test149ModifyUserJackDeleteAccount";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setOid(groupOid);
//		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), groupOid);
//		userDelta.addModification(accountRefDelta);
//        
//		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, groupOid, prismContext);
//		
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	/**
//	 * Assignment enforcement is set to RELATTIVE for this test. The account should be added.
//	 */
//	@Test
//    public void test151ModifyUserJackAssignAccountRelativeEnforcement() throws Exception {
//		final String TEST_NAME = "test151ModifyUserJackAssignAccountRelativeEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//        
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	/**
//	 * Assignment enforcement is set to RELATIVE for this test. The account should be gone.
//	 */
//	@Test
//    public void test158ModifyUserJackUnassignAccountRelativeEnforcement() throws Exception {
//		final String TEST_NAME = "test158ModifyUserJackUnassignAccountRelativeEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() 
//        		+ "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//        
//        // WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//	
//	/**
//	 * Assignment enforcement is set to NONE for this test.
//	 */
//	@Test
//    public void test161ModifyUserJackAssignAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test161ModifyUserJackAssignAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//        
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsNone();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//    }
//	
//	@Test
//    public void test163ModifyUserJackAddAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test163ModifyUserJackAddAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//        
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//		
//		purgeScriptHistory();
//		dummyAuditService.clear();
//        dummyNotifier.clearRecords();
//        dummyTransport.clearMessages();
//        notificationManager.setDisabled(false);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//        
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        notificationManager.setDisabled(true);
//
//        // Check notifications
//        display("Notifier", dummyNotifier);
//        checkTest100NotificationRecords("newAccounts");
//        checkTest100NotificationRecords("newAccountsViaExpression");
//
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//	}
//	
//	@Test
//    public void test164ModifyUserJackUnassignAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test164ModifyUserJackUnassignAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() 
//        		+ "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//        
//        // WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        assertDummyScriptsNone();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        notificationManager.setDisabled(true);
//
//        // Check notifications
//        display("Notifier", dummyNotifier);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//    }
//	
//	@Test
//    public void test169ModifyUserJackDeleteAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test169ModifyUserJackDeleteAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setOid(groupOid);
//		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), groupOid);
//		userDelta.addModification(accountRefDelta);
//        
//		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, groupOid, prismContext);
//		
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//
//	@Test
//    public void test180ModifyUserAddAccountFullEnforcement() throws Exception {
//		final String TEST_NAME = "test180ModifyUserAddAccountFullEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//		
//		dummyAuditService.clear();
//		purgeScriptHistory();
//        
//		try {
//		
//			// WHEN
//			modelService.executeChanges(deltas, null, task, result);
//			
//			AssertJUnit.fail("Unexpected executeChanges success");
//		} catch (PolicyViolationException e) {
//			// This is expected
//			display("Expected exception", e);
//		}
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check that shadow was not created
//        assertNoAccountShadow(groupOid);
//        
//        // Check that dummy resource account was not created
//        assertNoDummyAccount("jack");
//        
//        assertNoProvisioningScripts();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0, 0);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//	}
//	
//	@Test
//    public void test182ModifyUserAddAndAssignAccountPositiveEnforcement() throws Exception {
//		final String TEST_NAME = "test182ModifyUserAddAndAssignAccountPositiveEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//		
//		dummyAuditService.clear();
//		purgeScriptHistory();
//		
//		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//        
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//			
//		// THEN
//		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//		// Check accountRef
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
//        groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
//        assertEquals("OID mismatch in accountRefValue", groupOid, accountRefValue.getOid());
//        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//        
//        result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//	}
//	
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
//	 * Now we will explicitly delete the account.
//	 */
//	@Test
//    public void test189ModifyUserJackUnassignAndDeleteAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test189ModifyUserJackUnassignAndDeleteAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test149ModifyUserJackUnassignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        // Explicit unlink is not needed here, it should work without it
//        
//		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, groupOid, prismContext);
//		
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//	}
//	
//	/**
//	 * We try to both assign an account and modify that account in one operation.
//	 * Some changes should be reflected to account (e.g.  weapon) as the mapping is weak, other should be
//	 * overridded (e.g. fullname) as the mapping is strong.
//	 */
//	@Test
//    public void test190ModifyUserJackAssignAccountAndModify() throws Exception {
//        TestUtil.displayTestTile(this, "test190ModifyUserJackAssignAccountAndModify");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test190ModifyUserJackAssignAccountAndModify");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        ShadowDiscriminatorObjectDelta<ShadowType> accountDelta = ShadowDiscriminatorObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
//        accountDelta.addModificationAddProperty(
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME), 
//        		"smell");
//        deltas.add(accountDelta);
//        deltas.add(accountAssignmentUserDelta);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Jack Sparrow");
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, USER_JACK_USERNAME);
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
//        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "smell");
//        assertNull("Unexpected loot", dummyAccount.getAttributeValue("loot", Integer.class));
//        
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//
//    /**
//     * We try to modify an assignment of the account and see whether changes will be recorded in the account itself.
//     *
//     */
//    @Test
//    public void test191ModifyUserJackModifyAssignment() throws Exception {
//        TestUtil.displayTestTile(this, "test191ModifyUserJackModifyAssignment");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test191ModifyUserJackModifyAssignment");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//
//        //PrismPropertyDefinition definition = getAssignmentDefinition().findPropertyDefinition(new QName(SchemaConstantsGenerated.NS_COMMON, "accountConstruction"));
//
//        PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
//        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(dummyResource, prismContext);
//        RefinedObjectClassDefinition accountDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String) null);
//        PrismPropertyDefinition gossipDefinition = accountDefinition.findPropertyDefinition(new QName(
//                "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004",
//                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME));
//        assertNotNull("gossip attribute definition not found", gossipDefinition);
//
//        ConstructionType accountConstruction = createAccountConstruction(RESOURCE_DUMMY_OID, null);
//        ResourceAttributeDefinitionType radt = new ResourceAttributeDefinitionType();
//        radt.setRef(gossipDefinition.getName());
//        MappingType outbound = new MappingType();
//        radt.setOutbound(outbound);
//
//        ExpressionType expression = new ExpressionType();
//        outbound.setExpression(expression);
//
//        MappingType value = new MappingType();
//
//        PrismProperty<String> property = gossipDefinition.instantiate();
//        property.add(new PrismPropertyValue<String>("q"));
//
//        List evaluators = expression.getExpressionEvaluator();
//        Collection<?> collection = LiteralExpressionEvaluatorFactory.serializeValueElements(property, null);
//        ObjectFactory of = new ObjectFactory();
//        for (Object obj : collection) {
//            evaluators.add(of.createValue(obj));
//        }
//
//        value.setExpression(expression);
//        radt.setOutbound(value);
//        accountConstruction.getAttribute().add(radt);
//        ObjectDelta<UserType> accountAssignmentUserDelta =
//                createReplaceAccountConstructionUserDelta(USER_JACK_OID, 1L, accountConstruction);
//        deltas.add(accountAssignmentUserDelta);
//
//        PrismObject<UserType> userJackOld = getUser(USER_JACK_OID);
//        display("User before change execution", userJackOld);
//        display("Deltas to execute execution", deltas);
//
//        // WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//        // THEN
//        result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        display("User after change execution", userJack);
//        assertUserJack(userJack, "Jack Sparrow");
//        groupOid = getSingleUserAccountRef(userJack);
//
//        // Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, USER_JACK_USERNAME);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
//
//        // Check account in dummy resource
//        assertDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
//        display(dummyAccount.debugDump());
//        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "q");
//        //assertEquals("Missing or incorrect attribute value", "soda", dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class));
//
//        assertDummyScriptsModify(userJack, true);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        Collection<ObjectDeltaOperation<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
//        assertEquals("Wrong number of execution deltas", 2, auditExecutionDeltas.size());
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//    }
//
//    @Test
//    public void test195ModifyUserJack() throws Exception {
//        TestUtil.displayTestTile(this, "test195ModifyUserJack");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test195ModifyUserJack");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, 
//        		PrismTestUtil.createPolyString("Magnificent Captain Jack Sparrow"));
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Magnificent Captain Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//        
//        assertDummyScriptsModify(userJack);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//    
//    @Test
//    public void test196ModifyUserJackLocationEmpty() throws Exception {
//    	final String TEST_NAME = "test196ModifyUserJackLocationEmpty";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Magnificent Captain Jack Sparrow", "Jack", "Sparrow", null);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Magnificent Captain Jack Sparrow");
//        IntegrationTestTools.assertNoAttribute(accountModel, dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//        
//        assertDummyScriptsModify(userJack);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//    }
//
//    @Test
//    public void test197ModifyUserJackLocationNull() throws Exception {
//    	final String TEST_NAME = "test197ModifyUserJackLocationNull";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//               
//        try {
//			// WHEN
//	        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, (PolyString)null);
//	        
//	        AssertJUnit.fail("Unexpected success");
//        } catch (IllegalStateException e) {
//        	// This is expected
//        }
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure(result);
//        
//        assertNoProvisioningScripts();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        // This should fail even before the request record is created
//        dummyAuditService.assertRecords(0);
//	}
//    
//	@Test
//    public void test198ModifyUserJackRaw() throws Exception {
//        TestUtil.displayTestTile(this, "test198ModifyUserJackRaw");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test196ModifyUserJackRaw");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//
//        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
//        		PrismTestUtil.createPolyString("Marvelous Captain Jack Sparrow"));
//        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(objectDelta);
//                        
//		// WHEN
//		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Marvelous Captain Jack Sparrow", "Jack", "Sparrow", null);
//        groupOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "jack");
//        
//        // Check account - the original fullName should not be changed
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "jack", "Magnificent Captain Jack Sparrow");
//        
//        // Check account in dummy resource - the original fullName should not be changed
//        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//        
//        assertNoProvisioningScripts();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        // raw operation, no target
//        //dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//    }
//		
//	@Test
//    public void test199DeleteUserJack() throws Exception {
//        TestUtil.displayTestTile(this, "test199DeleteUserJack");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test199DeleteUserJack");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		try {
//			PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//			AssertJUnit.fail("Jack is still alive!");
//		} catch (ObjectNotFoundException ex) {
//			// This is OK
//		}
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(groupOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//        
//        assertDummyScriptsDelete();
//        
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 1);
//
//    }
//	
//	@Test
//    public void test200AddUserBlackbeardWithAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test200AddUserBlackbeardWithAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test200AddUserBlackbeardWithAccount");
//        // Use custom channel to trigger a special outbound mapping
//        task.setChannel("http://pirates.net/avast");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-blackbeard-account-dummy.xml"));
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userBlackbeard = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
//        UserType userBlackbeardType = userBlackbeard.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userBlackbeardType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userBlackbeardType.getLinkRef().get(0);
//        String groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "blackbeard");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "blackbeard", "Edward Teach");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("blackbeard", "Edward Teach", true);
//        DummyAccount dummyAccount = getDummyAccount(null, "blackbeard");
//        assertEquals("Wrong loot", (Integer)10000, dummyAccount.getAttributeValue("loot", Integer.class));
//        
//        assertDummyScriptsAdd(userBlackbeard, accountModel, resourceDummyType);
//        
//        // Check audit        
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(3);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0, 3);
//        dummyAuditService.asserHasDelta(0, ChangeType.ADD, UserType.class);
//        dummyAuditService.asserHasDelta(0, ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(0, ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertExecutionDeltas(1, 1);
//        dummyAuditService.asserHasDelta(1, ChangeType.MODIFY, UserType.class);
//     // raw operation, no target
////        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);
//
//    }
//
//	
//	@Test
//    public void test210AddUserMorganWithAssignment() throws Exception {
//        TestUtil.displayTestTile(this, "test210AddUserMorganWithAssignment");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + ".test210AddUserMorganWithAssignment");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-morgan-assignment-dummy.xml"));
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//        
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        
//		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
//        UserType userMorganType = userMorgan.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
//        String groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "morgan");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "morgan", "Sir Henry Morgan");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//        
//        // Check account in dummy resource
//        assertDummyAccount("morgan", "Sir Henry Morgan", true);
//        
//        assertDummyScriptsAdd(userMorgan, accountModel, resourceDummyType);
//        
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_MORGAN_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);
//
//    }
//	
//	@Test
//    public void test212RenameUserMorgan() throws Exception {
//		final String TEST_NAME = "test212RenameUserMorgan";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//        prepareNotifications();
//        
//                
//		// WHEN
//        modifyUserReplace(USER_MORGAN_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("sirhenry"));
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
//        UserType userMorganType = userMorgan.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
//        String groupOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(groupOid));
//        
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, groupOid, "sirhenry");
//        
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, groupOid, "sirhenry", "Sir Henry Morgan");
//        
//        // Check account in dummy resource
//        assertDummyAccount("sirhenry", "Sir Henry Morgan", true);
//        
//        assertDummyScriptsModify(userMorgan);
//        
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_MORGAN_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);
//
//    }
//	
//	/**
//	 * This basically tests for correct auditing.
//	 */
//	@Test
//    public void test240AddUserCharlesRaw() throws Exception {
//		final String TEST_NAME = "test240AddUserCharlesRaw";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//
//        PrismObject<UserType> user = createUser("charles", "Charles L. Charles");
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);                
//		
//		// WHEN
//		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//        PrismObject<UserType> userAfter = findUserByUsername("charles");
//        assertNotNull("No charles", userAfter);
//        userCharlesOid = userAfter.getOid();
//        
//        assertNoProvisioningScripts();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, UserType.class);
//     // raw operation, no target
////        dummyAuditService.assertTarget(userAfter.getOid());
//        dummyAuditService.assertExecutionSuccess();
//	}
//	
//	/**
//	 * This basically tests for correct auditing.
//	 */
//	@Test
//    public void test241DeleteUserCharlesRaw() throws Exception {
//		final String TEST_NAME = "test241DeleteUserCharlesRaw";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        dummyAuditService.clear();
//        purgeScriptHistory();
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, userCharlesOid, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);                
//		
//		// WHEN
//		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
//		
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        
//        PrismObject<UserType> userAfter = findUserByUsername("charles");
//        assertNull("Charles is not gone", userAfter);
//        
//		assertNoProvisioningScripts();
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, UserType.class);
//     // raw operation, no target
////        dummyAuditService.assertTarget(userCharlesOid);
//        dummyAuditService.assertExecutionSuccess();
//	}
//	
//	private void assertMessageContains(String message, String string) {
//		assert message.contains(string) : "Expected message to contain '"+string+"' but it does not; message: " + message;
//	}
//	
//	private void purgeScriptHistory() {
//		dummyResource.purgeScriptHistory();
//	}
//
//	private void assertNoProvisioningScripts() {
//		if (!dummyResource.getScriptHistory().isEmpty()) {
//			IntegrationTestTools.displayScripts(dummyResource.getScriptHistory());
//			AssertJUnit.fail(dummyResource.getScriptHistory().size()+" provisioning scripts were executed while not expected any");
//		}
//	}
//
//	private void assertDummyScriptsAdd(PrismObject<UserType> user, PrismObject<? extends ShadowType> account, ResourceType resource) {
//		ProvisioningScriptSpec script = new ProvisioningScriptSpec("\nto spiral :size\n" +
//				"   if  :size > 30 [stop]\n   fd :size rt 15\n   spiral :size *1.02\nend\n			");
//		
//		String userName = null;
//		if (user != null) {
//			userName = user.asObjectable().getName().getOrig();
//		}
//		script.addArgSingle("usr", "user: "+userName);
//		
//		// Note: We cannot test for account name as name is only assigned in provisioning
//		String accountEnabled = null;
//		if (account != null && account.asObjectable().getActivation() != null 
//				&& account.asObjectable().getActivation().getAdministrativeStatus() != null) {
//			accountEnabled = account.asObjectable().getActivation().getAdministrativeStatus().toString();
//		}
//		script.addArgSingle("acc", "account: "+accountEnabled);
//
//		String resourceName = null;
//		if (resource != null) {
//			resourceName = resource.getName().getOrig();
//		}
//		script.addArgSingle("res", "resource: "+resourceName);
//
//		script.addArgSingle("size", "3");
//		script.setLanguage("Logo");
//		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), script);
//	}
//
//	private void assertDummyScriptsModify(PrismObject<UserType> user) {
//		assertDummyScriptsModify(user, false);
//	}
//	
//	private void assertDummyScriptsModify(PrismObject<UserType> user, boolean recon) {
//		ProvisioningScriptSpec modScript = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
//		String name = null;
//		String fullName = null;
//		String costCenter = null;
//		if (user != null) {
//			name = user.asObjectable().getName().getOrig();
//			fullName = user.asObjectable().getFullName().getOrig();
//			costCenter = user.asObjectable().getCostCenter();
//		}
//		modScript.addArgSingle("howMuch", costCenter);
//		modScript.addArgSingle("howLong", "from here to there");
//		modScript.addArgSingle("who", name);
//		modScript.addArgSingle("whatchacallit", fullName);
//		if (recon) {
//			ProvisioningScriptSpec reconBeforeScript = new ProvisioningScriptSpec("The vorpal blade went snicker-snack!");
//			reconBeforeScript.addArgSingle("who", name);
//			ProvisioningScriptSpec reconAfterScript = new ProvisioningScriptSpec("He left it dead, and with its head");
//			reconAfterScript.addArgSingle("how", "enabled");
//			IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), reconBeforeScript, modScript, reconAfterScript);
//		} else {
//			IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), modScript);
//		}
//	}
//
//	private void assertDummyScriptsDelete() {
//		ProvisioningScriptSpec script = new ProvisioningScriptSpec("The Jabberwock, with eyes of flame");
//		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), script);
//	}
//	
//	private void assertDummyScriptsNone() {
//		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory());
//	}

}
